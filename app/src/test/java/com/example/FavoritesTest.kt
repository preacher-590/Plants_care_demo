package com.example

import com.example.data.AuthState
import com.example.data.FavoritesRepository
import com.example.model.PlantData
import com.example.model.firestore.FavoriteDocument
import com.example.viewmodel.FavoritesUiState
import com.example.viewmodel.FavoritesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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

/**
 * Faux repository en mémoire pour tester rigoureusement la logique des favoris,
 * la déduplication par clé déterministe (uid_plantId), et la gestion des utilisateurs non connectés.
 */
class FakeFavoritesRepository(
    initialUserId: String? = "test_user_123"
) : FavoritesRepository {

    private var _currentUserId: String? = initialUserId
    override val currentUserId: String? get() = _currentUserId

    fun setUserId(uid: String?) {
        _currentUserId = uid
        // Met à jour les flux selon le nouvel utilisateur
        val uidVal = _currentUserId
        if (uidVal == null) {
            favoritesFlow.value = emptyList()
            idsFlow.value = emptySet()
        } else {
            val userFavs = inMemoryStorage.values.filter { it.uid == uidVal }
            favoritesFlow.value = userFavs
            idsFlow.value = userFavs.map { it.plantId }.toSet()
        }
    }

    // Stockage indexé par docId "${uid}_${plantId}"
    val inMemoryStorage = mutableMapOf<String, FavoriteDocument>()

    private val favoritesFlow = MutableStateFlow<List<FavoriteDocument>>(emptyList())
    private val idsFlow = MutableStateFlow<Set<String>>(emptySet())

    private fun updateFlows() {
        val uid = currentUserId
        if (uid == null) {
            favoritesFlow.value = emptyList()
            idsFlow.value = emptySet()
        } else {
            val userFavs = inMemoryStorage.values.filter { it.uid == uid }
            favoritesFlow.value = userFavs
            idsFlow.value = userFavs.map { it.plantId }.toSet()
        }
    }

    override fun observeFavorites(): Flow<List<FavoriteDocument>> = favoritesFlow.asStateFlow()

    override fun observeFavoritePlantIds(): Flow<Set<String>> = idsFlow.asStateFlow()

    override suspend fun isPlantFavorite(plantId: String): Boolean {
        val uid = currentUserId ?: return false
        val docId = FavoriteDocument.buildDocId(uid, plantId)
        return inMemoryStorage.containsKey(docId)
    }

    override suspend fun addFavorite(plantId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("Utilisateur non authentifié"))
        val docId = FavoriteDocument.buildDocId(uid, plantId)
        val doc = FavoriteDocument(
            id = docId,
            uid = uid,
            plantId = plantId,
            dateAjout = System.currentTimeMillis()
        )
        inMemoryStorage[docId] = doc
        updateFlows()
        return Result.success(Unit)
    }

    override suspend fun removeFavorite(plantId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("Utilisateur non authentifié"))
        val docId = FavoriteDocument.buildDocId(uid, plantId)
        inMemoryStorage.remove(docId)
        updateFlows()
        return Result.success(Unit)
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

