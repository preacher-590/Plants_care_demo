package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthState
import com.example.model.firestore.PlantDocument
import com.example.ui.components.PlantImage
import com.example.viewmodel.AdminImageUiState
import com.example.viewmodel.AdminImageViewModel
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.WikimediaSearchState

/**
 * Écran d'administration permettant la sélection et l'attribution supervisée 
 * d'images Wikimedia Commons pour les fiches plantes de la base de données.
 * Accessible exclusivement aux administrateurs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminImageScreen(
    authViewModel: AuthViewModel,
    adminImageViewModel: AdminImageViewModel,
    onNavigateBack: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val uiState by adminImageViewModel.uiState.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }

    val isAuthenticatedAdmin = authState is AuthState.Authenticated && (authState as AuthState.Authenticated).isAdmin

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin - Photos Wikimedia",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("admin_image_back_button")
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
                .padding(horizontal = 16.dp)
                .testTag("admin_image_screen_root")
        ) {
            if (!isAuthenticatedAdmin) {
                Spacer(modifier = Modifier.height(24.dp))
                AdminAccessDeniedCard()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Sélectionner une plante à illustrer :",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PlantSelectionRow(
                            plants = uiState.plants,
                            selectedPlant = uiState.selectedPlant,
                            onSelectPlant = { adminImageViewModel.selectPlant(it) }
                        )
                    }

                    // Statut message (succès/erreur)
                    if (uiState.statusMessage != null) {
                        item {
                            StatusMessageBanner(
                                message = uiState.statusMessage!!,
                                isError = uiState.statusMessage!!.contains("Erreur") || uiState.statusMessage!!.contains("Échec"),
                                onDismiss = { adminImageViewModel.clearStatusMessage() }
                            )
                        }
                    }

                    if (uiState.selectedPlant != null) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Plante sélectionnée : ${uiState.selectedPlant!!.commonName} (${uiState.selectedPlant!!.scientificName})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (uiState.selectedPlant!!.imageUrl.isNotBlank()) {
                                        Text(
                                            text = "Image actuelle enregistrée :",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp)
                                        ) {
                                            PlantImage(
                                                imageUrl = uiState.selectedPlant!!.imageUrl,
                                                imageAuthor = uiState.selectedPlant!!.imageAuthor,
                                                imageLicense = uiState.selectedPlant!!.imageLicense,
                                                plantName = uiState.selectedPlant!!.commonName,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    Text(
                                        text = "2. Recherche sur Wikimedia Commons API :",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = uiState.searchQuery,
                                            onValueChange = { adminImageViewModel.onSearchQueryChanged(it) },
                                            label = { Text("Mot-clé de recherche (Nom scientifique)") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { adminImageViewModel.searchWikimediaImages() },
                                            modifier = Modifier.size(52.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Rechercher",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "3. Candidats d'images avec métadonnées d'attribution :",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        when (val state = uiState.searchState) {
                            is WikimediaSearchState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Interrogation de l'API Wikimedia Commons...",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                            is WikimediaSearchState.Error -> {
                                item {
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                            is WikimediaSearchState.Success -> {
                                items(state.candidates) { candidate ->
                                    val isSelected = uiState.selectedCandidate?.thumbnailUrl == candidate.thumbnailUrl
                                    WikimediaCandidateCard(
                                        candidate = candidate,
                                        isSelected = isSelected,
                                        onSelect = { adminImageViewModel.selectCandidate(candidate) }
                                    )
                                }
                            }
                            is WikimediaSearchState.Idle -> {
                                item {
                                    Text(
                                        text = "Lancez une recherche pour afficher les propositions Wikimedia Commons.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showConfirmDialog = true },
                                enabled = uiState.selectedCandidate != null && !uiState.isSaving,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("admin_save_image_button")
                            ) {
                                if (uiState.isSaving) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Enregistrement Firestore...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Valider et Publier l'Image Sélectionnée", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (showConfirmDialog && uiState.selectedCandidate != null && uiState.selectedPlant != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("Confirmer l'attribution d'image ?")
            },
            text = {
                Text(
                    "Vous allez associer cette photo à la plante '${uiState.selectedPlant!!.commonName}'.\n\n" +
                            "• Auteur : ${uiState.selectedCandidate!!.author}\n" +
                            "• Licence : ${uiState.selectedCandidate!!.license}\n\n" +
                            "L'attribution sera affichée sous l'image dans toutes les vues de l'application."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        adminImageViewModel.saveSelectedImage()
                    }
                ) {
                    Text("Confirmer & Publier")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun PlantSelectionRow(
    plants: List<PlantDocument>,
    selectedPlant: PlantDocument?,
    onSelectPlant: (PlantDocument) -> Unit
) {
    if (plants.isEmpty()) {
        Text(
            text = "Chargement des plantes de la base...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(plants) { plant ->
                val isSelected = selectedPlant?.id == plant.id
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.clickable { onSelectPlant(plant) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = plant.commonName.ifBlank { plant.scientificName },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WikimediaCandidateCard(
    candidate: com.example.network.wikimedia.WikimediaImageCandidate,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .padding(end = 12.dp)
            ) {
                PlantImage(
                    imageUrl = candidate.thumbnailUrl,
                    imageAuthor = candidate.author,
                    imageLicense = candidate.license,
                    plantName = candidate.title,
                    modifier = Modifier.fillMaxSize(),
                    showAttribution = false
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Auteur : ${candidate.author}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Licence : ${candidate.license}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun AdminAccessDeniedCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Accès Restreint - Administrateur uniquement",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Vous devez posséder le rôle 'admin' dans Firebase Firestore pour accéder à l'outil de sélection des images Wikimedia Commons.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun StatusMessageBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}
