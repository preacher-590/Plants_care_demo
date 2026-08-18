package com.example.data

import android.util.Log
import com.example.model.firestore.ScanHistoryDocument
import com.example.network.ResultState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository responsable de la persistance et de la gestion de l'historique des scans dans Firestore.
 * Conforme au RGPD :
 * - Aucune persistance locale alternative pour les utilisateurs anonymes/déconnectés.
 * - Droit à l'effacement : suppression définitive et irréversible des documents Firestore.
 * - Accès strictement restreint à l'UID propriétaire (sans exception même pour les administrateurs).
 */
class ScanHistoryRepository(
    private val authProvider: () -> FirebaseAuth? = {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseAuth non disponible", e)
            null
        }
    },
    private val firestoreProvider: () -> FirebaseFirestore? = {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseFirestore non disponible", e)
            null
        }
    }
) {

    private val auth: FirebaseAuth? get() = authProvider()
    private val firestore: FirebaseFirestore? get() = firestoreProvider()

    companion object {
        private const val TAG = "ScanHistoryRepository"
        const val COLLECTION_SCAN_HISTORY = "scanHistory"
    }

    /**
     * Retourne l'UID de l'utilisateur actuellement connecté ou null.
     */
    fun getCurrentUserUid(): String? {
        return auth?.currentUser?.uid
    }

    /**
     * Enregistre un scan dans Firestore si l'utilisateur est authentifié.
     * @param scan Document d'historique de scan à persister.
     * @return Result contenant l'ID du document créé.
     */
    suspend fun saveScan(scan: ScanHistoryDocument): Result<String> {
        val currentUid = auth?.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Utilisateur non authentifié : aucun historique n'est conservé."))

        val db = firestore
            ?: return Result.failure(IllegalStateException("Base de données Firestore indisponible."))

        return try {
            val docRef = db.collection(COLLECTION_SCAN_HISTORY).document()
            val documentToSave = scan.copy(
                id = docRef.id,
                uid = currentUid,
                dateScan = if (scan.dateScan > 0) scan.dateScan else System.currentTimeMillis()
            )
            docRef.set(documentToSave).await()
            Log.d(TAG, "Scan sauvegardé avec succès dans l'historique : ${docRef.id} pour l'utilisateur $currentUid")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'enregistrement du scan dans Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Écoute en temps réel l'historique des scans pour un UID donné, trié du plus récent au plus ancien.
     * @param uid Identifiant unique de l'utilisateur.
     */
    fun getScanHistoryFlow(uid: String): Flow<ResultState<List<ScanHistoryDocument>>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(ResultState.Error("Base de données Firestore indisponible"))
            close()
            return@callbackFlow
        }

        trySend(ResultState.Loading)

        val query = db.collection(COLLECTION_SCAN_HISTORY)
            .whereEqualTo("uid", uid)
            .orderBy("dateScan", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Erreur lors de l'écoute de l'historique des scans", error)
                trySend(ResultState.Error("Impossible de charger l'historique : ${error.localizedMessage}"))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ScanHistoryDocument::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.w(TAG, "Erreur de conversion du document ${doc.id}", e)
                        null
                    }
                }
                trySend(ResultState.Success(list))
            } else {
                trySend(ResultState.Success(emptyList()))
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Supprime définitivement une entrée unique d'historique de scan (action irréversible).
     * @param docId ID du document dans Firestore.
     * @param uid UID de l'utilisateur propriétaire pour validation de sécurité.
     */
    suspend fun deleteScanEntry(docId: String, uid: String): Result<Unit> {
        val currentUid = auth?.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Utilisateur non connecté"))

        if (currentUid != uid) {
            return Result.failure(SecurityException("Tentative de suppression d'un scan non autorisé"))
        }

        val db = firestore ?: return Result.failure(IllegalStateException("Firestore indisponible"))

        return try {
            db.collection(COLLECTION_SCAN_HISTORY).document(docId).delete().await()
            Log.d(TAG, "Entrée d'historique $docId supprimée définitivement")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la suppression de l'entrée $docId", e)
            Result.failure(e)
        }
    }

    /**
     * Supprime l'intégralité de l'historique de scan d'un utilisateur (droit à l'effacement RGPD).
     * Exécute un batch delete réel et irréversible sur tous les documents associés à cet UID.
     * @param uid UID de l'utilisateur concerné.
     */
    suspend fun clearAllUserHistory(uid: String): Result<Unit> {
        val currentUid = auth?.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Utilisateur non connecté"))

        if (currentUid != uid) {
            return Result.failure(SecurityException("Action non autorisée"))
        }

        val db = firestore ?: return Result.failure(IllegalStateException("Firestore indisponible"))

        return try {
            val snapshots = db.collection(COLLECTION_SCAN_HISTORY)
                .whereEqualTo("uid", uid)
                .get()
                .await()

            if (snapshots.isEmpty) {
                return Result.success(Unit)
            }

            // Exécution par lot (WriteBatch)
            val batch = db.batch()
            for (document in snapshots.documents) {
                batch.delete(document.reference)
            }
            batch.commit().await()

            Log.d(TAG, "Historique complet de l'utilisateur $uid purgé définitivement (${snapshots.size()} documents)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'effacement total de l'historique pour $uid", e)
            Result.failure(e)
        }
    }
}
