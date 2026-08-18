package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.PlantDatabase
import com.example.model.firestore.LegalContentDocument
import com.example.network.LegalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel gérant l'affichage et l'édition d'administration des notices légales.
 */
class LegalViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PlantDatabase.getDatabase(application)
    private val repository = LegalRepository(legalContentDao = database.legalContentDao())

    val mentionsContent: StateFlow<LegalContentDocument> = repository
        .getLegalContentFlow(LegalContentDocument.DOC_MENTIONS)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LegalContentDocument(
                docId = LegalContentDocument.DOC_MENTIONS,
                content = LegalRepository.DEFAULT_MENTIONS
            )
        )

    val privacyContent: StateFlow<LegalContentDocument> = repository
        .getLegalContentFlow(LegalContentDocument.DOC_PRIVACY)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LegalContentDocument(
                docId = LegalContentDocument.DOC_PRIVACY,
                content = LegalRepository.DEFAULT_PRIVACY
            )
        )

    // État d'édition Admin
    private val _selectedDocId = MutableStateFlow(LegalContentDocument.DOC_MENTIONS)
    val selectedDocId: StateFlow<String> = _selectedDocId.asStateFlow()

    private val _editableText = MutableStateFlow("")
    val editableText: StateFlow<String> = _editableText.asStateFlow()

    private val _isPreviewMode = MutableStateFlow(false)
    val isPreviewMode: StateFlow<Boolean> = _isPreviewMode.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        // Synchronisation au démarrage
        syncRemoteContent()

        // Synchroniser le champ d'édition avec le document sélectionné
        viewModelScope.launch {
            _selectedDocId.collectLatest { docId ->
                if (docId == LegalContentDocument.DOC_PRIVACY) {
                    _editableText.value = privacyContent.value.content
                } else {
                    _editableText.value = mentionsContent.value.content
                }
            }
        }
    }

    fun syncRemoteContent() {
        viewModelScope.launch {
            repository.syncLegalContentFromRemote(LegalContentDocument.DOC_MENTIONS)
            repository.syncLegalContentFromRemote(LegalContentDocument.DOC_PRIVACY)
        }
    }

    fun selectDocument(docId: String) {
        _selectedDocId.value = docId
        _statusMessage.value = null
        if (docId == LegalContentDocument.DOC_PRIVACY) {
            _editableText.value = privacyContent.value.content
        } else {
            _editableText.value = mentionsContent.value.content
        }
    }

    fun onTextChange(newText: String) {
        _editableText.value = newText
    }

    fun togglePreview() {
        _isPreviewMode.value = !_isPreviewMode.value
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun saveContent(adminUid: String) {
        if (adminUid.isBlank()) {
            _statusMessage.value = "Erreur : Utilisateur non identifié."
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _statusMessage.value = null

            val currentDocId = _selectedDocId.value
            val textToSave = _editableText.value

            val result = repository.saveLegalContent(
                docId = currentDocId,
                newContent = textToSave,
                adminUid = adminUid
            )

            _isSaving.value = false

            if (result.isSuccess) {
                _statusMessage.value = "Document sauvegardé avec succès sur Firestore !"
            } else {
                val err = result.exceptionOrNull()?.message ?: "Erreur inconnue"
                _statusMessage.value = "Échec de sauvegarde : $err (vérifiez vos droits d'administration)"
            }
        }
    }
}
