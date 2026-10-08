package com.floresvalle.schooltime.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalle.schooltime.data.AppDatabase
import com.floresvalle.schooltime.data.entity.AcademicPeriodEntity
import com.floresvalle.schooltime.data.entity.ClassSessionEntity
import com.floresvalle.schooltime.data.entity.CourseEntity
import com.floresvalle.schooltime.data.entity.ExamEntity
import com.floresvalle.schooltime.data.entity.NotificationEntity
import com.floresvalle.schooltime.data.entity.TaskEntity
import com.floresvalle.schooltime.data.entity.UserEntity
import com.floresvalle.schooltime.data.network.toSyncMap
import com.floresvalle.schooltime.data.sync.CloudSyncManager
import com.floresvalle.schooltime.scheduler.AlarmScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val sessionDao = db.sessionDao()
    private val evaluationDao = db.evaluationDao()
    private val userDao = db.userDao()
    private val notificationDao = db.notificationDao()
    private val academicPeriodDao = db.academicPeriodDao()
    private val courseDao = db.courseDao()

    private val scheduler = AlarmScheduler(application)

    private val currentUserIdFlow = MutableStateFlow(Firebase.auth.currentUser?.uid ?: "")

    val currentUserId: String
        get() = Firebase.auth.currentUser?.uid ?: currentUserIdFlow.value

    fun updateCurrentUserId() {
        val uid = Firebase.auth.currentUser?.uid ?: ""
        currentUserIdFlow.value = uid
        if (uid.isNotBlank()) {
            restoreUserDataIfMissing()
        }
    }

    init {
        Firebase.auth.addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid ?: ""
            currentUserIdFlow.value = uid
            if (uid.isNotBlank()) {
                viewModelScope.launch(Dispatchers.IO) {
                    restoreUserDataIfMissing()
                }
            }
        }
    }

    val currentUserData: StateFlow<UserEntity?> = currentUserIdFlow
        .flatMapLatest { uid ->
            if (uid.isNotBlank()) userDao.getUserById(uid) else flowOf(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val activeAcademicPeriod: StateFlow<AcademicPeriodEntity?> = currentUserIdFlow
        .flatMapLatest { uid ->
            if (uid.isNotBlank()) academicPeriodDao.getActivePeriod(uid) else flowOf(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val sessions: StateFlow<List<ClassSessionEntity>> = currentUserIdFlow
        .flatMapLatest { uid ->
            if (uid.isNotBlank()) sessionDao.getAllActiveSessions(uid) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val tasks: StateFlow<List<TaskEntity>> = currentUserIdFlow
        .flatMapLatest { uid ->
            if (uid.isNotBlank()) evaluationDao.getAllActiveTasks(uid) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val exams: StateFlow<List<ExamEntity>> = currentUserIdFlow
        .flatMapLatest { uid ->
            if (uid.isNotBlank()) evaluationDao.getAllActiveExams(uid) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dbNotifications: StateFlow<List<NotificationEntity>> = currentUserIdFlow
        .flatMapLatest { uid ->
            if (uid.isNotBlank()) notificationDao.getNotificationsForUser(uid) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isRefreshing = MutableStateFlow(false)

    fun restoreUserDataIfMissing() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = Firebase.auth.currentUser ?: return@launch
                val uid = user.uid
                val email = user.email ?: ""

                var existingInRoom = userDao.getUserByEmail(email) ?: userDao.getUserByIdOnce(uid)

                if (existingInRoom == null || existingInRoom.career.isBlank() || existingInRoom.career == "General") {
                    val cloudUser = CloudSyncManager.fetchUserProfile(uid, email)
                    if (cloudUser != null) {
                        userDao.insertUser(cloudUser)
                        existingInRoom = cloudUser
                        Log.d("SchoolTimeSync", "Perfil de usuario restaurado de Firestore: ${cloudUser.email} (${cloudUser.career})")
                    }
                }

                val userToSync = existingInRoom
                if (userToSync != null) {
                    userDao.insertUser(userToSync)
                    CloudSyncManager.syncUserProfile(userToSync)
                    val activePeriod = academicPeriodDao.getActivePeriodSync(uid)
                    if (activePeriod != null) {
                        CloudSyncManager.syncAcademicPeriod(activePeriod)
                    }
                    Log.d("SchoolTimeSync", "Perfil y periodo sincronizados con Firestore: ${userToSync.email}")
                } else {
                    Log.d("SchoolTimeSync", "Sin perfil académico en Room ni en Firestore; se espera onboarding.")
                }

                // Restaurar todas las entidades (cursos, clases, tareas, exámenes) desde Cloud Firestore
                CloudSyncManager.restoreAllFromCloud(uid, db)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun getOrCreateCourse(targetUid: String, courseName: String, docente: String?): CourseEntity {
        var period = academicPeriodDao.getActivePeriodSync(targetUid)
        if (period == null) {
            period = AcademicPeriodEntity(
                id = UUID.randomUUID().toString(),
                userId = targetUid,
                periodName = "2026-I",
                startDate = LocalDate.now().toString(),
                endDate = LocalDate.now().plusMonths(4).toString(),
                isActive = true
            )
            academicPeriodDao.upsertPeriod(period)
            CloudSyncManager.syncAcademicPeriod(period)
        } else {
            // Asegurar que el periodo existente esté sincronizado en la nube
            CloudSyncManager.syncAcademicPeriod(period)
        }

        var course = courseDao.getCourseByName(courseName, targetUid)
        if (course == null) {
            course = CourseEntity(
                id = UUID.randomUUID().toString(),
                userId = targetUid,
                periodId = period.id,
                name = courseName,
                teacherName = docente,
                colorHex = "#0E7490",
                credits = 3
            )
            courseDao.upsertCourse(course)
            CloudSyncManager.syncCourse(course)
        } else {
            // Asegurar sincronización
            CloudSyncManager.syncCourse(course)
        }
        return course
    }

    sealed class NextClassState {
        object None : NextClassState()
        data class InProgress(val sessions: List<ClassSessionEntity>, val timeRemainingText: String) : NextClassState()
        data class Imminent(val session: ClassSessionEntity, val timeUntilText: String) : NextClassState()
        data class Relaxed(val session: ClassSessionEntity, val futureDateText: String) : NextClassState()
    }

    private val tickerFlow = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(30000)
        }
    }

    val nextClassState: StateFlow<NextClassState> = combine(sessions, tickerFlow) { sessionList, now ->
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val validSessions = sessionList.mapNotNull { session ->
            try {
                val start = LocalDateTime.parse("${session.sessionDate} ${session.startTime}", formatter)
                val end = LocalDateTime.parse("${session.sessionDate} ${session.endTime}", formatter)
                Triple(session, start, end)
            } catch (e: Exception) {
                null
            }
        }.filter { it.third.isAfter(now) }.sortedWith(
            compareBy<Triple<ClassSessionEntity, LocalDateTime, LocalDateTime>> { it.second }
                .thenBy {
                    // Prioritize: Presencial over Virtual if simultaneous, then alphabetical
                    if (it.first.modality.equals("Presencial", ignoreCase = true)) 0 else 1
                }
                .thenBy { it.first.courseName }
        )

        // Clases que están ocurriendo ahora mismo simultáneamente
        val ongoingSessions = validSessions.filter { !it.second.isAfter(now) && it.third.isAfter(now) }
        if (ongoingSessions.isNotEmpty()) {
            val primary = ongoingSessions.first()
            val totalMinsLeft = ChronoUnit.MINUTES.between(now, primary.third)
            val hours = totalMinsLeft / 60
            val mins = totalMinsLeft % 60
            val timeText = if (hours > 0) {
                if (mins > 0) "${hours} h ${mins} min" else "${hours} h"
            } else {
                "${mins} min"
            }
            return@combine NextClassState.InProgress(
                sessions = ongoingSessions.map { it.first },
                timeRemainingText = timeText
            )
        }

        val next = validSessions.firstOrNull()
        if (next != null) {
            val totalMinsUntil = ChronoUnit.MINUTES.between(now, next.second)
            val hoursUntil = totalMinsUntil / 60
            val minsUntil = totalMinsUntil % 60
            if (hoursUntil <= 48) {
                val text = if (hoursUntil > 0) {
                    if (minsUntil > 0) "${hoursUntil} h y ${minsUntil} min" else "${hoursUntil} h"
                } else {
                    "$minsUntil min"
                }
                return@combine NextClassState.Imminent(next.first, text)
            } else {
                val outFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMM", Locale.getDefault())
                val dateStr = next.second.format(outFormatter).replaceFirstChar { it.uppercase() }
                return@combine NextClassState.Relaxed(next.first, dateStr)
            }
        }
        NextClassState.None
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NextClassState.None)

    val urgentTasksCount: StateFlow<Int> = combine(tasks, tickerFlow) { taskList, now ->
        taskList.count { task ->
            if (task.status == "Completado") return@count false
            try {
                val limit = LocalDateTime.parse("${task.dueDate} 23:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                limit.isAfter(now) && ChronoUnit.HOURS.between(now, limit) <= 48
            } catch (e: Exception) { false }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val urgentExamsCount: StateFlow<Int> = combine(exams, tickerFlow) { examList, now ->
        examList.count { exam ->
            if (exam.status == "Completado") return@count false
            try {
                val limit = LocalDateTime.parse("${exam.examDate} 23:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                limit.isAfter(now) && ChronoUnit.HOURS.between(now, limit) <= 48
            } catch (e: Exception) { false }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val averageGrade: StateFlow<Double?> = combine(tasks, exams) { taskList, examList ->
        val gradedTasks = taskList.mapNotNull { it.grade }
        val gradedExams = examList.mapNotNull { it.grade }
        val allGrades = gradedTasks + gradedExams
        if (allGrades.isNotEmpty()) {
            allGrades.average()
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val liveNotifications: StateFlow<List<NotificationEntity>> = combine(dbNotifications, sessions, tasks, exams, tickerFlow) { dbNotifs, sessionList, taskList, examList, now ->
        val generated = mutableListOf<NotificationEntity>()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        sessionList.forEach { session ->
            try {
                val start = LocalDateTime.parse("${session.sessionDate} ${session.startTime}", formatter)
                val minsDiff = ChronoUnit.MINUTES.between(start, now)
                val isVirtual = session.modality.equals("Virtual", ignoreCase = true)
                if (minsDiff in -15..30) {
                    val desc = if (isVirtual) {
                        "Tu clase virtual de ${session.courseName} ya inició. Toca para unirte."
                    } else {
                        "Tu clase presencial de ${session.courseName} ya inició en ${session.locationRoom ?: "tu aula asignada"}."
                    }
                    generated.add(
                        NotificationEntity(
                            id = "LIVE_CLASS_${session.id}",
                            userId = currentUserId,
                            title = if (isVirtual) "Clase Virtual Iniciada: ${session.courseName}" else "Clase en Curso: ${session.courseName}",
                            description = desc,
                            category = "Clases",
                            isRead = false,
                            virtualUrl = session.virtualUrl,
                            timestamp = System.currentTimeMillis(),
                            eventDate = session.sessionDate
                        )
                    )
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        taskList.filter { it.status != "Completado" }.forEach { task ->
            try {
                val limit = LocalDateTime.parse("${task.dueDate} 23:59", formatter)
                val hours = ChronoUnit.HOURS.between(now, limit)
                if (hours in 0..48) {
                    generated.add(
                        NotificationEntity(
                            id = "LIVE_TASK_${task.id}",
                            userId = currentUserId,
                            title = "Tarea Próxima a Vencer: ${task.title}",
                            description = "${task.courseName} • Vence en $hours horas (${task.dueDate})",
                            category = "Tareas",
                            isRead = false,
                            timestamp = System.currentTimeMillis() - 1000,
                            eventDate = task.dueDate
                        )
                    )
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        examList.filter { it.status != "Completado" }.forEach { exam ->
            try {
                val limit = LocalDateTime.parse("${exam.examDate} 23:59", formatter)
                val hours = ChronoUnit.HOURS.between(now, limit)
                if (hours in 0..48) {
                    generated.add(
                        NotificationEntity(
                            id = "LIVE_EXAM_${exam.id}",
                            userId = currentUserId,
                            title = "Examen Próximo: ${exam.type}",
                            description = "${exam.courseName} • Programado para el ${exam.examDate}",
                            category = "Exámenes",
                            isRead = false,
                            timestamp = System.currentTimeMillis() - 2000,
                            eventDate = exam.examDate
                        )
                    )
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        val all = (generated + dbNotifs).distinctBy { it.id }.filter { !it.isDismissed }
        all.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            updateCurrentUserId()

            try {
                if (currentUserId.isNotBlank()) {
                    val localUser = db.userDao().getUserByIdOnce(currentUserId)
                    if (localUser != null) {
                        CloudSyncManager.syncUserProfile(localUser)
                    }
                }
                CloudSyncManager.restoreAllFromCloud(currentUserId, db)
                sessions.value.forEach { CloudSyncManager.syncSession(it) }
                tasks.value.forEach { CloudSyncManager.syncTask(it) }
                exams.value.forEach { CloudSyncManager.syncExam(it) }
            } catch (e: Exception) {
                Log.e("SchoolTimeSync", "Error al refrescar y sincronizar con la nube: ${e.localizedMessage}")
            }

            delay(1000)
            isRefreshing.value = false
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                notificationDao.markAllAsRead(currentUserId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dismissNotification(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                notificationDao.dismissNotification(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateSessionDetails(sessionId: String, courseName: String? = null, docente: String?, startTime: String, endTime: String, sessionDate: String, updateAllRecurring: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val originalSession = sessionDao.getSessionById(sessionId) ?: return@launch
                val targetCourseName = courseName?.trim()?.ifBlank { null } ?: originalSession.courseName
                val oldStart = originalSession.startTime
                val oldEnd = originalSession.endTime
                val oldCourseName = originalSession.courseName
                val oldDate = originalSession.sessionDate
                val targetUid = originalSession.userId

                if (updateAllRecurring) {
                    val allUserSessions = sessionDao.getAllActiveSessionsSync(targetUid)
                    val sameScheduleSessions = allUserSessions.filter {
                        it.courseName == oldCourseName && it.startTime == oldStart && it.endTime == oldEnd
                    }
                    sameScheduleSessions.forEach { otherSession ->
                        sessionDao.updateSessionDetails(
                            sessionId = otherSession.id,
                            newName = targetCourseName,
                            docente = docente ?: otherSession.docente,
                            startTime = startTime,
                            endTime = endTime,
                            sessionDate = otherSession.sessionDate
                        )
                        val syncedOther = sessionDao.getSessionById(otherSession.id)
                        if (syncedOther != null) {
                            CloudSyncManager.syncSession(syncedOther)
                        }
                    }
                } else {
                    sessionDao.updateSessionDetails(
                        sessionId = sessionId,
                        newName = targetCourseName,
                        docente = docente ?: originalSession.docente,
                        startTime = startTime,
                        endTime = endTime,
                        sessionDate = sessionDate
                    )
                    val updated = sessionDao.getSessionById(sessionId)
                    if (updated != null) {
                        CloudSyncManager.syncSession(updated)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteSession(sessionId: String, deleteAllRecurring: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val session = sessionDao.getSessionById(sessionId) ?: return@launch
                if (deleteAllRecurring) {
                    val allUserSessions = sessionDao.getAllActiveSessionsSync(session.userId)
                    val sameScheduleSessions = allUserSessions.filter {
                        it.courseName == session.courseName && it.startTime == session.startTime && it.endTime == session.endTime
                    }
                    sameScheduleSessions.forEach { s ->
                        sessionDao.softDeleteSession(s.id)
                        CloudSyncManager.deleteFromCloud(s.userId, "class_sessions", s.id)
                    }
                } else {
                    sessionDao.softDeleteSession(sessionId)
                    CloudSyncManager.deleteFromCloud(session.userId, "class_sessions", sessionId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTaskDateTime(taskId: String, dueDate: String, dueTime: String) {
        updateTaskDetails(taskId, null, dueDate, dueTime)
    }

    fun updateTaskDetails(taskId: String, title: String?, dueDate: String, dueTime: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentTask = tasks.value.find { it.id == taskId }
                val targetTitle = title?.trim()?.ifBlank { null } ?: currentTask?.title ?: ""
                evaluationDao.updateTaskDetails(taskId, targetTitle, dueDate, dueTime)
                val updated = tasks.value.find { it.id == taskId }?.copy(title = targetTitle, dueDate = dueDate, dueTime = dueTime)
                if (updated != null) {
                    CloudSyncManager.syncTask(updated)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                evaluationDao.softDeleteTask(taskId)
                if (currentUserId.isNotBlank()) {
                    CloudSyncManager.deleteFromCloud(currentUserId, "tasks", taskId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateExamDateTime(examId: String, examDate: String, examTime: String) {
        updateExamDetails(examId, null, examDate, examTime)
    }

    fun updateExamDetails(examId: String, type: String?, examDate: String, examTime: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentExam = exams.value.find { it.id == examId }
                val targetType = type?.trim()?.ifBlank { null } ?: currentExam?.type ?: ""
                evaluationDao.updateExamDetails(examId, targetType, examDate, examTime)
                val updated = exams.value.find { it.id == examId }?.copy(type = targetType, examDate = examDate, examTime = examTime)
                if (updated != null) {
                    CloudSyncManager.syncExam(updated)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteExam(examId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                evaluationDao.softDeleteExam(examId)
                if (currentUserId.isNotBlank()) {
                    CloudSyncManager.deleteFromCloud(currentUserId, "exams", examId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun completeTaskWithGrade(taskId: String, grade: Double?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                evaluationDao.updateTaskStatusAndGrade(taskId, "Completado", grade)
                val updated = tasks.value.find { it.id == taskId }
                if (updated != null) {
                    CloudSyncManager.syncTask(updated)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markTaskAsNotDelivered(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                evaluationDao.updateTaskStatusAndGrade(taskId, "No entregado", null)
                val updated = tasks.value.find { it.id == taskId }
                if (updated != null) {
                    CloudSyncManager.syncTask(updated)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun completeExamWithGrade(examId: String, grade: Double?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                evaluationDao.updateExamStatusAndGrade(examId, "Completado", grade)
                val updated = exams.value.find { it.id == examId }
                if (updated != null) {
                    CloudSyncManager.syncExam(updated)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markExamAsNotDelivered(examId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                evaluationDao.updateExamStatusAndGrade(examId, "No entregado", null)
                val updated = exams.value.find { it.id == examId }
                if (updated != null) {
                    CloudSyncManager.syncExam(updated)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addSession(
        courseName: String,
        sessionDate: String,
        startTime: String,
        endTime: String,
        modality: String,
        locationRoom: String?,
        virtualUrl: String?,
        docente: String?,
        color: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                updateCurrentUserId()
                val targetUid = currentUserId

                // 1° & 2°: Get/create period and course to satisfy Foreign Keys
                val course = getOrCreateCourse(targetUid, courseName, docente)

                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val baseLocalDate = try {
                    LocalDate.parse(sessionDate, formatter)
                } catch (e: Exception) {
                    LocalDate.now()
                }

                val period = academicPeriodDao.getActivePeriodSync(targetUid)
                val endLocalDate = try {
                    if (!period?.endDate.isNullOrBlank()) {
                        LocalDate.parse(period.endDate, formatter)
                    } else {
                        baseLocalDate.plusMonths(4)
                    }
                } catch (e: Exception) {
                    baseLocalDate.plusMonths(4)
                }

                val sessionsToInsert = mutableListOf<ClassSessionEntity>()
                var currentLocalDate = baseLocalDate

                while (!currentLocalDate.isAfter(endLocalDate)) {
                    val dateStr = currentLocalDate.format(formatter)
                    val newSession = ClassSessionEntity(
                        id = UUID.randomUUID().toString(),
                        userId = targetUid,
                        courseId = course.id,
                        courseName = courseName,
                        sessionDate = dateStr,
                        startTime = startTime,
                        endTime = endTime,
                        modality = modality,
                        locationRoom = locationRoom,
                        virtualUrl = virtualUrl,
                        docente = docente,
                        color = color
                    )
                    sessionsToInsert.add(newSession)
                    currentLocalDate = currentLocalDate.plusWeeks(1)
                }

                // Save in Room FIRST (Instant UI update!)
                sessionDao.upsertSessions(sessionsToInsert)

                // Schedule local alarms
                sessionsToInsert.take(5).forEach { session ->
                    scheduler.scheduleClassReminders(session)
                }

                // 3°: Sync sessions to Cloud
                sessionsToInsert.forEach { session ->
                    CloudSyncManager.syncSession(session)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addTask(
        courseName: String,
        title: String,
        dueDate: String,
        dueTime: String,
        modality: String,
        urgency: String,
        link: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                updateCurrentUserId()
                val targetUid = currentUserId

                // 1° & 2°: Get/create period and course to satisfy Foreign Keys
                val course = getOrCreateCourse(targetUid, courseName, null)

                val task = TaskEntity(
                    id = UUID.randomUUID().toString(),
                    userId = targetUid,
                    courseId = course.id,
                    courseName = courseName,
                    title = title,
                    dueDate = dueDate,
                    dueTime = dueTime,
                    modality = modality,
                    urgency = urgency,
                    link = link
                )

                // Save in Room FIRST
                evaluationDao.upsertTask(task)
                scheduler.scheduleTaskReminders(task)

                // 3°: Sync task to Cloud
                CloudSyncManager.syncTask(task)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addExam(
        courseName: String,
        type: String,
        examDate: String,
        examTime: String,
        modality: String,
        location: String?,
        weight: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                updateCurrentUserId()
                val targetUid = currentUserId

                // 1° & 2°: Get/create period and course to satisfy Foreign Keys
                val course = getOrCreateCourse(targetUid, courseName, null)

                val exam = ExamEntity(
                    id = UUID.randomUUID().toString(),
                    userId = targetUid,
                    courseId = course.id,
                    courseName = courseName,
                    type = type,
                    examDate = examDate,
                    examTime = examTime,
                    modality = modality,
                    location = location,
                    weight = weight
                )

                // Save in Room FIRST
                evaluationDao.upsertExam(exam)
                scheduler.scheduleExamReminders(exam)

                // 3°: Sync exam to Cloud
                CloudSyncManager.syncExam(exam)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
