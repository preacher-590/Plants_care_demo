package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.SyncPreferencesRepository
import com.example.data.SyncPreferencesRepositoryImpl
import com.example.database.PlantDatabase
import com.example.network.PlantRepositoryImpl
import com.example.network.ResultState
import java.util.concurrent.TimeUnit

/**
 * Worker WorkManager exécutant la synchronisation périodique en arrière-plan
 * des fiches plantes et catégories de symptômes depuis Firestore vers le cache local Room.
 */
class PlantSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = PlantDatabase.getDatabase(applicationContext)
            val preferences: SyncPreferencesRepository = SyncPreferencesRepositoryImpl(applicationContext)
            val repository = PlantRepositoryImpl(
                plantDao = database.plantDao(),
                symptomDao = database.symptomDao(),
                syncPreferences = preferences
            )

            val result = repository.refreshData()
            if (result is ResultState.Success) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "PlantSyncWorker"

        /**
         * Planifie la tâche de synchronisation arrière-plan récurrente (une fois toutes les 24h, lorsque le réseau est actif).
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<PlantSyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
