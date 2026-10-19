package com.floresvalle.schooltime.viewmodel

import android.app.Application
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalle.schooltime.data.AppDatabase
import com.floresvalle.schooltime.data.entity.ClassSessionEntity
import com.floresvalle.schooltime.data.entity.UserEntity
import com.floresvalle.schooltime.data.sync.CloudSyncManager
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val userDao = db.userDao()
    private val sessionDao = db.sessionDao()
    private val evaluationDao = db.evaluationDao()

    val currentUserId: String
        get() = Firebase.auth.currentUser?.uid ?: ""

    val currentUserData: StateFlow<UserEntity?> = userDao.getUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val activeSessions: StateFlow<List<ClassSessionEntity>> = sessionDao.getAllActiveSessions(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val tasks = evaluationDao.getAllActiveTasks(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams = evaluationDao.getAllActiveExams(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weightedAverage: StateFlow<Double> = combine(tasks, exams) { taskList, examList ->
        val gradedTasks = taskList.mapNotNull { it.grade }
        val gradedExams = examList.mapNotNull { it.grade }
        val all = gradedTasks + gradedExams
        if (all.isNotEmpty()) all.average() else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val courseCount: StateFlow<Int> = combine(activeSessions) { sessionsList ->
        sessionsList.first().map { it.courseName }.distinct().size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Password Security Dialog States
    val failedPasswordAttempts = MutableStateFlow(0)
    val isPasswordDialogLocked = MutableStateFlow(false)
    val generatedOtp = MutableStateFlow<String?>(null)

    fun updateProfilePhoto(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userDao.updatePhotoUri(currentUserId, uriString)
                val updated = userDao.getUserByIdOnce(currentUserId)
                if (updated != null) {
                    CloudSyncManager.syncUserProfile(updated)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateProfileRestricted(
        newCareer: String,
        newEmail: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = userDao.getUserByEmail(newEmail)
                if (current != null && current.id != currentUserId) {
                    onError("El correo $newEmail ya está en uso por otra cuenta.")
                    return@launch
                }

                userDao.updateUserProfile(currentUserId, newCareer, newEmail)
                val updated = userDao.getUserByIdOnce(currentUserId)
                if (updated != null) {
                    CloudSyncManager.syncUserProfile(updated)
                }

                // If email changed, trigger Firebase update email verification
                val firebaseUser = Firebase.auth.currentUser
                if (firebaseUser != null && firebaseUser.email != newEmail) {
                    firebaseUser.sendEmailVerification().await()
                }

                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al actualizar perfil.")
            }
        }
    }

    fun reauthenticateAndSendOtp(
        currentPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (isPasswordDialogLocked.value) {
            onError("Diálogo bloqueado por demasiados intentos fallidos. Inténtalo más tarde.")
            return
        }

        viewModelScope.launch {
            try {
                val user = Firebase.auth.currentUser
                if (user != null && user.email != null) {
                    val credential = EmailAuthProvider.getCredential(user.email!!, currentPass)
                    user.reauthenticate(credential).await()

                    // Reauth success: Generate 6-digit OTP
                    val otp = String.format(Locale.getDefault(), "%06d", Random.nextInt(100000, 999999))
                    generatedOtp.value = otp
                    failedPasswordAttempts.value = 0

                    onSuccess()
                } else {
                    onError("Usuario no autenticado.")
                }
            } catch (e: Exception) {
                failedPasswordAttempts.value += 1
                if (failedPasswordAttempts.value >= 3) {
                    isPasswordDialogLocked.value = true
                    onError("Demasiados intentos fallidos. Diálogo bloqueado.")
                } else {
                    onError("Contraseña actual incorrecta (${failedPasswordAttempts.value}/3 intentos).")
                }
            }
        }
    }

    fun confirmPasswordChangeWithOtp(
        enteredOtp: String,
        newPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (generatedOtp.value != enteredOtp) {
            onError("Código OTP incorrecto.")
            return
        }

        viewModelScope.launch {
            try {
                val user = Firebase.auth.currentUser
                user?.updatePassword(newPass)?.await()
                generatedOtp.value = null
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al actualizar la contraseña.")
            }
        }
    }

    fun sendForgotPasswordEmail(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = Firebase.auth.currentUser
        val email = user?.email
        if (!email.isNullOrBlank()) {
            Firebase.auth.sendPasswordResetEmail(email)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onError(it.localizedMessage ?: "Error al enviar correo.") }
        } else {
            onError("No hay un correo registrado activo.")
        }
    }

    fun generateAcademicSheetPdf(onPdfGenerated: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = currentUserData.value
                val sessionsList = activeSessions.value
                val avg = weightedAverage.value

                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val titlePaint = Paint().apply {
                    color = Color.parseColor("#006C88")
                    textSize = 22f
                    isFakeBoldText = true
                }

                val headerBgPaint = Paint().apply {
                    color = Color.parseColor("#E0F7FA")
                }

                val textPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 12f
                }

                val boldPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 13f
                    isFakeBoldText = true
                }

                // Header Banner
                canvas.drawRect(0f, 0f, 595f, 100f, headerBgPaint)
                canvas.drawText("SchoolTime - Ficha Académica", 40f, 50f, titlePaint)

                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                canvas.drawText("Generado el: $dateStr", 40f, 80f, textPaint)

                // Student Info
                var y = 140f
                canvas.drawText("INFORMACIÓN DEL ESTUDIANTE", 40f, y, boldPaint)
                y += 20f
                canvas.drawText("Nombre: ${user?.firstName ?: ""} ${user?.lastName ?: ""}", 40f, y, textPaint)
                y += 18f
                canvas.drawText("Correo: ${user?.email ?: ""}", 40f, y, textPaint)
                y += 18f
                canvas.drawText("Carrera: ${user?.career ?: "Ingeniería"}", 40f, y, textPaint)
                y += 18f
                canvas.drawText("Ciclo: ${user?.currentCycle ?: "1er Ciclo"} (${user?.currentSemester ?: "2025-I"})", 40f, y, textPaint)
                y += 18f
                canvas.drawText("Promedio Ponderado Actual: ${String.format(Locale.getDefault(), "%.2f", avg)} / 20.00", 40f, y, boldPaint)

                y += 35f
                canvas.drawText("CURSOS MATRICULADOS (${sessionsList.map { it.courseName }.distinct().size})", 40f, y, boldPaint)
                y += 20f

                val distinctCourses = sessionsList.groupBy { it.courseName }
                distinctCourses.forEach { (courseName, sessions) ->
                    val sample = sessions.first()
                    canvas.drawText("• $courseName - ${sample.modality} (${sample.docente ?: "Docente N/A"})", 50f, y, textPaint)
                    y += 18f
                }

                pdfDocument.finishPage(page)

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val pdfFile = File(downloadsDir, "Ficha_Academica_${user?.firstName ?: "Estudiante"}.pdf")
                pdfDocument.writeTo(FileOutputStream(pdfFile))
                pdfDocument.close()

                onPdfGenerated(pdfFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearLocalRoomDatabase(onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.clearAllTables()
                withContext(Dispatchers.Main) {
                    onDone()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
