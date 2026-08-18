package com.example.model.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Niveau de preuve scientifique associé à l'usage d'une plante.
 */
enum class ScientificEvidenceLevel(val labelFr: String) {
    @PropertyName("usage_traditionnel")
    TRADITIONAL_USE("Usage traditionnel"),

    @PropertyName("etudie_cliniquement")
    CLINICALLY_STUDIED("Étudié cliniquement");

    companion object {
        fun fromValue(value: String?): ScientificEvidenceLevel = when (value?.lowercase()?.trim()) {
            "clinically_studied", "etudie_cliniquement", "étudié cliniquement", "étudié" -> CLINICALLY_STUDIED
            else -> TRADITIONAL_USE
        }
    }
}

/**
 * Statut de validation médicale / scientifique de la fiche plante.
 */
enum class VerificationStatus(val labelFr: String) {
    @PropertyName("non_verifie")
    UNVERIFIED("Non vérifié"),

    @PropertyName("en_cours_de_revision")
    UNDER_REVIEW("En cours de révision"),

    @PropertyName("verifie_par_professionnel")
    VERIFIED_BY_PROFESSIONAL("Vérifié par un professionnel de santé");

    companion object {
        fun fromValue(value: String?): VerificationStatus = when (value?.lowercase()?.trim()) {
            "verifie_par_professionnel", "verified_by_professional", "vérifié par un professionnel" -> VERIFIED_BY_PROFESSIONAL
            "en_cours_de_revision", "under_review", "en cours de révision" -> UNDER_REVIEW
            else -> UNVERIFIED
        }
    }
}

/**
 * Data class représentant un document dans la collection Firestore 'plants'.
 * Permet une désérialisation automatique et sécurisée par le SDK Firebase Firestore.
 */
data class PlantDocument(
    @DocumentId
    val id: String = "",

    @get:PropertyName("common_name")
    @set:PropertyName("common_name")
    var commonName: String = "",

    @get:PropertyName("scientific_name")
    @set:PropertyName("scientific_name")
    var scientificName: String = "",

    @get:PropertyName("family")
    @set:PropertyName("family")
    var family: String = "",

    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String = "",

    @get:PropertyName("traditional_uses")
    @set:PropertyName("traditional_uses")
    var traditionalUses: List<String> = emptyList(),

    @get:PropertyName("contraindications")
    @set:PropertyName("contraindications")
    var contraindications: List<String> = emptyList(),

    @get:PropertyName("drug_interactions")
    @set:PropertyName("drug_interactions")
    var drugInteractions: List<String> = emptyList(),

    @get:PropertyName("scientific_evidence_level")
    @set:PropertyName("scientific_evidence_level")
    var scientificEvidenceLevelRaw: String = "usage_traditionnel",

    @get:PropertyName("verification_status")
    @set:PropertyName("verification_status")
    var verificationStatusRaw: String = "non_verifie",

    @get:PropertyName("last_updated")
    @set:PropertyName("last_updated")
    var lastUpdated: Timestamp? = null,

    @get:PropertyName("sources")
    @set:PropertyName("sources")
    var sources: List<String> = emptyList(),

    @get:PropertyName("symptom_ids")
    @set:PropertyName("symptom_ids")
    var symptomIds: List<String> = emptyList(),

    @get:PropertyName("imageUrl")
    @set:PropertyName("imageUrl")
    var imageUrl: String = "",

    @get:PropertyName("imageAuthor")
    @set:PropertyName("imageAuthor")
    var imageAuthor: String = "",

    @get:PropertyName("imageLicense")
    @set:PropertyName("imageLicense")
    var imageLicense: String = ""
) {
    val scientificEvidenceLevel: ScientificEvidenceLevel
        get() = ScientificEvidenceLevel.fromValue(scientificEvidenceLevelRaw)

    val verificationStatus: VerificationStatus
        get() = VerificationStatus.fromValue(verificationStatusRaw)

    fun toPlant(): com.example.model.Plant {
        return com.example.model.Plant(
            id = id.ifBlank { commonName.lowercase().replace(" ", "_") },
            name = commonName,
            scientificName = scientificName,
            category = if (family.isNotBlank()) "Famille des $family" else "Plante Médicinale",
            shortDescription = description.take(120).let { if (it.length >= 120) "$it..." else it },
            fullDescription = description,
            ailmentsAndBenefits = traditionalUses,
            careInstructions = com.example.model.CareInstructions(
                watering = "Modéré selon la saison",
                sunlight = "Lumière naturelle",
                difficulty = "Facile"
            ),
            colorHex = 0xFF3E8E5A,
            matchedKeywords = traditionalUses + symptomIds,
            verificationStatus = verificationStatus,
            scientificEvidenceLevel = scientificEvidenceLevel,
            contraindications = contraindications,
            drugInteractions = drugInteractions,
            sources = sources,
            imageUrl = imageUrl,
            imageAuthor = imageAuthor,
            imageLicense = imageLicense
        )
    }
}
