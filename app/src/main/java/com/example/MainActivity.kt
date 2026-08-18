package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.navigation.Screen
import com.example.ui.screens.AdviceScreen
import com.example.ui.screens.AdminEditLegalScreen
import com.example.ui.screens.AdminImageScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LegalNoticeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PlantLibraryScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.ScanHistoryScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.PlantCareTheme
import com.example.viewmodel.AdminImageViewModel
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.FavoritesViewModel
import com.example.viewmodel.LegalViewModel
import com.example.viewmodel.PlantViewModel
import com.example.viewmodel.ScanHistoryViewModel
import com.example.data.AuthState

/**
 * Activité principale de l'application PlantCare.
 * Utilise 100% Jetpack Compose et Jetpack Navigation Compose sans layout XML.
 */
class MainActivity : ComponentActivity() {

    private val plantViewModel: PlantViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val legalViewModel: LegalViewModel by viewModels()
    private val adminImageViewModel: AdminImageViewModel by viewModels()
    private val scanHistoryViewModel: ScanHistoryViewModel by viewModels()
    private val favoritesViewModel: FavoritesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PlantCareTheme {
                val navController = rememberNavController()
                val authState by authViewModel.authState.collectAsState()

                val navigateToAccount = {
                    if (authState is AuthState.Authenticated) {
                        navController.navigate(Screen.Profile.route)
                    } else {
                        navController.navigate(Screen.Login.route)
                    }
                }

                val navigateToHistory = {
                    navController.navigate(Screen.History.route)
                }

                val navigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                }

