package com.floresvalle.schooltime.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalle.schooltime.data.AppDatabase
import com.floresvalle.schooltime.data.dao.*
import com.floresvalle.schooltime.data.entity.*
import com.floresvalle.schooltime.data.sync.CloudSyncManager
import com.floresvalle.schooltime.scheduler.AlarmScheduler
import com.floresvalle.schooltime.util.AuthPreferences
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    val db = AppDatabase.getDatabase(application)
    val sessionDao: ClassSessionDao = db.sessionDao()
    val evaluationDao: EvaluationDao = db.evaluationDao()
    val userDao: UserDao = db.userDao()
    val notificationDao: NotificationDao = db.notificationDao()
    val academicPeriodDao: AcademicPeriodDao = db.academicPeriodDao()
    val courseDao: CourseDao = db.courseDao()
    val scheduler = AlarmScheduler(application)
    private val cloudSync = CloudSyncManager(application)

    private val currentUserIdFlow = MutableStateFlow(
        Firebase.auth.currentUser?.uid ?: AuthPreferences.getLastUserId(application) ?: ""
    )

    val currentUserId: String
        get() = currentUserIdFlow.value

    fun updateCurrentUserId(newUid: String) {
        currentUserIdFlow.value = newUid
    }

    val currentUserData: StateFlow<UserEntity?> = currentUserIdFlow.flatMapLatest { uid ->
        if (uid.isNotBlank()) userDao.getUserByIdFlow(uid) else userDao.getFirstUserFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeAcademicPeriod: StateFlow<AcademicPeriodEntity?> = currentUserIdFlow.flatMapLatest { uid ->
        academicPeriodDao.getActivePeriodFlow(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sessions: StateFlow<List<ClassSessionEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        sessionDao.getAllActiveSessions(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        evaluationDao.getTasks(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams: StateFlow<List<ExamEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        evaluationDao.getExams(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dbNotifications: StateFlow<List<NotificationEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        notificationDao.getNotifications(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = dbNotifications

    val isRefreshing = MutableStateFlow(false)

    val urgentTasksCount: StateFlow<Int> = tasks.map { list ->
        list.count { it.status == "Pendiente" && (it.urgency == "Alta" || it.urgency == "Urgente") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val urgentExamsCount: StateFlow<Int> = exams.map { list ->
        list.count { it.status == "Pendiente" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val averageGrade: StateFlow<Double> = combine(tasks, exams) { tList, eList ->
        val gradedTasks = tList.mapNotNull { it.grade }
        val gradedExams = eList.mapNotNull { it.grade }
        val allGrades = gradedTasks + gradedExams
        if (allGrades.isNotEmpty()) (allGrades.sum() / allGrades.size * 10).toInt() / 10.0 else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val tickerFlow: Flow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(30000)
        }
    }

    sealed class NextClassState {
        object NoClasses : NextClassState()
        data class Live(val session: ClassSessionEntity, val timeRemainingText: String) : NextClassState()
        data class Upcoming(val session: ClassSessionEntity, val timeRemainingText: String) : NextClassState()
    }

    val nextClassState: StateFlow<NextClassState> = combine(sessions, tickerFlow) { sessionList, now ->
        if (sessionList.isEmpty()) return@combine NextClassState.NoClasses
        val todayStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val todaySessions = sessionList.filter { it.sessionDate == todayStr }
        val nowTime = now.toLocalTime()

        for (s in todaySessions) {
            try {
                val start = LocalTime.parse(s.startTime, DateTimeFormatter.ofPattern("HH:mm"))
                val end = LocalTime.parse(s.endTime, DateTimeFormatter.ofPattern("HH:mm"))
                if (!nowTime.isBefore(start) && nowTime.isBefore(end)) {
                    val minutesLeft = java.time.Duration.between(nowTime, end).toMinutes()
                    return@combine NextClassState.Live(s, "Finaliza en $minutesLeft min")
                }
            } catch (e: Exception) {}
        }

        for (s in todaySessions) {
            try {
                val start = LocalTime.parse(s.startTime, DateTimeFormatter.ofPattern("HH:mm"))
                if (nowTime.isBefore(start)) {
                    val minutesToStart = java.time.Duration.between(nowTime, start).toMinutes()
                    val text = if (minutesToStart < 60) "Inicia en $minutesToStart min" else "Inicia a las ${s.startTime}"
                    return@combine NextClassState.Upcoming(s, text)
                }
            } catch (e: Exception) {}
        }

        val upcoming = sessionList.firstOrNull { it.sessionDate > todayStr }
        if (upcoming != null) {
            NextClassState.Upcoming(upcoming, "Próxima: ${upcoming.sessionDate} ${upcoming.startTime}")
        } else {
            NextClassState.NoClasses
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NextClassState.NoClasses)

    fun refreshData() {
        viewModelScope.launch {
            isRefreshing.value = true
            val uid = currentUserId
            if (uid.isNotBlank()) {
                cloudSync.restoreAllFromCloud(uid)
            }
            delay(600)
            isRefreshing.value = false
        }
    }

    fun restoreUserDataIfMissing() {
        viewModelScope.launch {
            val uid = currentUserId
            if (uid.isNotBlank()) {
                val user = userDao.getUserById(uid)
                if (user == null) {
                    cloudSync.restoreAllFromCloud(uid)
                }
            }
        }
    }

    suspend fun getOrCreateCourse(targetUid: String, courseName: String, docente: String?): CourseEntity {
        var course = courseDao.getCourseByName(courseName, targetUid)
        if (course == null) {
            val period = academicPeriodDao.getActivePeriod(targetUid)
            course = CourseEntity(
                userId = targetUid,
                periodId = period?.id ?: "",
                name = courseName,
                teacherName = docente
            )
            courseDao.upsertCourse(course)
            cloudSync.syncCourse(course)
        }
        return course
    }

    fun addSession(
        courseName: String,
        startTime: String,
        endTime: String,
        modality: String,
        sessionDate: String,
        locationRoom: String? = null,
        virtualUrl: String? = null,
        docente: String? = null,
        color: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = currentUserId
            val course = getOrCreateCourse(uid, courseName, docente)
            val session = ClassSessionEntity(
                userId = uid,
                courseId = course.id,
                courseName = courseName,
                sessionDate = sessionDate,
                startTime = startTime,
                endTime = endTime,
                modality = modality,
                locationRoom = locationRoom,
                virtualUrl = virtualUrl,
                docente = docente,
                color = color ?: course.colorHex
            )
            sessionDao.upsertSession(session)
            cloudSync.syncSession(session)
            scheduler.scheduleClassReminders(session)
        }
    }

    fun addTask(
        courseName: String,
        title: String,
        dueDate: String,
        dueTime: String,
        urgency: String,
        modality: String = "Individual",
        link: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = currentUserId
            val course = getOrCreateCourse(uid, courseName, null)
            val task = TaskEntity(
                userId = uid,
                courseId = course.id,
                courseName = courseName,
                title = title,
                dueDate = dueDate,
                dueTime = dueTime,
                urgency = urgency,
                modality = modality,
                link = link
            )
            evaluationDao.upsertTask(task)
            cloudSync.syncTask(task)
            scheduler.scheduleTaskReminders(task)
        }
    }

    fun addExam(
        courseName: String,
        type: String,
        examDate: String,
        examTime: String,
        modality: String = "Presencial",
        location: String? = null,
        weight: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = currentUserId
            val course = getOrCreateCourse(uid, courseName, null)
            val exam = ExamEntity(
                userId = uid,
                courseId = course.id,
                courseName = courseName,
                type = type,
                examDate = examDate,
                examTime = examTime,
                modality = modality,
                location = location,
                weight = weight
            )
            evaluationDao.upsertExam(exam)
            cloudSync.syncExam(exam)
            scheduler.scheduleExamReminders(exam)
        }
    }

    fun updateSessionDetails(
        sessionId: String,
        startTime: String,
        endTime: String,
        sessionDate: String,
        docente: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            sessionDao.updateSessionDetails(sessionId, startTime, endTime, sessionDate, docente)
            val updated = sessionDao.getSessionById(sessionId)
            if (updated != null) {
                cloudSync.syncSession(updated)
                scheduler.scheduleClassReminders(updated)
            }
        }
    }

    fun updateTaskDateTime(taskId: String, dueDate: String, dueTime: String) {
        viewModelScope.launch(Dispatchers.IO) {
            evaluationDao.updateTaskDateTime(taskId, dueDate, dueTime)
        }
    }

    fun updateExamDateTime(examId: String, examDate: String, examTime: String) {
        viewModelScope.launch(Dispatchers.IO) {
            evaluationDao.updateExamDateTime(examId, examDate, examTime)
        }
    }

    fun completeTaskWithGrade(taskId: String, grade: Double?) {
        viewModelScope.launch(Dispatchers.IO) {
            evaluationDao.updateTaskStatusAndGrade(taskId, "Entregada", grade)
        }
    }

    fun markTaskAsNotDelivered(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            evaluationDao.updateTaskStatusAndGrade(taskId, "No Entregada", null)
        }
    }

    fun completeExamWithGrade(examId: String, grade: Double?) {
        viewModelScope.launch(Dispatchers.IO) {
            evaluationDao.updateExamStatusAndGrade(examId, "Rendido", grade)
        }
    }

    fun markExamAsNotDelivered(examId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            evaluationDao.updateExamStatusAndGrade(examId, "No Rendido", null)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            sessionDao.softDeleteSession(sessionId)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            evaluationDao.softDeleteTask(taskId)
        }
    }

    fun deleteExam(examId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            evaluationDao.softDeleteExam(examId)
        }
    }

    fun dismissNotification(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            notificationDao.dismissNotification(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            notificationDao.markAllAsRead(currentUserId)
        }
    }
}
