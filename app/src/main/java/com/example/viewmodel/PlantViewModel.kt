package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ScanHistoryRepository
import com.example.model.firestore.ScanHistoryDocument
import com.example.model.Plant
import com.example.model.PlantData
import com.example.model.PlantMatch
import com.example.network.GeminiPlantRepository
import com.example.network.GeminiResult
import com.example.network.PlantRepository
import com.example.network.ResultState
import com.example.network.plantnet.PlantCandidate
import com.example.network.plantnet.PlantNetRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer
import kotlin.random.Random

/**
 * État réactif de l'écran d'identification de plante (Scan Pl@ntNet).
 */
sealed class ScanState {
    object Idle : ScanState()
    data class Scanning(val message: String = "Analyse de la photo en cours...") : ScanState()
    data class MultipleCandidates(val candidates: List<PlantCandidate>) : ScanState()
    data class Success(val candidate: PlantCandidate) : ScanState()
    data class Error(val message: String) : ScanState()
}

/**
 * Représente les 5 états UI distincts pour la recherche de conseils par symptôme.
 */
sealed class AdviceState {
    object Idle : AdviceState()
    object Loading : AdviceState()
    data class Success(
        val query: String,
        val matches: List<PlantMatch>,
        val matchedSymptoms: List<com.example.model.firestore.SymptomDocument> = emptyList()
    ) : AdviceState()
    data class NoResults(
        val query: String,
        val suggestedSymptoms: List<com.example.model.firestore.SymptomDocument> = emptyList()
    ) : AdviceState()
    data class Error(
        val message: String
    ) : AdviceState()
}

/**
 * État de l'écran de demande de conseil (Recherche par symptôme).
 */
data class AdviceUiState(
    val query: String = "",
    val state: AdviceState = AdviceState.Idle
) {
    val isSearching: Boolean get() = state is AdviceState.Loading
    val results: List<PlantMatch> get() = (state as? AdviceState.Success)?.matches ?: emptyList()
    val searched: Boolean get() = state is AdviceState.Success || state is AdviceState.NoResults
}

/**
 * ViewModel central gérant l'identification via l'API Pl@ntNet REST et le croisement Firestore/Room.
 */
