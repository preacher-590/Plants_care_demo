package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Lock
import com.example.model.Plant
import com.example.model.firestore.ScientificEvidenceLevel
import com.example.model.firestore.VerificationStatus
import com.example.ui.components.EvidenceLevelBadge
import com.example.ui.components.FavoriteButton
import com.example.ui.components.PlantImage
import com.example.ui.components.SafetyInfoSection
import com.example.viewmodel.FavoritesViewModel

/**
 * Écran de détail affichant les informations complètes sur une plante sélectionnée.
 * Intègre nativement le disclaimer santé légal, le statut de vérification médicale,
 * les niveaux de preuve scientifique, contre-indications et sources bibliographiques.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    plant: Plant?,
    favoritesViewModel: FavoritesViewModel? = null,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: (() -> Unit)? = null
) {
    var showAuthRequiredDialog by remember { mutableStateOf(false) }
    val isFavorite = if (plant != null && favoritesViewModel != null) {
        val favIds by favoritesViewModel.favoritePlantIds.collectAsState()
        favIds.contains(plant.id)
    } else false

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = plant?.name ?: "Détail de la plante",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    if (plant != null && favoritesViewModel != null) {
                        FavoriteButton(
                            isFavorite = isFavorite,
                            onToggle = {
                                favoritesViewModel.toggleFavorite(
                                    plantId = plant.id,
                                    plantName = plant.name,
                                    isCurrentlyFavorite = isFavorite,
                                    onUnauthenticated = { showAuthRequiredDialog = true }
                                )
                            },
                            plantName = plant.name,
                            isCompact = false,
                            testTag = "detail_favorite_button"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (plant == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Plante introuvable.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("detail_screen_root"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Disclaimer Santé Légal / Avertissement Médical OBLIGATOIRE
                item {
                    HealthDisclaimerCard()
                }

                // Image réelle Wikimedia avec attribution CC et fallback
                item {
                    PlantImage(
                        imageUrl = plant.imageUrl,
                        imageAuthor = plant.imageAuthor,
                        imageLicense = plant.imageLicense,
                        plantName = plant.name,
                        colorHex = plant.colorHex,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        showAttribution = true,
                        testTag = "detail_plant_image"
                    )
                }

                // Titre, Nom Scientifique et Badges de Statut
                item {
                    Column {
                        Text(
                            text = plant.name,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = plant.scientificName,
                            style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        // Badges de vérification médicale & preuve scientifique
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VerificationStatusChip(status = plant.verificationStatus)
                            EvidenceLevelBadge(level = plant.scientificEvidenceLevel, isCompact = false)
                        }
                    }
                }

                // Section Sécurité & Précautions (OBLIGATOIREMENT placée AVANT la description des usages)
                item {
                    SafetyInfoSection(plant = plant)
                }

                // Section 1: Vertus & Maux Traités
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Usages Traditionnels & Maux Traités",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                plant.ailmentsAndBenefits.forEach { ailment ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(ailment, fontSize = 12.sp) },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Guide d'Entretien Botanique
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Conseils d'Entretien Botanique",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            CareInfoRow(
                                icon = Icons.Default.WaterDrop,
                                title = "Arrosage",
                                description = plant.careInstructions.watering,
                                tint = Color(0xFF1E88E5)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            CareInfoRow(
                                icon = Icons.Default.WbSunny,
                                title = "Exposition Solaire",
                                description = plant.careInstructions.sunlight,
                                tint = Color(0xFFF57C00)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            CareInfoRow(
                                icon = Icons.Default.Speed,
                                title = "Niveau de difficulté",
                                description = plant.careInstructions.difficulty,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Section 4: Description & Propriétés Botaniques
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Description & Propriétés Botaniques",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = plant.fullDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                // Section 5: Sources Bibliographiques
                if (plant.sources.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Book,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sources & Références Médicales",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                plant.sources.forEach { source ->
                                    Text(
                                        text = "📚 $source",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
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
                        onNavigateToLogin?.invoke()
                    },
                    modifier = Modifier.testTag("detail_dialog_login_button")
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
 * Bannière d'avertissement légal et médical omniprésente en haut des fiches plantes.
 */
@Composable
private fun HealthDisclaimerCard() {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("health_disclaimer_card")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.HealthAndSafety,
                contentDescription = "Avertissement santé",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Avertissement Santé / Phytothérapie",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Les informations fournies ne remplacent pas un avis médical. Le contenu n'a pas encore été intégralement validé par un professionnel de santé.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/**
 * Badge affichant le statut de vérification médicale de la fiche plante.
 */
@Composable
private fun VerificationStatusChip(status: VerificationStatus) {
    val (backgroundColor, textColor, icon, label) = when (status) {
        VerificationStatus.VERIFIED_BY_PROFESSIONAL -> Quadruple(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            Icons.Default.CheckCircle,
            "Vérifié par un professionnel"
        )
        VerificationStatus.UNDER_REVIEW -> Quadruple(
            Color(0xFFFFF3E0),
            Color(0xFFE65100),
            Icons.Default.Info,
            "En cours de révision"
        )
        VerificationStatus.UNVERIFIED -> Quadruple(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            Icons.Default.HealthAndSafety,
            "Non vérifié par un professionnel"
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        modifier = Modifier.testTag("verification_status_chip_${status.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}

@Composable
private fun CareInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = tint.copy(alpha = 0.12f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
