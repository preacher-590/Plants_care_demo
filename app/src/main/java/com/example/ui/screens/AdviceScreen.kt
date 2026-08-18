package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Plant
import com.example.model.PlantData
import com.example.model.PlantMatch
import com.example.model.firestore.SymptomDocument
import com.example.model.firestore.VerificationStatus
import com.example.ui.components.CondensedSafetyBadge
import com.example.ui.components.EvidenceLevelBadge
import com.example.ui.components.PlantImage
import com.example.viewmodel.AdviceState
import com.example.viewmodel.PlantViewModel

/**
 * Écran de demande de conseil en phytothérapie par saisie libre de symptôme ou d'affection.
 * Gère les 5 états UI : Idle, Loading, Success, NoResults, Error.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdviceScreen(
    viewModel: PlantViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val adviceState by viewModel.adviceState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Demander un conseil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("advice_back_button")
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
                .testTag("advice_screen_root")
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // En-tête informatif et rappel légal
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Conseils Botaniques & Usages Traditionnels",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Décrivez votre besoin. Ces suggestions à visée informative ne constituent pas un diagnostic médical.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Champ de saisie OutlinedTextField
            OutlinedTextField(
                value = adviceState.query,
                onValueChange = { viewModel.onQueryChanged(it) },
                label = { Text("Ex : mal de gorge, difficulté à dormir, stress...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (adviceState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("advice_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bouton de déclenchement de la recherche
            Button(
                onClick = { viewModel.searchAdvice() },
                enabled = adviceState.query.isNotBlank() && adviceState.state !is AdviceState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("advice_search_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rechercher des plantes", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bandeau de raccourcis rapides par symptôme
            Text(
                text = "Symptômes fréquents :",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(PlantData.popularSymptoms) { symptom ->
                    FilterChip(
                        selected = adviceState.query.equals(symptom, ignoreCase = true),
                        onClick = {
                            viewModel.onQueryChanged(symptom)
                            viewModel.searchAdvice(symptom)
                        },
                        label = { Text(symptom, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Gestion dynamique des 5 états UI (Idle, Loading, Success, NoResults, Error)
            when (val state = adviceState.state) {
                is AdviceState.Idle -> {
                    AdviceIdleView(
                        onSelectSymptom = { symptom ->
                            viewModel.onQueryChanged(symptom)
                            viewModel.searchAdvice(symptom)
                        }
                    )
                }
                is AdviceState.Loading -> {
                    AdviceLoadingView()
                }
                is AdviceState.Success -> {
                    AdviceSuccessView(
                        query = state.query,
                        matches = state.matches,
                        onDetailClick = { plant ->
                            viewModel.selectPlant(plant)
                            onNavigateToDetail(plant.id)
                        }
                    )
                }
                is AdviceState.NoResults -> {
                    AdviceNoResultsView(
                        query = state.query,
                        suggestedSymptoms = state.suggestedSymptoms,
                        onSelectSymptom = { symptom ->
                            viewModel.onQueryChanged(symptom)
                            viewModel.searchAdvice(symptom)
                        }
                    )
                }
                is AdviceState.Error -> {
                    AdviceErrorView(
                        message = state.message,
                        onRetry = { viewModel.searchAdvice() }
                    )
                }
            }
        }
    }
}

/**
 * Vue pour l'état Idle (avant toute recherche).
 */
@Composable
private fun AdviceIdleView(
    onSelectSymptom: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Trouvez une plante selon votre symptôme",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Saisissez un besoin dans la barre de recherche ci-dessus ou appuyez sur l'un des symptômes suggérés pour afficher les fiches associées.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * Vue pour l'état Loading (recherche et analyse en cours).
 */
@Composable
private fun AdviceLoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("advice_loading_view"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Recherche des remèdes botaniques...",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Analyse des correspondances de symptômes & de la base Room local",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Vue pour l'état Success (affichage des cartes de résultats).
 */
@Composable
private fun AdviceSuccessView(
    query: String,
    matches: List<PlantMatch>,
    onDetailClick: (Plant) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Texte d'introduction explicite rappelant la nature informative
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ces suggestions sont basées sur des usages traditionnels documentés et ne constituent pas une recommandation médicale personnalisée.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "Plantes correspondantes (${matches.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("advice_results_list")
        ) {
            items(
                items = matches,
                key = { it.plant.id }
            ) { match ->
                PlantAdviceCard(
                    match = match,
                    onDetailClick = { onDetailClick(match.plant) }
                )
            }
        }
    }
}

