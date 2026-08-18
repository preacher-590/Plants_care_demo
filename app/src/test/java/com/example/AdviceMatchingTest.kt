package com.example

import com.example.database.PlantDao
import com.example.database.PlantEntity
import com.example.database.SymptomDao
import com.example.database.SymptomEntity
import com.example.data.SyncPreferencesRepository
import com.example.model.firestore.PlantDocument
import com.example.model.firestore.ScientificEvidenceLevel
import com.example.model.firestore.SymptomDocument
import com.example.model.firestore.VerificationStatus
import com.example.network.ErrorType
import com.example.network.PlantRepository
import com.example.network.PlantRepositoryImpl
import com.example.network.ResultState
import com.example.network.SymptomSearchResult
import com.example.viewmodel.AdviceState
import com.example.viewmodel.PlantViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Suite de tests unitaires pour le moteur de recherche de conseils par symptôme (Advice Matching).
 *
 * Cette classe valide la logique de correspondance symptomatique, le tri multicritère
 * (pertinence, statut de validation professionnelle, niveau de preuve clinique),
 * la présence obligatoire des contre-indications médicales, et la résilience hors-ligne (Cache-First).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdviceMatchingTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // FAUX OBJETS (Fakes) TESTABLES SANS DÉPENDANCE FLUIDE EXTERNE
    // =========================================================================

    /**
     * Implémentation Fake de PlantDao pour tester la persistance locale Room en mémoire.
     */
    private class FakePlantDao(
        private val initialPlants: List<PlantEntity> = emptyList()
    ) : PlantDao {
        val storage = initialPlants.toMutableList()

        override fun getAllPlants(): Flow<List<PlantEntity>> = flowOf(storage)
        override suspend fun getAllPlantsList(): List<PlantEntity> = storage.toList()
        override suspend fun getPlantById(id: String): PlantEntity? = storage.find { it.id == id }
        override suspend fun getPlantByScientificName(scientificName: String): PlantEntity? =
            storage.find { it.scientificName.equals(scientificName, ignoreCase = true) }

        override suspend fun getPlantsBySymptomId(symptomId: String): List<PlantEntity> =
            storage.filter { it.symptomIds.contains(symptomId) }

        override suspend fun insertPlants(plants: List<PlantEntity>) {
            storage.removeAll { existing -> plants.any { it.id == existing.id } }
            storage.addAll(plants)
        }

        override suspend fun clearAll() {
            storage.clear()
        }

        override suspend fun getCount(): Int = storage.size
    }

    /**
     * Implémentation Fake de SymptomDao pour tester la recherche de symptômes en mémoire.
     */
    private class FakeSymptomDao(
        private val initialSymptoms: List<SymptomEntity> = emptyList()
    ) : SymptomDao {
        val storage = initialSymptoms.toMutableList()

        override fun getAllSymptoms(): Flow<List<SymptomEntity>> = flowOf(storage)
        override suspend fun getAllSymptomsList(): List<SymptomEntity> = storage.toList()
        override suspend fun searchSymptomsByKeyword(keyword: String): List<SymptomEntity> =
            storage.filter {
                it.name.contains(keyword, ignoreCase = true) ||
                        it.category.contains(keyword, ignoreCase = true) ||
                        it.description.contains(keyword, ignoreCase = true)
            }

        override suspend fun insertSymptoms(symptoms: List<SymptomEntity>) {
            storage.removeAll { existing -> symptoms.any { it.id == existing.id } }
            storage.addAll(symptoms)
        }

        override suspend fun clearAll() {
            storage.clear()
        }

        override suspend fun getCount(): Int = storage.size
    }

    /**
     * Fake pour SyncPreferencesRepository (gestion des préférences de synchronisation).
     * Implémente directement l'interface SyncPreferencesRepository sans nécessiter de mock DataStore.
     */
    private class FakeSyncPreferencesRepository(
        private var initialTimestamp: Long? = System.currentTimeMillis()
    ) : SyncPreferencesRepository {
        override val lastSyncTimestamp: Flow<Long?> = flowOf(initialTimestamp)
        override suspend fun updateLastSyncTimestamp(timestamp: Long) {
            initialTimestamp = timestamp
        }
    }

    /**
     * Fake PlantRepository simulant des réponses contrôlées pour les tests du ViewModel.
     */
    private class FakePlantRepository(
        var searchAdviceResult: ResultState<SymptomSearchResult> = ResultState.Success(SymptomSearchResult(query = ""))
    ) : PlantRepository {
        override val lastSyncTimestamp: Flow<Long?> = flowOf(null)
        override fun getAllPlants(): Flow<ResultState<List<PlantDocument>>> = flowOf(ResultState.Success(emptyList()))
        override suspend fun getPlantById(plantId: String): ResultState<PlantDocument> = ResultState.Error("Not implemented")
        override suspend fun getPlantByScientificName(scientificName: String): ResultState<PlantDocument?> = ResultState.Success(null)
        override suspend fun getPlantsBySymptomId(symptomId: String): ResultState<List<PlantDocument>> = ResultState.Success(emptyList())
        override suspend fun searchAdviceBySymptom(userQuery: String): ResultState<SymptomSearchResult> = searchAdviceResult
        override fun getAllSymptoms(): Flow<ResultState<List<SymptomDocument>>> = flowOf(ResultState.Success(emptyList()))
        override suspend fun refreshData(): ResultState<Unit> = ResultState.Success(Unit)
        override suspend fun updatePlantImage(plantId: String, imageUrl: String, imageAuthor: String, imageLicense: String): ResultState<Unit> = ResultState.Success(Unit)
    }

    // =========================================================================
    // JEUX DE DONNÉES DE TEST RÉALISTES
    // =========================================================================

    private val symptomMalDeGorge = SymptomDocument(
        id = "mal_de_gorge",
        name = "Mal de gorge",
        category = "ORL & Respiratoire",
        description = "Douleur, picotement et irritation au niveau du pharynx ou du larynx."
    )

    private val symptomSommeil = SymptomDocument(
        id = "troubles_du_sommeil",
        name = "Troubles du sommeil",
        category = "Système Nerveux",
        description = "Difficultés d'endormissement, réveils nocturnes et insomnie passagère."
    )

    private val plantSauge = PlantDocument(
        id = "sauge",
        commonName = "Sauge Officinale",
        scientificName = "Salvia officinalis",
        family = "Lamiaceae",
        description = "Plante antiseptique et anti-inflammatoire pour les voies respiratoires.",
        traditionalUses = listOf("Mal de gorge & Gingivite", "Troubles digestifs"),
        contraindications = listOf(
            "Grossesse et allaitement (effet emménagogue et risque neurotoxique de la thuyone)",
            "Enfants de moins de 12 ans",
            "Antécédents de cancers hormonodépendants"
        ),
        drugInteractions = listOf("Traitements anti-épileptiques"),
        scientificEvidenceLevelRaw = "etudie_cliniquement",
        verificationStatusRaw = "verifie_par_professionnel",
        symptomIds = listOf("mal_de_gorge", "digestion_difficile")
    )

    private val plantThym = PlantDocument(
        id = "thym",
        commonName = "Thym Commun",
        scientificName = "Thymus vulgaris",
        family = "Lamiaceae",
        description = "Puissant antiseptique des voies respiratoires supérieures.",
        traditionalUses = listOf("Mal de gorge & Toux", "Infections respiratoires"),
        contraindications = listOf("Allergie aux Lamiacées"),
        scientificEvidenceLevelRaw = "etudie_cliniquement",
        verificationStatusRaw = "verifie_par_professionnel",
        symptomIds = listOf("mal_de_gorge")
    )

    private val plantCamomille = PlantDocument(
        id = "camomille",
        commonName = "Camomille Matricaire",
        scientificName = "Matricaria chamomilla",
        family = "Asteraceae",
        description = "Plante apaisante pour le sommeil et les spasmes digestifs.",
        traditionalUses = listOf("Insomnie & Troubles du sommeil", "Stress & Anxiété"),
        contraindications = listOf("Allergie connue aux Astéracées"),
        scientificEvidenceLevelRaw = "etudie_cliniquement",
        verificationStatusRaw = "verifie_par_professionnel",
        symptomIds = listOf("troubles_du_sommeil", "stress_anxiete")
    )

    // =========================================================================
    // SCÉNARIOS DE TEST UNITAIRES
    // =========================================================================

    /**
     * 1. Exact Match : Un symptôme saisi correspond mot pour mot à une entrée de la collection symptoms
     * -> vérifier que les plantes associées sont correctement retournées.
     */
    @Test
    fun `exact match between user input and symptom returns associated plants`() = runTest {
        val fakePlantDao = FakePlantDao(listOf(PlantEntity.fromDocument(plantSauge), PlantEntity.fromDocument(plantCamomille)))
        val fakeSymptomDao = FakeSymptomDao(listOf(SymptomEntity.fromDocument(symptomMalDeGorge), SymptomEntity.fromDocument(symptomSommeil)))
        val repository = PlantRepositoryImpl(
            firestoreProvider = { null },
            plantDao = fakePlantDao,
            symptomDao = fakeSymptomDao,
            syncPreferences = FakeSyncPreferencesRepository()
        )

        val result = repository.searchAdviceBySymptom("Mal de gorge")

        assertTrue("Le résultat doit être un succès", result is ResultState.Success)
        val data = (result as ResultState.Success).data
        assertEquals("Un symptôme doit être reconnu", 1, data.matchedSymptoms.size)
        assertEquals("mal_de_gorge", data.matchedSymptoms.first().id)
        assertTrue("La sauge doit être présente dans les résultats", data.plants.any { it.id == "sauge" })
        assertFalse("La camomille ne doit pas être associée au mal de gorge", data.plants.any { it.id == "camomille" })
    }

    /**
     * 2. Approximate Match : Un symptôme saisi est proche d'un existant mais sans correspondance exacte
     * -> vérifier que la liste des symptômes suggérés est renvoyée (et non une liste vide ou une erreur).
     */
    @Test
    fun `approximate or misspelled symptom query returns suggested similar symptoms`() = runTest {
        val fakePlantDao = FakePlantDao(listOf(PlantEntity.fromDocument(plantSauge)))
        val fakeSymptomDao = FakeSymptomDao(listOf(SymptomEntity.fromDocument(symptomSommeil), SymptomEntity.fromDocument(symptomMalDeGorge)))
        val repository = PlantRepositoryImpl(
            firestoreProvider = { null },
            plantDao = fakePlantDao,
            symptomDao = fakeSymptomDao,
            syncPreferences = FakeSyncPreferencesRepository()
        )

        // Saisie approchante avec préfixe identique "somm..." (ex: "sommeil profond")
        val result = repository.searchAdviceBySymptom("sommeil")

        assertTrue("Le résultat doit réussir avec des suggestions ou correspondances", result is ResultState.Success)
        val data = (result as ResultState.Success).data
        val foundInMatches = data.matchedSymptoms.any { it.id == "troubles_du_sommeil" }
        val foundInSuggestions = data.suggestedSymptoms.any { it.id == "troubles_du_sommeil" }

        assertTrue(
            "Le symptôme 'troubles_du_sommeil' doit être soit matché soit suggéré",
            foundInMatches || foundInSuggestions
        )
    }

    /**
     * 3. No Match at All : Texte sans rapport (ex: "voiture", caractères spéciaux)
     * -> vérifier que l'état retourné est NoResults, jamais un crash ni un succès vide silencieux.
     */
    @Test
    fun `irrelevant or empty text returns NoResults state without crashing`() = runTest {
        val fakeRepo = FakePlantRepository(
            searchAdviceResult = ResultState.Success(
                SymptomSearchResult(
                    query = "voiture xyz 123",
                    matchedSymptoms = emptyList(),
                    plants = emptyList(),
                    suggestedSymptoms = listOf(symptomMalDeGorge, symptomSommeil)
                )
            )
        )

        val viewModel = PlantViewModel(plantRepository = fakeRepo)

        viewModel.searchAdvice("voiture xyz 123")
        advanceUntilIdle()

        val uiState = viewModel.adviceState.value
        assertTrue(
            "L'état doit être AdviceState.NoResults lors d'une saisie sans correspondance",
            uiState.state is AdviceState.NoResults
        )
        val noResultsState = uiState.state as AdviceState.NoResults
        assertEquals("voiture xyz 123", noResultsState.query)
        assertTrue("Des suggestions de repli doivent être fournies", noResultsState.suggestedSymptoms.isNotEmpty())
    }

    /**
     * 4. Sort by relevance : Plusieurs plantes correspondent avec des scores de match différents
     * -> vérifier que le tri privilégie le plus grand nombre de correspondances (symptôme + mots-clés).
     */
    @Test
    fun `sorting prioritizes highest number of keyword and symptom matches`() = runTest {
        // Sauge matche à la fois le symptôme 'mal_de_gorge' ET le mot-clé 'gorge' dans traditionalUses
        val plantHighRelevance = plantSauge.copy(
            id = "plant_high",
            traditionalUses = listOf("Mal de gorge intense", "Infection de la gorge")
        )
        // Plante Low ne matche que par son identifiant de symptôme
        val plantLowRelevance = plantThym.copy(
            id = "plant_low",
            traditionalUses = listOf("Usage général respiratoire")
        )

        val fakePlantDao = FakePlantDao(listOf(PlantEntity.fromDocument(plantLowRelevance), PlantEntity.fromDocument(plantHighRelevance)))
        val fakeSymptomDao = FakeSymptomDao(listOf(SymptomEntity.fromDocument(symptomMalDeGorge)))
        val repository = PlantRepositoryImpl(
            firestoreProvider = { null },
            plantDao = fakePlantDao,
            symptomDao = fakeSymptomDao,
            syncPreferences = FakeSyncPreferencesRepository()
        )

        val result = repository.searchAdviceBySymptom("gorge")
        assertTrue(result is ResultState.Success)
        val plants = (result as ResultState.Success).data.plants

        assertEquals("plant_high doit être en 1ère position car elle a plus de matches", "plant_high", plants[0].id)
        assertEquals("plant_low doit être en 2ème position", "plant_low", plants[1].id)
    }

    /**
     * 5. Sort by verification status with equal relevance : Deux plantes ont le même score de match
     * -> vérifier que la plante "vérifié par un professionnel" précède celle "non vérifié".
     */
    @Test
    fun `sorting with equal relevance prioritizes verified by professional over unverified`() = runTest {
        val plantUnverified = PlantDocument(
            id = "plant_unverified",
            commonName = "Plante Non Vérifiée",
            scientificName = "Planta ignota",
            traditionalUses = listOf("Mal de gorge"),
            verificationStatusRaw = "non_verifie",
            scientificEvidenceLevelRaw = "etudie_cliniquement",
            symptomIds = listOf("mal_de_gorge")
        )

        val plantVerified = PlantDocument(
            id = "plant_verified",
            commonName = "Plante Vérifiée",
            scientificName = "Planta probata",
            traditionalUses = listOf("Mal de gorge"),
            verificationStatusRaw = "verifie_par_professionnel",
            scientificEvidenceLevelRaw = "etudie_cliniquement",
            symptomIds = listOf("mal_de_gorge")
        )

        val fakePlantDao = FakePlantDao(listOf(PlantEntity.fromDocument(plantUnverified), PlantEntity.fromDocument(plantVerified)))
        val fakeSymptomDao = FakeSymptomDao(listOf(SymptomEntity.fromDocument(symptomMalDeGorge)))
        val repository = PlantRepositoryImpl(
            firestoreProvider = { null },
            plantDao = fakePlantDao,
            symptomDao = fakeSymptomDao,
            syncPreferences = FakeSyncPreferencesRepository()
        )

        val result = repository.searchAdviceBySymptom("Mal de gorge")
        assertTrue(result is ResultState.Success)
        val plants = (result as ResultState.Success).data.plants

        assertEquals("La plante vérifiée par un professionnel doit être en tête", "plant_verified", plants[0].id)
        assertEquals("La plante non vérifiée doit être en seconde position", "plant_unverified", plants[1].id)
    }

    /**
     * 6. Sort by level of evidence with equal verification status :
     * -> vérifier que "étudié cliniquement" prime sur "usage traditionnel" lorsque le statut de vérification est identique.
     */
    @Test
    fun `sorting with equal verification status prioritizes clinically studied over traditional use`() = runTest {
        val plantTraditional = PlantDocument(
            id = "plant_traditional",
            commonName = "Plante Traditionnelle",
            scientificName = "Planta antiqua",
            traditionalUses = listOf("Troubles du sommeil"),
            verificationStatusRaw = "verifie_par_professionnel",
            scientificEvidenceLevelRaw = "usage_traditionnel",
            symptomIds = listOf("troubles_du_sommeil")
        )

        val plantClinical = PlantDocument(
            id = "plant_clinical",
            commonName = "Plante Clinique",
            scientificName = "Planta clinica",
            traditionalUses = listOf("Troubles du sommeil"),
            verificationStatusRaw = "verifie_par_professionnel",
            scientificEvidenceLevelRaw = "etudie_cliniquement",
            symptomIds = listOf("troubles_du_sommeil")
        )

        val fakePlantDao = FakePlantDao(listOf(PlantEntity.fromDocument(plantTraditional), PlantEntity.fromDocument(plantClinical)))
        val fakeSymptomDao = FakeSymptomDao(listOf(SymptomEntity.fromDocument(symptomSommeil)))
        val repository = PlantRepositoryImpl(
            firestoreProvider = { null },
            plantDao = fakePlantDao,
            symptomDao = fakeSymptomDao,
            syncPreferences = FakeSyncPreferencesRepository()
        )

        val result = repository.searchAdviceBySymptom("Troubles du sommeil")
        assertTrue(result is ResultState.Success)
        val plants = (result as ResultState.Success).data.plants

        assertEquals("La plante avec niveau de preuve clinique doit être en 1ère position", "plant_clinical", plants[0].id)
        assertEquals("La plante avec usage traditionnel doit être en 2ème position", "plant_traditional", plants[1].id)
    }

    /**
     * 7. Systematic presence of contraindications in result :
     * -> vérifier que les champs 'contraindications' ne sont JAMAIS omis ou silencieusement nuls dans le modèle UI.
     */
    @Test
    fun `contraindications are systematically present and non null in UI result model`() = runTest {
        val plantWithEmptyContraindications = PlantDocument(
            id = "plant_no_ci",
            commonName = "Plante Douce",
            scientificName = "Planta mitis",
            traditionalUses = listOf("Mal de gorge"),
            contraindications = emptyList(),
            symptomIds = listOf("mal_de_gorge")
        )

        val fakeRepo = FakePlantRepository(
            searchAdviceResult = ResultState.Success(
                SymptomSearchResult(
                    query = "Mal de gorge",
                    matchedSymptoms = listOf(symptomMalDeGorge),
                    plants = listOf(plantSauge, plantWithEmptyContraindications)
                )
            )
        )

        val viewModel = PlantViewModel(plantRepository = fakeRepo)
        viewModel.searchAdvice("Mal de gorge")
        advanceUntilIdle()

        val uiState = viewModel.adviceState.value
        assertTrue(uiState.state is AdviceState.Success)
        val matches = (uiState.state as AdviceState.Success).matches

        assertEquals(2, matches.size)
        for (match in matches) {
            assertNotNull("La liste des contre-indications ne doit jamais être nulle", match.plant.contraindications)
            assertNotNull("La liste des interactions ne doit jamais être nulle", match.plant.drugInteractions)
        }
        assertTrue("La sauge doit comporter ses contre-indications majeures", matches[0].plant.contraindications.isNotEmpty())
        assertTrue("La liste vide doit être une List valide et non un null masqué", matches[1].plant.contraindications.isEmpty())
    }

    /**
     * 8. Borderline case : plante avec contre-indication sévère mais forte pertinence
     * -> vérifier que la présence d'une contre-indication n'exclut JAMAIS silencieusement la plante des résultats (l'alerte doit être visible).
     */
    @Test
    fun `plant with severe contraindications is not silently filtered out of results`() = runTest {
        // La sauge a une contre-indication sévère (neurotoxicité thuyone, grossesse) mais est hautement pertinente
        val fakePlantDao = FakePlantDao(listOf(PlantEntity.fromDocument(plantSauge)))
        val fakeSymptomDao = FakeSymptomDao(listOf(SymptomEntity.fromDocument(symptomMalDeGorge)))
        val repository = PlantRepositoryImpl(
            firestoreProvider = { null },
            plantDao = fakePlantDao,
            symptomDao = fakeSymptomDao,
            syncPreferences = FakeSyncPreferencesRepository()
        )

        val result = repository.searchAdviceBySymptom("Mal de gorge")
        assertTrue(result is ResultState.Success)
        val plants = (result as ResultState.Success).data.plants

        assertTrue("La plante avec contre-indications majeures doit être retournée pour informer l'utilisateur", plants.any { it.id == "sauge" })
        val retrievedSauge = plants.first { it.id == "sauge" }
        assertTrue("Les alertes de contre-indications doivent être conservées", retrievedSauge.contraindications.any { it.contains("Grossesse", ignoreCase = true) })
    }

    /**
     * 9. Handling network outages with available local cache :
     * -> simule une indisponibilité Firestore avec un cache Room non-vide : vérifie que la recherche fonctionne depuis le cache.
     */
    @Test
    fun `network outage with populated local cache successfully returns cached results`() = runTest {
        // Firestore est indisponible (null provider simulant le hors-ligne) mais Room contient les entités
        val fakePlantDao = FakePlantDao(listOf(PlantEntity.fromDocument(plantSauge)))
        val fakeSymptomDao = FakeSymptomDao(listOf(SymptomEntity.fromDocument(symptomMalDeGorge)))
        val repository = PlantRepositoryImpl(
            firestoreProvider = { throw IOException("Réseau indisponible (Simulated Offline)") },
            plantDao = fakePlantDao,
            symptomDao = fakeSymptomDao,
            syncPreferences = FakeSyncPreferencesRepository()
        )

        val result = repository.searchAdviceBySymptom("Mal de gorge")

        assertTrue("La recherche doit réussir en mode hors-ligne grâce au cache Room", result is ResultState.Success)
        val data = (result as ResultState.Success).data
        assertEquals(1, data.plants.size)
        assertEquals("sauge", data.plants.first().id)
    }

    /**
     * 10. Handling network outages with empty local cache :
     * -> vérifie qu'un état d'erreur approprié (distinct de NoResults) est renvoyé lorsque le réseau échoue et que le cache est vide.
     */
    @Test
    fun `network outage with empty local cache returns explicit error state distinct from NoResults`() = runTest {
        // ViewModel configuré avec un repository retournant une erreur réseau
        val fakeRepo = FakePlantRepository(
            searchAdviceResult = ResultState.Error(
                message = "Connexion réseau indisponible et aucun cache local.",
                errorType = ErrorType.NETWORK_DISCONNECTED
            )
        )

        val viewModel = PlantViewModel(plantRepository = fakeRepo)
        viewModel.searchAdvice("Mal de gorge")
        advanceUntilIdle()

        val uiState = viewModel.adviceState.value
        assertTrue(
            "L'état doit être explicitement AdviceState.Error et non NoResults lors d'une panne réseau",
            uiState.state is AdviceState.Error
        )
        val errorState = uiState.state as AdviceState.Error
        assertTrue("Le message d'erreur doit indiquer le problème de connectivité", errorState.message.contains("réseau") || errorState.message.contains("cache"))
    }
}
