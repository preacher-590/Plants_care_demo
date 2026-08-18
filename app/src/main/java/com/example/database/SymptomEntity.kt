package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.firestore.SymptomDocument

/**
 * Entité Room (SQLite) représentant une catégorie de symptôme en cache local.
 */
@Entity(tableName = "symptoms")
data class SymptomEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    val parentCategoryId: String?,
    val description: String
) {
    /**
     * Convertit l'entité Room en document Firestore pour la couche métier/UI.
     */
    fun toDocument(): SymptomDocument = SymptomDocument(
        id = id,
        name = name,
        category = category,
        parentCategoryId = parentCategoryId,
        description = description
    )

    companion object {
        /**
         * Construit une entité Room à partir d'un document Firestore.
         */
        fun fromDocument(doc: SymptomDocument): SymptomEntity = SymptomEntity(
            id = doc.id,
            name = doc.name,
            category = doc.category,
            parentCategoryId = doc.parentCategoryId,
            description = doc.description
        )
    }
}
