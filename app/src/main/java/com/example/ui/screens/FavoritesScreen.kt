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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Plant
import com.example.ui.components.CondensedSafetyBadge
import com.example.ui.components.EvidenceLevelBadge
import com.example.ui.components.FavoriteButton
import com.example.ui.components.PlantImage
import com.example.viewmodel.FavoritesUiState
import com.example.viewmodel.FavoritesViewModel
import com.example.viewmodel.PlantWithFavoriteMeta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Écran "Mes favoris" affichant la liste des plantes enregistrées par l'utilisateur connecté.
 * Gère les 5 états UI : Idle, Loading, Success, Empty, Error, ainsi que l'état non authentifié.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favoritesViewModel: FavoritesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlantDetail: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToAdvice: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by favoritesViewModel.uiState.collectAsState()
    val actionMessage by favoritesViewModel.actionMessage.collectAsState()
    val favoriteIds by favoritesViewModel.favoritePlantIds.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAuthRequiredDialog by remember { mutableStateOf(false) }

    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            favoritesViewModel.clearActionMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mes Favoris",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("favorites_back_button")
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is FavoritesUiState.Unauthenticated -> {
                    UnauthenticatedFavoritesView(
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToLibrary = onNavigateToLibrary
                    )
                }

                is FavoritesUiState.Loading, is FavoritesUiState.Idle -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("favorites_loading_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chargement de vos favoris...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is FavoritesUiState.Empty -> {
                    EmptyFavoritesView(
                        onNavigateToLibrary = onNavigateToLibrary,
                        onNavigateToAdvice = onNavigateToAdvice
                    )
                }

                is FavoritesUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .testTag("favorites_error_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Erreur de chargement des favoris",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is FavoritesUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("favorites_list_view"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            FavoritesHeaderCard(count = state.favoritePlants.size)
                        }

                        items(
                            items = state.favoritePlants,
                            key = { it.plant.id }
                        ) { itemMeta ->
                            val isFav = favoriteIds.contains(itemMeta.plant.id)
                            FavoritePlantCard(
                                plant = itemMeta.plant,
                                dateAjout = itemMeta.dateAjout,
                                isFavorite = isFav,
                                onToggleFavorite = {
                                    favoritesViewModel.toggleFavorite(
                                        plantId = itemMeta.plant.id,
                                        plantName = itemMeta.plant.name,
                                        isCurrentlyFavorite = isFav,
                                        onUnauthenticated = { showAuthRequiredDialog = true }
                                    )
                                },
                                onClick = { onNavigateToPlantDetail(itemMeta.plant.id) }
                            )
                        }
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
                        onNavigateToLogin()
                    },
                    modifier = Modifier.testTag("dialog_login_confirm_button")
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

@Composable
private fun FavoritesHeaderCard(count: Int) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFCE4EC),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "$count plante(s) dans votre carnet",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Accédez rapidement à leurs fiches d'usages et précautions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FavoritePlantCard(
    plant: Plant,
    dateAjout: Long,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("Ajouté le dd MMM yyyy", Locale.FRENCH) }
    val formattedDate = remember(dateAjout) { dateFormat.format(Date(dateAjout)) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("favorite_card_${plant.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    PlantImage(
                        imageUrl = plant.imageUrl,
                        imageAuthor = plant.imageAuthor,
                        imageLicense = plant.imageLicense,
                        plantName = plant.name,
                        colorHex = plant.colorHex,
                        modifier = Modifier.fillMaxSize(),
                        showAttribution = false,
                        testTag = "favorite_plant_image_${plant.id}"
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = plant.category,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        EvidenceLevelBadge(
                            level = plant.scientificEvidenceLevel,
                            isCompact = true,
                            testTag = "favorite_evidence_badge_${plant.id}"
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = plant.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = plant.scientificName,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FavoriteButton(
                    isFavorite = isFavorite,
                    onToggle = onToggleFavorite,
                    plantName = plant.name,
                    isCompact = true,
                    testTag = "favorite_toggle_button_${plant.id}"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = plant.shortDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Précautions / Contre-indications condensées
            CondensedSafetyBadge(plant = plant)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Voir fiche",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFavoritesView(
    onNavigateToLibrary: () -> Unit,
    onNavigateToAdvice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("favorites_empty_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFFCE4EC),
            modifier = Modifier.size(84.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Aucune plante favorite enregistrée",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Vous n'avez pas encore de plante favorite. Explorez la bibliothèque botanique ou faites une recherche par symptôme pour en enregistrer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNavigateToLibrary,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp)
                .testTag("favorites_explore_library_button")
        ) {
            Icon(imageVector = Icons.Default.MenuBook, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Explorer la bibliothèque", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onNavigateToAdvice,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp)
                .testTag("favorites_search_symptoms_button")
        ) {
            Icon(imageVector = Icons.Default.Psychology, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Rechercher par symptôme")
        }
    }
}

@Composable
private fun UnauthenticatedFavoritesView(
    onNavigateToLogin: () -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("favorites_unauthenticated_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Favoris réservés aux utilisateurs connectés",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Connectez-vous à votre compte pour sauvegarder vos plantes favorites et les synchroniser en toute sécurité sur votre profil.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNavigateToLogin,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("favorites_login_button")
        ) {
            Icon(imageVector = Icons.Default.Login, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Se connecter / Créer un compte", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNavigateToLibrary,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("favorites_browse_guest_button")
        ) {
            Icon(imageVector = Icons.Default.Spa, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Parcourir le catalogue sans compte")
        }
    }
}
