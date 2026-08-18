package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Barre de filtres horizontale scrollable (Material 3 FilterChip) avec support de la multi-sélection.
 *
 * @param categories Liste ordonnée des catégories de symptômes disponibles.
 * @param selectedCategories Ensemble des catégories actuellement actives.
 * @param onCategoryToggled Callback déclenché lors du clic sur un chip.
 * @param onResetAll Callback pour réinitialiser le filtre à "Toutes".
 */
@Composable
fun SymptomCategoryFilterBar(
    categories: List<String>,
    selectedCategories: Set<String>,
    onCategoryToggled: (String) -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Persistance de la position de scroll horizontal lors des changements de configuration (ex : rotation d'écran)
    val scrollState = rememberSaveable(saver = ScrollState.Saver) {
        ScrollState(initial = 0)
    }
    val isAllSelected = selectedCategories.isEmpty()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("symptom_category_filter_bar"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Chip "Toutes" (sélectionné par défaut lorsque aucune catégorie spécifique n'est active)
        FilterChip(
            selected = isAllSelected,
            onClick = onResetAll,
            label = {
                Text(
                    text = "Toutes",
                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("filter_chip_all")
        )

        // Chips dynamiques pour chaque catégorie de symptôme
        categories.forEach { category ->
            val isSelected = selectedCategories.contains(category)
            FilterChip(
                selected = isSelected,
                onClick = { onCategoryToggled(category) },
                label = {
                    Text(
                        text = category,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Sélectionné",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("filter_chip_${category.lowercase().replace(" ", "_")}")
            )
        }
    }
}
