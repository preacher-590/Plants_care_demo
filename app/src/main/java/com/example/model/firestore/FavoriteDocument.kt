package com.example.model.firestore

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

/**
 * Modèle de document pour la collection Firestore 'favorites'.
 * 
 * L'identifiant de document unique est structuré sous la forme "${uid}_${plantId}"
 * pour garantir au niveau du schéma qu'un couple (uid, plantId) est strictement unique.
 */
@IgnoreExtraProperties
data class FavoriteDocument(
    @DocumentId
    val id: String = "",

    @get:PropertyName("uid")
    @set:PropertyName("uid")
    var uid: String = "",

    @get:PropertyName("plantId")
    @set:PropertyName("plantId")
    var plantId: String = "",

    @get:PropertyName("dateAjout")
    @set:PropertyName("dateAjout")
    var dateAjout: Long = System.currentTimeMillis()
) {
    companion object {
        const val COLLECTION_NAME = "favorites"

        /**
         * Génère un identifiant déterministe garantissant l'unicité du couple (uid, plantId).
         */
        fun buildDocId(uid: String, plantId: String): String = "${uid}_${plantId}"
    }
}
