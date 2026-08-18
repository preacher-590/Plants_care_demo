package com.example.network.plantnet

import com.example.model.Plant
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO Moshi représentant la réponse complète renvoyée par l'API REST Pl@ntNet v2.
 */
@JsonClass(generateAdapter = true)
data class PlantNetResponse(
    @Json(name = "query") val query: PlantNetQuery? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "results") val results: List<PlantNetResult>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class PlantNetQuery(
    @Json(name = "project") val project: String? = null,
    @Json(name = "images") val images: List<String>? = emptyList(),
    @Json(name = "organs") val organs: List<String>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class PlantNetResult(
    @Json(name = "score") val score: Double = 0.0,
    @Json(name = "species") val species: PlantNetSpecies? = null,
    @Json(name = "images") val images: List<PlantNetImage>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class PlantNetSpecies(
    @Json(name = "scientificNameWithoutAuthor") val scientificNameWithoutAuthor: String? = null,
    @Json(name = "scientificNameAuthorship") val scientificNameAuthorship: String? = null,
    @Json(name = "scientificName") val scientificName: String? = null,
    @Json(name = "genus") val genus: PlantNetTaxon? = null,
    @Json(name = "family") val family: PlantNetTaxon? = null,
    @Json(name = "commonNames") val commonNames: List<String>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class PlantNetTaxon(
    @Json(name = "scientificNameWithoutAuthor") val scientificNameWithoutAuthor: String? = null,
    @Json(name = "scientificNameAuthorship") val scientificNameAuthorship: String? = null,
    @Json(name = "scientificName") val scientificName: String? = null
)

@JsonClass(generateAdapter = true)
data class PlantNetImage(
    @Json(name = "organ") val organ: String? = null,
    @Json(name = "url") val url: PlantNetImageUrl? = null
)

@JsonClass(generateAdapter = true)
data class PlantNetImageUrl(
    @Json(name = "o") val o: String? = null,
    @Json(name = "m") val m: String? = null,
    @Json(name = "s") val s: String? = null
)

/**
 * Modèle Domaine représentant une espèce candidate identifiée par Pl@ntNet,
 * croisée avec la fiche d'usages de la base de données Firestore.
 */
data class PlantCandidate(
    val scientificName: String,
    val fullScientificName: String,
    val commonName: String,
    val familyName: String,
    val confidencePercent: Int,
    val referenceImageUrl: String? = null,
    val matchedPlantInDb: Plant? = null
) {
    /**
     * Indique si l'espèce identifiée possède une fiche d'usages médicinaux dans la base Firestore.
     */
    val hasUsageSheetInDb: Boolean get() = matchedPlantInDb != null
}
