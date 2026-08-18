package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Composant de chargement d'image pour les cartes et fiches plantes.
 *
 * - Utilise Coil pour charger [imageUrl] avec mise en cache disque.
 * - En cas d'échec ou d'URL vide, bascule vers l'illustration vectorielle/placeholder d'origine.
 * - Affiche une attribution CC discrète ("Photo : [auteur] — [licence]") obligatoire.
 */
@Composable
fun PlantImage(
    imageUrl: String?,
    imageAuthor: String? = null,
    imageLicense: String? = null,
    plantName: String = "",
    colorHex: Long = 0xFF3E8E5A,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showAttribution: Boolean = true,
    testTag: String = "plant_image"
) {
    val context = LocalContext.current
    val hasCompleteAttribution = !imageUrl.isNullOrBlank() && !imageAuthor.isNullOrBlank() && !imageLicense.isNullOrBlank()

    Box(
        modifier = modifier
            .background(Color(colorHex))
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (hasCompleteAttribution) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Photographie de $plantName",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                error = {
                    // Fallback vers l'illustration placeholder originale
                    PlantFallbackPlaceholder(plantName = plantName, colorHex = colorHex)
                }
            )

            // Superposition d'attribution légale Creative Commons obligatoire (seulement si requise et métadonnées exactes présentes)
            if (showAttribution) {
                val attributionText = "Photo : $imageAuthor — $imageLicense"

                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = attributionText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                            .testTag("plant_image_attribution")
                    )
                }
            }
        } else {
            // Attribution incomplète ou URL manquante -> illustration placeholder d'origine
            PlantFallbackPlaceholder(plantName = plantName, colorHex = colorHex)
        }
    }
}

@Composable
private fun PlantFallbackPlaceholder(
    plantName: String,
    colorHex: Long
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(colorHex)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocalFlorist,
            contentDescription = "Illustration de $plantName",
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(54.dp)
        )
    }
}
