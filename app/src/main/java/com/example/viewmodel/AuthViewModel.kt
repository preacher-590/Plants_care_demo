package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.AuthState
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * État du formulaire de connexion.
 */
data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * État du formulaire d'inscription.
 */
data class RegisterFormState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * État du formulaire de réinitialisation de mot de passe.
 */
data class ForgotPasswordFormState(
    val email: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel gérant la logique métier d'authentification, de gestion de compte et de validation de formulaires.
 */
class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    // État d'authentification global
    val authState: StateFlow<AuthState> = repository.authStateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuthState.Loading
    )

    // Formulaires
    private val _loginForm = MutableStateFlow(LoginFormState())
    val loginForm: StateFlow<LoginFormState> = _loginForm.asStateFlow()

    private val _registerForm = MutableStateFlow(RegisterFormState())
    val registerForm: StateFlow<RegisterFormState> = _registerForm.asStateFlow()

    private val _forgotPasswordForm = MutableStateFlow(ForgotPasswordFormState())
    val forgotPasswordForm: StateFlow<ForgotPasswordFormState> = _forgotPasswordForm.asStateFlow()

    // État spécifique du profil (ex: suppression de compte)
    private val _profileActionState = MutableStateFlow<String?>(null)
    val profileActionState: StateFlow<String?> = _profileActionState.asStateFlow()

    private val _profileErrorState = MutableStateFlow<String?>(null)
    val profileErrorState: StateFlow<String?> = _profileErrorState.asStateFlow()

    // === Mises à jour des champs de saisie ===

    fun updateLoginEmail(email: String) {
        _loginForm.value = _loginForm.value.copy(email = email, errorMessage = null)
    }

    fun updateLoginPassword(password: String) {
        _loginForm.value = _loginForm.value.copy(password = password, errorMessage = null)
    }

    fun updateRegisterEmail(email: String) {
        _registerForm.value = _registerForm.value.copy(email = email, errorMessage = null)
    }

    fun updateRegisterPassword(password: String) {
        _registerForm.value = _registerForm.value.copy(password = password, errorMessage = null)
    }

    fun updateRegisterConfirmPassword(password: String) {
        _registerForm.value = _registerForm.value.copy(confirmPassword = password, errorMessage = null)
    }

    fun updateForgotPasswordEmail(email: String) {
        _forgotPasswordForm.value = _forgotPasswordForm.value.copy(email = email, errorMessage = null)
    }

    fun clearMessages() {
        _loginForm.value = _loginForm.value.copy(errorMessage = null, successMessage = null)
        _registerForm.value = _registerForm.value.copy(errorMessage = null, successMessage = null)
        _forgotPasswordForm.value = _forgotPasswordForm.value.copy(errorMessage = null, successMessage = null)
        _profileActionState.value = null
        _profileErrorState.value = null
    }

    // === Actions d'authentification ===

    /**
     * Connexion par email et mot de passe.
     */
    fun loginWithEmail(onSuccess: () -> Unit) {
        val form = _loginForm.value
        val email = form.email.trim()
        val password = form.password

        if (email.isBlank()) {
            _loginForm.value = form.copy(errorMessage = "Veuillez saisir votre adresse email.")
            return
        }
        if (!isValidEmail(email)) {
            _loginForm.value = form.copy(errorMessage = "L'adresse email saisie est invalide.")
            return
        }
        if (password.isBlank()) {
            _loginForm.value = form.copy(errorMessage = "Veuillez saisir votre mot de passe.")
            return
        }

        _loginForm.value = form.copy(isSubmitting = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.loginWithEmail(email, password)
            _loginForm.value = _loginForm.value.copy(isSubmitting = false)

            result.onSuccess {
                clearMessages()
                onSuccess()
            }.onFailure { exception ->
                _loginForm.value = _loginForm.value.copy(
                    errorMessage = mapFirebaseErrorToFrench(exception)
                )
            }
        }
    }

    /**
     * Inscription par email et mot de passe.
     */
    fun registerWithEmail(onSuccess: () -> Unit) {
        val form = _registerForm.value
        val email = form.email.trim()
        val password = form.password
        val confirmPassword = form.confirmPassword

        if (email.isBlank()) {
            _registerForm.value = form.copy(errorMessage = "Veuillez saisir une adresse email.")
            return
        }
        if (!isValidEmail(email)) {
            _registerForm.value = form.copy(errorMessage = "L'adresse email n'est pas valide (ex: nom@domaine.com).")
            return
        }
        if (password.length < 6) {
            _registerForm.value = form.copy(errorMessage = "Le mot de passe doit contenir au moins 6 caractères.")
            return
        }
        if (password != confirmPassword) {
            _registerForm.value = form.copy(errorMessage = "Les mots de passe ne correspondent pas.")
            return
        }

        _registerForm.value = form.copy(isSubmitting = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.registerWithEmail(email, password)
            _registerForm.value = _registerForm.value.copy(isSubmitting = false)

            result.onSuccess {
                clearMessages()
                onSuccess()
            }.onFailure { exception ->
                _registerForm.value = _registerForm.value.copy(
                    errorMessage = mapFirebaseErrorToFrench(exception)
                )
            }
        }
    }

    /**
     * Envoi d'email de réinitialisation.
     */
    fun sendPasswordResetEmail() {
        val form = _forgotPasswordForm.value
        val email = form.email.trim()

        if (email.isBlank() || !isValidEmail(email)) {
            _forgotPasswordForm.value = form.copy(errorMessage = "Veuillez saisir une adresse email valide.")
            return
        }

        _forgotPasswordForm.value = form.copy(isSubmitting = true, errorMessage = null, successMessage = null)

        viewModelScope.launch {
            val result = repository.sendPasswordResetEmail(email)
            _forgotPasswordForm.value = _forgotPasswordForm.value.copy(isSubmitting = false)

            result.onSuccess {
                _forgotPasswordForm.value = _forgotPasswordForm.value.copy(
                    successMessage = "Un email de réinitialisation a été envoyé à $email. Vérifiez vos spams si besoin."
                )
            }.onFailure { exception ->
                _forgotPasswordForm.value = _forgotPasswordForm.value.copy(
                    errorMessage = mapFirebaseErrorToFrench(exception)
                )
            }
        }
    }

    /**
     * Authentification avec jeton Google ID (Credential Manager).
     */
    fun signInWithGoogleToken(idToken: String, onSuccess: () -> Unit) {
        _loginForm.value = _loginForm.value.copy(isSubmitting = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.signInWithGoogleCredential(idToken)
            _loginForm.value = _loginForm.value.copy(isSubmitting = false)

            result.onSuccess {
                clearMessages()
                onSuccess()
            }.onFailure { exception ->
                _loginForm.value = _loginForm.value.copy(
                    errorMessage = "Erreur de connexion avec Google : ${mapFirebaseErrorToFrench(exception)}"
                )
            }
        }
    }

    /**
     * Déconnexion.
     */
    fun logout() {
        repository.logout()
        clearMessages()
    }

    /**
     * Suppression définitive du compte.
     */
    fun deleteAccount(onAccountDeleted: () -> Unit) {
        viewModelScope.launch {
            _profileActionState.value = "Suppression du compte en cours..."
            val result = repository.deleteAccount()
            if (result.isSuccess) {
                _profileActionState.value = null
                clearMessages()
                onAccountDeleted()
            } else {
                _profileActionState.value = null
                _profileErrorState.value = "Impossible de supprimer le compte. " +
                        (result.exceptionOrNull()?.let { mapFirebaseErrorToFrench(it) } ?: "Veuillez vous réauthentifier.")
            }
        }
    }

    // === Utilitaires de validation & messages en français ===

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun mapFirebaseErrorToFrench(e: Throwable): String {
        return when (e) {
            is FirebaseAuthInvalidUserException -> "Aucun compte correspondant à cette adresse email."
            is FirebaseAuthInvalidCredentialsException -> "Adresse email ou mot de passe incorrect."
            is FirebaseAuthUserCollisionException -> "Un compte existe déjà avec cette adresse email."
            is FirebaseAuthWeakPasswordException -> "Le mot de passe est trop faible. Utilisez au moins 6 caractères."
            else -> e.localizedMessage ?: "Une erreur de connexion est survenue. Vérifiez votre réseau."
        }
    }
}
