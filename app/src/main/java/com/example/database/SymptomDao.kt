package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) pour la gestion en cache local des symptômes avec Room.
 */
@Dao
interface SymptomDao {
    @Query("SELECT * FROM symptoms ORDER BY name ASC")
    fun getAllSymptoms(): Flow<List<SymptomEntity>>

    @Query("SELECT * FROM symptoms ORDER BY name ASC")
    suspend fun getAllSymptomsList(): List<SymptomEntity>

    @Query("SELECT * FROM symptoms WHERE LOWER(name) LIKE '%' || LOWER(:keyword) || '%' OR LOWER(category) LIKE '%' || LOWER(:keyword) || '%' OR LOWER(description) LIKE '%' || LOWER(:keyword) || '%'")
    suspend fun searchSymptomsByKeyword(keyword: String): List<SymptomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptoms(symptoms: List<SymptomEntity>)

    @Query("DELETE FROM symptoms")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM symptoms")
    suspend fun getCount(): Int
}
