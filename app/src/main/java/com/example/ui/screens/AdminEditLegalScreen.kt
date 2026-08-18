package com.example.ui.screens

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.model.firestore.LegalContentDocument
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.LegalViewModel

/**
 * Écran d'Administration "Éditer le contenu légal".
 * Accessible exclusivement aux utilisateurs possédant le rôle 'admin'.
 * Permet d'éditer le texte des Mentions Légales et de la Politique de Confidentialité,
 * de prévisualiser le rendu et de publier sur Firestore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditLegalScreen(
    authViewModel: AuthViewModel,
    legalViewModel: LegalViewModel,
    onNavigateBack: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val selectedDocId by legalViewModel.selectedDocId.collectAsState()
    val editableText by legalViewModel.editableText.collectAsState()
    val isPreviewMode by legalViewModel.isPreviewMode.collectAsState()
    val isSaving by legalViewModel.isSaving.collectAsState()
    val statusMessage by legalViewModel.statusMessage.collectAsState()

    val mentionsDoc by legalViewModel.mentionsContent.collectAsState()
    val privacyDoc by legalViewModel.privacyContent.collectAsState()

    var showConfirmSaveDialog by remember { mutableStateOf(false) }

    val isAuthenticatedAdmin = authState is AuthState.Authenticated && (authState as AuthState.Authenticated).isAdmin
    val currentAdminUid = (authState as? AuthState.Authenticated)?.uid ?: ""

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
                            text = "Administration - Notices Légales",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("admin_legal_back_button")
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
                .testTag("admin_legal_screen_root")
        ) {
            // Sécurité : Vérification du rôle Admin
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
                        AdminHeaderCard(email = (authState as AuthState.Authenticated).email)
                    }

                    // Sélecteur de document (Mentions Légales / Politique de Confidentialité)
                    item {
                        Text(
                            text = "Sélectionner le document à éditer :",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        DocumentSelectorTabs(
                            selectedDocId = selectedDocId,
                            onSelectDoc = { legalViewModel.selectDocument(it) }
                        )
                    }

                    // Infos de dernière modification
                    item {
                        val activeDoc = if (selectedDocId == LegalContentDocument.DOC_PRIVACY) privacyDoc else mentionsDoc
                        LastUpdatedInfoCard(activeDoc = activeDoc)
                    }

                    // Mode Édition / Mode Prévisualisation
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPreviewMode) "Aperçu en direct :" else "Éditeur de texte :",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            OutlinedButton(
                                onClick = { legalViewModel.togglePreview() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Default.Preview,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPreviewMode) "Mode Édition" else "Prévisualiser")
                            }
                        }
                    }

                    // Bannière de statut (Erreur / Succès)
                    if (statusMessage != null) {
                        item {
                            StatusMessageBanner(
                                message = statusMessage!!,
                                isError = statusMessage!!.contains("Erreur") || statusMessage!!.contains("Échec"),
                                onDismiss = { legalViewModel.clearStatusMessage() }
                            )
                        }
                    }

                    // Zone d'édition ou de prévisualisation
                    item {
                        if (isPreviewMode) {
                            PreviewLegalContentCard(
                                title = if (selectedDocId == LegalContentDocument.DOC_PRIVACY) "Politique de Confidentialité (Aperçu)" else "Mentions Légales (Aperçu)",
                                text = editableText
                            )
                        } else {
                            OutlinedTextField(
                                value = editableText,
                                onValueChange = { legalViewModel.onTextChange(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                                    .testTag("admin_legal_text_input"),
                                label = { Text("Texte du document (Formatez avec paragraphes)") },
                                placeholder = { Text("Saisissez le contenu juridique...") },
                                shape = RoundedCornerShape(16.dp),
                                minLines = 10
                            )
                        }
                    }

                    // Bouton de sauvegarde
                    item {
                        Button(
                            onClick = { showConfirmSaveDialog = true },
                            enabled = !isSaving && editableText.isNotBlank(),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("admin_save_legal_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Publication sur Firestore en cours...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Publier sur Firestore & Synchroniser", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Dialog de confirmation de sauvegarde
    if (showConfirmSaveDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmSaveDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Confirmer la publication ?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Cette action mettra à jour le document '${if (selectedDocId == LegalContentDocument.DOC_PRIVACY) "Politique de Confidentialité" else "Mentions Légales"}' directement sur Firebase Firestore.\n\n" +
                            "Le nouveau texte sera immédiatement visible par l'ensemble des utilisateurs de l'application PlantCare.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmSaveDialog = false
                        legalViewModel.saveContent(currentAdminUid)
                    }
                ) {
                    Text("Confirmer & Publier")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSaveDialog = false }) {
                    Text("Annuler")
                }
            }
        )
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
                text = "Vous ne disposez pas des privilèges 'admin' requis pour modifier le contenu légal de l'application. Seuls les comptes configurés avec le rôle administrateur dans Firebase Firestore peuvent accéder à cette fonction.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun AdminHeaderCard(email: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Session Administrateur Active",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Connecté en tant que : $email",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun DocumentSelectorTabs(
    selectedDocId: String,
    onSelectDoc: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        TabRow(
            selectedTabIndex = if (selectedDocId == LegalContentDocument.DOC_PRIVACY) 1 else 0,
            containerColor = Color.Transparent
        ) {
            Tab(
                selected = selectedDocId == LegalContentDocument.DOC_MENTIONS,
                onClick = { onSelectDoc(LegalContentDocument.DOC_MENTIONS) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mentions Légales", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedDocId == LegalContentDocument.DOC_PRIVACY,
                onClick = { onSelectDoc(LegalContentDocument.DOC_PRIVACY) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Politique Confidentialité", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
private fun LastUpdatedInfoCard(activeDoc: LegalContentDocument) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val formattedDate = remember(activeDoc.lastUpdated) {
        dateFormat.format(Date(activeDoc.lastUpdated))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Dernière mise à jour enregistrée :",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• Date : $formattedDate\n• Modifié par (UID) : ${activeDoc.updatedByUid.ifBlank { "system_default" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PreviewLegalContentCard(title: String, text: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = text.ifBlank { "(Aucun contenu à prévisualiser)" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
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
