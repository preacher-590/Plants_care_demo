package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.model.firestore.FavoriteDocument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Interface du Repository pour la gestion des plantes favorites.
 */
interface FavoritesRepository {
    val currentUserId: String?

    /**
     * Observe en temps réel l'ensemble des favoris de l'utilisateur connecté.
     */
    fun observeFavorites(): Flow<List<FavoriteDocument>>

    /**
     * Observe en temps réel l'ensemble des IDs de plantes favorites de l'utilisateur connecté sous forme d'un Set.
     */
    fun observeFavoritePlantIds(): Flow<Set<String>>

    /**
     * Vérifie de manière ponctuelle si une plante est marquée comme favorite par l'utilisateur connecté.
     */
    suspend fun isPlantFavorite(plantId: String): Boolean

    /**
     * Ajoute une plante aux favoris de l'utilisateur connecté.
     */
    suspend fun addFavorite(plantId: String): Result<Unit>

    /**
     * Retire une plante des favoris de l'utilisateur connecté.
     */
    suspend fun removeFavorite(plantId: String): Result<Unit>

    /**
     * Bascule l'état de favori (ajoute si non présent, supprime si présent).
     */
    suspend fun toggleFavorite(plantId: String): Result<Boolean>
}

/**
 * Implémentation Firestore de FavoritesRepository.
 */
class FavoritesRepositoryImpl(
    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) Log.e(TAG, "FirebaseAuth non disponible", e)
        null
    },
    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) Log.e(TAG, "FirebaseFirestore non disponible", e)
        null
    }
) : FavoritesRepository {

    companion object {
        private const val TAG = "FavoritesRepository"
    }

    override val currentUserId: String?
        get() = auth?.currentUser?.uid

    /**
     * Observe la collection 'favorites' filtrée sur l'UID courant et triée par date d'ajout décroissante.
     */
    override fun observeFavorites(): Flow<List<FavoriteDocument>> = callbackFlow {
        val uid = currentUserId
        if (uid.isNullOrBlank() || firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = firestore.collection(FavoriteDocument.COLLECTION_NAME)
                .whereEqualTo("uid", uid)
                .orderBy("dateAjout", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        if (BuildConfig.DEBUG) {
                            Log.e(TAG, "Erreur lors de l'écoute des favoris", error)
                        }
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val items = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(FavoriteDocument::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            if (BuildConfig.DEBUG) {
                                Log.w(TAG, "Erreur de désérialisation du favori ${doc.id}", e)
                            }
                            null
                        }
                    } ?: emptyList()

                    trySend(items)
                }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Exception lors de l'attachement du listener des favoris", e)
            }
            trySend(emptyList())
        }

        awaitClose {
            registration?.remove()
        }
    }

    /**
     * Observe en temps réel le set des plantIds favoris pour une vérification instantanée O(1).
     */
    override fun observeFavoritePlantIds(): Flow<Set<String>> = callbackFlow {
        val uid = currentUserId
        if (uid.isNullOrBlank() || firestore == null) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = firestore.collection(FavoriteDocument.COLLECTION_NAME)
                .whereEqualTo("uid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        if (BuildConfig.DEBUG) {
                            Log.e(TAG, "Erreur lors de l'écoute des IDs favoris", error)
                        }
                        trySend(emptySet())
                        return@addSnapshotListener
                    }

                    val ids = snapshot?.documents?.mapNotNull { doc ->
                        doc.getString("plantId")
                    }?.toSet() ?: emptySet()

                    trySend(ids)
                }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Exception lors de l'écoute des IDs favoris", e)
            }
            trySend(emptySet())
        }

        awaitClose {
            registration?.remove()
        }
    }

    override suspend fun isPlantFavorite(plantId: String): Boolean {
        val uid = currentUserId ?: return false
        val fs = firestore ?: return false
        val docId = FavoriteDocument.buildDocId(uid, plantId)

        return try {
            val doc = fs.collection(FavoriteDocument.COLLECTION_NAME).document(docId).get().await()
            doc.exists()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Erreur lors de la vérification de favori pour $plantId", e)
            }
            false
        }
    }

    override suspend fun addFavorite(plantId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("Utilisateur non authentifié"))
        val fs = firestore ?: return Result.failure(IllegalStateException("Service Firestore indisponible"))

        val docId = FavoriteDocument.buildDocId(uid, plantId)
        val docData = FavoriteDocument(
            id = docId,
            uid = uid,
            plantId = plantId,
            dateAjout = System.currentTimeMillis()
        )

        return try {
            fs.collection(FavoriteDocument.COLLECTION_NAME)
                .document(docId)
                .set(docData)
                .await()
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Plante $plantId ajoutée aux favoris (doc: $docId)")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Échec de l'ajout aux favoris pour $plantId", e)
            }
            Result.failure(e)
        }
    }

    override suspend fun removeFavorite(plantId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("Utilisateur non authentifié"))
        val fs = firestore ?: return Result.failure(IllegalStateException("Service Firestore indisponible"))

        val docId = FavoriteDocument.buildDocId(uid, plantId)

        return try {
            fs.collection(FavoriteDocument.COLLECTION_NAME)
                .document(docId)
                .delete()
                .await()
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Plante $plantId retirée des favoris (doc: $docId)")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Échec de la suppression du favori pour $plantId", e)
            }
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(plantId: String): Result<Boolean> {
        val isFav = isPlantFavorite(plantId)
        return if (isFav) {
            removeFavorite(plantId).map { false }
        } else {
            addFavorite(plantId).map { true }
        }
    }
}
