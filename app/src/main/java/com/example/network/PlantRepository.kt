package com.example.network

import com.example.data.SyncPreferencesRepository
import com.example.database.PlantDao
import com.example.database.PlantEntity
import com.example.database.SymptomDao
import com.example.database.SymptomEntity
import com.example.model.firestore.PlantDocument
import com.example.model.firestore.SymptomDocument
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException

/**
 * Résultat d'une recherche de conseil botanique par symptôme ou texte libre.
 */
data class SymptomSearchResult(
    val query: String,
    val matchedSymptoms: List<SymptomDocument> = emptyList(),
    val plants: List<PlantDocument> = emptyList(),
    val suggestedSymptoms: List<SymptomDocument> = emptyList()
)

/**
 * Interface du Repository définissant les accès aux données botaniques et symptômes
 * (avec architecture Offline-First & synchronisation Firestore).
 */
interface PlantRepository {
    /**
     * Flux réactif (Flow) renvoyant l'horodatage de la dernière synchronisation globale.
     */
    val lastSyncTimestamp: Flow<Long?>

    /**
     * Récupère la liste de toutes les plantes selon la stratégie Cache-First, Network-Refresh.
     */
    fun getAllPlants(): Flow<ResultState<List<PlantDocument>>>

    /**
     * Récupère une fiche plante par son ID (depuis Room ou bascule sur Firestore).
     */
    suspend fun getPlantById(plantId: String): ResultState<PlantDocument>

    /**
     * Recherche une fiche plante dans Firestore/Room par son nom scientifique.
     * Renvoie null dans Success si aucune fiche correspondante n'existe dans la base.
     */
    suspend fun getPlantByScientificName(scientificName: String): ResultState<PlantDocument?>

    /**
     * Recherche les plantes associées à un symptôme donné.
     */
    suspend fun getPlantsBySymptomId(symptomId: String): ResultState<List<PlantDocument>>

    /**
     * Recherche par mots-clés dans les symptômes et récupération des plantes associées.
     * Tri par nombre de correspondances puis statut de vérification. Propose des suggestions si aucun résultat.
     */
    suspend fun searchAdviceBySymptom(userQuery: String): ResultState<SymptomSearchResult>

    /**
     * Récupère la liste des catégories de symptômes.
     */
    fun getAllSymptoms(): Flow<ResultState<List<SymptomDocument>>>

    /**
     * Force une synchronisation explicite des données entre Firestore et la BDD Room local.
     */
    suspend fun refreshData(): ResultState<Unit>

    /**
     * Met à jour l'image d'une plante (URL et métadonnées d'attribution Creative Commons)
     * dans Firestore et synchronise le cache local Room. (Supervisé par l'administrateur).
     */
    suspend fun updatePlantImage(
        plantId: String,
        imageUrl: String,
        imageAuthor: String,
        imageLicense: String
    ): ResultState<Unit>
}

/**
 * Implémentation du repository combinant Room (SQLite local) et Firebase Firestore.
 */
