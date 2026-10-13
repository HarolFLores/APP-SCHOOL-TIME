package com.floresvalle.schooltime.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalle.schooltime.data.repository.AuthRepository
import com.floresvalle.schooltime.data.repository.AuthResult
import com.floresvalle.schooltime.util.AuthPreferences
import com.floresvalle.schooltime.util.AuthValidation
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val initialEmail: String = AuthPreferences.getSavedEmail(application) ?: ""
    val initialRememberMe: Boolean = AuthPreferences.shouldRememberMe(application)
    var isUserLoggedIn: Boolean = AuthPreferences.isSessionActive(application)

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        object Success : AuthState()
        data class Error(val message: String) : AuthState()
    }

    data class GoogleCredentialDetails(
        val email: String,
        val displayName: String,
        val photoUrl: String?,
        val idToken: String
    )

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }

    fun login(email: String, pass: String, rememberMe: Boolean) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            when (val res = repository.loginWithEmail(email, pass, rememberMe)) {
                is AuthResult.Success -> {
                    isUserLoggedIn = true
                    _authState.value = AuthState.Success
                }
                is AuthResult.Error -> {
                    _authState.value = AuthState.Error(res.message)
                }
            }
        }
    }

    fun loginWithGoogle(
        context: Context,
        rememberMe: Boolean,
        onNewUser: (email: String, name: String, photo: String?, uid: String) -> Unit
    ) {
        // Handled via CredentialManager or Google Sign In in AuthScreen
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String) -> Unit) {
        val error = AuthValidation.emailError(email)
        if (error != null) {
            onResult(false, error)
            return
        }
        viewModelScope.launch {
            try {
                Firebase.auth.sendPasswordResetEmail(AuthValidation.normalizeEmail(email)).await()
                onResult(true, "Se ha enviado un correo para restablecer tu contraseña.")
            } catch (e: Exception) {
                onResult(false, AuthValidation.mapFirebaseAuthError(e))
            }
        }
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
