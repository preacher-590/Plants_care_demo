package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.firestore.PlantDocument
import com.google.firebase.Timestamp

/**
 * Entité Room (SQLite) représentant une fiche plante en cache local.
 */
@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey
    val id: String,
    val commonName: String,
    val scientificName: String,
    val family: String,
    val description: String,
    val traditionalUses: List<String>,
    val contraindications: List<String>,
    val drugInteractions: List<String>,
    val scientificEvidenceLevelRaw: String,
    val verificationStatusRaw: String,
    val lastUpdatedEpochSeconds: Long,
    val sources: List<String>,
    val symptomIds: List<String>,
    val imageUrl: String = "",
    val imageAuthor: String = "",
    val imageLicense: String = ""
) {
    /**
     * Convertit l'entité Room en document Firestore pour la couche métier/UI.
     */
    fun toDocument(): PlantDocument = PlantDocument(
        id = id,
        commonName = commonName,
        scientificName = scientificName,
        family = family,
        description = description,
        traditionalUses = traditionalUses,
        contraindications = contraindications,
        drugInteractions = drugInteractions,
        scientificEvidenceLevelRaw = scientificEvidenceLevelRaw,
        verificationStatusRaw = verificationStatusRaw,
        lastUpdated = if (lastUpdatedEpochSeconds > 0) Timestamp(lastUpdatedEpochSeconds, 0) else null,
        sources = sources,
        symptomIds = symptomIds,
        imageUrl = imageUrl,
        imageAuthor = imageAuthor,
        imageLicense = imageLicense
    )

    companion object {
        /**
         * Construit une entité Room à partir d'un document Firestore.
         */
        fun fromDocument(doc: PlantDocument): PlantEntity = PlantEntity(
            id = doc.id,
            commonName = doc.commonName,
            scientificName = doc.scientificName,
            family = doc.family,
            description = doc.description,
            traditionalUses = doc.traditionalUses,
            contraindications = doc.contraindications,
            drugInteractions = doc.drugInteractions,
            scientificEvidenceLevelRaw = doc.scientificEvidenceLevelRaw,
            verificationStatusRaw = doc.verificationStatusRaw,
            lastUpdatedEpochSeconds = doc.lastUpdated?.seconds ?: 0L,
            sources = doc.sources,
            symptomIds = doc.symptomIds,
            imageUrl = doc.imageUrl,
            imageAuthor = doc.imageAuthor,
            imageLicense = doc.imageLicense
        )
    }
}