class PlantRepositoryImpl(
    private val firestoreProvider: () -> FirebaseFirestore? = {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    },
    private val plantDao: PlantDao,
    private val symptomDao: SymptomDao,
    private val syncPreferences: SyncPreferencesRepository
) : PlantRepository {

    private val firestore: FirebaseFirestore? get() = firestoreProvider()

    companion object {
        private const val COLLECTION_PLANTS = "plants"
        private const val COLLECTION_SYMPTOMS = "symptoms"
        private const val FIELD_SYMPTOM_IDS = "symptom_ids"
        private const val FIELD_LAST_UPDATED = "last_updated"
    }

    override val lastSyncTimestamp: Flow<Long?> = syncPreferences.lastSyncTimestamp

    override fun getAllPlants(): Flow<ResultState<List<PlantDocument>>> = channelFlow {
        send(ResultState.Loading)

        // 1. Déclencher le rafraîchissement réseau dans le ProducerScope pour supporter l'annulation structurée
        launch(Dispatchers.IO) {
            try {
                refreshDataInternal()
            } catch (e: Exception) {
                // Si la BDD locale est vide et que le réseau échoue, transmettre l'erreur à l'UI
                val count = plantDao.getCount()
                if (count == 0) {
                    send(
                        ResultState.Error(
                            message = "Aucune donnée locale en cache et réseau indisponible. Veuillez vous connecter au réseau pour la première synchronisation.",
                            cause = e,
                            errorType = if (e is IOException) ErrorType.NETWORK_DISCONNECTED else ErrorType.FIREBASE_EXCEPTION
                        )
                    )
                }
            }
        }

        // 2. Émettre de manière fluide et réactive les plantes enregistrées en BDD locale Room
        plantDao.getAllPlants()
            .map { entities -> entities.map { it.toDocument() } }
            .collectLatest { plants ->
                if (plants.isNotEmpty()) {
                    send(ResultState.Success(plants))
                }
            }
    }

    override suspend fun getPlantById(plantId: String): ResultState<PlantDocument> {
        if (plantId.isBlank()) {
            return ResultState.Error("L'identifiant de la plante ne peut pas être vide.", errorType = ErrorType.UNKNOWN)
        }

        // 1. Consultation prioritaire du cache local Room
        val localEntity = plantDao.getPlantById(plantId)
        if (localEntity != null) {
            return ResultState.Success(localEntity.toDocument())
        }

        // 2. Bascule sur Firestore si la fiche n'est pas trouvée localement
        val db = firestore ?: return ResultState.Error("Firestore indisponible", errorType = ErrorType.NETWORK_DISCONNECTED)
        return try {
            val snapshot = db.collection(COLLECTION_PLANTS)
                .document(plantId)
                .get()
                .await()

            if (!snapshot.exists()) {
                ResultState.Error(
                    message = "Fiche plante introuvable pour l'identifiant : $plantId",
                    errorType = ErrorType.DOCUMENT_NOT_FOUND
                )
            } else {
                val plant = snapshot.toObject(PlantDocument::class.java)
                if (plant != null) {
                    plantDao.insertPlants(listOf(PlantEntity.fromDocument(plant)))
                    ResultState.Success(plant)
                } else {
                    ResultState.Error(
                        message = "Échec de désérialisation de la fiche plante.",
                        errorType = ErrorType.DESERIALIZATION
                    )
                }
            }
        } catch (e: FirebaseFirestoreException) {
            ResultState.Error(
                message = "Erreur Firestore lors de la récupération de la fiche plante.",
                cause = e,
                errorType = parseFirebaseError(e)
            )
        } catch (e: IOException) {
            ResultState.Error(
                message = "Connexion réseau indisponible et document non trouvé en cache.",
                cause = e,
                errorType = ErrorType.NETWORK_DISCONNECTED
            )
        } catch (e: Exception) {
            ResultState.Error(
                message = "Une erreur imprévue est survenue.",
                cause = e,
                errorType = ErrorType.UNKNOWN
            )
        }
    }

    override suspend fun getPlantByScientificName(scientificName: String): ResultState<PlantDocument?> {
        val trimmed = scientificName.trim()
        if (trimmed.isBlank()) {
            return ResultState.Success(null)
        }

        // Normalisation du nom scientifique (ex: "Matricaria chamomilla L." -> "matricaria chamomilla")
        val normalizedQuery = trimmed.lowercase().replace(Regex("\\b[a-z]\\.$"), "").trim()

        // 1. Consultation du cache local Room
        try {
            val localEntity = plantDao.getPlantByScientificName(normalizedQuery)
            if (localEntity != null) {
                return ResultState.Success(localEntity.toDocument())
            }
        } catch (_: Exception) {
            // Ignorer l'exception locale et passer à Firestore/Fallback
        }

        // 2. Consultation de Firestore
        val db = firestore
        if (db != null) {
            try {
                val snapshot = db.collection(COLLECTION_PLANTS)
                    .whereEqualTo("scientific_name", trimmed)
                    .get()
                    .await()

                val document = snapshot.documents.firstOrNull()?.toObject(PlantDocument::class.java)
                if (document != null) {
                    plantDao.insertPlants(listOf(PlantEntity.fromDocument(document)))
                    return ResultState.Success(document)
                }

                // Recherche avec correspondance tolérante dans les documents Firestore
                val allSnapshot = db.collection(COLLECTION_PLANTS).get().await()
                val match = allSnapshot.documents.mapNotNull { it.toObject(PlantDocument::class.java) }
                    .firstOrNull { doc ->
                        val docNorm = doc.scientificName.lowercase().replace(Regex("\\b[a-z]\\.$"), "").trim()
                        docNorm == normalizedQuery || (docNorm.length > 3 && normalizedQuery.contains(docNorm)) || (normalizedQuery.length > 3 && docNorm.contains(normalizedQuery))
                    }
                if (match != null) {
                    plantDao.insertPlants(listOf(PlantEntity.fromDocument(match)))
                    return ResultState.Success(match)
                }
            } catch (_: Exception) {
                // Basculer sur le fallback catalogue en cas d'erreur réseau
            }
        }

        // 3. Fallback sur le catalogue statique local si disponible
        val mockMatch = com.example.model.PlantData.mockPlants.firstOrNull { mock ->
            val mockNorm = mock.scientificName.lowercase().replace(Regex("\\b[a-z]\\.$"), "").trim()
            mockNorm == normalizedQuery || (mockNorm.length > 3 && normalizedQuery.contains(mockNorm)) || (normalizedQuery.length > 3 && mockNorm.contains(normalizedQuery))
        }

        if (mockMatch != null) {
            val doc = PlantDocument(
                id = mockMatch.id,
                commonName = mockMatch.name,
                scientificName = mockMatch.scientificName,
                family = mockMatch.category,
                description = mockMatch.fullDescription,
                traditionalUses = mockMatch.ailmentsAndBenefits,
                contraindications = mockMatch.contraindications,
                drugInteractions = mockMatch.drugInteractions,
                sources = mockMatch.sources,
                symptomIds = mockMatch.matchedKeywords
            )
            return ResultState.Success(doc)
        }

        return ResultState.Success(null)
    }

    override suspend fun getPlantsBySymptomId(symptomId: String): ResultState<List<PlantDocument>> {
        if (symptomId.isBlank()) {
            return ResultState.Success(emptyList())
        }

        // 1. Recherche prioritaire dans le cache Room
        val localEntities = plantDao.getPlantsBySymptomId(symptomId)
        if (localEntities.isNotEmpty()) {
            return ResultState.Success(localEntities.map { it.toDocument() })
        }

        // 2. Bascule réseau si pas de résultat local
        val dbSymptom = firestore ?: return ResultState.Error("Firestore indisponible", errorType = ErrorType.NETWORK_DISCONNECTED)
        return try {
            val snapshot = dbSymptom.collection(COLLECTION_PLANTS)
                .whereArrayContains(FIELD_SYMPTOM_IDS, symptomId)
                .get()
                .await()

            val plants = snapshot.documents.mapNotNull { doc ->
                doc.toObject(PlantDocument::class.java)
            }
            if (plants.isNotEmpty()) {
                plantDao.insertPlants(plants.map { PlantEntity.fromDocument(it) })
            }
            ResultState.Success(plants)
        } catch (e: FirebaseFirestoreException) {
            ResultState.Error(
                message = "Erreur Firestore lors du filtrage par symptôme.",
                cause = e,
                errorType = parseFirebaseError(e)
            )
        } catch (e: IOException) {
            ResultState.Error(
                message = "Connexion réseau requise pour rechercher de nouveaux symptômes non mis en cache.",
                cause = e,
                errorType = ErrorType.NETWORK_DISCONNECTED
            )
        } catch (e: Exception) {
            ResultState.Error(
                message = "Erreur lors de la désérialisation du résultat de recherche.",
                cause = e,
                errorType = ErrorType.DESERIALIZATION
            )
        }
    }

    override suspend fun searchAdviceBySymptom(userQuery: String): ResultState<SymptomSearchResult> {
        val trimmed = userQuery.trim()
        if (trimmed.isBlank()) {
            return ResultState.Success(SymptomSearchResult(query = userQuery))
        }

        return try {
            val normalizedQuery = normalizeText(trimmed)
            val tokens = normalizedQuery.split("\\s+".toRegex()).filter { it.length >= 2 }

            // 1. Récupération des symptômes (Cache Room en priorité, puis Firestore si vide)
            var allSymptoms = symptomDao.getAllSymptomsList().map { it.toDocument() }
            if (allSymptoms.isEmpty()) {
                val db = firestore
                if (db != null) {
                    val symptomSnap = db.collection(COLLECTION_SYMPTOMS).get().await()
                    allSymptoms = symptomSnap.documents.mapNotNull { it.toObject(SymptomDocument::class.java) }
                    if (allSymptoms.isNotEmpty()) {
                        symptomDao.insertSymptoms(allSymptoms.map { SymptomEntity.fromDocument(it) })
                    }
                }
            }

            // 2. Recherche par correspondance de mots-clés sur la collection symptoms
            val matchedSymptoms = allSymptoms.filter { symptom ->
                val normName = normalizeText(symptom.name)
                val normCategory = normalizeText(symptom.category)
                val normDesc = normalizeText(symptom.description)

                tokens.any { token ->
                    normName.contains(token) || normCategory.contains(token) || normDesc.contains(token)
                } || normName.contains(normalizedQuery) || normalizedQuery.contains(normName)
            }

            if (matchedSymptoms.isNotEmpty()) {
                val matchedSymptomIds = matchedSymptoms.map { it.id }.toSet()

                // Récupération des plantes (Cache Room en priorité)
                var allPlants = plantDao.getAllPlantsList().map { it.toDocument() }
                if (allPlants.isEmpty()) {
                    val db = firestore
                    if (db != null) {
                        val plantSnap = db.collection(COLLECTION_PLANTS).get().await()
                        allPlants = plantSnap.documents.mapNotNull { it.toObject(PlantDocument::class.java) }
                        if (allPlants.isNotEmpty()) {
                            plantDao.insertPlants(allPlants.map { PlantEntity.fromDocument(it) })
                        }
                    }
                }

                // Fallback catalogue statique si la BDD est encore vide
                if (allPlants.isEmpty()) {
                    allPlants = com.example.model.PlantData.mockPlants.map { mock ->
                        PlantDocument(
                            id = mock.id,
                            commonName = mock.name,
                            scientificName = mock.scientificName,
                            family = mock.category,
                            description = mock.fullDescription,
                            traditionalUses = mock.ailmentsAndBenefits,
                            contraindications = mock.contraindications,
                            drugInteractions = mock.drugInteractions,
                            verificationStatusRaw = when (mock.verificationStatus) {
                                com.example.model.firestore.VerificationStatus.VERIFIED_BY_PROFESSIONAL -> "verifie_par_professionnel"
                                com.example.model.firestore.VerificationStatus.UNDER_REVIEW -> "en_cours_de_revision"
                                else -> "non_verifie"
                            },
                            symptomIds = mock.matchedKeywords
                        )
                    }
                }

                // Filtrage Many-To-Many des plantes associées à ces symptômes
                val matchingPlants = allPlants.filter { plant ->
                    val plantSymptomMatch = plant.symptomIds.any { id -> matchedSymptomIds.contains(id) }
                    val keywordMatch = tokens.any { token ->
                        plant.traditionalUses.any { use -> normalizeText(use).contains(token) } ||
                                plant.symptomIds.any { id -> normalizeText(id).contains(token) }
                    }
                    plantSymptomMatch || keywordMatch
                }

                // Tri par pertinence :
                // 1. Nombre de correspondances de symptômes et de mots-clés
                // 2. Statut de vérification éditoriale (vérifié par pro > en cours de révision > non vérifié)
                // 3. Niveau de preuve scientifique (étudié cliniquement > usage traditionnel)
                val sortedPlants = matchingPlants.sortedWith(
                    compareByDescending<PlantDocument> { plant ->
                        val symptomOverlap = plant.symptomIds.count { matchedSymptomIds.contains(it) }
                        val keywordOverlap = tokens.count { token ->
                            plant.traditionalUses.any { normalizeText(it).contains(token) }
                        }
                        symptomOverlap + keywordOverlap
                    }.thenByDescending { plant ->
                        when (plant.verificationStatus) {
                            com.example.model.firestore.VerificationStatus.VERIFIED_BY_PROFESSIONAL -> 2
                            com.example.model.firestore.VerificationStatus.UNDER_REVIEW -> 1
                            else -> 0
                        }
                    }.thenByDescending { plant ->
                        when (plant.scientificEvidenceLevel) {
                            com.example.model.firestore.ScientificEvidenceLevel.CLINICALLY_STUDIED -> 1
                            com.example.model.firestore.ScientificEvidenceLevel.TRADITIONAL_USE -> 0
                        }
                    }
                )

                ResultState.Success(
                    SymptomSearchResult(
                        query = userQuery,
                        matchedSymptoms = matchedSymptoms,
                        plants = sortedPlants,
                        suggestedSymptoms = emptyList()
                    )
                )
            } else {
                // Si aucune correspondance exacte n'est trouvée : propose des symptômes suggérés proches
                val suggestedSymptoms = allSymptoms.filter { symptom ->
                    val normName = normalizeText(symptom.name)
                    tokens.any { token -> token.length >= 3 && (normName.startsWith(token.take(3)) || normName.contains(token)) }
                }.ifEmpty {
                    // Suggestions de secours si la BDD locale est vide
                    listOf(
                        SymptomDocument(id = "mal_de_gorge", name = "Mal de gorge", category = "ORL & Respiratoire"),
                        SymptomDocument(id = "troubles_du_sommeil", name = "Troubles du sommeil", category = "Système Nerveux"),
                        SymptomDocument(id = "stress_anxiete", name = "Stress & Anxiété", category = "Système Nerveux"),
                        SymptomDocument(id = "digestion_difficile", name = "Digestion difficile", category = "Système Digestif")
                    )
                }

                ResultState.Success(
                    SymptomSearchResult(
                        query = userQuery,
                        matchedSymptoms = emptyList(),
                        plants = emptyList(),
                        suggestedSymptoms = suggestedSymptoms.take(6)
                    )
                )
            }
        } catch (e: FirebaseFirestoreException) {
            ResultState.Error(
                message = "Erreur Firestore lors de la recherche par symptôme.",
                cause = e,
                errorType = parseFirebaseError(e)
            )
        } catch (e: IOException) {
            ResultState.Error(
                message = "Réseau indisponible. Veuillez vérifier votre connexion.",
                cause = e,
                errorType = ErrorType.NETWORK_DISCONNECTED
            )
        } catch (e: Exception) {
            ResultState.Error(
                message = "Une erreur est survenue lors du traitement de la recherche.",
                cause = e,
                errorType = ErrorType.UNKNOWN
            )
        }
    }

    private fun normalizeText(text: String): String {
        val regex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
        val temp = java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD)
        return regex.replace(temp, "").trim()
    }

    override fun getAllSymptoms(): Flow<ResultState<List<SymptomDocument>>> = channelFlow {
        send(ResultState.Loading)

        launch(Dispatchers.IO) {
            try {
                refreshSymptomsInternal()
            } catch (e: Exception) {
                val count = symptomDao.getCount()
                if (count == 0) {
                    send(
                        ResultState.Error(
                            message = "Aucun symptôme en cache et réseau indisponible.",
                            cause = e,
                            errorType = ErrorType.NETWORK_DISCONNECTED
                        )
                    )
                }
            }
        }

        symptomDao.getAllSymptoms()
            .map { entities -> entities.map { it.toDocument() } }
            .collectLatest { symptoms ->
                if (symptoms.isNotEmpty()) {
                    send(ResultState.Success(symptoms))
                }
            }
    }

    override suspend fun refreshData(): ResultState<Unit> {
        return try {
            refreshDataInternal()
            refreshSymptomsInternal()
            syncPreferences.updateLastSyncTimestamp(System.currentTimeMillis())
            ResultState.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            ResultState.Error("Erreur Firestore lors de la synchronisation", cause = e, errorType = parseFirebaseError(e))
        } catch (e: IOException) {
            ResultState.Error("Impossible d'effectuer la synchronisation sans connexion réseau", cause = e, errorType = ErrorType.NETWORK_DISCONNECTED)
        } catch (e: Exception) {
            ResultState.Error("Erreur imprévue lors de la synchronisation", cause = e, errorType = ErrorType.UNKNOWN)
        }
    }

    private suspend fun refreshDataInternal() {
        val db = firestore ?: return
        val lastSync = syncPreferences.lastSyncTimestamp.firstOrNull() ?: 0L

        var query: Query = db.collection(COLLECTION_PLANTS)
        if (lastSync > 0) {
            val lastTimestamp = Timestamp(lastSync / 1000, ((lastSync % 1000) * 1_000_000).toInt())
            query = query.whereGreaterThan(FIELD_LAST_UPDATED, lastTimestamp)
        }

        val snapshot = query.get().await()
        val plants = snapshot.documents.mapNotNull { doc ->
            doc.toObject(PlantDocument::class.java)
        }

        if (plants.isNotEmpty()) {
            plantDao.insertPlants(plants.map { PlantEntity.fromDocument(it) })
        }
        syncPreferences.updateLastSyncTimestamp(System.currentTimeMillis())
    }

    private suspend fun refreshSymptomsInternal() {
        val db = firestore ?: return
        val snapshot = db.collection(COLLECTION_SYMPTOMS).get().await()
        val symptoms = snapshot.documents.mapNotNull { doc ->
            doc.toObject(SymptomDocument::class.java)
        }
        if (symptoms.isNotEmpty()) {
            symptomDao.insertSymptoms(symptoms.map { SymptomEntity.fromDocument(it) })
        }
    }

    override suspend fun updatePlantImage(
        plantId: String,
        imageUrl: String,
        imageAuthor: String,
        imageLicense: String
    ): ResultState<Unit> {
        if (plantId.isBlank()) {
            return ResultState.Error("L'identifiant de la plante ne peut pas être vide.", errorType = ErrorType.UNKNOWN)
        }

        val db = firestore
        return try {
            if (db != null) {
                db.collection(COLLECTION_PLANTS)
                    .document(plantId)
                    .update(
                        mapOf(
                            "imageUrl" to imageUrl,
                            "imageAuthor" to imageAuthor,
                            "imageLicense" to imageLicense
                        )
                    )
                    .await()
            }

            // Mettre à jour le cache local Room
            val localEntity = plantDao.getPlantById(plantId)
            if (localEntity != null) {
                val updatedEntity = localEntity.copy(
                    imageUrl = imageUrl,
                    imageAuthor = imageAuthor,
                    imageLicense = imageLicense,
                    lastUpdatedEpochSeconds = System.currentTimeMillis() / 1000
                )
                plantDao.insertPlants(listOf(updatedEntity))
            }
            ResultState.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            ResultState.Error(
                message = "Erreur Firestore lors de la mise à jour de l'image.",
                cause = e,
                errorType = parseFirebaseError(e)
            )
        } catch (e: Exception) {
            ResultState.Error(
                message = "Échec de la mise à jour de l'image de la plante.",
                cause = e,
                errorType = ErrorType.UNKNOWN
            )
        }
    }

    private fun parseFirebaseError(error: FirebaseFirestoreException): ErrorType {
        return when (error.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE -> ErrorType.NETWORK_DISCONNECTED
            FirebaseFirestoreException.Code.NOT_FOUND -> ErrorType.DOCUMENT_NOT_FOUND
            else -> ErrorType.FIREBASE_EXCEPTION
        }
    }
}
