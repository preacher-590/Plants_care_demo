package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.firestore.ScientificEvidenceLevel

/**
 * Spécification graphique et sémantique pour chaque niveau de preuve scientifique.
 * Conçu pour respecter Material Design 3 tout en évitant toute confusion visuelle
 * avec les badges de statut de vérification éditoriale (Vérifié / En révision / Non vérifié).
 */
private data class EvidenceBadgeStyle(
    val backgroundColor: Color,
    val borderColor: Color,
    val contentColor: Color,
    val icon: ImageVector,
    val fullLabel: String,
    val compactLabel: String
)

private fun getEvidenceBadgeStyle(level: ScientificEvidenceLevel): EvidenceBadgeStyle {
    return when (level) {
        ScientificEvidenceLevel.CLINICALLY_STUDIED -> EvidenceBadgeStyle(
            backgroundColor = Color(0xFFE0F2FE), // Bleu ciel doux (Sky 100)
            borderColor = Color(0xFF7DD3FC),     // Bordure bleue claire (Sky 300)
            contentColor = Color(0xFF0369A1),    // Bleu océan profond (Sky 700)
            icon = Icons.Default.Science,
            fullLabel = "Étudié cliniquement",
            compactLabel = "Étudié cliniquement"
        )
        ScientificEvidenceLevel.TRADITIONAL_USE -> EvidenceBadgeStyle(
            backgroundColor = Color(0xFFF1F5F9), // Ardoise / Gris neutre clair (Slate 100)
            borderColor = Color(0xFFCBD5E1),     // Bordure ardoise douce (Slate 300)
            contentColor = Color(0xFF334155),    // Ardoise foncée (Slate 700)
            icon = Icons.Default.HistoryEdu,
            fullLabel = "Usage traditionnel documenté",
            compactLabel = "Usage traditionnel"
        )
    }
}

/**
 * Composant de badge de niveau de preuve scientifique réutilisable.
 *
 * @param level Le niveau de preuve scientifique de la plante.
 * @param modifier Modificateurs de disposition Compose.
 * @param isCompact Si true, affiche la version compacte adaptée aux cartes de recherche/scan.
 * @param showInfoButton Si true, affiche un bouton d'information permettant d'ouvrir la modale explicative.
 * @param testTag Tag pour les tests d'UI automatisés.
 */
@Composable
fun EvidenceLevelBadge(
    level: ScientificEvidenceLevel,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    showInfoButton: Boolean = true,
    testTag: String = if (isCompact) "evidence_level_badge_compact" else "evidence_level_badge_full"
) {
    var showDialog by remember { mutableStateOf(false) }
    val style = getEvidenceBadgeStyle(level)

    if (isCompact) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = style.backgroundColor,
            border = BorderStroke(1.dp, style.borderColor),
            modifier = modifier
                .testTag("${testTag}_${level.name}")
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Voir les détails sur le niveau de preuve"
                ) {
                    showDialog = true
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.contentColor,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = style.compactLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = style.contentColor
                )
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = style.backgroundColor,
            border = BorderStroke(1.dp, style.borderColor),
            modifier = modifier
                .testTag("${testTag}_${level.name}")
                .clip(RoundedCornerShape(10.dp))
                .clickable(
                    role = Role.Button,
                    onClickLabel = "En savoir plus sur le niveau de preuve"
                ) {
                    showDialog = true
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = style.fullLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = style.contentColor
                )
                if (showInfoButton) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = style.contentColor.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(15.dp)
                            .testTag("evidence_level_info_icon")
                    )
                }
            }
        }
    }

    if (showDialog) {
        EvidenceLevelInfoDialog(
            currentLevel = level,
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Boîte de dialogue d'explication accessible détaillant les différents niveaux de preuve scientifique.
 * Clarifie la différence fondamentale entre usage traditionnel et études cliniques,
 * tout en rappelant qu'une étude clinique ne constitue pas une certitude médicale.
 */
@Composable
fun EvidenceLevelInfoDialog(
    currentLevel: ScientificEvidenceLevel? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("evidence_level_info_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Niveau de preuve de l'usage",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Comprendre les sources scientifiques",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avertissement de distinction : Fiabilité éditoriale vs Preuve scientifique
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Ce badge indique la nature des données associées à la plante. Il est distinct du statut de relecture éditoriale (Vérifié par un pro).",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                // Section 1 : Étudié cliniquement
                EvidenceLevelExplanationItem(
                    level = ScientificEvidenceLevel.CLINICALLY_STUDIED,
                    title = "Étudié cliniquement",
                    description = "Des études scientifiques ou essais cliniques existent sur cette plante ou ses principes actifs.",
                    nuance = "Important : ces recherches documentent des mécanismes ou des effets observés, mais ne constituent en aucun cas une garantie d'efficacité médicale ni un traitement sans avis professionnel.",
                    isCurrent = currentLevel == ScientificEvidenceLevel.CLINICALLY_STUDIED
                )

                // Section 2 : Usage traditionnel documenté
                EvidenceLevelExplanationItem(
                    level = ScientificEvidenceLevel.TRADITIONAL_USE,
                    title = "Usage traditionnel documenté",
                    description = "L'usage repose sur des traditions populaires, des pharmacopées historiques ou des herbiers anciens répertoriés.",
                    nuance = "Important : ces usages séculaires n'ont pas nécessairement été validés par des protocoles d'essais cliniques modernes contrôlés.",
                    isCurrent = currentLevel == ScientificEvidenceLevel.TRADITIONAL_USE
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("evidence_level_info_dialog_close")
            ) {
                Text("J'ai compris")
            }
        }
    )
}

/**
 * Élément descriptif individuel d'un niveau de preuve dans la boîte de dialogue d'explication.
 */
@Composable
private fun EvidenceLevelExplanationItem(
    level: ScientificEvidenceLevel,
    title: String,
    description: String,
    nuance: String,
    isCurrent: Boolean
) {
    val style = getEvidenceBadgeStyle(level)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) style.backgroundColor else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isCurrent) 1.5.dp else 1.dp,
            color = if (isCurrent) style.borderColor else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = style.contentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = null,
                            tint = style.contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = style.contentColor
                )

                if (isCurrent) {
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = style.contentColor
                    ) {
                        Text(
                            text = "Actuel",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = nuance,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