                val navigateToLogin = {
                    navController.navigate(Screen.Login.route)
                }

                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route
                ) {
                    // 1. Écran d'accueil
                    composable(Screen.Home.route) {
                        HomeScreen(
                            onNavigateToScan = {
                                plantViewModel.resetScan()
                                navController.navigate(Screen.Scan.route)
                            },
                            onNavigateToAdvice = {
                                navController.navigate(Screen.Advice.route)
                            },
                            onNavigateToLibrary = {
                                navController.navigate(Screen.Library.route)
                            },
                            onNavigateToDetail = { plantId ->
                                plantViewModel.selectPlant(plantId)
                                navController.navigate(Screen.Detail.createRoute(plantId))
                            },
                            onNavigateToSettings = {
                                navController.navigate(Screen.Settings.route)
                            },
                            onNavigateToLegalNotice = {
                                navController.navigate(Screen.LegalNotice.route)
                            },
                            onNavigateToPrivacyPolicy = {
                                navController.navigate(Screen.PrivacyPolicy.route)
                            },
                            onNavigateToAccount = navigateToAccount,
                            onNavigateToHistory = navigateToHistory,
                            onNavigateToFavorites = navigateToFavorites
                        )
                    }

                    // 2. Écran de scan photo
                    composable(Screen.Scan.route) {
                        ScanScreen(
                            viewModel = plantViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDetail = { plantId ->
                                navController.navigate(Screen.Detail.createRoute(plantId))
                            }
                        )
                    }

                    // 3. Écran de demande de conseil
                    composable(Screen.Advice.route) {
                        AdviceScreen(
                            viewModel = plantViewModel,
                            favoritesViewModel = favoritesViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDetail = { plantId ->
                                navController.navigate(Screen.Detail.createRoute(plantId))
                            },
                            onNavigateToLogin = navigateToLogin
                        )
                    }

                    // 4. Écran de bibliothèque des plantes
                    composable(Screen.Library.route) {
                        PlantLibraryScreen(
                            plantViewModel = plantViewModel,
                            favoritesViewModel = favoritesViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDetail = { plantId ->
                                plantViewModel.selectPlant(plantId)
                                navController.navigate(Screen.Detail.createRoute(plantId))
                            },
                            onNavigateToLogin = navigateToLogin
                        )
                    }

                    // 5. Écran Paramètres & Législation
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToLegalNotice = {
                                navController.navigate(Screen.LegalNotice.route)
                            },
                            onNavigateToPrivacyPolicy = {
                                navController.navigate(Screen.PrivacyPolicy.route)
                            },
                            onNavigateToAccount = navigateToAccount,
                            onNavigateToHistory = navigateToHistory
                        )
                    }

                    // 6. Écran Mentions Légales (LCEN / SREN)
                    composable(Screen.LegalNotice.route) {
                        LegalNoticeScreen(
                            legalViewModel = legalViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // 7. Écran Politique de Confidentialité (RGPD)
                    composable(Screen.PrivacyPolicy.route) {
                        PrivacyPolicyScreen(
                            legalViewModel = legalViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // 8. Écran de Connexion
                    composable(Screen.Login.route) {
                        LoginScreen(
                            authViewModel = authViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToRegister = {
                                authViewModel.clearMessages()
                                navController.navigate(Screen.Register.route)
                            },
                            onNavigateToForgotPassword = {
                                authViewModel.clearMessages()
                                navController.navigate(Screen.ForgotPassword.route)
                            },
                            onLoginSuccess = {
                                navController.navigate(Screen.Profile.route) {
                                    popUpTo(Screen.Home.route)
                                }
                            }
                        )
                    }

                    // 9. Écran d'Inscription
                    composable(Screen.Register.route) {
                        RegisterScreen(
                            authViewModel = authViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToLogin = {
                                authViewModel.clearMessages()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Register.route) { inclusive = true }
                                }
                            },
                            onRegisterSuccess = {
                                navController.navigate(Screen.Profile.route) {
                                    popUpTo(Screen.Home.route)
                                }
                            }
                        )
                    }

                    // 10. Écran Mot de passe oublié
                    composable(Screen.ForgotPassword.route) {
                        ForgotPasswordScreen(
                            authViewModel = authViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToLogin = {
                                authViewModel.clearMessages()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    // 11. Écran de Profil Utilisateur
                    composable(Screen.Profile.route) {
                        ProfileScreen(
                            authViewModel = authViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onLoggedOut = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            },
                            onNavigateToHistory = navigateToHistory,
                            onNavigateToFavorites = navigateToFavorites,
                            onNavigateToAdminLegal = {
                                navController.navigate(Screen.AdminEditLegal.route)
                            },
                            onNavigateToAdminImage = {
                                navController.navigate(Screen.AdminImage.route)
                            }
                        )
                    }

                    // 12. Écran d'Historique des Scans
                    composable(Screen.History.route) {
                        ScanHistoryScreen(
                            viewModel = scanHistoryViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToPlantDetail = { plantId ->
                                plantViewModel.selectPlant(plantId)
                                navController.navigate(Screen.Detail.createRoute(plantId))
                            },
                            onNavigateToScan = {
                                plantViewModel.resetScan()
                                navController.navigate(Screen.Scan.route)
                            },
                            onNavigateToLogin = navigateToLogin
                        )
                    }

                    // 13. Écran des Plantes Favorites
                    composable(Screen.Favorites.route) {
                        FavoritesScreen(
                            favoritesViewModel = favoritesViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToPlantDetail = { plantId ->
                                plantViewModel.selectPlant(plantId)
                                navController.navigate(Screen.Detail.createRoute(plantId))
                            },
                            onNavigateToLibrary = {
                                navController.navigate(Screen.Library.route)
                            },
                            onNavigateToAdvice = {
                                navController.navigate(Screen.Advice.route)
                            },
                            onNavigateToLogin = navigateToLogin
                        )
                    }

                    // 14. Écran d'Administration - Édition du Contenu Légal
                    composable(Screen.AdminEditLegal.route) {
                        AdminEditLegalScreen(
                            authViewModel = authViewModel,
                            legalViewModel = legalViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // 15. Écran d'Administration - Photos Wikimedia Commons
                    composable(Screen.AdminImage.route) {
                        AdminImageScreen(
                            authViewModel = authViewModel,
                            adminImageViewModel = adminImageViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // 16. Écran de détail d'une plante
                    composable(
                        route = Screen.Detail.route,
                        arguments = listOf(navArgument("plantId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val plantId = backStackEntry.arguments?.getString("plantId") ?: ""
                        if (plantId.isNotEmpty()) {
                            plantViewModel.selectPlant(plantId)
                        }
                        val selectedPlant by plantViewModel.selectedPlant.collectAsState()

                        DetailScreen(
                            plant = selectedPlant,
                            favoritesViewModel = favoritesViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToLogin = navigateToLogin
                        )
                    }
                }
            }
        }
    }
}
