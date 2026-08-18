package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Plant
import com.example.model.PlantData
import com.example.ui.components.CondensedSafetyBadge
import com.example.ui.components.EvidenceLevelBadge
import com.example.ui.components.FavoriteButton
import com.example.ui.components.PlantImage
import com.example.ui.components.SymptomCategoryFilterBar
import com.example.viewmodel.FavoritesViewModel
import com.example.viewmodel.PlantViewModel
import java.text.Normalizer

/**
 * Mapping des catégories de symptômes vers des mots-clés et affections associés.
 */
private val SYMPTOM_CATEGORY_KEYWORDS = mapOf(
    "Digestif" to listOf("digestion", "digestif", "estomac", "ballonnement", "nausée", "nausee", "spasme", "foie", "repas"),
    "Respiratoire" to listOf("gorge", "toux", "rhume", "bronch", "respiration", "respiratoire", "nez", "poumon", "orl", "grippe", "angine"),
    "Sommeil" to listOf("sommeil", "insomnie", "nuit", "endormissement", "dormir", "nocturne", "somnifere"),
    "Peau" to listOf("peau", "brulure", "brûlure", "cicatrisation", "coup de soleil", "eczema", "dermatologie", "rougeur", "irritation", "gercure", "plaie"),
    "Stress & Anxiété" to listOf("stress", "anxiete", "anxiété", "nervosite", "nervosité", "calme", "relaxation", "surmenage", "moral", "depression"),
    "Douleur" to listOf("migraine", "mal de tete", "maux de tete", "douleur", "articulation", "courbature", "crampe", "musculaire"),
    "Immunitaire" to listOf("immunite", "immunité", "infection", "anti-infectieux", "antibacterien", "antiseptique", "tonique", "vitalite", "vitalité", "energie", "énergie", "hiver")
)

private val SYMPTOM_CATEGORIES_LIST = listOf(
    "Digestif",
    "Respiratoire",
    "Sommeil",
    "Peau",
    "Stress & Anxiété",
    "Douleur",
    "Immunitaire"
)

private fun normalizeText(text: String): String {
    val regex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    val temp = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
    return regex.replace(temp, "")
}