/**
 * Vue pour l'état NoResults (aucun résultat exact trouvé).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdviceNoResultsView(
    query: String,
    suggestedSymptoms: List<SymptomDocument>,
    onSelectSymptom: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("advice_no_results_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Aucune correspondance directe pour \"$query\"",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Nous n'avons pas trouvé de plante associée exactement à ce terme. Essayez de reformuler votre demande ou découvrez l'un des symptômes suggérés ci-dessous :",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (suggestedSymptoms.isNotEmpty()) {
            Text(
                text = "Symptômes suggérés proches :",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                suggestedSymptoms.forEach { symptom ->
                    FilterChip(
                        selected = false,
                        onClick = { onSelectSymptom(symptom.name) },
                        label = { Text(symptom.name, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                }
            }
        }
    }
}

/**
 * Vue pour l'état Error (erreur technique de recherche).
 */
@Composable
private fun AdviceErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("advice_error_view"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Erreur de recherche",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Réessayer la recherche")
                }
            }
        }
    }
}

/**
 * Carte de résultat d'une plante associée à un symptôme.
 * Affiche obligatoirement : Nom, Statut de vérification, Niveau de preuve, Motif d'usage, Contre-indications condensées, Disclaimer de bas de carte.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlantAdviceCard(
    match: PlantMatch,
    onDetailClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailClick() }
            .testTag("advice_item_${match.plant.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    PlantImage(
                        imageUrl = match.plant.imageUrl,
                        imageAuthor = match.plant.imageAuthor,
                        imageLicense = match.plant.imageLicense,
                        plantName = match.plant.name,
                        colorHex = match.plant.colorHex,
                        modifier = Modifier.fillMaxSize(),
                        showAttribution = false,
                        testTag = "advice_plant_image_${match.plant.id}"
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = match.plant.category,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Badge de statut de vérification médicale
                        VerificationStatusBadge(status = match.plant.verificationStatus)

                        // Badge compact du niveau de preuve scientifique
                        EvidenceLevelBadge(
                            level = match.plant.scientificEvidenceLevel,
                            isCompact = true,
                            testTag = "advice_evidence_badge_${match.plant.id}"
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = match.plant.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = match.plant.scientificName,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Motif d'association (nuancé au conditionnel / usage traditionnel)
            Text(
                text = match.matchReason,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Badge de sécurité condensé (Contre-indications)
            CondensedSafetyBadge(plant = match.plant)

            Spacer(modifier = Modifier.height(8.dp))

            // Rappel de disclaimer en pied de carte
            Text(
                text = "Nature informative • Ne constitue pas une prescription médicale",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Composant de badge pour afficher le statut de vérification d'une plante.
 */
@Composable
private fun VerificationStatusBadge(
    status: VerificationStatus,
    modifier: Modifier = Modifier
) {
    val bgColor: Color
    val textColor: Color
    val icon: androidx.compose.ui.graphics.vector.ImageVector
    val label: String

    when (status) {
        VerificationStatus.VERIFIED_BY_PROFESSIONAL -> {
            bgColor = Color(0xFFE8F5E9)
            textColor = Color(0xFF2E7D32)
            icon = Icons.Default.Verified
            label = "Vérifié"
        }
        VerificationStatus.UNDER_REVIEW -> {
            bgColor = Color(0xFFFFF8E1)
            textColor = Color(0xFFE65100)
            icon = Icons.Default.HourglassTop
            label = "En révision"
        }
        VerificationStatus.UNVERIFIED -> {
            bgColor = Color(0xFFF5F5F5)
            textColor = Color(0xFF616161)
            icon = Icons.Default.Info
            label = "Non vérifié"
        }
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        modifier = modifier.testTag("verification_badge_${status.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}