/**
 * Suite de tests unitaires pour la fonctionnalité des Favoris.
 * Couvre :
 * 1. Ajout d'un favori
 * 2. Suppression d'un favori
 * 3. Tentative de double-ajout sur le même plantId (vérification de non-duplication par clé déterministe)
 * 4. Comportement pour un utilisateur non authentifié (refus d'écriture, absence de cache résiduel)
 * 5. Format et schéma du document FavoriteDocument
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test FavoriteDocument deterministic id generator`() {
        val docId = FavoriteDocument.buildDocId(uid = "user_42", plantId = "plant_chamomile")
        assertEquals("user_42_plant_chamomile", docId)
    }

    @Test
    fun `test add favorite successfully adds document`() = runTest {
        val repo = FakeFavoritesRepository(initialUserId = "user_123")
        val plantId = "plant_menthe"

        assertFalse(repo.isPlantFavorite(plantId))

        val result = repo.addFavorite(plantId)
        assertTrue(result.isSuccess)
        assertTrue(repo.isPlantFavorite(plantId))

        val docId = FavoriteDocument.buildDocId("user_123", plantId)
        val storedDoc = repo.inMemoryStorage[docId]
        assertNotNull(storedDoc)
        assertEquals("user_123", storedDoc?.uid)
        assertEquals(plantId, storedDoc?.plantId)
        assertTrue((storedDoc?.dateAjout ?: 0) > 0)
    }

    @Test
    fun `test remove favorite deletes document`() = runTest {
        val repo = FakeFavoritesRepository(initialUserId = "user_123")
        val plantId = "plant_lavande"

        repo.addFavorite(plantId)
        assertTrue(repo.isPlantFavorite(plantId))

        val removeResult = repo.removeFavorite(plantId)
        assertTrue(removeResult.isSuccess)
        assertFalse(repo.isPlantFavorite(plantId))

        val docId = FavoriteDocument.buildDocId("user_123", plantId)
        assertFalse(repo.inMemoryStorage.containsKey(docId))
    }

    @Test
    fun `test duplicate add on same plantId prevents duplicates via deterministic key`() = runTest {
        val repo = FakeFavoritesRepository(initialUserId = "user_123")
        val plantId = "plant_thym"

        // 1er ajout
        repo.addFavorite(plantId)
        assertEquals(1, repo.inMemoryStorage.size)

        // 2e ajout avec le même plantId
        val secondResult = repo.addFavorite(plantId)
        assertTrue(secondResult.isSuccess)

        // La taille du stockage doit rester exactement de 1 car la clé docId est écrasée à l'identique
        assertEquals(1, repo.inMemoryStorage.size)

        val docId = FavoriteDocument.buildDocId("user_123", plantId)
        assertEquals("user_123_plant_thym", docId)
        assertTrue(repo.inMemoryStorage.containsKey(docId))
    }

    @Test
    fun `test unauthenticated user cannot add or remove favorite`() = runTest {
        val repo = FakeFavoritesRepository(initialUserId = null)
        val plantId = "plant_arnica"

        // Tentative d'ajout sans utilisateur
        val addResult = repo.addFavorite(plantId)
        assertTrue(addResult.isFailure)
        assertEquals("Utilisateur non authentifié", addResult.exceptionOrNull()?.message)
        assertEquals(0, repo.inMemoryStorage.size)

        // Tentative de suppression sans utilisateur
        val removeResult = repo.removeFavorite(plantId)
        assertTrue(removeResult.isFailure)
        assertEquals("Utilisateur non authentifié", removeResult.exceptionOrNull()?.message)

        // Vérification que isPlantFavorite retourne false
        assertFalse(repo.isPlantFavorite(plantId))
    }

    @Test
    fun `test user isolation - user B cannot see user A favorites`() = runTest {
        val repo = FakeFavoritesRepository(initialUserId = "user_A")
        repo.addFavorite("plant_menthe")
        repo.addFavorite("plant_lavande")

        // Passage à l'utilisateur B
        repo.setUserId("user_B")
        assertFalse(repo.isPlantFavorite("plant_menthe"))
        assertFalse(repo.isPlantFavorite("plant_lavande"))

        val userBFavs = repo.observeFavorites().first()
        assertEquals(0, userBFavs.size)

        val userBIds = repo.observeFavoritePlantIds().first()
        assertTrue(userBIds.isEmpty())
    }

    @Test
    fun `test toggle favorite behavior`() = runTest {
        val repo = FakeFavoritesRepository(initialUserId = "user_123")
        val plantId = "plant_melisse"

        // Premier toggle -> ajout -> retourne true
        val toggle1 = repo.toggleFavorite(plantId)
        assertTrue(toggle1.isSuccess)
        assertEquals(true, toggle1.getOrNull())
        assertTrue(repo.isPlantFavorite(plantId))

        // Deuxième toggle -> suppression -> retourne false
        val toggle2 = repo.toggleFavorite(plantId)
        assertTrue(toggle2.isSuccess)
        assertEquals(false, toggle2.getOrNull())
        assertFalse(repo.isPlantFavorite(plantId))
    }
}
