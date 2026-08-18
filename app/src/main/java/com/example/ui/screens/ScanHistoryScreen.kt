package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.model.firestore.ScanHistoryDocument
import com.example.ui.components.PlantImage
import com.example.viewmodel.ScanHistoryUiState
import com.example.viewmodel.ScanHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Écran d'Historique des Scans de Plantes.
 * Affiche la chronologie des reconnaissances effectuées par l'utilisateur connecté.
 * Offre la consultation des fiches associées, la suppression unitaire et la purge complète (RGPD).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryScreen(
    viewModel: ScanHistoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlantDetail: (String) -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showClearAllConfirmationDialog by remember { mutableStateOf(false) }
    var scanToDelete by remember { mutableStateOf<ScanHistoryDocument?>(null) }
    var unindexedScanDialog by remember { mutableStateOf<ScanHistoryDocument?>(null) }

    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Historique des Scans",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    if (uiState is ScanHistoryUiState.Success) {
                        IconButton(
                            onClick = { showClearAllConfirmationDialog = true },
                            modifier = Modifier.testTag("history_clear_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Effacer tout l'historique",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
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
                is ScanHistoryUiState.Unauthenticated -> {
                    UnauthenticatedHistoryView(
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToScan = onNavigateToScan
                    )
                }

                is ScanHistoryUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("history_loading_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chargement de votre historique...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is ScanHistoryUiState.Empty -> {
                    EmptyHistoryView(onNavigateToScan = onNavigateToScan)
                }

                is ScanHistoryUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .testTag("history_error_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Erreur de chargement",
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

                is ScanHistoryUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("history_list_view"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            HistoryPrivacyNoticeCard()
                        }

                        items(
                            items = state.scans,
                            key = { it.id }
                        ) { scanItem ->
                            ScanHistoryItemCard(
                                scan = scanItem,
                                onClick = {
                                    if (!scanItem.plantId.isNullOrBlank()) {
                                        onNavigateToPlantDetail(scanItem.plantId)
                                    } else {
                                        unindexedScanDialog = scanItem
                                    }
                                },
                                onDelete = { scanToDelete = scanItem }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            if (isDeleting) {
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
    }

    // Dialogue de confirmation de suppression unitaire
    scanToDelete?.let { scan ->
        AlertDialog(
            onDismissRequest = { scanToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Supprimer ce scan ?")
            },
            text = {
                Text("Voulez-vous supprimer définitivement l'identification de \"${scan.commonName.ifBlank { scan.nomIdentifie }}\" de votre historique ? Cette action est irréversible.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteScanEntry(scan.id)
                        scanToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_scan_item")
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { scanToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_scan_item")
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialogue de confirmation de purge totale
    if (showClearAllConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Effacer tout l'historique ?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Tous vos scans enregistrés dans Firestore seront supprimés définitivement de manière irréversible, conformément à votre droit à l'effacement RGPD.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllConfirmationDialog = false
                        viewModel.clearAllHistory()
                    },
                    modifier = Modifier.testTag("confirm_clear_all_history")
                ) {
                    Text("Tout effacer", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearAllConfirmationDialog = false },
                    modifier = Modifier.testTag("cancel_clear_all_history")
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialogue d'information pour une plante identifiée sans fiche médicale associée
    unindexedScanDialog?.let { scan ->
        val dateFormat = SimpleDateFormat("dd MMMM yyyy 'à' HH:mm", Locale.FRENCH)
        val formattedDate = dateFormat.format(Date(scan.dateScan))

        AlertDialog(
            onDismissRequest = { unindexedScanDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = scan.commonName.ifBlank { scan.nomIdentifie },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = scan.nomIdentifie,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Score de certitude taxonomique : ${scan.scoreConfiance}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Date du scan : $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Text(
                            text = "Cette espèce a été reconnue par Pl@ntNet mais ne dispose pas d'une fiche thérapeutique validée dans le catalogue PlantCare.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { unindexedScanDialog = null },
                    modifier = Modifier.testTag("close_unindexed_scan_dialog")
                ) {
                    Text("Fermer", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * Carte de notice de confidentialité RGPD pour l'historique.
 */
@Composable
private fun HistoryPrivacyNoticeCard() {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Historique strictement privé",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Vos scans sont enregistrés uniquement sur votre compte Firestore sécurisé. Aucun autre utilisateur ou administrateur n'y a accès.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Carte représentant une entrée unique d'historique de scan.
 */
@Composable
private fun ScanHistoryItemCard(
    scan: ScanHistoryDocument,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.FRENCH) }
    val formattedDate = remember(scan.dateScan) { dateFormat.format(Date(scan.dateScan)) }

    val hasLinkedDetail = !scan.plantId.isNullOrBlank()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("history_item_${scan.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vignette de la plante
            PlantImage(
                imageUrl = scan.imageThumbnailUrl,
                plantName = scan.nomIdentifie,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
                showAttribution = false,
                testTag = "history_thumb_${scan.id}"
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Informations du scan
            Column(modifier = Modifier.weight(1f)) {
                // Nom vernaculaire / identifié
                Text(
                    text = scan.commonName.ifBlank { scan.nomIdentifie },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Nom scientifique en italique
                Text(
                    text = scan.nomIdentifie,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Date et Score de confiance
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Badge Score
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            scan.scoreConfiance >= 70 -> Color(0xFFE8F5E9)
                            scan.scoreConfiance >= 40 -> Color(0xFFFFF8E1)
                            else -> Color(0xFFFFEBEE)
                        }
                    ) {
                        Text(
                            text = "${scan.scoreConfiance}% certitude",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = when {
                                scan.scoreConfiance >= 70 -> Color(0xFF1B5E20)
                                scan.scoreConfiance >= 40 -> Color(0xFFF57F17)
                                else -> Color(0xFFC62828)
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (hasLinkedDetail) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "Fiche BDD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }

            // Bouton de suppression
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_scan_item_${scan.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer ce scan",
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Vue affichée lorsque l'utilisateur n'est pas connecté.
 */
@Composable
private fun UnauthenticatedHistoryView(
    onNavigateToLogin: () -> Unit,
    onNavigateToScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("history_unauthenticated_view"),
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
            text = "Historique réservé aux comptes connectés",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Vous pouvez utiliser la reconnaissance de plantes sans compte. Cependant, pour retrouver automatiquement vos scans précédents, vous devez vous connecter.",
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
                .testTag("history_login_button")
        ) {
            Icon(imageVector = Icons.Default.Login, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Se connecter / Créer un compte", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNavigateToScan,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("history_scan_now_button")
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scanner sans compte")
        }
    }
}

/**
 * Vue affichée lorsque l'historique est vide.
 */
@Composable
private fun EmptyHistoryView(
    onNavigateToScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("history_empty_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Spa,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Aucun scan enregistré",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Vos prochaines identifications botaniques apparaîtront ici automatiquement.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNavigateToScan,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
                .testTag("history_start_first_scan_button")
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scanner une plante", fontWeight = FontWeight.Bold)
        }
    }
}
