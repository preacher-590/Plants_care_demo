package com.example.model.firestore

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data class représentant un document dans la collection Firestore 'symptoms'.
 * Permet de catégoriser les affections et symptômes pour le filtrage et le matching.
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

    @get:PropertyName("parent_category_id")
    @set:PropertyName("parent_category_id")
    var parentCategoryId: String? = null,

    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String = ""
)