class PlantViewModel(
    private val plantNetRepository: PlantNetRepository = PlantNetRepository(),
    private val geminiRepository: GeminiPlantRepository = GeminiPlantRepository(),
    private val plantRepository: PlantRepository? = null,
    private val scanHistoryRepository: ScanHistoryRepository = ScanHistoryRepository()
) : ViewModel() {

    // État du scan de plante
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // État de la recherche de conseil
    private val _adviceState = MutableStateFlow(AdviceUiState())
    val adviceState: StateFlow<AdviceUiState> = _adviceState.asStateFlow()

    // Plante sélectionnée pour l'écran de détail
    private val _selectedPlant = MutableStateFlow<Plant?>(null)
    val selectedPlant: StateFlow<Plant?> = _selectedPlant.asStateFlow()

    /**
     * Enregistre automatiquement le résultat du scan dans Firestore si l'utilisateur est authentifié.
     * Conforme au RGPD : pas de stockage local alternatif pour les invités.
     */
    private fun saveScanToHistoryIfAuthenticated(candidate: PlantCandidate) {
        val currentUid = scanHistoryRepository.getCurrentUserUid() ?: return
        val plantInDb = candidate.matchedPlantInDb

        val historyDoc = ScanHistoryDocument(
            uid = currentUid,
            plantId = plantInDb?.id,
            nomIdentifie = candidate.scientificName,
            commonName = candidate.commonName.ifBlank { plantInDb?.name ?: candidate.scientificName },
            scoreConfiance = candidate.confidencePercent,
            dateScan = System.currentTimeMillis(),
            imageThumbnailUrl = candidate.referenceImageUrl ?: plantInDb?.imageUrl,
            verificationStatus = plantInDb?.verificationStatus?.name,
            scientificEvidenceLevel = plantInDb?.scientificEvidenceLevel?.name
        )

        viewModelScope.launch {
            scanHistoryRepository.saveScan(historyDoc)
        }
    }

    /**
     * Analyse une photo avec l'API Pl@ntNet et croise le nom scientifique avec la BDD Firestore/Room.
     */
    fun analyzePlantBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning("Envoi de la photo à l'API Pl@ntNet...")

            val result = plantNetRepository.identifyPlantFromBitmap(bitmap)
            when (result) {
                is ResultState.Success -> {
                    _scanState.value = ScanState.Scanning("Croisement des résultats avec la BDD Firestore...")
                    val rawCandidates = result.data

                    val enrichedCandidates = rawCandidates.map { candidate ->
                        val matchedDoc = if (plantRepository != null) {
                            val res = plantRepository.getPlantByScientificName(candidate.scientificName)
                            if (res is ResultState.Success) res.data else null
                        } else null

                        val plantInDb = matchedDoc?.toPlant() ?: PlantData.mockPlants.firstOrNull { mock ->
                            val normMock = mock.scientificName.lowercase().trim()
                            val normCand = candidate.scientificName.lowercase().trim()
                            normMock == normCand || (normMock.length > 3 && normCand.contains(normMock)) || (normCand.length > 3 && normMock.contains(normCand))
                        }

                        candidate.copy(matchedPlantInDb = plantInDb)
                    }

                    if (enrichedCandidates.isEmpty()) {
                        _scanState.value = ScanState.Error("Aucune espèce n'a pu être identifiée avec un score de certitude suffisant (>= 20%).")
                    } else if (enrichedCandidates.size > 1 && enrichedCandidates[1].confidencePercent >= 15) {
                        // Proposer le choix entre 2 à 3 candidates si les scores sont proches
                        _scanState.value = ScanState.MultipleCandidates(enrichedCandidates.take(3))
                    } else {
                        // Afficher directement le premier résultat et sauvegarder dans l'historique si connecté
                        val selected = enrichedCandidates.first()
                        _scanState.value = ScanState.Success(selected)
                        saveScanToHistoryIfAuthenticated(selected)
                    }
                }
                is ResultState.Error -> {
                    _scanState.value = ScanState.Error(result.message)
                }
                is ResultState.Loading -> {
                    _scanState.value = ScanState.Scanning("Analyse taxonomique Pl@ntNet...")
                }
            }
        }
    }

    /**
     * Sélectionne un candidat confirmé par l'utilisateur parmi la liste de suggestions Pl@ntNet.
     */
    fun selectCandidate(candidate: PlantCandidate) {
        _scanState.value = ScanState.Success(candidate)
        saveScanToHistoryIfAuthenticated(candidate)
    }

    /**
     * Charge une photo sélectionnée dans la galerie via son Uri et lance l'analyse Pl@ntNet.
     */
    fun analyzePlantUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning("Chargement et préparation de l'image...")
            val bitmap = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (_: Exception) {
                null
            }

            if (bitmap != null) {
                analyzePlantBitmap(bitmap)
            } else {
                _scanState.value = ScanState.Error("Impossible de lire la photo sélectionnée dans la galerie.")
            }
        }
    }

    /**
     * Simule un scan complet Pl@ntNet avec croisement Firestore/Room.
     * Permet de tester le flux multi-candidats, l'espèce trouvée en BDD et l'espèce sans fiche d'usages.
     */
    fun startScanSimulation() {
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning("Envoi de l'image à l'API Pl@ntNet (Mode Démo)...")
            delay(700)
            _scanState.value = ScanState.Scanning("Analyse de la taxonomie et calcul des certitudes...")
            delay(700)
            _scanState.value = ScanState.Scanning("Croisement avec la base de données Firestore 'plants'...")
            delay(600)

            val plantInDb1 = PlantData.getPlantById("camomille")
            val plantInDb2 = PlantData.getPlantById("sauge")

            val candidates = listOf(
                PlantCandidate(
                    scientificName = "Matricaria chamomilla",
                    fullScientificName = "Matricaria chamomilla L.",
                    commonName = "Camomille sauvage",
                    familyName = "Asteraceae",
                    confidencePercent = 92,
                    referenceImageUrl = "https://images.unsplash.com/photo-1588880331179-bc9b93a8cb5e?w=500",
                    matchedPlantInDb = plantInDb1
                ),
                PlantCandidate(
                    scientificName = "Salvia officinalis",
                    fullScientificName = "Salvia officinalis L.",
                    commonName = "Sauge officinale",
                    familyName = "Lamiaceae",
                    confidencePercent = 86,
                    referenceImageUrl = "https://images.unsplash.com/photo-1606567595334-d39972c85dbe?w=500",
                    matchedPlantInDb = plantInDb2
                ),
                PlantCandidate(
                    scientificName = "Bellis perennis",
                    fullScientificName = "Bellis perennis L.",
                    commonName = "Pâquerette commune",
                    familyName = "Asteraceae",
                    confidencePercent = 74,
                    referenceImageUrl = "https://images.unsplash.com/photo-1533038590840-1cde6e668a91?w=500",
                    matchedPlantInDb = null // Espèce non répertoriée dans la BDD médicinale
                )
            )

            _scanState.value = ScanState.MultipleCandidates(candidates)
        }
    }

    /**
     * Réinitialise l'état du scanner.
     */
    fun resetScan() {
        _scanState.value = ScanState.Idle
    }

    /**
     * Met à jour la recherche de conseil et filtre les plantes selon le symptôme saisi.
     */
    fun onQueryChanged(newQuery: String) {
        _adviceState.update { it.copy(query = newQuery) }
    }

    /**
     * Exécute le matching de plantes d'après la saisie de symptômes ou le tag sélectionné.
     */
    fun searchAdvice(symptomQuery: String = _adviceState.value.query) {
        val trimmed = symptomQuery.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            _adviceState.update { it.copy(query = trimmed, state = AdviceState.Loading) }

            if (plantRepository != null) {
                when (val repoResult = plantRepository.searchAdviceBySymptom(trimmed)) {
                    is ResultState.Success -> {
                        val searchData = repoResult.data
                        if (searchData.plants.isNotEmpty()) {
                            val matches = searchData.plants.map { doc ->
                                val plant = doc.toPlant()
                                val matchingSymptom = searchData.matchedSymptoms.firstOrNull { sym ->
                                    plant.matchedKeywords.contains(sym.id) || plant.ailmentsAndBenefits.any { normalizeString(it).contains(normalizeString(sym.name)) }
                                }?.name ?: trimmed
                                PlantMatch(
                                    plant = plant,
                                    matchReason = "Usage traditionnel associé à : $matchingSymptom"
                                )
                            }
                            _adviceState.update {
                                it.copy(state = AdviceState.Success(query = trimmed, matches = matches, matchedSymptoms = searchData.matchedSymptoms))
                            }
                        } else {
                            _adviceState.update {
                                it.copy(state = AdviceState.NoResults(query = trimmed, suggestedSymptoms = searchData.suggestedSymptoms))
                            }
                        }
                    }
                    is ResultState.Error -> {
                        _adviceState.update {
                            it.copy(state = AdviceState.Error(repoResult.message))
                        }
                    }
                    is ResultState.Loading -> {
                        _adviceState.update { it.copy(state = AdviceState.Loading) }
                    }
                }
            } else {
                // Fallback direct sur mock local si le repository n'est pas injecté
                delay(300)
                val normalizedQuery = normalizeString(trimmed)
                val matchedList = mutableListOf<PlantMatch>()

                PlantData.mockPlants.forEach { plant ->
                    val matchesKeyword = plant.matchedKeywords.any { kw ->
                        normalizeString(kw).contains(normalizedQuery) || normalizedQuery.contains(normalizeString(kw))
                    }
                    val matchesBenefit = plant.ailmentsAndBenefits.any { benefit ->
                        normalizeString(benefit).contains(normalizedQuery)
                    }

                    if (matchesKeyword || matchesBenefit) {
                        val matchingBenefit = plant.ailmentsAndBenefits.firstOrNull {
                            normalizeString(it).contains(normalizedQuery)
                        } ?: plant.shortDescription

                        matchedList.add(
                            PlantMatch(
                                plant = plant,
                                matchReason = "Usage traditionnel associé à : $matchingBenefit"
                            )
                        )
                    }
                }

                if (matchedList.isNotEmpty()) {
                    val sortedMatches = matchedList.sortedWith(
                        compareByDescending<PlantMatch> {
                            when (it.plant.verificationStatus) {
                                com.example.model.firestore.VerificationStatus.VERIFIED_BY_PROFESSIONAL -> 2
                                com.example.model.firestore.VerificationStatus.UNDER_REVIEW -> 1
                                else -> 0
                            }
                        }.thenByDescending {
                            when (it.plant.scientificEvidenceLevel) {
                                com.example.model.firestore.ScientificEvidenceLevel.CLINICALLY_STUDIED -> 1
                                com.example.model.firestore.ScientificEvidenceLevel.TRADITIONAL_USE -> 0
                            }
                        }
                    )
                    _adviceState.update {
                        it.copy(state = AdviceState.Success(query = trimmed, matches = sortedMatches))
                    }
                } else {
                    val fallbackSuggestions = listOf(
                        com.example.model.firestore.SymptomDocument(id = "mal_de_gorge", name = "Mal de gorge", category = "ORL & Respiratoire"),
                        com.example.model.firestore.SymptomDocument(id = "troubles_du_sommeil", name = "Troubles du sommeil", category = "Système Nerveux"),
                        com.example.model.firestore.SymptomDocument(id = "stress_anxiete", name = "Stress & Anxiété", category = "Système Nerveux"),
                        com.example.model.firestore.SymptomDocument(id = "digestion_difficile", name = "Digestion difficile", category = "Système Digestif")
                    )
                    _adviceState.update {
                        it.copy(state = AdviceState.NoResults(query = trimmed, suggestedSymptoms = fallbackSuggestions))
                    }
                }
            }
        }
    }

    /**
     * Sélectionne une plante par son identifiant pour afficher le détail.
     */
    fun selectPlant(plantId: String) {
        _selectedPlant.value = PlantData.getPlantById(plantId)
    }

    /**
     * Sélectionne directement une plante spécifique.
     */
    fun selectPlant(plant: Plant) {
        _selectedPlant.value = plant
    }

    private fun normalizeString(text: String): String {
        val regex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
        val temp = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        return regex.replace(temp, "")
    }
}
