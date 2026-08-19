package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Application principale de PlantCare.
 * Initialise FirebaseApp de manière sécurisée même en l'absence du fichier google-services.json.
 */
class PlantCareApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initFirebaseSafely()
    }

    private fun initFirebaseSafely() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                // Initialisation standard automatique depuis google-services.json ou fallback configuré
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:392990464767:android:283d9394872d586eee0aca")
                    .setProjectId("plantcare-7a9a8")
                    .setApiKey("AIzaSyCGThsVaNP1b_1-soOLIkyZpc1kRz5JG1g")
                    .setStorageBucket("plantcare-7a9a8.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.i(TAG, "FirebaseApp initialisé avec succès pour le projet plantcare-7a9a8")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'initialisation de FirebaseApp", e)
        }
    }

    companion object {
        private const val TAG = "PlantCareApplication"
    }
}
