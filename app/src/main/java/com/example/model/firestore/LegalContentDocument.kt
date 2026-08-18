package com.example.model.firestore

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Document Firestore représentant le contenu d'une notice légale (Mentions Légales / Politique de Confidentialité).
 * Stocké dans la collection 'legalContent'.
 */
@IgnoreExtraProperties
data class LegalContentDocument(
    val docId: String = "",
    val content: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val updatedByUid: String = ""
) {
    companion object {
        const val DOC_MENTIONS = "mentionsLegales"
        const val DOC_PRIVACY = "politiqueConfidentialite"
    }
}
