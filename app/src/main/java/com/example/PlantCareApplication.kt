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
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:100000000000:android:0000000000000000")
                    .setProjectId("plantcare-app")
                    .setApiKey("AIzaSyPlantCareFallbackKey1234567890")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.i(TAG, "FirebaseApp initialisé avec succès (options de secours)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'initialisation de FirebaseApp", e)
        }
    }

    companion object {
        private const val TAG = "PlantCareApplication"
    }
}
