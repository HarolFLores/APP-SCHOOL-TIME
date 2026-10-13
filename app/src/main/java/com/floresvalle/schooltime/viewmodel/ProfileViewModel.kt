package com.floresvalle.schooltime.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalle.schooltime.data.AppDatabase
import com.floresvalle.schooltime.data.entity.*
import com.floresvalle.schooltime.data.sync.CloudSyncManager
import com.floresvalle.schooltime.util.AuthPreferences
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val userDao = db.userDao()
    private val sessionDao = db.sessionDao()
    private val evaluationDao = db.evaluationDao()
    private val cloudSync = CloudSyncManager(application)

    val currentUserId: String
        get() = Firebase.auth.currentUser?.uid ?: AuthPreferences.getLastUserId(getApplication()) ?: ""

    val currentUserData: StateFlow<UserEntity?> = flow {
        val uid = currentUserId
        if (uid.isNotBlank()) {
            emitAll(userDao.getUserByIdFlow(uid))
        } else {
            emitAll(userDao.getFirstUserFlow())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeSessions: StateFlow<List<ClassSessionEntity>> = flow {
        emitAll(sessionDao.getAllActiveSessions(currentUserId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = flow {
        emitAll(evaluationDao.getTasks(currentUserId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams: StateFlow<List<ExamEntity>> = flow {
        emitAll(evaluationDao.getExams(currentUserId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weightedAverage: StateFlow<Double> = currentUserData.map { it?.weightedAverage ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val courseCount: StateFlow<Int> = activeSessions.map { sessions ->
        sessions.map { it.courseName }.distinct().size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedCoursesCount: StateFlow<Int> = flowOf(0)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failedPasswordAttempts = MutableStateFlow(0)
    val isPasswordDialogLocked = MutableStateFlow(false)
    val generatedOtp = MutableStateFlow<String?>(null)

    fun updateProfilePhoto(uriString: String) {
        viewModelScope.launch {
            val uid = currentUserId
            if (uid.isNotBlank()) {
                userDao.updatePhotoUri(uid, uriString)
                val user = userDao.getUserById(uid)
                if (user != null) {
                    cloudSync.syncUserProfile(user)
                }
            }
        }
    }

    fun updateProfileRestricted(
        newCareer: String,
        newEmail: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val uid = currentUserId
                if (uid.isNotBlank()) {
                    userDao.updateRestrictedProfile(uid, newCareer, newEmail)
                    val user = userDao.getUserById(uid)
                    if (user != null) {
                        cloudSync.syncUserProfile(user)
                    }
                    onSuccess()
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error al actualizar")
            }
        }
    }

    fun reauthenticateAndSendOtp(
        currentPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val user = Firebase.auth.currentUser
                if (user != null && user.email != null) {
                    val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, currentPass)
                    user.reauthenticate(credential).await()
                    val otp = (100000..999999).random().toString()
                    generatedOtp.value = otp
                    onSuccess()
                } else {
                    val local = userDao.getUserById(currentUserId)
                    if (local != null && local.password == currentPass) {
                        val otp = (100000..999999).random().toString()
                        generatedOtp.value = otp
                        onSuccess()
                    } else {
                        onError("Contraseña actual incorrecta")
                    }
                }
            } catch (e: Exception) {
                onError("Contraseña incorrecta")
            }
        }
    }

    fun confirmPasswordChangeWithOtp(
        enteredOtp: String,
        newPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (enteredOtp != generatedOtp.value) {
            onError("Código OTP incorrecto")
            return
        }
        viewModelScope.launch {
            try {
                Firebase.auth.currentUser?.updatePassword(newPass)?.await()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Error al actualizar contraseña")
            }
        }
    }

    fun sendForgotPasswordEmail(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val email = currentUserData.value?.email ?: Firebase.auth.currentUser?.email
                if (email != null) {
                    Firebase.auth.sendPasswordResetEmail(email).await()
                    onSuccess()
                } else {
                    onError("No se encontró el correo del usuario")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error al enviar correo de recuperación")
            }
        }
    }

    fun generateAcademicSheetPdf(onPdfGenerated: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(getApplication<Application>().cacheDir, "Ficha_Academica_SchoolTime.txt")
                val text = buildString {
                    appendLine("=========================================")
                    appendLine("         FICHA ACADÉMICA - SCHOOLTIME    ")
                    appendLine("=========================================")
                    val user = currentUserData.value
                    appendLine("Estudiante: ${user?.firstName} ${user?.lastName}")
                    appendLine("Carrera: ${user?.career}")
                    appendLine("Ciclo: ${user?.currentCycle} | Semestre: ${user?.currentSemester}")
                    appendLine("Promedio Ponderado: ${user?.weightedAverage}")
                    appendLine("=========================================")
                }
                file.writeText(text)
                onPdfGenerated(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearLocalRoomDatabase(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            AppDatabase.clearAllData(getApplication())
            AuthPreferences.clearSession(getApplication())
            onDone()
        }
    }
}
