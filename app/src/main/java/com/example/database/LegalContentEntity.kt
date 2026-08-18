package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room pour le stockage en cache local du contenu légal (fallback hors-ligne).
 */
@Entity(tableName = "legal_content")
data class LegalContentEntity(
    @PrimaryKey val docId: String,
    val content: String,
    val lastUpdated: Long,
    val updatedByUid: String
)
