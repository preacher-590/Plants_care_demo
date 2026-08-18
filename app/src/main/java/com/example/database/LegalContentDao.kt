package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Interface d'accès aux données Room pour la table 'legal_content'.
 */
@Dao
interface LegalContentDao {

    @Query("SELECT * FROM legal_content WHERE docId = :docId")
    fun getLegalContentFlow(docId: String): Flow<LegalContentEntity?>

    @Query("SELECT * FROM legal_content WHERE docId = :docId")
    suspend fun getLegalContent(docId: String): LegalContentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: LegalContentEntity)
}
