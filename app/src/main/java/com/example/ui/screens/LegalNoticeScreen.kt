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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.runtime.getValue
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
 * Écran des Mentions Légales de l'application PlantCare.
 * Synchronisé dynamiquement avec Firestore / Room via LegalViewModel.
 * Conforme LCEN.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalNoticeScreen(
    legalViewModel: LegalViewModel? = null,
    onNavigateBack: () -> Unit
) {
    val mentionsDocState = legalViewModel?.mentionsContent?.collectAsState()
    val dynamicContent = mentionsDocState?.value?.content

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mentions Légales",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("legal_notice_back_button")
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
                .testTag("legal_notice_screen_column"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Banner d'information
                LegalHeaderCard()
            }

            if (!dynamicContent.isNullOrBlank()) {
                item {
                    LegalDynamicContentCard(content = dynamicContent)
                }
            } else {
                // Section 1: Éditeur de l'application
                item {
                    LegalSectionCard(
                        title = "1. Éditeur de l'application",
                        icon = Icons.Default.Business,
                        iconTint = MaterialTheme.colorScheme.primary
                    ) {
                        LegalInfoRow(label = "Nom / Raison sociale", value = "[À COMPLÉTER : Nom ou Raison sociale du propriétaire/entreprise]")
                        LegalInfoRow(label = "Forme juridique", value = "[À COMPLÉTER : ex. SAS, SARL, Auto-entrepreneur, Association]")
                        LegalInfoRow(label = "Adresse du siège social", value = "[À COMPLÉTER : Adresse postale complète, Code postal, Ville, Pays]")
                        LegalInfoRow(label = "SIREN / SIRET", value = "[À COMPLÉTER : Numéro SIREN ou SIRET]")
                        LegalInfoRow(label = "RCS / Immatriculation", value = "[À COMPLÉTER : Ville du RCS d'immatriculation]")
                        LegalInfoRow(label = "Contact Email", value = "[À COMPLÉTER : email.de.contact@domaine.com]")
                        LegalInfoRow(label = "Téléphone", value = "[À COMPLÉTER : +33 X XX XX XX XX]")
                    }
                }

                // Section 2: Directeur de la publication
                item {
                    LegalSectionCard(
                        title = "2. Directeur de la publication",
                        icon = Icons.Default.Person,
                        iconTint = MaterialTheme.colorScheme.secondary
                    ) {
                        LegalInfoRow(label = "Nom du responsable", value = "[À COMPLÉTER : Prénom et Nom du directeur de la publication]")
                        LegalInfoRow(label = "Qualité", value = "[À COMPLÉTER : ex. Fondateur / Gérant / Représentant légal]")
                        LegalInfoRow(label = "Contact direct", value = "[À COMPLÉTER : email.publication@domaine.com]")
                    }
                }

                // Section 3: Hébergeur de l'application
                item {
                    LegalSectionCard(
                        title = "3. Hébergement des services",
                        icon = Icons.Default.Cloud,
                        iconTint = MaterialTheme.colorScheme.tertiary
                    ) {
                        LegalInfoRow(label = "Hébergeur Cloud", value = "Google Cloud Platform / Firebase (Google Ireland Limited)")
                        LegalInfoRow(label = "Adresse de l'hébergeur", value = "Gordon House, Barrow Street, Dublin 4, Irlande")
                        LegalInfoRow(label = "Contact hébergeur", value = "[À COMPLÉTER : Lien ou email support Google Cloud / Firebase]")
                    }
                }

                // Section 4: Cadre Juridique & Limitation de responsabilité
                item {
                    LegalSectionCard(
                        title = "4. Nature de l'application & Cadre Juridique",
                        icon = Icons.Default.Security,
                        iconTint = Color(0xFFD84315)
                    ) {
                        Text(
                            text = "Avertissement Médical et Botanique (Non Thérapeutique)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFD84315)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "L'application \"PlantCare\" est un outil d'information générale, pédagogique et documentaire sur la phytothérapie et la botanique. Elle ne constitue en aucun cas un dispositif médical ni une prestation de conseil médical.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Limitation de Responsabilité :",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Les informations et suggestions botaniques fournies ne remplacent en aucun cas l'avis, le diagnostic ou le traitement dispensé par un professionnel de santé qualifié.\n" +
                                    "• L'utilisateur est seul responsable de l'usage qu'il fait des informations présentées dans l'application.\n" +
                                    "• En cas d'urgence médicale ou de doute, consultez immédiatement votre médecin ou le SAMU (15).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Section 5: Propriété Intellectuelle
                item {
                    LegalSectionCard(
                        title = "5. Propriété Intellectuelle",
                        icon = Icons.Default.Gavel,
                        iconTint = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "L'ensemble des contenus (textes, graphismes, logos, icônes, illustrations, structures et code source) de l'application PlantCare est protégé par les lois françaises et internationales relatives à la propriété intellectuelle.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Toute reproduction, représentation, modification ou adaptation de tout ou partie des éléments de l'application sans l'autorisation écrite préalable de l'éditeur est strictement interdite.",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
private fun LegalDynamicContentCard(content: String) {
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
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Contenu des Mentions Légales",
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
private fun LegalHeaderCard() {
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
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Informations Légales & Réglementaires",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Conformité LCEN & Transparence Éditoriale (Mise à jour en temps réel)",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun LegalSectionCard(
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
private fun LegalInfoRow(
    label: String,
    value: String
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (value.startsWith("[À COMPLÉTER")) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
        )
    }
}