/**
 * Écran d'exploration de la Bibliothèque des plantes (catalogue complet).
 * Permet la recherche textuelle dynamique et le filtrage combiné par multi-sélection de catégories de symptômes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlantLibraryScreen(
    plantViewModel: PlantViewModel? = null,
    favoritesViewModel: FavoritesViewModel? = null,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToLogin: (() -> Unit)? = null
) {
    // État local de la session (réinitialisé à chaque nouvelle ouverture d'écran)
    var localSearchQuery by remember { mutableStateOf("") }
    var localSelectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showAuthRequiredDialog by remember { mutableStateOf(false) }

    // Utilisation des données Room / Cache local exposées par le ViewModel ou fallback PlantData
    val allPlants: List<Plant> = if (plantViewModel != null) {
        val plantsList by plantViewModel.libraryPlants.collectAsState()
        plantsList
    } else {
        PlantData.mockPlants
    }

    val favoriteIds: Set<String> = favoritesViewModel?.let {
        val ids by it.favoritePlantIds.collectAsState()
        ids
    } ?: emptySet()

    // Filtrage combiné : (Catégorie A OU Catégorie B) ET Recherche textuelle
    val filteredPlants: List<Plant> by remember(allPlants, localSearchQuery, localSelectedCategories) {
        derivedStateOf {
            allPlants.filter { plant: Plant ->
                // 1. Condition recherche textuelle
                val matchesQuery = if (localSearchQuery.isBlank()) {
                    true
                } else {
                    val normalizedQuery = normalizeText(localSearchQuery.trim())
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

                // 2. Condition catégorie de symptôme (OU logique entre les catégories sélectionnées)
                val matchesCategory = if (localSelectedCategories.isEmpty()) {
                    true
                } else {
                    localSelectedCategories.any { selectedCategory ->
                        val targetKeywords = SYMPTOM_CATEGORY_KEYWORDS[selectedCategory] ?: listOf(selectedCategory.lowercase())
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
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bibliothèque Botanique",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (filteredPlants.size <= 1) "${filteredPlants.size} plante trouvée" else "${filteredPlants.size} plantes trouvées",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("library_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("library_screen_root")
        ) {
            // Zone de recherche textuelle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = localSearchQuery,
                    onValueChange = { localSearchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_input"),
                    placeholder = { Text("Rechercher par nom, symptôme ou mal...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Icône de recherche",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (localSearchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { localSearchQuery = "" },
                                modifier = Modifier.testTag("library_clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Effacer la recherche"
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Barre de filtres par catégories de symptômes (FilterChip scrollable M3 avec multi-sélection)
            SymptomCategoryFilterBar(
                categories = SYMPTOM_CATEGORIES_LIST,
                selectedCategories = localSelectedCategories,
                onCategoryToggled = { category ->
                    localSelectedCategories = if (localSelectedCategories.contains(category)) {
                        localSelectedCategories - category
                    } else {
                        localSelectedCategories + category
                    }
                },
                onResetAll = {
                    localSelectedCategories = emptySet()
                },
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Indicateur textuel des filtres actifs et nombre de résultats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filterDesc = if (localSelectedCategories.isEmpty()) {
                    "Toutes les catégories"
                } else {
                    localSelectedCategories.joinToString(", ")
                }
                Text(
                    text = filterDesc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${filteredPlants.size} résultat${if (filteredPlants.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Liste des plantes ou état vide explicatif
            if (filteredPlants.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FilterListOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Aucune plante trouvée",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Aucune plante ne correspond à la combinaison des filtres de catégories et de la recherche actuelle.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                localSearchQuery = ""
                                localSelectedCategories = emptySet()
                            },
                            modifier = Modifier.testTag("reset_all_filters_button")
                        ) {
                            Text("Réinitialiser les filtres")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredPlants, key = { it.id }) { plant ->
                        val isFav = favoriteIds.contains(plant.id)
                        LibraryPlantCard(
                            plant = plant,
                            isFavorite = isFav,
                            onToggleFavorite = {
                                favoritesViewModel?.toggleFavorite(
                                    plantId = plant.id,
                                    plantName = plant.name,
                                    isCurrentlyFavorite = isFav,
                                    onUnauthenticated = { showAuthRequiredDialog = true }
                                )
                            },
                            onClick = { onNavigateToDetail(plant.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAuthRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showAuthRequiredDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Connexion requise", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Pour sauvegarder vos plantes favorites et les retrouver sur tous vos appareils, veuillez vous connecter.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAuthRequiredDialog = false
                        onNavigateToLogin?.invoke()
                    },
                    modifier = Modifier.testTag("library_dialog_login_button")
                ) {
                    Text("Se connecter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthRequiredDialog = false }) {
                    Text("Plus tard")
                }
            }
        )
    }
}

/**
 * Carte individuelle pour chaque plante dans la bibliothèque.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryPlantCard(
    plant: Plant,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("plant_card_${plant.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    PlantImage(
                        imageUrl = plant.imageUrl,
                        imageAuthor = plant.imageAuthor,
                        imageLicense = plant.imageLicense,
                        plantName = plant.name,
                        colorHex = plant.colorHex,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = plant.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = plant.scientificName,
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = plant.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Bouton favori animé (M3)
                FavoriteButton(
                    isFavorite = isFavorite,
                    onToggle = { onToggleFavorite?.invoke() },
                    plantName = plant.name
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges de niveau de preuve scientifique
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EvidenceLevelBadge(level = plant.scientificEvidenceLevel)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Avertissement de sécurité / contre-indications condensées
            CondensedSafetyBadge(
                plant = plant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = plant.shortDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Liste des bienfaits / affections sous forme de chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                plant.ailmentsAndBenefits.take(3).forEach { benefit ->
                    SuggestionChip(
                        onClick = onClick,
                        label = {
                            Text(
                                text = benefit,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pied de carte invitant à consulter la fiche
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Voir la fiche détaillée",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
