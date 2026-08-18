package com.example.model.firestore

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Modèle de document Firestore pour la collection 'scanHistory'.
 * Représente l'historique d'identification de plante d'un utilisateur authentifié.
 * Donnée personnelle protégée par le RGPD (accès strictement restreint à son propriétaire).
 */
@Keep
data class ScanHistoryDocument(
    @DocumentId
    val id: String = "",

    @PropertyName("uid")
    val uid: String = "",

    @PropertyName("plantId")
    val plantId: String? = null,

    @PropertyName("nomIdentifie")
    val nomIdentifie: String = "",

    @PropertyName("commonName")
    val commonName: String = "",

    @PropertyName("scoreConfiance")
    val scoreConfiance: Int = 0,

    @PropertyName("dateScan")
    val dateScan: Long = System.currentTimeMillis(),

    @PropertyName("imageThumbnailUrl")
    val imageThumbnailUrl: String? = null,

    @PropertyName("verificationStatus")
    val verificationStatus: String? = null,

    @PropertyName("scientificEvidenceLevel")
    val scientificEvidenceLevel: String? = null
)
