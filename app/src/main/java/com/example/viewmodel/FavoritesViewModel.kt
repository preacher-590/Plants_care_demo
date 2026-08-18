package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.AuthState
import com.example.data.FavoritesRepository
import com.example.data.FavoritesRepositoryImpl
import com.example.model.Plant
import com.example.model.PlantData
import com.example.model.firestore.FavoriteDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * État de l'UI pour l'écran "Mes Favoris".
 */
sealed interface FavoritesUiState {
    object Idle : FavoritesUiState
    object Loading : FavoritesUiState
    data class Success(val favoritePlants: List<PlantWithFavoriteMeta>) : FavoritesUiState
    object Empty : FavoritesUiState
    data class Error(val message: String) : FavoritesUiState
    object Unauthenticated : FavoritesUiState
}

/**
 * Modèle associant une fiche plante avec les métadonnées de favori (date d'ajout).
 */
data class PlantWithFavoriteMeta(
    val plant: Plant,
    val dateAjout: Long
)

/**
 * ViewModel centralisé pour la gestion des favoris et la cohérence inter-écrans.
 */
class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository = FavoritesRepositoryImpl(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    // Set réactif des IDs de plantes favorites pour synchronisation instantanée inter-écrans
    val favoritePlantIds: StateFlow<Set<String>> = favoritesRepository.observeFavoritePlantIds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    // État UI complet de l'écran des favoris
    val uiState: StateFlow<FavoritesUiState> = combine(
        authRepository.authStateFlow,
        favoritesRepository.observeFavorites()
    ) { authState: AuthState, favoriteDocs: List<FavoriteDocument> ->
        when (authState) {
            is AuthState.NotAuthenticated, is AuthState.Error -> FavoritesUiState.Unauthenticated
            is AuthState.Loading -> FavoritesUiState.Loading
            is AuthState.Authenticated -> {
                if (favoriteDocs.isEmpty()) {
                    FavoritesUiState.Empty
                } else {
                    val allPlants = PlantData.mockPlants
                    val plantMap = allPlants.associateBy { it.id }

                    val resolvedFavorites = favoriteDocs.mapNotNull { favDoc ->
                        val plant = plantMap[favDoc.plantId]
                        if (plant != null) {
                            PlantWithFavoriteMeta(plant = plant, dateAjout = favDoc.dateAjout)
                        } else null
                    }

                    if (resolvedFavorites.isEmpty()) {
                        FavoritesUiState.Empty
                    } else {
                        FavoritesUiState.Success(resolvedFavorites)
                    }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FavoritesUiState.Loading
    )

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    /**
     * Bascule le statut favori d'une plante avec gestion de l'échec et du retour d'erreur.
     */
    fun toggleFavorite(
        plantId: String,
        plantName: String,
        isCurrentlyFavorite: Boolean,
        onUnauthenticated: () -> Unit
    ) {
        val currentUid = favoritesRepository.currentUserId
        if (currentUid.isNullOrBlank()) {
            onUnauthenticated()
            return
        }

        viewModelScope.launch {
            val result = if (isCurrentlyFavorite) {
                favoritesRepository.removeFavorite(plantId)
            } else {
                favoritesRepository.addFavorite(plantId)
            }

            result.fold(
                onSuccess = {
                    _actionMessage.value = if (isCurrentlyFavorite) {
                        "\"$plantName\" retirée de vos favoris."
                    } else {
                        "\"$plantName\" ajoutée à vos favoris."
                    }
                },
                onFailure = { error ->
                    _actionMessage.value = "Erreur lors de la mise à jour des favoris : ${error.localizedMessage ?: "Problème réseau"}"
                }
            )
        }
    }
}
