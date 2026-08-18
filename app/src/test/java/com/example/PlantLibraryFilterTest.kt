package com.example

import com.example.model.PlantData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Normalizer

/**
 * Tests unitaires pour le filtrage combiné de la bibliothèque des plantes :
 * 1. Filtre par catégorie de symptôme unique
 * 2. Multi-sélection de catégories (OU logique entre catégories)
 * 3. Combinaison recherche textuelle + multi-sélection (ET logique)
 * 4. Cas limite : aucun résultat trouvé (état vide)
 */
class PlantLibraryFilterTest {

    private val allPlants = PlantData.mockPlants

    private val symptomCategoryKeywords = mapOf(
        "Digestif" to listOf("digestion", "digestif", "estomac", "ballonnement", "nausée", "nausee", "spasme", "foie", "repas"),
        "Respiratoire" to listOf("gorge", "toux", "rhume", "bronch", "respiration", "respiratoire", "nez", "poumon", "orl", "grippe", "angine"),
        "Sommeil" to listOf("sommeil", "insomnie", "nuit", "endormissement", "dormir", "nocturne", "somnifere"),
        "Peau" to listOf("peau", "brulure", "brûlure", "cicatrisation", "coup de soleil", "eczema", "dermatologie", "rougeur", "irritation", "gercure", "plaie"),
        "Stress & Anxiété" to listOf("stress", "anxiete", "anxiété", "nervosite", "nervosité", "calme", "relaxation", "surmenage", "moral", "depression"),
        "Douleur" to listOf("migraine", "mal de tete", "maux de tete", "douleur", "articulation", "courbature", "crampe", "musculaire"),
        "Immunitaire" to listOf("immunite", "immunité", "infection", "anti-infectieux", "antibacterien", "antiseptique", "tonique", "vitalite", "vitalité", "energie", "énergie", "hiver")
    )

    private fun normalizeText(text: String): String {
        val regex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
        val temp = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        return regex.replace(temp, "")
    }

    private fun filterPlants(searchQuery: String, selectedCategories: Set<String>) = allPlants.filter { plant ->
        val matchesQuery = if (searchQuery.isBlank()) {
            true
        } else {
            val normalizedQuery = normalizeText(searchQuery.trim())
            val normalizedName = normalizeText(plant.name)
            val normalizedSciName = normalizeText(plant.scientificName)
            val normalizedCat = normalizeText(plant.category)
            val normalizedDesc = normalizeText(plant.shortDescription)

            normalizedName.contains(normalizedQuery) ||
                    normalizedSciName.contains(normalizedQuery) ||
                    normalizedCat.contains(normalizedQuery) ||
                    normalizedDesc.contains(normalizedQuery) ||
                    plant.ailmentsAndBenefits.any { normalizeText(it).contains(normalizedQuery) } ||
                    plant.matchedKeywords.any { normalizeText(it).contains(normalizedQuery) }
        }

        val matchesCategory = if (selectedCategories.isEmpty()) {
            true
        } else {
            selectedCategories.any { selectedCategory ->
                val targetKeywords = symptomCategoryKeywords[selectedCategory] ?: listOf(selectedCategory.lowercase())
                val normalizedCategoryName = normalizeText(selectedCategory)

                val plantKeywordsNormalized = plant.matchedKeywords.map { normalizeText(it) }
                val plantBenefitsNormalized = plant.ailmentsAndBenefits.map { normalizeText(it) }
                val plantCategoryNormalized = normalizeText(plant.category)

                plantCategoryNormalized.contains(normalizedCategoryName) ||
                        targetKeywords.any { kw ->
                            val nKw = normalizeText(kw)
                            plantKeywordsNormalized.any { it.contains(nKw) } ||
                                    plantBenefitsNormalized.any { it.contains(nKw) }
                        }
            }
        }

        matchesQuery && matchesCategory
    }

    @Test
    fun `test default filter with empty query and empty categories returns all plants`() {
        val results = filterPlants("", emptySet())
        assertEquals(allPlants.size, results.size)
    }

    @Test
    fun `test single category filter returns matching plants`() {
        val digestivePlants = filterPlants("", setOf("Digestif"))
        assertTrue(digestivePlants.isNotEmpty())
        assertTrue(digestivePlants.any { it.name.contains("Menthe") || it.name.contains("Sauge") || it.name.contains("Gingembre") })
        assertFalse(digestivePlants.any { it.name.contains("Aloe") })
    }

    @Test
    fun `test multi category selection performs logical OR between categories`() {
        val digestive = filterPlants("", setOf("Digestif"))
        val skin = filterPlants("", setOf("Peau"))
        val combined = filterPlants("", setOf("Digestif", "Peau"))

        // La combinaison OR doit contenir au moins autant ou plus que chaque catégorie individuelle
        assertTrue(combined.size >= digestive.size)
        assertTrue(combined.size >= skin.size)
        assertTrue(combined.any { it.name.contains("Aloe") || it.name.contains("Calendula") })
        assertTrue(combined.any { it.name.contains("Menthe") || it.name.contains("Gingembre") })
    }

    @Test
    fun `test combination of search query AND categories performs logical AND`() {
        // Recherche du mot "menthe" dans la catégorie "Digestif" -> doit trouver la Menthe Poivrée
        val result = filterPlants("menthe", setOf("Digestif"))
        assertEquals(1, result.size)
        assertEquals("menthe", result.first().id)

        // Recherche du mot "menthe" dans la catégorie "Peau" -> 0 résultat
        val noResult = filterPlants("menthe", setOf("Peau"))
        assertEquals(0, noResult.size)
    }

    @Test
    fun `test no match scenario returns empty list`() {
        val result = filterPlants("mot_inexistant_xyz_12345", setOf("Digestif"))
        assertTrue(result.isEmpty())
    }
}
