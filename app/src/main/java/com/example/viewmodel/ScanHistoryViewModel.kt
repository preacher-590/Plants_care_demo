package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.AuthState
import com.example.data.ScanHistoryRepository
import com.example.model.firestore.ScanHistoryDocument
import com.example.network.ResultState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * États scellés pour l'écran d'historique des scans.
 */
sealed class ScanHistoryUiState {
    object Unauthenticated : ScanHistoryUiState()
    object Loading : ScanHistoryUiState()
    object Empty : ScanHistoryUiState()
    data class Success(val scans: List<ScanHistoryDocument>) : ScanHistoryUiState()
    data class Error(val message: String) : ScanHistoryUiState()
}

/**
 * ViewModel responsable de la gestion de l'affichage, de la suppression individuelle
 * et de la purge totale de l'historique de scan conformément au RGPD.
 */
class ScanHistoryViewModel(
    private val historyRepository: ScanHistoryRepository = ScanHistoryRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanHistoryUiState>(ScanHistoryUiState.Loading)
    val uiState: StateFlow<ScanHistoryUiState> = _uiState.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private var currentAuthenticatedUid: String? = null

    init {
        observeAuthState()
    }

    /**
     * Observe l'état d'authentification pour adapter dynamiquement l'état de l'historique.
     */
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authStateFlow.collectLatest { authState ->
                when (authState) {
                    is AuthState.Authenticated -> {
                        currentAuthenticatedUid = authState.uid
                        observeScanHistory(authState.uid)
                    }
                    is AuthState.NotAuthenticated -> {
                        currentAuthenticatedUid = null
                        _uiState.value = ScanHistoryUiState.Unauthenticated
                    }
                    is AuthState.Loading -> {
                        _uiState.value = ScanHistoryUiState.Loading
                    }
                    is AuthState.Error -> {
                        currentAuthenticatedUid = null
                        _uiState.value = ScanHistoryUiState.Unauthenticated
                    }
                }
            }
        }
    }

    /**
     * Écoute en continu la collection Firestore 'scanHistory' pour l'UID connecté.
     */
    private fun observeScanHistory(uid: String) {
        viewModelScope.launch {
            historyRepository.getScanHistoryFlow(uid).collectLatest { result ->
                when (result) {
                    is ResultState.Loading -> {
                        _uiState.value = ScanHistoryUiState.Loading
                    }
                    is ResultState.Success -> {
                        if (result.data.isEmpty()) {
                            _uiState.value = ScanHistoryUiState.Empty
                        } else {
                            _uiState.value = ScanHistoryUiState.Success(result.data)
                        }
                    }
                    is ResultState.Error -> {
                        _uiState.value = ScanHistoryUiState.Error(result.message)
                    }
                }
            }
        }
    }

    /**
     * Supprime une entrée individuelle de l'historique.
     */
    fun deleteScanEntry(docId: String) {
        val uid = currentAuthenticatedUid ?: return
        viewModelScope.launch {
            _isDeleting.value = true
            val result = historyRepository.deleteScanEntry(docId, uid)
            _isDeleting.value = false
            if (result.isSuccess) {
                _actionMessage.value = "Entrée de scan supprimée avec succès."
            } else {
                _actionMessage.value = "Erreur lors de la suppression du scan."
            }
        }
    }

    /**
     * Efface définitivement l'intégralité de l'historique de scan de l'utilisateur (droit à l'effacement RGPD).
     */
    fun clearAllHistory() {
        val uid = currentAuthenticatedUid ?: return
        viewModelScope.launch {
            _isDeleting.value = true
            val result = historyRepository.clearAllUserHistory(uid)
            _isDeleting.value = false
            if (result.isSuccess) {
                _actionMessage.value = "Historique complet effacé définitivement."
            } else {
                _actionMessage.value = "Erreur lors de l'effacement de l'historique."
            }
        }
    }

    /**
     * Réinitialise le message d'action (feedback Snackbar / Notification).
     */
    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
