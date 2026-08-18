package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.model.firestore.ScientificEvidenceLevel
import com.example.model.firestore.VerificationStatus

/**
 * Modèle de données représentant une plante dans l'application PlantCare.
 */
data class Plant(
    val id: String,
    val name: String,
    val scientificName: String,
    val category: String, // ex: "Plante Médicinale", "Herbe Aromatique", "Plante Succulente"
    val shortDescription: String,
    val fullDescription: String,
    val ailmentsAndBenefits: List<String>, // Maladies ou problèmes traités / bienfaits
    val careInstructions: CareInstructions, // Conseils d'entretien (Arrosage, Lumière, Température)
    val colorHex: Long, // Couleur d'illustration placeholder
    val matchedKeywords: List<String>, // Mots-clés pour le matching de conseils
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED_BY_PROFESSIONAL,
    val scientificEvidenceLevel: ScientificEvidenceLevel = ScientificEvidenceLevel.CLINICALLY_STUDIED,
    val contraindications: List<String> = emptyList(),
    val drugInteractions: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    val imageUrl: String = "",
    val imageAuthor: String = "",
    val imageLicense: String = ""
)

/**
 * Modèle représentant les conseils d'entretien pour une plante.
 */
data class CareInstructions(
    val watering: String,   // Fréquence d'arrosage
    val sunlight: String,   // Exposition au soleil
    val difficulty: String  // Niveau de difficulté (Facile, Modéré, Expert)
)

/**
 * Modèle de résultat lors d'une recherche de conseil par symptôme/problème.
 */
data class PlantMatch(
    val plant: Plant,
    val matchReason: String // Justification courte affichée à l'utilisateur
)
