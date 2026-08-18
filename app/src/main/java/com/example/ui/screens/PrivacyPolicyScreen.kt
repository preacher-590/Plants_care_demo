package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactSupport
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.LegalViewModel

/**
 * Écran de la Politique de Confidentialité de l'application PlantCare.
 * Synchronisé dynamiquement avec Firestore / Room via LegalViewModel.
 * Conforme au RGPD (Règlement Général sur la Protection des Données - UE 2016/679).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    legalViewModel: LegalViewModel? = null,
    onNavigateBack: () -> Unit
) {
    val privacyDocState = legalViewModel?.privacyContent?.collectAsState()
    val dynamicContent = privacyDocState?.value?.content

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Politique de Confidentialité",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("privacy_policy_back_button")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("privacy_policy_screen_column"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                PrivacyHeaderCard()
            }

            if (!dynamicContent.isNullOrBlank()) {
                item {
                    PrivacyDynamicContentCard(content = dynamicContent)
                }
            } else {
                // Section 1: Données collectées
                item {
                    PrivacySectionCard(
                        title = "1. Données personnelles collectées",
                        icon = Icons.Default.Cookie,
                        iconTint = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "L'application PlantCare collecte uniquement les données nécessaires au fonctionnement du compte utilisateur :",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Adresse email personnelle\n" +
                                    "• Méthode d'authentification (Email/Mot de passe ou Google Sign-In)\n" +
                                    "• Identifiant unique Firebase (UID)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Section 2: Finalité du traitement
                item {
                    PrivacySectionCard(
                        title = "2. Finalités du traitement",
                        icon = Icons.Default.Share,
                        iconTint = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = "Vos données sont strictement réservées aux usages suivants : gestion du compte, authentification sécurisée, attribution du rôle utilisateur/administrateur, et préparation des fonctionnalités personnelles futures (favoris et historique de scan).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Section 3: Sous-traitants & Hébergement
                item {
                    PrivacySectionCard(
                        title = "3. Sous-traitants & Hébergement Cloud",
                        icon = Icons.Default.Update,
                        iconTint = MaterialTheme.colorScheme.tertiary
                    ) {
                        Text(
                            text = "L'hébergement et la gestion des comptes sont assurés par Google Cloud / Firebase (Google Ireland Limited). Vos données sont protégées conformément aux normes européennes RGPD.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Section 4: Droits RGPD & Effacement
                item {
                    PrivacySectionCard(
                        title = "4. Vos Droits RGPD & Droit à l'effacement",
                        icon = Icons.Default.Lock,
                        iconTint = Color(0xFF1565C0)
                    ) {
                        Text(
                            text = "Vous disposez des droits d'accès, de rectification et d'effacement de vos données. L'effacement s'exerce de manière autonome et immédiate via le bouton 'Supprimer mon compte' disponible dans votre espace Profil.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Section 5: Coordonnées DPO
                item {
                    PrivacySectionCard(
                        title = "5. Contact & Délégué à la Protection des Données (DPO)",
                        icon = Icons.Default.ContactSupport,
                        iconTint = Color(0xFFD84315)
                    ) {
                        PrivacyInfoRow(label = "DPO / Responsable de traitement", value = "[À COMPLÉTER : Nom du DPO ou Nom de l'Éditeur]")
                        PrivacyInfoRow(label = "Email dédié confidentialité", value = "[À COMPLÉTER : dpo.privacy@domaine.com]")
                        PrivacyInfoRow(label = "Adresse postale DPO", value = "[À COMPLÉTER : Adresse postale du service DPO / Réclamations]")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PrivacyDynamicContentCard(content: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Politique de Confidentialité (RGPD)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PrivacyHeaderCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PrivacyTip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Protection de la Vie Privée",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Engagement RGPD : Transparence & Protection des Données Comportementales",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun PrivacySectionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
private fun PrivacyInfoRow(
    label: String,
    value: String
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (value.startsWith("[À COMPLÉTER")) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
        )
    }
}
