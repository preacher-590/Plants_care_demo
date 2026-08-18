package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.firestore.PlantDocument
import com.example.network.PlantRepository
import com.example.network.ResultState
import com.example.network.wikimedia.WikimediaImageCandidate
import com.example.network.wikimedia.WikimediaSearchResult
import com.example.network.wikimedia.WikimediaService
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class WikimediaSearchState {
    object Idle : WikimediaSearchState()
    object Loading : WikimediaSearchState()
    data class Success(val candidates: List<WikimediaImageCandidate>) : WikimediaSearchState()
    data class Error(val message: String) : WikimediaSearchState()
}

data class AdminImageUiState(
    val plants: List<PlantDocument> = emptyList(),
    val selectedPlant: PlantDocument? = null,
    val searchQuery: String = "",
    val searchState: WikimediaSearchState = WikimediaSearchState.Idle,
    val selectedCandidate: WikimediaImageCandidate? = null,
    val isSaving: Boolean = false,
    val statusMessage: String? = null
)

class AdminImageViewModel(
    private val plantRepository: PlantRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminImageUiState())
    val uiState: StateFlow<AdminImageUiState> = _uiState.asStateFlow()

    init {
        loadPlants()
    }

    fun loadPlants() {
        viewModelScope.launch {
            if (plantRepository != null) {
                plantRepository.getAllPlants().collect { result ->
                    if (result is ResultState.Success) {
                        _uiState.update { it.copy(plants = result.data) }
                    }
                }
            } else {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val snapshot = firestore.collection("plants").get().await()
                    val plants = snapshot.documents.mapNotNull { it.toObject(PlantDocument::class.java) }
                    if (plants.isNotEmpty()) {
                        _uiState.update { it.copy(plants = plants) }
                    } else {
                        val mockPlants = com.example.model.PlantData.mockPlants.map { mock ->
                            PlantDocument(
                                id = mock.id,
                                commonName = mock.name,
                                scientificName = mock.scientificName,
                                family = mock.category,
                                description = mock.fullDescription
                            )
                        }
                        _uiState.update { it.copy(plants = mockPlants) }
                    }
                } catch (e: Exception) {
                    val mockPlants = com.example.model.PlantData.mockPlants.map { mock ->
                        PlantDocument(
                            id = mock.id,
                            commonName = mock.name,
                            scientificName = mock.scientificName,
                            family = mock.category,
                            description = mock.fullDescription
                        )
                    }
                    _uiState.update { it.copy(plants = mockPlants) }
                }
            }
        }
    }

    fun selectPlant(plant: PlantDocument) {
        _uiState.update {
            it.copy(
                selectedPlant = plant,
                searchQuery = plant.scientificName.ifBlank { plant.commonName },
                selectedCandidate = null,
                searchState = WikimediaSearchState.Idle
            )
        }
        searchWikimediaImages(plant.scientificName.ifBlank { plant.commonName })
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun searchWikimediaImages(query: String = _uiState.value.searchQuery) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(searchState = WikimediaSearchState.Loading) }
            when (val result = WikimediaService.searchPlantImages(query)) {
                is WikimediaSearchResult.Success -> {
                    if (result.candidates.isNotEmpty()) {
                        _uiState.update { it.copy(searchState = WikimediaSearchState.Success(result.candidates)) }
                    } else {
                        val msg = if (result.excludedCount > 0) {
                            "Aucune image avec attribution complète (auteur et licence CC) trouvée pour '$query' (${result.excludedCount} fichier(s) exclu(s) car manquant d'auteur ou de licence)."
                        } else {
                            "Aucune image trouvée sur Wikimedia Commons pour '$query'."
                        }
                        _uiState.update { it.copy(searchState = WikimediaSearchState.Error(msg)) }
                    }
                }
                is WikimediaSearchResult.NetworkError -> {
                    _uiState.update { it.copy(searchState = WikimediaSearchState.Error("Erreur réseau : ${result.message}")) }
                }
                is WikimediaSearchResult.ParseError -> {
                    _uiState.update { it.copy(searchState = WikimediaSearchState.Error("Erreur d'analyse des données : ${result.message}")) }
                }
            }
        }
    }

    fun selectCandidate(candidate: WikimediaImageCandidate) {
        _uiState.update { it.copy(selectedCandidate = candidate) }
    }

    fun saveSelectedImage() {
        val selectedPlant = _uiState.value.selectedPlant ?: return
        val selectedCandidate = _uiState.value.selectedCandidate ?: return

        // Validation légale : interdiction de sauvegarder si l'auteur ou la licence est manquante ou vide
        if (selectedCandidate.thumbnailUrl.isBlank() || selectedCandidate.author.isBlank() || selectedCandidate.license.isBlank()) {
            _uiState.update {
                it.copy(
                    statusMessage = "Erreur : Impossible de sauvegarder une image sans attribution complète (l'auteur et la licence doivent être renseignés)."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, statusMessage = null) }
            
            try {
                if (plantRepository != null) {
                    val result = plantRepository.updatePlantImage(
                        plantId = selectedPlant.id,
                        imageUrl = selectedCandidate.thumbnailUrl,
                        imageAuthor = selectedCandidate.author,
                        imageLicense = selectedCandidate.license
                    )

                    when (result) {
                        is ResultState.Success -> handleSaveSuccess(selectedPlant, selectedCandidate)
                        is ResultState.Error -> handleSaveError(result.message)
                        is ResultState.Loading -> {}
                    }
                } else {
                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("plants")
                        .document(selectedPlant.id)
                        .update(
                            mapOf(
                                "imageUrl" to selectedCandidate.thumbnailUrl,
                                "imageAuthor" to selectedCandidate.author,
                                "imageLicense" to selectedCandidate.license
                            )
                        )
                        .await()
                    handleSaveSuccess(selectedPlant, selectedCandidate)
                }
            } catch (e: Exception) {
                handleSaveError(e.localizedMessage ?: "Erreur de sauvegarde.")
            }
        }
    }

    private fun handleSaveSuccess(selectedPlant: PlantDocument, selectedCandidate: WikimediaImageCandidate) {
        _uiState.update {
            it.copy(
                isSaving = false,
                statusMessage = "Image mise à jour avec succès pour '${selectedPlant.commonName}' !",
                selectedPlant = selectedPlant.copy(
                    imageUrl = selectedCandidate.thumbnailUrl,
                    imageAuthor = selectedCandidate.author,
                    imageLicense = selectedCandidate.license
                )
            )
        }
        loadPlants()
    }

    private fun handleSaveError(message: String) {
        _uiState.update {
            it.copy(
                isSaving = false,
                statusMessage = "Erreur lors de la sauvegarde : $message"
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
