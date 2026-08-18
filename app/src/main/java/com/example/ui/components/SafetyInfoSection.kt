package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Plant
import com.example.model.firestore.VerificationStatus

/**
 * Palette de couleurs M3 personnalisée pour la sécurité santé.
 * Utilise des nuances Ambre / Terracotta pour être bien visible et distinguer 
 * les avertissements de sécurité des erreurs système cliquables Android (rouge vif).
 */
private val SafetyWarningBackground = Color(0xFFFFF8E1) // Fond Ambre clair doux
private val SafetyWarningBorder = Color(0xFFFFB74D) // Bordure Ambre / Ochre
private val SafetyHeaderTitleColor = Color(0xFFD84315) // Terracotta intense pour le titre
private val SafetyIconColor = Color(0xFFE65100) // Ambre foncé chaud pour l'icône

/**
 * Composant complet de sécurité médicale "SafetyInfoSection" affiché dans la fiche de détail plante.
 * 
 * - Placé immédiatement après le statut de vérification médicale.
 * - Sous-section "Contre-indications" TOUJOURS visible à 100% (jamais repliée).
 * - Sous-section "Interactions médicamenteuses" repliable via accordéon si > 4 éléments.
 * - Gestion explicite des listes vides avec mention "(information non exhaustive, en cours de complétion)".
 */
@Composable
fun SafetyInfoSection(
    plant: Plant,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SafetyWarningBackground
        ),
        border = BorderStroke(1.5.dp, SafetyWarningBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("safety_info_section")
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            // Entête principal de la section de sécurité
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SafetyHeaderTitleColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = "Alerte Sécurité Plante",
                        tint = SafetyHeaderTitleColor,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sécurité & Précautions d'Emploi",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SafetyHeaderTitleColor
                    )
                    Text(
                        text = "Informations critiques à lire avant toute utilisation",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = SafetyWarningBorder.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            // ----------------------------------------------------------------
            // Sous-section 1 : CONTRE-INDICATIONS (Jamais repliée)
            // ----------------------------------------------------------------
            Column(modifier = Modifier.testTag("safety_contraindications_subsection")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = SafetyIconColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Contre-indications",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF3E2723)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (plant.contraindications.isEmpty()) {
                    // Liste vide : message explicite obligatoire + mention non exhaustive
                    EmptySafetyListMessage(
                        message = "Aucune contre-indication connue à ce jour.",
                        isVerified = plant.verificationStatus == VerificationStatus.VERIFIED_BY_PROFESSIONAL
                    )
                } else {
                    plant.contraindications.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SafetyIconColor
                                )
                            )
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF212121)
                                ),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = SafetyWarningBorder.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(14.dp))

            // ----------------------------------------------------------------
            // Sous-section 2 : INTERACTIONS MÉDICAMENTEUSES (Accordéon si > 4)
            // ----------------------------------------------------------------
            Column(modifier = Modifier.testTag("safety_interactions_subsection")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = SafetyHeaderTitleColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Interactions médicamenteuses connues",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF3E2723)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (plant.drugInteractions.isEmpty()) {
                    // Liste vide : message explicite obligatoire + mention non exhaustive
                    EmptySafetyListMessage(
                        message = "Aucune interaction médicamenteuse connue à ce jour.",
                        isVerified = plant.verificationStatus == VerificationStatus.VERIFIED_BY_PROFESSIONAL
                    )
                } else {
                    val needsAccordion = plant.drugInteractions.size > 4
                    var isExpanded by remember { mutableStateOf(false) }

                    val visibleInteractions = if (needsAccordion && !isExpanded) {
                        plant.drugInteractions.take(4)
                    } else {
                        plant.drugInteractions
                    }

                    visibleInteractions.forEach { interaction ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "💊 ",
                                fontSize = 12.sp
                            )
                            Text(
                                text = interaction,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF212121)
                                ),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Bouton accordéon uniquement si > 4 éléments
                    if (needsAccordion) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("toggle_interactions_accordion")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isExpanded) {
                                        "Réduire les interactions"
                                    } else {
                                        "Voir les ${plant.drugInteractions.size - 4} autres interactions"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SafetyHeaderTitleColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = SafetyHeaderTitleColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Message affiché lorsque la liste de contre-indications ou d'interactions est vide.
 */
@Composable
private fun EmptySafetyListMessage(
    message: String,
    isVerified: Boolean
) {
    Column(modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CheckCircleOutline,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2E7D32)
                )
            )
        }
        if (!isVerified) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "(information non exhaustive, en cours de complétion)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(start = 22.dp)
            )
        }
    }
}

/**
 * Version condensée du composant de sécurité (CondensedSafetyBadge) destinée aux cartes 
 * de résultat de recherche par symptôme et cartes de bibliothèque.
 * 
 * Permet à l'utilisateur de repérer immédiatement les alertes de contre-indications 
 * avant même de cliquer pour ouvrir la fiche complète.
 */
@Composable
fun CondensedSafetyBadge(
    plant: Plant,
    modifier: Modifier = Modifier
) {
    val hasContraindications = plant.contraindications.isNotEmpty()

    val backgroundColor = if (hasContraindications) {
        Color(0xFFFFF3E0) // Ambre clair
    } else {
        Color(0xFFE8F5E9) // Vert très clair
    }

    val contentColor = if (hasContraindications) {
        Color(0xFFE65100) // Orange/Ambre foncé
    } else {
        Color(0xFF2E7D32) // Vert foncé
    }

    val icon = if (hasContraindications) {
        Icons.Default.ReportProblem
    } else {
        Icons.Default.CheckCircleOutline
    }

    val labelText = if (hasContraindications) {
        "Contre-indiqué : ${plant.contraindications.first()}"
    } else {
        "Pas de contre-indication majeure connue"
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = BorderStroke(0.8.dp, contentColor.copy(alpha = 0.4f)),
        modifier = modifier.testTag("condensed_safety_badge_${plant.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Alerte contre-indication",
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
