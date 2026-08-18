package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Bouton de Favori réutilisable supportant la mise à jour optimiste et la réconciliation.
 * 
 * @param isFavorite État réel courant fourni par le ViewModel / StateFlow global.
 * @param onToggle Action appelée lors du clic utilisateur.
 * @param plantName Nom de la plante pour accessibilité et retours.
 * @param isCompact Format compact pour intégration dans les cartes de liste.
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    plantName: String = "",
    isCompact: Boolean = false,
    testTag: String = "favorite_button"
) {
    var isPressedAnimation by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "favorite_scale_animation"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        label = "favorite_tint_animation"
    )

    if (isCompact) {
        IconButton(
            onClick = {
                onToggle()
            },
            modifier = modifier
                .size(36.dp)
                .scale(scale)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Retirer $plantName des favoris" else "Ajouter $plantName aux favoris",
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
    } else {
        FilledTonalIconButton(
            onClick = {
                onToggle()
            },
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isFavorite) Color(0xFFFCE4EC) else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = modifier
                .size(44.dp)
                .scale(scale)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Retirer $plantName des favoris" else "Ajouter $plantName aux favoris",
                tint = if (isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
