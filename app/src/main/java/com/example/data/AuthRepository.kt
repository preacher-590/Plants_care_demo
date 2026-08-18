package com.example.data

import android.util.Log
import com.example.model.firestore.UserDocument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * État d'authentification scellé exposé à l'interface utilisateur.
 */
sealed class AuthState {
    object Loading : AuthState()
    object NotAuthenticated : AuthState()
    data class Authenticated(
        val uid: String,
        val email: String,
        val role: String,
        val isGoogleProvider: Boolean = false
    ) : AuthState() {
        val isAdmin: Boolean get() = role == UserDocument.ROLE_ADMIN
    }
    data class Error(val message: String) : AuthState()
}

/**
 * Repository gérant l'authentification Firebase et la synchronisation du rôle utilisateur dans Firestore.
 */
class AuthRepository(
    private val authProvider: () -> FirebaseAuth? = {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Firebase Auth non disponible", e)
            null
        }
    },
    private val firestoreProvider: () -> FirebaseFirestore? = {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Firebase Firestore non disponible", e)
            null
        }
    }
) {

    private val auth: FirebaseAuth? get() = authProvider()
    private val firestore: FirebaseFirestore? get() = firestoreProvider()

    companion object {
        private const val TAG = "AuthRepository"
        private const val USERS_COLLECTION = "users"
    }

    /**
     * Flux en temps réel de l'état d'authentification de l'utilisateur.
     * Met à jour automatiquement le rôle depuis la collection Firestore 'users/{uid}'.
     */
    val authStateFlow: Flow<AuthState> = callbackFlow {
        val currentAuth = auth
        if (currentAuth == null) {
            trySend(AuthState.NotAuthenticated)
            close()
            return@callbackFlow
        }

        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                trySend(AuthState.NotAuthenticated)
            } else {
                // Traitement asynchrone du rôle utilisateur dans Firestore
                fetchOrCreateUserRole(user) { authState ->
                    trySend(authState)
                }
            }
        }

        currentAuth.addAuthStateListener(listener)

        awaitClose {
            currentAuth.removeAuthStateListener(listener)
        }
    }

    /**
     * Récupère le document utilisateur Firestore ou le crée avec le rôle par défaut 'user'.
     */
    private fun fetchOrCreateUserRole(user: FirebaseUser, onResult: (AuthState) -> Unit) {
        val db = firestore
        if (db == null) {
            val isGoogle = user.providerData.any { it.providerId == "google.com" }
            onResult(
                AuthState.Authenticated(
                    uid = user.uid,
                    email = user.email ?: "",
                    role = UserDocument.ROLE_USER,
                    isGoogleProvider = isGoogle
                )
            )
            return
        }

        val userDocRef = db.collection(USERS_COLLECTION).document(user.uid)
        
        userDocRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val role = documentSnapshot.getString("role") ?: UserDocument.ROLE_USER
                    val isGoogle = user.providerData.any { it.providerId == "google.com" }
                    onResult(
                        AuthState.Authenticated(
                            uid = user.uid,
                            email = user.email ?: "",
                            role = role,
                            isGoogleProvider = isGoogle
                        )
                    )
                } else {
                    // Premier enregistrement : création du document avec le rôle par défaut "user"
                    val newUser = UserDocument(
                        uid = user.uid,
                        email = user.email ?: "",
                        role = UserDocument.ROLE_USER,
                        createdAt = System.currentTimeMillis()
                    )
                    userDocRef.set(newUser)
                        .addOnSuccessListener {
                            val isGoogle = user.providerData.any { it.providerId == "google.com" }
                            onResult(
                                AuthState.Authenticated(
                                    uid = user.uid,
                                    email = user.email ?: "",
                                    role = UserDocument.ROLE_USER,
                                    isGoogleProvider = isGoogle
                                )
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Erreur lors de la création du profil utilisateur Firestore", e)
                            // Repli sécurisé avec le rôle "user"
                            onResult(
                                AuthState.Authenticated(
                                    uid = user.uid,
                                    email = user.email ?: "",
                                    role = UserDocument.ROLE_USER
                                )
                            )
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erreur lors de la lecture du rôle Firestore", e)
                // Repli si Firestore est inaccessible ou hors ligne
                val isGoogle = user.providerData.any { it.providerId == "google.com" }
                onResult(
                    AuthState.Authenticated(
                        uid = user.uid,
                        email = user.email ?: "",
                        role = UserDocument.ROLE_USER,
                        isGoogleProvider = isGoogle
                    )
                )
            }
    }

    /**
     * Connexion par Email / Mot de passe.
     */
    suspend fun loginWithEmail(email: String, password: CharSequence): Result<Unit> {
        val currentAuth = auth ?: return Result.failure(Exception("Service d'authentification indisponible"))
        return try {
            currentAuth.signInWithEmailAndPassword(email.trim(), password.toString()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Échec de connexion par email", e)
            Result.failure(e)
        }
    }

    /**
     * Inscription par Email / Mot de passe.
     */
    suspend fun registerWithEmail(email: String, password: CharSequence): Result<Unit> {
        val currentAuth = auth ?: return Result.failure(Exception("Service d'authentification indisponible"))
        return try {
            val authResult = currentAuth.createUserWithEmailAndPassword(email.trim(), password.toString()).await()
            val user = authResult.user
            if (user != null) {
                // Création explicite initiale du document utilisateur
                val newUserDoc = UserDocument(
                    uid = user.uid,
                    email = user.email ?: email.trim(),
                    role = UserDocument.ROLE_USER,
                    createdAt = System.currentTimeMillis()
                )
                try {
                    firestore?.collection(USERS_COLLECTION)?.document(user.uid)?.set(newUserDoc)?.await()
                } catch (firestoreErr: Exception) {
                    Log.w(TAG, "Document Firestore sera créé automatiquement via AuthStateListener", firestoreErr)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Échec de création de compte par email", e)
            Result.failure(e)
        }
    }

    /**
     * Réinitialisation de mot de passe par email.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val currentAuth = auth ?: return Result.failure(Exception("Service d'authentification indisponible"))
        return try {
            currentAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Échec d'envoi d'email de réinitialisation", e)
            Result.failure(e)
        }
    }

    /**
     * Connexion via un jeton Google (Google Sign-In / Credential Manager).
     */
    suspend fun signInWithGoogleCredential(idToken: String): Result<Unit> {
        val currentAuth = auth ?: return Result.failure(Exception("Service d'authentification indisponible"))
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = currentAuth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                // Vérification / création du rôle utilisateur dans Firestore
                val db = firestore
                if (db != null) {
                    val docRef = db.collection(USERS_COLLECTION).document(user.uid)
                    val snapshot = docRef.get().await()
                    if (!snapshot.exists()) {
                        val newUser = UserDocument(
                            uid = user.uid,
                            email = user.email ?: "",
                            role = UserDocument.ROLE_USER,
                            createdAt = System.currentTimeMillis()
                        )
                        docRef.set(newUser).await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Échec de la connexion Google", e)
            Result.failure(e)
        }
    }

    /**
     * Déconnexion de l'utilisateur.
     */
    fun logout() {
        auth?.signOut()
    }

    /**
     * Suppression définitive du compte utilisateur courant.
     */
    suspend fun deleteAccount(): Result<Unit> {
        val currentAuth = auth ?: return Result.failure(Exception("Service d'authentification indisponible"))
        val user = currentAuth.currentUser ?: return Result.failure(Exception("Aucun utilisateur connecté"))
        return try {
            // Tentative de nettoyage du document Firestore
            try {
                firestore?.collection(USERS_COLLECTION)?.document(user.uid)?.delete()?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Suppression document Firestore échouée ou restreinte par règles", e)
            }
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la suppression du compte", e)
            Result.failure(e)
        }
    }
}
