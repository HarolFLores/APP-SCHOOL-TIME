package com.floresvalle.schooltime.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalle.schooltime.R
import com.floresvalle.schooltime.data.repository.AuthRepository
import com.floresvalle.schooltime.data.repository.AuthResult
import com.floresvalle.schooltime.util.AuthPreferences
import com.floresvalle.schooltime.util.AuthValidation
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    sealed class AuthState {
        object Idle : AuthState()
        data class Loading(val mode: AuthMode = AuthMode.GENERAL) : AuthState()
        data class Success(val message: String? = null, val isOffline: Boolean = false) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    enum class AuthMode {
        GENERAL,
        EMAIL,
        GOOGLE
    }

    private val repository = AuthRepository(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val initialRememberMe: Boolean = AuthPreferences.shouldRememberMe(application)
    val initialEmail: String = AuthPreferences.getSavedEmail(application).orEmpty()

    var isUserLoggedIn: Boolean = (Firebase.auth.currentUser != null || AuthPreferences.isSessionActive(application))

    init {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null && !currentUser.isEmailVerified && currentUser.providerData.none { it.providerId == "google.com" }) {
            Firebase.auth.signOut()
            isUserLoggedIn = false
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }

    private fun findActivity(context: Context): Activity? {
        var current = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    fun login(email: String, pass: String, rememberMe: Boolean) {
        val normalizedEmail = AuthValidation.normalizeEmail(email)
        val emailError = AuthValidation.emailError(normalizedEmail)
        val passwordError = AuthValidation.passwordError(pass, forLogin = true)

        if (emailError != null || passwordError != null) {
            _authState.value = AuthState.Error(emailError ?: passwordError.orEmpty())
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading(AuthMode.EMAIL)
            when (val result = repository.loginWithEmail(normalizedEmail, pass, rememberMe)) {
                is AuthResult.Success -> {
                    isUserLoggedIn = true
                    _authState.value = AuthState.Success(
                        message = result.message,
                        isOffline = result.isOffline
                    )
                }
                is AuthResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    fun loginWithGoogle(
        context: Context,
        rememberMe: Boolean,
        onNewUser: (email: String, name: String, photo: String?, uid: String) -> Unit
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading(AuthMode.GOOGLE)
            try {
                val hostActivity = findActivity(context)
                val targetContext = hostActivity ?: context
                val credentialManager = CredentialManager.create(targetContext)
                val webClientId = context.getString(R.string.default_web_client_id)

                val result = try {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(false)
                        .build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    credentialManager.getCredential(targetContext, request)
                } catch (_: Exception) {
                    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                        .build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(signInWithGoogleOption)
                        .build()
                    credentialManager.getCredential(targetContext, fallbackRequest)
                }

                val extracted = extractGoogleCredentialDetails(result.credential)
                val targetEmail = extracted?.email?.takeIf { it.isNotBlank() }
                if (targetEmail.isNullOrBlank()) {
                    _authState.value = AuthState.Error("No se pudo obtener el correo de la cuenta de Google.")
                    return@launch
                }
                val targetName = extracted.displayName?.takeIf { it.isNotBlank() } ?: "Estudiante"
                val targetPhoto = extracted.photoUrl

                // 1. Intentar autenticar en Firebase si hay idToken disponible
                var firebaseUser = Firebase.auth.currentUser
                if (!extracted.idToken.isNullOrBlank()) {
                    try {
                        val firebaseCredential = GoogleAuthProvider.getCredential(extracted.idToken, null)
                        val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
                        firebaseUser = authResult.user
                    } catch (_: Exception) {}
                }

                val finalUid = firebaseUser?.uid ?: UUID.nameUUIDFromBytes(targetEmail.toByteArray()).toString()
                val finalEmail = firebaseUser?.email?.takeIf { it.isNotBlank() } ?: targetEmail
                val finalName = firebaseUser?.displayName?.takeIf { it.isNotBlank() } ?: targetName
                val finalPhoto = firebaseUser?.photoUrl?.toString() ?: targetPhoto

                // 2. Comprobar si el usuario ya está registrado en la base de datos
                val userExistsInDb = try {
                    when (val syncResult = repository.checkAndSyncGoogleUser(
                        uid = finalUid,
                        email = finalEmail,
                        name = finalName,
                        photoUrl = finalPhoto,
                        rememberMe = rememberMe
                    )) {
                        is AuthResult.Success -> syncResult.data
                        is AuthResult.Error -> false
                    }
                } catch (_: Exception) {
                    false
                }

                if (userExistsInDb) {
                    isUserLoggedIn = true
                    _authState.value = AuthState.Success()
                } else {
                    _authState.value = AuthState.Idle
                    onNewUser(finalEmail, finalName, finalPhoto, finalUid)
                }
            } catch (_: GetCredentialCancellationException) {
                _authState.value = AuthState.Idle
            } catch (_: NoCredentialException) {
                _authState.value = AuthState.Error(
                    "No se seleccionó una cuenta de Google o no hay cuentas activas en este dispositivo."
                )
            } catch (e: GetCredentialException) {
                val message = e.localizedMessage.orEmpty()
                _authState.value = AuthState.Error(
                    if (message.contains("No credentials", ignoreCase = true) ||
                        e.type.contains("NO_CREDENTIAL", ignoreCase = true)
                    ) {
                        "No se encontró una cuenta de Google activa en el dispositivo. Selecciona una cuenta o ingresa con correo."
                    } else {
                        "Error al autenticar con Google: ${e.message ?: "Inténtalo de nuevo."}"
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(AuthValidation.mapFirebaseAuthError(e))
            }
        }
    }

    private data class GoogleCredentialDetails(
        val idToken: String,
        val email: String,
        val displayName: String?,
        val photoUrl: String?
    )

    private fun extractGoogleCredentialDetails(credential: androidx.credentials.Credential): GoogleCredentialDetails? {
        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL || credential is CustomCredential) {
            try {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                return GoogleCredentialDetails(
                    idToken = googleCred.idToken,
                    email = googleCred.id,
                    displayName = googleCred.displayName,
                    photoUrl = googleCred.profilePictureUri?.toString()
                )
            } catch (_: Exception) {
                val bundle = credential.data
                val token = bundle.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
                    ?: bundle.getString("id_token")
                    ?: bundle.getString("google_id_token")
                if (!token.isNullOrBlank()) {
                    val email = bundle.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID")
                        ?: bundle.getString("email").orEmpty()
                    val name = bundle.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_DISPLAY_NAME")
                        ?: bundle.getString("name")
                    val photo = bundle.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_PROFILE_PICTURE_URI")
                        ?: bundle.getString("photo_url")
                    return GoogleCredentialDetails(token, email, name, photo)
                }
            }
        }
        return null
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            isUserLoggedIn = false
            _authState.value = AuthState.Idle
        }
    }

    fun clearRoomData(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllRoomData()
            isUserLoggedIn = false
            _authState.value = AuthState.Idle
            onDone()
        }
    }
}
