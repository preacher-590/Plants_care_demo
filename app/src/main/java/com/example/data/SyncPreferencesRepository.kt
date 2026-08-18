package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_settings")

/**
 * Interface du Repository pour la gestion persistante des métadonnées de synchronisation.
 */
interface SyncPreferencesRepository {
    val lastSyncTimestamp: Flow<Long?>
    suspend fun updateLastSyncTimestamp(timestamp: Long)
}

/**
 * Implémentation concrète de SyncPreferencesRepository basée sur Jetpack DataStore Preferences.
 */
class SyncPreferencesRepositoryImpl(private val context: Context) : SyncPreferencesRepository {

    companion object {
        private val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }

    /**
     * Timestamp de la dernière synchronisation globale réussie (en millisecondes Epoch).
     */
    override val lastSyncTimestamp: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_LAST_SYNC_TIMESTAMP]
        }

    /**
     * Enregistre l'horodatage de la dernière synchronisation globale effectuée.
     */
    override suspend fun updateLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_SYNC_TIMESTAMP] = timestamp
        }
    }
}
