package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) pour la gestion en cache local des plantes avec Room.
 */
@Dao
interface PlantDao {
    @Query("SELECT * FROM plants ORDER BY commonName ASC")
    fun getAllPlants(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants ORDER BY commonName ASC")
    suspend fun getAllPlantsList(): List<PlantEntity>

    @Query("SELECT * FROM plants WHERE id = :id")
    suspend fun getPlantById(id: String): PlantEntity?

    @Query("SELECT * FROM plants WHERE LOWER(scientificName) = LOWER(:scientificName) OR LOWER(scientificName) LIKE LOWER(:scientificName) || '%' LIMIT 1")
    suspend fun getPlantByScientificName(scientificName: String): PlantEntity?

    @Query("SELECT * FROM plants WHERE symptomIds LIKE '%' || :symptomId || '%'")
    suspend fun getPlantsBySymptomId(symptomId: String): List<PlantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlants(plants: List<PlantEntity>)

    @Query("DELETE FROM plants")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM plants")
    suspend fun getCount(): Int
}
