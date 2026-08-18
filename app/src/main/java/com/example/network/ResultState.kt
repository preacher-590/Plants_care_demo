package com.example.network

/**
 * Type d'erreur rencontré lors des opérations d'accès aux données.
 */
enum class ErrorType {
    NETWORK_DISCONNECTED, // Absence de connexion réseau ou Firestore indisponible
    DOCUMENT_NOT_FOUND,   // Document demandé introuvable dans Firestore
    DESERIALIZATION,      // Échec de désérialisation de l'objet Kotlin
    FIREBASE_EXCEPTION,   // Exception retournée par le SDK Firebase
    UNKNOWN               // Erreur imprévue
}

/**
 * Sealed class générique représentant l'état du résultat d'une opération asynchrone (Chargement, Succès, Erreur).
 */
sealed class ResultState<out T> {
    object Loading : ResultState<Nothing>()
    data class Success<out T>(val data: T) : ResultState<T>()
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val errorType: ErrorType = ErrorType.UNKNOWN
    ) : ResultState<Nothing>()
}
