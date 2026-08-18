package com.example.model.firestore

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data class représentant un document dans la collection Firestore 'symptoms'.
 * Permet de catégoriser les affections et symptômes pour le filtrage et le matching.
 * Supporte le champ "category" ou "categorie" (ex : digestif, respiratoire, sommeil, peau, immunitaire, douleur, stress_anxiete).
 */
data class SymptomDocument(
    @DocumentId
    val id: String = "",

    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("category")
    @set:PropertyName("category")
    var category: String = "",

    @get:PropertyName("categorie")
    @set:PropertyName("categorie")
    var categorieAlias: String = "",

    @get:PropertyName("parent_category_id")
    @set:PropertyName("parent_category_id")
    var parentCategoryId: String? = null,

    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String = ""
) {
    /**
     * Retourne la catégorie normalisée de l'affection/symptôme.
     */
    val effectiveCategory: String
        get() = category.ifBlank { categorieAlias }.trim()
}
