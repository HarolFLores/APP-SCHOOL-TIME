package com.floresvalle.schooltime.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalle.schooltime.data.AppDatabase
import com.floresvalle.schooltime.data.entity.AcademicPeriodEntity
import com.floresvalle.schooltime.data.entity.ClassSessionEntity
import com.floresvalle.schooltime.data.entity.ExamEntity
import com.floresvalle.schooltime.data.entity.TaskEntity
import com.floresvalle.schooltime.data.entity.UserEntity
import com.floresvalle.schooltime.data.sync.CloudSyncManager
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
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
    private val academicPeriodDao = db.academicPeriodDao()

    val currentUserId: String
        get() = Firebase.auth.currentUser?.uid ?: ""

    val currentUserData: StateFlow<UserEntity?> = userDao.getUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val activeAcademicPeriod: StateFlow<AcademicPeriodEntity?> = academicPeriodDao.getActivePeriod(currentUserId)
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

    val tasks: StateFlow<List<TaskEntity>> = evaluationDao.getAllActiveTasks(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams: StateFlow<List<ExamEntity>> = evaluationDao.getAllActiveExams(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weighted average matching HomeScreen calculation exactly (null if no evaluations recorded)
    val weightedAverage: StateFlow<Double?> = combine(tasks, exams) { taskList, examList ->
        val gradedTasks = taskList.mapNotNull { it.grade }
        val gradedExams = examList.mapNotNull { it.grade }
        val all = gradedTasks + gradedExams
        if (all.isNotEmpty()) all.average() else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val courseCount: StateFlow<Int> = combine(activeSessions) { sessionsList ->
        if (sessionsList.isNotEmpty()) {
            sessionsList.first().map { it.courseName.trim().lowercase() }.distinct().size
        } else {
            0
        }
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

    /**
     * Updates only Name and Career (Email is immutable as it is the verified institutional credential).
     */
    fun updateProfileNameAndCareer(
        newFullName: String,
        newCareer: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val trimmedName = newFullName.trim()
                val trimmedCareer = newCareer.trim()
                if (trimmedName.isBlank()) {
                    withContext(Dispatchers.Main) { onError("El nombre no puede estar vacío.") }
                    return@launch
                }
                if (trimmedCareer.isBlank()) {
                    withContext(Dispatchers.Main) { onError("La carrera profesional no puede estar vacía.") }
                    return@launch
                }

                val parts = trimmedName.split(" ", limit = 2)
                val firstName = parts[0]
                val lastName = if (parts.size > 1) parts[1] else ""

                userDao.updateNameAndCareer(currentUserId, firstName, lastName, trimmedCareer)
                val updated = userDao.getUserByIdOnce(currentUserId)
                if (updated != null) {
                    CloudSyncManager.syncUserProfile(updated)
                }

                // Update Firebase Auth displayName
                val firebaseUser = Firebase.auth.currentUser
                if (firebaseUser != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(trimmedName)
                        .build()
                    firebaseUser.updateProfile(profileUpdates).await()
                }

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Error al actualizar perfil.")
                }
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

    /**
     * Generates a modern, executive academic sheet in PDF format with corporate styling.
     */
    fun generateAcademicSheetPdf(context: Context, onPdfGenerated: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = currentUserData.value
                val period = activeAcademicPeriod.value
                val sessionsList = activeSessions.value
                val taskList = tasks.value
                val examList = exams.value
                val avg = weightedAverage.value

                val fullName = user?.let { "${it.firstName} ${it.lastName}".trim() }
                    ?: Firebase.auth.currentUser?.displayName
                    ?: "Estudiante Universitario"
                val email = user?.email ?: Firebase.auth.currentUser?.email ?: "correo@uni.edu.pe"
                val career = user?.career ?: "Carrera Profesional"
                val cycle = user?.currentCycle ?: "1er Ciclo"
                val semester = period?.periodName ?: user?.currentSemester ?: "2026-I"

                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (595 x 842 pt)
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Paints
                val brandPrimary = Color.parseColor("#006C88")
                val brandDark = Color.parseColor("#004D61")
                val brandLight = Color.parseColor("#E0F7FA")
                val textDark = Color.parseColor("#1E293B")
                val textMuted = Color.parseColor("#64748B")
                val tableHeaderBg = Color.parseColor("#006C88")
                val tableRowAlt = Color.parseColor("#F8FAFC")
                val borderGray = Color.parseColor("#E2E8F0")

                val brandHeaderPaint = Paint().apply { color = brandPrimary }
                val brandAccentPaint = Paint().apply { color = brandDark }
                val cardBgPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
                val cardBorderPaint = Paint().apply {
                    color = borderGray
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }

                val titlePaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 22f
                    isFakeBoldText = true
                }
                val subtitlePaint = Paint().apply {
                    color = Color.parseColor("#B2EBF2")
                    textSize = 10f
                    letterSpacing = 0.08f
                    isFakeBoldText = true
                }

                val sectionTitlePaint = Paint().apply {
                    color = brandPrimary
                    textSize = 12f
                    isFakeBoldText = true
                    letterSpacing = 0.05f
                }

                val bodyPaint = Paint().apply {
                    color = textDark
                    textSize = 9.5f
                }
                val boldBodyPaint = Paint().apply {
                    color = textDark
                    textSize = 9.5f
                    isFakeBoldText = true
                }
                val mutedPaint = Paint().apply {
                    color = textMuted
                    textSize = 8.5f
                }

                // 1. Top Header Banner
                canvas.drawRect(0f, 0f, 595f, 105f, brandHeaderPaint)
                canvas.drawRect(0f, 102f, 595f, 105f, brandAccentPaint)

                canvas.drawText("SchoolTime", 40f, 48f, titlePaint)
                canvas.drawText("REPORTE ACADÉMICO OFICIAL Y FICHA DE MATRÍCULA", 40f, 66f, subtitlePaint)

                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                canvas.drawText("EMITIDO: $dateStr", 40f, 85f, subtitlePaint)

                // Status Badge on top right
                val badgePaint = Paint().apply { color = Color.parseColor("#2E7D32") }
                canvas.drawRoundRect(420f, 34f, 555f, 62f, 14f, 14f, badgePaint)
                val badgeTextPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 9f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("ESTUDIANTE REGULAR", 487f, 51f, badgeTextPaint)

                // 2. Student Info Card
                var y = 125f
                canvas.drawRoundRect(40f, y, 555f, y + 80f, 12f, 12f, cardBgPaint)
                canvas.drawRoundRect(40f, y, 555f, y + 80f, 12f, 12f, cardBorderPaint)

                canvas.drawText("DATOS DEL ESTUDIANTE", 55f, y + 20f, sectionTitlePaint)

                canvas.drawText("Estudiante:", 55f, y + 40f, mutedPaint)
                canvas.drawText(fullName, 120f, y + 40f, boldBodyPaint)

                canvas.drawText("Correo:", 55f, y + 56f, mutedPaint)
                canvas.drawText(email, 120f, y + 56f, bodyPaint)

                canvas.drawText("Carrera:", 55f, y + 72f, mutedPaint)
                canvas.drawText(career, 120f, y + 72f, boldBodyPaint)

                // Right column of Student Info
                canvas.drawText("Ciclo / Semestre:", 330f, y + 40f, mutedPaint)
                canvas.drawText("$cycle • $semester", 420f, y + 40f, boldBodyPaint)

                canvas.drawText("Periodo Activo:", 330f, y + 56f, mutedPaint)
                canvas.drawText(period?.periodName ?: semester, 420f, y + 56f, bodyPaint)

                canvas.drawText("Estado Matrícula:", 330f, y + 72f, mutedPaint)
                canvas.drawText("Confirmada / Al Día", 420f, y + 72f, boldBodyPaint)

                // 3. Academic Metrics KPI Row
                y += 95f
                val kpiWidth = 160f
                val kpiHeight = 48f

                // KPI 1: Promedio Ponderado
                canvas.drawRoundRect(40f, y, 40f + kpiWidth, y + kpiHeight, 10f, 10f, cardBgPaint)
                canvas.drawRoundRect(40f, y, 40f + kpiWidth, y + kpiHeight, 10f, 10f, cardBorderPaint)
                canvas.drawText("RENDIMIENTO PROMEDIO", 52f, y + 16f, mutedPaint)
                val avgText = if (avg != null) String.format(Locale.getDefault(), "%.2f", avg) else "--"
                val kpiValuePaint = Paint().apply {
                    color = brandPrimary
                    textSize = 15f
                    isFakeBoldText = true
                }
                canvas.drawText("$avgText / 20.00 pts", 52f, y + 36f, kpiValuePaint)

                // KPI 2: Cursos Matriculados
                val distinctCoursesCount = sessionsList.map { it.courseName.trim().lowercase() }.distinct().size
                canvas.drawRoundRect(217f, y, 217f + kpiWidth, y + kpiHeight, 10f, 10f, cardBgPaint)
                canvas.drawRoundRect(217f, y, 217f + kpiWidth, y + kpiHeight, 10f, 10f, cardBorderPaint)
                canvas.drawText("CURSOS MATRICULADOS", 229f, y + 16f, mutedPaint)
                canvas.drawText("$distinctCoursesCount asignaturas", 229f, y + 36f, kpiValuePaint)

                // KPI 3: Evaluaciones Registradas
                val totalEvals = taskList.size + examList.size
                canvas.drawRoundRect(395f, y, 395f + kpiWidth, y + kpiHeight, 10f, 10f, cardBgPaint)
                canvas.drawRoundRect(395f, y, 395f + kpiWidth, y + kpiHeight, 10f, 10f, cardBorderPaint)
                canvas.drawText("EVALUACIONES TOTALES", 407f, y + 16f, mutedPaint)
                canvas.drawText("$totalEvals registradas", 407f, y + 36f, kpiValuePaint)

                // 4. Courses Table
                y += 65f
                canvas.drawText("HORARIO Y ASIGNATURAS INSCRITAS", 40f, y, sectionTitlePaint)
                y += 8f

                // Table Header
                val thPaint = Paint().apply { color = tableHeaderBg }
                val thTextPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 8.5f
                    isFakeBoldText = true
                }
                canvas.drawRoundRect(40f, y, 555f, y + 20f, 6f, 6f, thPaint)
                canvas.drawText("CURSO / ASIGNATURA", 50f, y + 14f, thTextPaint)
                canvas.drawText("HORARIO", 230f, y + 14f, thTextPaint)
                canvas.drawText("MODALIDAD", 320f, y + 14f, thTextPaint)
                canvas.drawText("DOCENTE", 400f, y + 14f, thTextPaint)
                canvas.drawText("AULA / SALA", 490f, y + 14f, thTextPaint)

                y += 20f

                val distinctCourses = sessionsList.groupBy { it.courseName }
                if (distinctCourses.isEmpty()) {
                    canvas.drawText("No hay cursos registrados en el horario actual.", 50f, y + 18f, mutedPaint)
                    y += 24f
                } else {
                    var isAlt = false
                    val altPaint = Paint().apply { color = tableRowAlt }
                    distinctCourses.forEach { (courseName, list) ->
                        val sample = list.first()
                        val rowHeight = 22f
                        if (isAlt) {
                            canvas.drawRect(40f, y, 555f, y + rowHeight, altPaint)
                        }

                        // Course name (shortened if necessary)
                        val displayCourse = if (courseName.length > 28) courseName.take(26) + "..." else courseName
                        canvas.drawText(displayCourse, 50f, y + 15f, boldBodyPaint)
                        canvas.drawText("${sample.startTime} - ${sample.endTime}", 230f, y + 15f, bodyPaint)
                        canvas.drawText(sample.modality, 320f, y + 15f, bodyPaint)

                        val doc = sample.docente?.ifBlank { "Por asignar" } ?: "Por asignar"
                        val displayDoc = if (doc.length > 16) doc.take(14) + "..." else doc
                        canvas.drawText(displayDoc, 400f, y + 15f, bodyPaint)

                        val room = sample.locationRoom ?: sample.virtualUrl ?: "Aula universitaria"
                        val displayRoom = if (room.length > 12) room.take(10) + "..." else room
                        canvas.drawText(displayRoom, 490f, y + 15f, bodyPaint)

                        y += rowHeight
                        isAlt = !isAlt
                    }
                }

                // 5. Tasks and Exams Summary
                y += 18f
                canvas.drawText("RESUMEN DE EVALUACIONES Y TRABAJOS PENDIENTES / REGISTRADOS", 40f, y, sectionTitlePaint)
                y += 8f

                canvas.drawRoundRect(40f, y, 555f, y + 20f, 6f, 6f, thPaint)
                canvas.drawText("ACTIVIDAD", 50f, y + 14f, thTextPaint)
                canvas.drawText("CURSO", 230f, y + 14f, thTextPaint)
                canvas.drawText("FECHA LÍMITE", 360f, y + 14f, thTextPaint)
                canvas.drawText("ESTADO / NOTA", 460f, y + 14f, thTextPaint)
                y += 20f

                val combinedEvals = (taskList.map { Triple(it.title, it.courseName, "${it.dueDate} (${it.status}${it.grade?.let { g -> " - $g pts" } ?: ""})") } +
                        examList.map { Triple(it.type, it.courseName, "${it.examDate} (${it.status}${it.grade?.let { g -> " - $g pts" } ?: ""})") }).take(7)

                if (combinedEvals.isEmpty()) {
                    canvas.drawText("Sin evaluaciones registradas en el periodo actual.", 50f, y + 18f, mutedPaint)
                    y += 24f
                } else {
                    var isAlt = false
                    val altPaint = Paint().apply { color = tableRowAlt }
                    combinedEvals.forEach { (title, course, dateStatus) ->
                        val rowHeight = 20f
                        if (isAlt) {
                            canvas.drawRect(40f, y, 555f, y + rowHeight, altPaint)
                        }
                        val displayTitle = if (title.length > 28) title.take(26) + "..." else title
                        canvas.drawText(displayTitle, 50f, y + 14f, boldBodyPaint)

                        val displayCourse = if (course.length > 20) course.take(18) + "..." else course
                        canvas.drawText(displayCourse, 230f, y + 14f, bodyPaint)
                        canvas.drawText(dateStatus, 360f, y + 14f, bodyPaint)

                        y += rowHeight
                        isAlt = !isAlt
                    }
                }

                // 6. Footer & Security Watermark
                val footerY = 790f
                val linePaint = Paint().apply {
                    color = borderGray
                    strokeWidth = 1f
                }
                canvas.drawLine(40f, footerY - 15f, 555f, footerY - 15f, linePaint)

                canvas.drawText(
                    "SchoolTime Mobile App • Ficha generada con validación de credenciales institucionales.",
                    40f,
                    footerY,
                    mutedPaint
                )
                canvas.drawText(
                    "Válido para fines informativos y seguimiento de avance académico universitario.",
                    40f,
                    footerY + 12f,
                    mutedPaint
                )

                // Stamp Box simulation on right
                canvas.drawRoundRect(420f, footerY - 10f, 555f, footerY + 25f, 6f, 6f, cardBgPaint)
                canvas.drawRoundRect(420f, footerY - 10f, 555f, footerY + 25f, 6f, 6f, cardBorderPaint)
                val stampPaint = Paint().apply {
                    color = brandPrimary
                    textSize = 8f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("REGISTRO ACADÉMICO", 487f, footerY + 6f, stampPaint)
                canvas.drawText("OFICIAL - VERIFICADO", 487f, footerY + 18f, stampPaint)

                pdfDocument.finishPage(page)

                // Save to accessible directory (App External Files Documents or FilesDir)
                val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                val safeFileName = "Ficha_Academica_${user?.firstName?.replace(" ", "_") ?: "Estudiante"}.pdf"
                val pdfFile = File(outputDir, safeFileName)
                pdfDocument.writeTo(FileOutputStream(pdfFile))
                pdfDocument.close()

                withContext(Dispatchers.Main) {
                    onPdfGenerated(pdfFile)
                }
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
