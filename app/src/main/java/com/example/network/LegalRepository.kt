package com.example.network

import android.util.Log
import com.example.database.LegalContentDao
import com.example.database.LegalContentEntity
import com.example.model.firestore.LegalContentDocument
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Repository gérant la lecture et l'édition dynamique du contenu légal (Mentions Légales & Politique de Confidentialité)
 * via Firebase Firestore avec cache local SQLite/Room.
 */
class LegalRepository(
    private val firestoreProvider: () -> FirebaseFirestore? = {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    },
    private val legalContentDao: LegalContentDao
) {

    private val firestore: FirebaseFirestore? get() = firestoreProvider()

    companion object {
        private const val TAG = "LegalRepository"
        private const val COLLECTION_LEGAL = "legalContent"

        val DEFAULT_MENTIONS = """
=== 1. ÉDITEUR DE L'APPLICATION ===
• Nom / Raison sociale : [À COMPLÉTER : Nom ou Raison sociale du propriétaire/entreprise]
• Forme juridique : [À COMPLÉTER : ex. SAS, SARL, Auto-entrepreneur, Association]
• Adresse du siège social : [À COMPLÉTER : Adresse postale complète, Code postal, Ville, Pays]
• SIREN / SIRET : [À COMPLÉTER : Numéro SIREN ou SIRET]
• RCS / Immatriculation : [À COMPLÉTER : Ville du RCS d'immatriculation]
• Contact Email : [À COMPLÉTER : email.de.contact@domaine.com]
• Téléphone : [À COMPLÉTER : +33 X XX XX XX XX]

=== 2. DIRECTEUR DE LA PUBLICATION ===
• Nom du responsable : [À COMPLÉTER : Prénom et Nom du directeur de la publication]
• Qualité : [À COMPLÉTER : ex. Fondateur / Gérant / Représentant légal]
• Contact direct : [À COMPLÉTER : email.publication@domaine.com]

=== 3. HÉBERGEMENT DES SERVICES ===
• Hébergeur Cloud : Google Cloud Platform / Firebase (Google Ireland Limited)
• Adresse de l'hébergeur : Gordon House, Barrow Street, Dublin 4, Irlande
• Contact hébergeur : [À COMPLÉTER : Lien ou email support Google Cloud / Firebase]

=== 4. CADRE JURIDIQUE & LIMITATION DE RESPONSABILITÉ ===
L'application "PlantCare" est un outil d'information générale, pédagogique et documentaire sur la phytothérapie et la botanique. Elle ne constitue en aucun cas un dispositif médical ni une prestation de conseil médical.
• Les informations botaniques ne remplacent aucunement l'avis d'un professionnel de santé qualifié.
• L'utilisateur est seul responsable de l'usage qu'il fait des informations présentées.
• En cas de doute ou d'urgence médicale, consultez immédiatement un médecin ou le 15.

=== 5. PROPRIÉTÉ INTELLECTUELLE ===
L'ensemble des contenus (textes, graphismes, logos, icônes, illustrations et code source) de l'application PlantCare est protégé par les lois relatives à la propriété intellectuelle. Toute reproduction non autorisée est interdite.
""".trimIndent()

        val DEFAULT_PRIVACY = """
=== 1. DONNÉES PERSONNELLES COLLECTÉES ===
L'application PlantCare collecte uniquement les données nécessaires au fonctionnement du compte utilisateur et à la sécurisation des accès :
• Adresse email personnelle de l'utilisateur.
• Méthode d'authentification utilisée (Email / Mot de passe ou Google Sign-In).
• Identifiant unique de compte (UID Firebase).

=== 2. FINALITÉ DU TRAITEMENT ===
Vos données sont traitées pour les finalités suivantes :
• Création et gestion sécurisée de votre compte utilisateur.
• Contrôle d'accès et d'attribution des rôles d'administration (User / Admin).
• Préparation des fonctionnalités personnalisées futures (synchronisation des favoris et historique des diagnostics de plantes).

=== 3. DURÉE DE CONSERVATION DES DONNÉES ===
Vos données personnelles sont conservées tant que votre compte utilisateur demeure actif dans l'application. En cas de suppression du compte par l'utilisateur, l'ensemble de vos données (profil Firebase Auth et document Firestore associé) est supprimé définitivement et de manière irréversible.

=== 4. SOUS-TRAITANTS ET TRANSFERT DE DONNÉES ===
L'hébergement des données et les services d'authentification sont confiés au sous-traitant suivant :
• Hébergeur / Authentification : Google Cloud Platform / Firebase (Google Ireland Limited, Gordon House, Barrow Street, Dublin 4, Irlande).
• Les transferts de données hors de l'Union Européenne sont encadrés par des clauses contractuelles types établies par la Commission Européenne garantissant un niveau de protection élevé.

=== 5. DROITS RGPD ET MODALITÉS D'EXERCICE ===
Conformément au Règlement Général sur la Protection des Données (RGPD - Règlement UE 2016/679), vous disposez des droits suivants :
• Droit d'accès et de rectification de vos données.
• Droit à l'effacement ("droit à l'oubli") : ce droit s'exerce de manière autonome et directe à tout moment via le bouton "Supprimer mon compte" présent dans l'écran Profil de l'application.
• Droit à la limitation et d'opposition au traitement.

=== 6. RESPONSABLE DE TRAITEMENT ET CONTACT DPO ===
Pour toute question relative à la protection des données ou pour exercer vos droits :
• Responsable du traitement / DPO : [À COMPLÉTER : Nom du DPO ou Nom de l'Éditeur]
• Email dédié confidentialité : [À COMPLÉTER : dpo.privacy@domaine.com]
• Adresse postale : [À COMPLÉTER : Adresse postale du service DPO / Réclamations]
• Téléphone DPO : [À COMPLÉTER : Téléphone du DPO / Support]
• En cas de litige non résolu, vous pouvez adresser une réclamation auprès de la CNIL (www.cnil.fr).
""".trimIndent()
    }

    /**
     * Flow retournant le contenu légal réactif depuis le cache Room,
     * et initialisant la valeur par défaut si Room est vide.
     */
    fun getLegalContentFlow(docId: String): Flow<LegalContentDocument> {
        val defaultText = if (docId == LegalContentDocument.DOC_PRIVACY) DEFAULT_PRIVACY else DEFAULT_MENTIONS
        return legalContentDao.getLegalContentFlow(docId).map { entity ->
            if (entity != null && entity.content.isNotBlank()) {
                LegalContentDocument(
                    docId = entity.docId,
                    content = entity.content,
                    lastUpdated = entity.lastUpdated,
                    updatedByUid = entity.updatedByUid
                )
            } else {
                LegalContentDocument(
                    docId = docId,
                    content = defaultText,
                    lastUpdated = System.currentTimeMillis(),
                    updatedByUid = "system_default"
                )
            }
        }
    }

    /**
     * Synchronise le document légal depuis Firestore et met à jour Room.
     */
    suspend fun syncLegalContentFromRemote(docId: String) {
        val db = firestore ?: return
        try {
            val snapshot = db.collection(COLLECTION_LEGAL)
                .document(docId)
                .get()
                .await()

            if (snapshot.exists()) {
                val doc = snapshot.toObject(LegalContentDocument::class.java)
                if (doc != null && doc.content.isNotBlank()) {
                    legalContentDao.insertOrUpdate(
                        LegalContentEntity(
                            docId = docId,
                            content = doc.content,
                            lastUpdated = doc.lastUpdated,
                            updatedByUid = doc.updatedByUid
                        )
                    )
                }
            } else {
                // Si le document n'existe pas encore sur Firestore, insérer la version par défaut dans Room
                val defaultText = if (docId == LegalContentDocument.DOC_PRIVACY) DEFAULT_PRIVACY else DEFAULT_MENTIONS
                val localContent = legalContentDao.getLegalContent(docId)
                if (localContent == null) {
                    legalContentDao.insertOrUpdate(
                        LegalContentEntity(
                            docId = docId,
                            content = defaultText,
                            lastUpdated = System.currentTimeMillis(),
                            updatedByUid = "system_default"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Impossible de synchroniser le contenu légal '$docId' depuis Firestore", e)
        }
    }

    /**
     * Sauvegarde le contenu légal édité par un administrateur sur Firestore et dans le cache Room local.
     */
    suspend fun saveLegalContent(docId: String, newContent: String, adminUid: String): Result<Unit> {
        val now = System.currentTimeMillis()
        val docData = mapOf(
            "docId" to docId,
            "content" to newContent.trim(),
            "lastUpdated" to now,
            "updatedByUid" to adminUid
        )

        val db = firestore ?: return Result.failure(Exception("Service Firestore non disponible"))

        return try {
            db.collection(COLLECTION_LEGAL)
                .document(docId)
                .set(docData)
                .await()

            // Mise à jour immédiate du cache local Room
            legalContentDao.insertOrUpdate(
                LegalContentEntity(
                    docId = docId,
                    content = newContent.trim(),
                    lastUpdated = now,
                    updatedByUid = adminUid
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Échec de sauvegarde du contenu légal sur Firestore", e)
            Result.failure(e)
        }
    }
}
