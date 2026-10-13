package com.floresvalle.schooltime.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalle.schooltime.data.entity.AcademicPeriodEntity
import com.floresvalle.schooltime.data.entity.UserEntity
import com.floresvalle.schooltime.data.repository.AuthRepository
import com.floresvalle.schooltime.data.repository.AuthResult
import com.floresvalle.schooltime.util.AuthValidation
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application)
    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Form)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _isEmailVerified = MutableStateFlow(false)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()

    private var pollingJob: Job? = null
    private var pendingUserEntity: UserEntity? = null
    private var pendingPeriodEntity: AcademicPeriodEntity? = null

    var isGoogleFlow by mutableStateOf(false)
        private set
    var googleFlowEmail by mutableStateOf("")
        private set
    var googleFlowName by mutableStateOf("")
        private set
    var googleFlowPhoto by mutableStateOf<String?>(null)
        private set
    var googleFlowUid by mutableStateOf("")
        private set

    sealed class RegisterUiState {
        object Form : RegisterUiState()
        object Loading : RegisterUiState()
        data class VerificationSent(val email: String) : RegisterUiState()
        object Success : RegisterUiState()
        data class Error(val message: String) : RegisterUiState()
    }

    fun setGoogleFlow(uid: String, email: String, name: String, photo: String?) {
        isGoogleFlow = true
        googleFlowUid = uid
        googleFlowEmail = email
        googleFlowName = name
        googleFlowPhoto = photo
    }

    fun clearGoogleFlow() {
        isGoogleFlow = false
        googleFlowUid = ""
        googleFlowEmail = ""
        googleFlowName = ""
        googleFlowPhoto = null
    }

    fun resetState() {
        pollingJob?.cancel()
        _uiState.value = RegisterUiState.Form
        _isEmailVerified.value = false
    }

    fun cancelRegistration() {
        resetState()
        clearGoogleFlow()
    }

    fun registerWithEmail(
        email: String,
        pass: String,
        confirmPass: String,
        firstName: String,
        lastName: String,
        phone: String,
        career: String,
        semester: String,
        cycle: String,
        startDate: String,
        endDate: String
    ) {
        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            try {
                val normalizedEmail = AuthValidation.normalizeEmail(email)
                val authResult = Firebase.auth.createUserWithEmailAndPassword(normalizedEmail, pass).await()
                val firebaseUser = authResult.user ?: throw Exception("Error al crear usuario")
                firebaseUser.sendEmailVerification().await()

                val user = UserEntity(
                    id = firebaseUser.uid,
                    email = normalizedEmail,
                    password = pass,
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    career = career.trim(),
                    currentSemester = semester.trim(),
                    currentCycle = cycle.trim(),
                    phone = phone.trim()
                )
                val period = AcademicPeriodEntity(
                    userId = firebaseUser.uid,
                    periodName = semester.trim(),
                    startDate = startDate.trim(),
                    endDate = endDate.trim(),
                    isActive = true
                )
                pendingUserEntity = user
                pendingPeriodEntity = period

                _uiState.value = RegisterUiState.VerificationSent(normalizedEmail)
                startEmailVerificationPolling()
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error(AuthValidation.mapFirebaseAuthError(e, isRegister = true))
            }
        }
    }

    fun startEmailVerificationPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                try {
                    val user = Firebase.auth.currentUser
                    user?.reload()?.await()
                    if (user?.isEmailVerified == true) {
                        _isEmailVerified.value = true
                        completeRegistration()
                        break
                    }
                } catch (e: Exception) {
                    // continue polling
                }
            }
        }
    }

    fun checkVerificationNow(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val user = Firebase.auth.currentUser
                user?.reload()?.await()
                if (user?.isEmailVerified == true) {
                    _isEmailVerified.value = true
                    completeRegistration()
                    onResult(true, "¡Correo verificado con éxito!")
                } else {
                    onResult(false, "El correo aún no ha sido verificado. Revisa tu bandeja.")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Error al verificar")
            }
        }
    }

    fun resendVerification() {
        viewModelScope.launch {
            try {
                Firebase.auth.currentUser?.sendEmailVerification()?.await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun completeRegistration() {
        viewModelScope.launch {
            val user = pendingUserEntity ?: return@launch
            val period = pendingPeriodEntity ?: return@launch
            when (val res = repository.saveAndSyncUserRegistration(user, period, "email")) {
                is AuthResult.Success -> {
                    _uiState.value = RegisterUiState.Success
                }
                is AuthResult.Error -> {
                    _uiState.value = RegisterUiState.Error(res.message)
                }
            }
        }
    }

    fun completeGoogleRegistration(
        firstName: String,
        lastName: String,
        phone: String,
        career: String,
        semester: String,
        cycle: String,
        startDate: String,
        endDate: String,
        email: String = googleFlowEmail
    ) {
        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            try {
                val uid = googleFlowUid
                val user = UserEntity(
                    id = uid,
                    email = AuthValidation.normalizeEmail(email),
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    career = career.trim(),
                    currentSemester = semester.trim(),
                    currentCycle = cycle.trim(),
                    phone = phone.trim(),
                    photoUri = googleFlowPhoto
                )
                val period = AcademicPeriodEntity(
                    userId = uid,
                    periodName = semester.trim(),
                    startDate = startDate.trim(),
                    endDate = endDate.trim(),
                    isActive = true
                )
                when (val res = repository.saveAndSyncUserRegistration(user, period, "google")) {
                    is AuthResult.Success -> {
                        _uiState.value = RegisterUiState.Success
                    }
                    is AuthResult.Error -> {
                        _uiState.value = RegisterUiState.Error(res.message)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error(e.message ?: "Error al registrar cuenta Google")
            }
        }
    }
}
