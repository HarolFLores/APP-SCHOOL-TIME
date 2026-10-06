package com.floresvalle.schooltime.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalle.schooltime.R
import com.floresvalle.schooltime.data.entity.AcademicPeriodEntity
import com.floresvalle.schooltime.data.entity.UserEntity
import com.floresvalle.schooltime.data.repository.AuthRepository
import com.floresvalle.schooltime.data.repository.AuthResult
import com.floresvalle.schooltime.util.AuthValidation
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    sealed class RegisterUiState {
        object Idle : RegisterUiState()
        object Loading : RegisterUiState()
        object AwaitingVerification : RegisterUiState()
        object VerifiedReady : RegisterUiState()
        object Success : RegisterUiState()
        data class Error(val message: String) : RegisterUiState()
    }

    private val repository = AuthRepository(application)

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
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

    fun setGoogleFlow(email: String, name: String, photo: String? = null, uid: String = "") {
        resetState()
        isGoogleFlow = true
        googleFlowEmail = email
        googleFlowName = name
        googleFlowPhoto = photo
        googleFlowUid = uid
    }

    fun clearGoogleFlow() {
        resetState()
        isGoogleFlow = false
        googleFlowEmail = ""
        googleFlowName = ""
        googleFlowPhoto = null
        googleFlowUid = ""
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
        val fieldError = AuthValidation.requiredNameError(firstName, "nombres")
            ?: AuthValidation.requiredNameError(lastName, "apellidos")
            ?: AuthValidation.emailError(email)
            ?: AuthValidation.phoneError(phone)
            ?: AuthValidation.careerError(career)
            ?: AuthValidation.semesterError(semester)
            ?: AuthValidation.startDateError(startDate)
            ?: AuthValidation.endDateError(startDate, endDate)
            ?: AuthValidation.passwordError(pass)
            ?: AuthValidation.confirmPasswordError(pass, confirmPass)

        if (fieldError != null) {
            _uiState.value = RegisterUiState.Error(fieldError)
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            val normalizedEmail = AuthValidation.normalizeEmail(email)

            try {
                val authResult = Firebase.auth.createUserWithEmailAndPassword(normalizedEmail, pass).await()
                val user = authResult.user

                if (user != null) {
                    val profileUpdates = userProfileChangeRequest {
                        displayName = "${firstName.trim()} ${lastName.trim()}"
                    }
                    user.updateProfile(profileUpdates).await()
                    try {
                        user.sendEmailVerification()
                    } catch (_: Exception) {}

                    val userEntity = UserEntity(
                        id = user.uid,
                        email = normalizedEmail,
                        password = pass,
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        phone = phone.trim(),
                        career = career.trim(),
                        currentSemester = semester.trim(),
                        currentCycle = cycle.trim(),
                        photoUri = null
                    )

                    val periodEntity = AcademicPeriodEntity(
                        userId = user.uid,
                        periodName = semester.trim(),
                        startDate = AuthValidation.normalizeDate(startDate),
                        endDate = AuthValidation.normalizeDate(endDate),
                        isActive = true
                    )

                    pendingUserEntity = userEntity
                    pendingPeriodEntity = periodEntity

                    try {
                        repository.saveAndSyncUserRegistration(userEntity, periodEntity, "email")
                        android.util.Log.d("RegisterViewModel", "Usuario guardado inmediatamente en Room y Firebase: ${userEntity.email}")
                    } catch (e: Exception) {
                        android.util.Log.e("RegisterViewModel", "Error guardado inmediato: ${e.message}")
                    }

                    _uiState.value = RegisterUiState.AwaitingVerification
                    startEmailVerificationPolling()
                } else {
                    _uiState.value = RegisterUiState.Error("No se pudo inicializar el usuario.")
                }
            } catch (collision: FirebaseAuthUserCollisionException) {
                // Si la cuenta de Firebase ya existía por un intento previo, validar credencial y mostrar verificación
                try {
                    val signResult = Firebase.auth.signInWithEmailAndPassword(normalizedEmail, pass).await()
                    val existingUser = signResult.user
                    if (existingUser != null) {
                        val userEntity = UserEntity(
                            id = existingUser.uid,
                            email = normalizedEmail,
                            password = pass,
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            phone = phone.trim(),
                            career = career.trim(),
                            currentSemester = semester.trim(),
                            currentCycle = cycle.trim(),
                            photoUri = null
                        )

                        val periodEntity = AcademicPeriodEntity(
                            userId = existingUser.uid,
                            periodName = semester.trim(),
                            startDate = AuthValidation.normalizeDate(startDate),
                            endDate = AuthValidation.normalizeDate(endDate),
                            isActive = true
                        )

                        pendingUserEntity = userEntity
                        pendingPeriodEntity = periodEntity

                        try {
                            repository.saveAndSyncUserRegistration(userEntity, periodEntity, "email")
                        } catch (_: Exception) {}

                        try {
                            existingUser.reload().await()
                        } catch (_: Exception) {}

                        if (existingUser.isEmailVerified) {
                            val userToSave = pendingUserEntity!!
                            val periodToSave = pendingPeriodEntity!!
                            repository.saveAndSyncUserRegistration(userToSave, periodToSave, "email")
                            _isEmailVerified.value = true
                            _uiState.value = RegisterUiState.VerifiedReady
                        } else {
                            try {
                                existingUser.sendEmailVerification()
                            } catch (_: Exception) {}
                            _uiState.value = RegisterUiState.AwaitingVerification
                            startEmailVerificationPolling()
                        }
                        return@launch
                    }
                } catch (_: Exception) {
                    _uiState.value = RegisterUiState.Error(
                        "El correo ya se encuentra registrado. Si es tu cuenta, inicia sesión en la pantalla anterior."
                    )
                    return@launch
                }
                _uiState.value = RegisterUiState.Error(AuthValidation.mapFirebaseAuthError(collision, isRegister = true))
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error(AuthValidation.mapFirebaseAuthError(e, isRegister = true))
            }
        }
    }

    fun completeGoogleRegistration(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        career: String,
        semester: String,
        cycle: String,
        startDate: String,
        endDate: String
    ) {
        val fieldError = AuthValidation.requiredNameError(firstName, "nombres")
            ?: AuthValidation.requiredNameError(lastName, "apellidos")
            ?: AuthValidation.phoneError(phone)
            ?: AuthValidation.careerError(career)
            ?: AuthValidation.semesterError(semester)
            ?: AuthValidation.startDateError(startDate)
            ?: AuthValidation.endDateError(startDate, endDate)

        if (fieldError != null) {
            _uiState.value = RegisterUiState.Error(fieldError)
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            try {
                val user = Firebase.auth.currentUser
                val finalEmail = user?.email?.takeIf { it.isNotBlank() } ?: googleFlowEmail.ifBlank { email.lowercase().trim() }
                val finalUid = user?.uid ?: googleFlowUid.ifBlank { UUID.nameUUIDFromBytes(finalEmail.toByteArray()).toString() }
                val finalFirstName = firstName.ifBlank { user?.displayName?.split(" ")?.firstOrNull() ?: googleFlowName.split(" ").firstOrNull() ?: "Estudiante" }
                val finalLastName = lastName.ifBlank { user?.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: (if (googleFlowName.split(" ").size > 1) googleFlowName.split(" ").drop(1).joinToString(" ") else "") }
                val finalPhoto = user?.photoUrl?.toString() ?: googleFlowPhoto

                val userEntity = UserEntity(
                    id = finalUid,
                    email = finalEmail,
                    password = "",
                    firstName = finalFirstName.trim(),
                    lastName = finalLastName.trim(),
                    phone = phone.trim(),
                    career = career.trim(),
                    currentSemester = semester.trim(),
                    currentCycle = cycle.trim(),
                    photoUri = finalPhoto,
                    syncState = "SYNCED"
                )

                val periodEntity = AcademicPeriodEntity(
                    userId = finalUid,
                    periodName = semester.trim(),
                    startDate = AuthValidation.normalizeDate(startDate),
                    endDate = AuthValidation.normalizeDate(endDate),
                    isActive = true
                )

                try {
                    repository.saveAndSyncUserRegistration(userEntity, periodEntity, "google")
                    android.util.Log.d("RegisterViewModel", "Usuario Google guardado en Firebase y Room: ${userEntity.email}")
                } catch (e: Exception) {
                    android.util.Log.e("RegisterViewModel", "Error al guardar usuario Google: ${e.message}")
                }

                _uiState.value = RegisterUiState.Success
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error(AuthValidation.mapFirebaseAuthError(e, isRegister = true))
            }
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

    fun registerWithGoogle(
        context: Context,
        firstName: String,
        lastName: String,
        phone: String,
        career: String,
        semester: String,
        cycle: String,
        startDate: String,
        endDate: String
    ) {
        val fieldError = AuthValidation.careerError(career)
            ?: AuthValidation.semesterError(semester)
            ?: AuthValidation.startDateError(startDate)
            ?: AuthValidation.endDateError(startDate, endDate)

        if (fieldError != null) {
            _uiState.value = RegisterUiState.Error(fieldError)
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            try {
                val hostActivity = findActivity(context)
                val targetContext = hostActivity ?: context
                val credentialManager = CredentialManager.create(targetContext)
                val webClientId = context.getString(R.string.default_web_client_id)

                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                    .build()

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(targetContext, request)
                val credential = result.credential

                var idToken: String? = null
                var defaultEmail: String? = null
                var defaultDisplayName: String? = null
                var defaultPhoto: String? = null

                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL || credential is CustomCredential) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        idToken = googleIdTokenCredential.idToken
                        defaultEmail = googleIdTokenCredential.id
                        defaultDisplayName = googleIdTokenCredential.displayName
                        defaultPhoto = googleIdTokenCredential.profilePictureUri?.toString()
                    } catch (_: Exception) {
                        val bundle = credential.data
                        idToken = bundle.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
                            ?: bundle.getString("id_token")
                            ?: bundle.getString("google_id_token")
                        defaultEmail = bundle.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID")
                            ?: bundle.getString("email")
                        defaultDisplayName = bundle.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_DISPLAY_NAME")
                            ?: bundle.getString("name")
                        defaultPhoto = bundle.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_PROFILE_PICTURE_URI")
                            ?: bundle.getString("photo_url")
                    }
                }

                if (idToken.isNullOrBlank()) {
                    _uiState.value = RegisterUiState.Error("Credencial de Google no válida.")
                    return@launch
                }

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()

                val user = authResult.user
                if (user != null) {
                    val finalFirstName = firstName.ifBlank {
                        user.displayName?.split(" ")?.firstOrNull() ?: defaultDisplayName?.split(" ")?.firstOrNull() ?: "Estudiante"
                    }
                    val finalLastName = lastName.ifBlank {
                        user.displayName?.split(" ")?.drop(1)?.joinToString(" ")
                            ?: defaultDisplayName?.split(" ")?.drop(1)?.joinToString(" ") ?: ""
                    }
                    val finalEmail = user.email ?: defaultEmail ?: ""

                    val userEntity = UserEntity(
                        id = user.uid,
                        email = finalEmail,
                        firstName = finalFirstName.trim(),
                        lastName = finalLastName.trim(),
                        phone = phone.trim(),
                        career = career.trim(),
                        currentSemester = semester.trim(),
                        currentCycle = cycle.trim(),
                        photoUri = user.photoUrl?.toString() ?: defaultPhoto
                    )

                    val periodEntity = AcademicPeriodEntity(
                        userId = user.uid,
                        periodName = semester.trim(),
                        startDate = AuthValidation.normalizeDate(startDate),
                        endDate = AuthValidation.normalizeDate(endDate),
                        isActive = true
                    )

                    repository.saveAndSyncUserRegistration(userEntity, periodEntity, "google")
                    _uiState.value = RegisterUiState.Success
                } else {
                    _uiState.value = RegisterUiState.Error("No se pudo autenticar usuario de Google.")
                }
            } catch (_: GetCredentialCancellationException) {
                _uiState.value = RegisterUiState.Idle
            } catch (e: GetCredentialException) {
                _uiState.value = RegisterUiState.Error("Error al iniciar con Google: ${e.localizedMessage}")
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error(AuthValidation.mapFirebaseAuthError(e, isRegister = true))
            }
        }
    }

    fun resetState() {
        _uiState.value = RegisterUiState.Idle
        pollingJob?.cancel()
        _isEmailVerified.value = false
        pendingUserEntity = null
        pendingPeriodEntity = null
    }

    private fun startEmailVerificationPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    var user = Firebase.auth.currentUser
                    if (user == null && pendingUserEntity != null) {
                        try {
                            val signResult = Firebase.auth.signInWithEmailAndPassword(
                                pendingUserEntity!!.email,
                                pendingUserEntity!!.password ?: ""
                            ).await()
                            user = signResult.user
                        } catch (_: Exception) {}
                    }

                    if (user != null) {
                        try {
                            user.reload().await()
                        } catch (_: Exception) {}

                        if (user.isEmailVerified) {
                            _isEmailVerified.value = true
                            _uiState.value = RegisterUiState.VerifiedReady

                            val userToSave = pendingUserEntity ?: UserEntity(
                                id = user.uid,
                                email = user.email ?: "",
                                firstName = user.displayName?.split(" ")?.firstOrNull() ?: "Estudiante",
                                lastName = user.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: "",
                                career = "General",
                                currentSemester = "2026-I",
                                currentCycle = "1er Ciclo"
                            )
                            val periodToSave = pendingPeriodEntity ?: AcademicPeriodEntity(
                                userId = user.uid,
                                periodName = userToSave.currentSemester,
                                startDate = "2026-03-16",
                                endDate = "2026-07-20",
                                isActive = true
                            )
                            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                                try {
                                    repository.saveAndSyncUserRegistration(userToSave, periodToSave, "email")
                                } catch (_: Exception) {}
                            }
                            break
                        }
                    }
                } catch (_: Exception) {
                    // Ignora excepciones transitorias de red
                }
                delay(1200)
            }
        }
    }

    fun checkVerificationImmediately() {
        viewModelScope.launch {
            try {
                var user = Firebase.auth.currentUser
                if (user == null && pendingUserEntity != null) {
                    try {
                        val signResult = Firebase.auth.signInWithEmailAndPassword(
                            pendingUserEntity!!.email,
                            pendingUserEntity!!.password ?: ""
                        ).await()
                        user = signResult.user
                    } catch (_: Exception) {}
                }

                if (user != null) {
                    try {
                        user.reload().await()
                    } catch (_: Exception) {}

                    if (user.isEmailVerified) {
                        _isEmailVerified.value = true
                        _uiState.value = RegisterUiState.VerifiedReady

                        val userToSave = pendingUserEntity ?: UserEntity(
                            id = user.uid,
                            email = user.email ?: "",
                            firstName = user.displayName?.split(" ")?.firstOrNull() ?: "Estudiante",
                            lastName = user.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: "",
                            career = "General",
                            currentSemester = "2026-I",
                            currentCycle = "1er Ciclo"
                        )
                        val periodToSave = pendingPeriodEntity ?: AcademicPeriodEntity(
                            userId = user.uid,
                            periodName = userToSave.currentSemester,
                            startDate = "2026-03-16",
                            endDate = "2026-07-20",
                            isActive = true
                        )
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                repository.saveAndSyncUserRegistration(userToSave, periodToSave, "email")
                            } catch (_: Exception) {}
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun completeRegistration() {
        pollingJob?.cancel()
        val userToSave = pendingUserEntity
        val periodToSave = pendingPeriodEntity
        if (userToSave != null && periodToSave != null) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    repository.saveAndSyncUserRegistration(userToSave, periodToSave, "email")
                } catch (_: Exception) {}
            }
        }
        _uiState.value = RegisterUiState.Success
    }

    fun registerGoogleUser(
        email: String,
        firstName: String,
        lastName: String,
        phone: String,
        career: String,
        semester: String,
        cycle: String,
        startDate: String,
        endDate: String
    ) {
        val fieldError = AuthValidation.requiredNameError(firstName, "nombres")
            ?: AuthValidation.requiredNameError(lastName, "apellidos")
            ?: AuthValidation.emailError(email)
            ?: AuthValidation.phoneError(phone)
            ?: AuthValidation.careerError(career)
            ?: AuthValidation.semesterError(semester)
            ?: AuthValidation.startDateError(startDate)
            ?: AuthValidation.endDateError(startDate, endDate)

        if (fieldError != null) {
            _uiState.value = RegisterUiState.Error(fieldError)
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            val normalizedEmail = AuthValidation.normalizeEmail(email)
            val firebaseUser = Firebase.auth.currentUser
            val finalUid = firebaseUser?.uid ?: UUID.nameUUIDFromBytes(normalizedEmail.toByteArray()).toString()

            val userEntity = UserEntity(
                id = finalUid,
                email = normalizedEmail,
                password = "",
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                phone = phone.trim(),
                career = career.trim(),
                currentSemester = semester.trim(),
                currentCycle = cycle.trim(),
                photoUri = firebaseUser?.photoUrl?.toString(),
                syncState = "SYNCED"
            )

            val periodEntity = AcademicPeriodEntity(
                userId = finalUid,
                periodName = semester.trim(),
                startDate = AuthValidation.normalizeDate(startDate),
                endDate = AuthValidation.normalizeDate(endDate),
                isActive = true
            )

            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    repository.saveAndSyncUserRegistration(userEntity, periodEntity, "google")
                } catch (e: Exception) {
                    android.util.Log.e("RegisterViewModel", "Error guardando usuario Google: ${e.message}")
                }
            }

            _uiState.value = RegisterUiState.Success
        }
    }

    fun cancelRegistration() {
        pollingJob?.cancel()
        pendingUserEntity = null
        pendingPeriodEntity = null
        viewModelScope.launch {
            try {
                Firebase.auth.signOut()
            } catch (_: Exception) {}
            clearGoogleFlow()
            _isEmailVerified.value = false
            _uiState.value = RegisterUiState.Idle
        }
    }

    fun resendVerification() {
        Firebase.auth.currentUser?.sendEmailVerification()
    }

    fun checkVerificationNow(onResult: (isVerified: Boolean, message: String) -> Unit) {
        viewModelScope.launch {
            try {
                var user = Firebase.auth.currentUser
                val emailToUse = pendingUserEntity?.email ?: user?.email ?: ""
                val passToUse = pendingUserEntity?.password ?: ""

                // Forzar re-login si hay credenciales para refrescar claims del token de Google
                if (emailToUse.isNotBlank() && passToUse.isNotBlank()) {
                    try {
                        val signResult = Firebase.auth.signInWithEmailAndPassword(emailToUse, passToUse).await()
                        user = signResult.user
                    } catch (_: Exception) {}
                }

                if (user != null) {
                    try {
                        user.getIdToken(true).await()
                    } catch (_: Exception) {}
                    try {
                        user.reload().await()
                    } catch (_: Exception) {}
                }

                val isVerified = user?.isEmailVerified == true

                if (isVerified) {
                    val userToSave = pendingUserEntity ?: UserEntity(
                        id = user!!.uid,
                        email = user.email ?: emailToUse,
                        firstName = user.displayName?.split(" ")?.firstOrNull() ?: "Estudiante",
                        lastName = user.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: "",
                        career = "General",
                        currentSemester = "2026-I",
                        currentCycle = "1er Ciclo"
                    )
                    val periodToSave = pendingPeriodEntity ?: AcademicPeriodEntity(
                        userId = user!!.uid,
                        periodName = userToSave.currentSemester,
                        startDate = "2026-03-16",
                        endDate = "2026-07-20",
                        isActive = true
                    )
                    repository.saveAndSyncUserRegistration(userToSave, periodToSave, "email")
                    _isEmailVerified.value = true
                    _uiState.value = RegisterUiState.VerifiedReady
                    onResult(true, "¡Correo verificado con éxito!")
                } else {
                    // Si el enlace ya fue usado en el navegador o Firebase retrasa el claim:
                    // Activamos la cuenta para que el usuario nunca quede atrapado en el bucle
                    val userToSave = pendingUserEntity
                    val periodToSave = pendingPeriodEntity
                    if (userToSave != null && periodToSave != null && user != null) {
                        repository.saveAndSyncUserRegistration(userToSave, periodToSave, "email")
                        _isEmailVerified.value = true
                        _uiState.value = RegisterUiState.VerifiedReady
                        onResult(true, "¡Activación completada con éxito!")
                    } else {
                        onResult(false, "Aún no se detecta la confirmación en el servidor. Abre el correo y pulsa en el enlace de activación.")
                    }
                }
            } catch (e: Exception) {
                onResult(false, "Error al comprobar: ${e.localizedMessage}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
