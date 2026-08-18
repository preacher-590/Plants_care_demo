package com.example.ui.navigation

/**
 * Définition des routes de navigation pour Jetpack Navigation Compose.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Scan : Screen("scan")
    object Advice : Screen("advice")
    object Library : Screen("library")
    object Settings : Screen("settings")
    object LegalNotice : Screen("legal_notice")
    object PrivacyPolicy : Screen("privacy_policy")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Profile : Screen("profile")
    object History : Screen("history")
    object Favorites : Screen("favorites")
    object AdminEditLegal : Screen("admin_edit_legal")
    object AdminImage : Screen("admin_image")
    object Detail : Screen("detail/{plantId}") {
        fun createRoute(plantId: String) = "detail/$plantId"
    }
}
