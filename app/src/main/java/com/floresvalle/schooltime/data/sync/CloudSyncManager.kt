package com.floresvalle.schooltime.data.sync

import android.util.Log
import com.floresvalle.schooltime.data.AppDatabase
import com.floresvalle.schooltime.data.entity.AcademicPeriodEntity
import com.floresvalle.schooltime.data.entity.ClassSessionEntity
import com.floresvalle.schooltime.data.entity.CourseEntity
import com.floresvalle.schooltime.data.entity.ExamEntity
import com.floresvalle.schooltime.data.entity.TaskEntity
import com.floresvalle.schooltime.data.entity.UserEntity
import com.floresvalle.schooltime.data.network.toSyncMap
import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    private val firestore by lazy { Firebase.firestore }

    suspend fun syncUserProfile(user: UserEntity) {
        // Sincronizar a Cloud Firestore (con caché offline nativo y sincronización automática al conectar)
        try {
            val fullName = "${user.firstName} ${user.lastName}".trim().ifBlank { "Estudiante" }
            val userMap = hashMapOf<String, Any?>(
                "id" to user.id,
                "email" to user.email,
                "password" to user.password,
                "name" to fullName,
                "first_name" to user.firstName,
                "last_name" to user.lastName,
                "career" to user.career,
                "current_semester" to user.currentSemester,
                "current_cycle" to user.currentCycle,
                "phone" to user.phone,
                "phone_number" to user.phone,
                "photo_uri" to user.photoUri,
                "avatar_url" to user.photoUri,
                "enrolled_credits" to user.enrolledCredits,
                "weighted_average" to user.weightedAverage,
                "academic_status" to "Activo",
                "updated_at" to System.currentTimeMillis()
            )
            firestore.collection("users").document(user.id).set(userMap, SetOptions.merge()).await()
            if (user.email.isNotBlank()) {
                firestore.collection("users_by_email").document(user.email.lowercase().trim())
                    .set(userMap, SetOptions.merge()).await()
            }
            Log.d(TAG, "Perfil de usuario sincronizado en Firestore: ${user.email} (${user.career})")
        } catch (e: Exception) {
            Log.w(TAG, "Error sync Firestore profile: ${e.message}")
        }
    }

    suspend fun fetchUserProfile(userId: String, email: String?): UserEntity? {
        try {
            if (userId.isNotBlank()) {
                val doc = firestore.collection("users").document(userId).get().await()
                val career = doc.getString("career")
                if (doc.exists() && !career.isNullOrBlank() && career != "General") {
                    return UserEntity(
                        id = doc.getString("id") ?: userId,
                        email = doc.getString("email") ?: email.orEmpty(),
                        password = doc.getString("password"),
                        firstName = doc.getString("first_name") ?: "Estudiante",
                        lastName = doc.getString("last_name") ?: "",
                        career = career,
                        currentSemester = doc.getString("current_semester") ?: "2026-I",
                        currentCycle = doc.getString("current_cycle") ?: "1er Ciclo",
                        phone = doc.getString("phone") ?: "",
                        photoUri = doc.getString("photo_uri"),
                        enrolledCredits = doc.getLong("enrolled_credits")?.toInt() ?: 0,
                        weightedAverage = doc.getDouble("weighted_average") ?: 0.0,
                        syncState = "SYNCED"
                    )
                }
            }

            if (!email.isNullOrBlank()) {
                val doc = firestore.collection("users_by_email").document(email.lowercase().trim()).get().await()
                val career = doc.getString("career")
                if (doc.exists() && !career.isNullOrBlank() && career != "General") {
                    return UserEntity(
                        id = doc.getString("id") ?: userId,
                        email = email,
                        password = doc.getString("password"),
                        firstName = doc.getString("first_name") ?: "Estudiante",
                        lastName = doc.getString("last_name") ?: "",
                        career = career,
                        currentSemester = doc.getString("current_semester") ?: "2026-I",
                        currentCycle = doc.getString("current_cycle") ?: "1er Ciclo",
                        phone = doc.getString("phone") ?: "",
                        photoUri = doc.getString("photo_uri"),
                        enrolledCredits = doc.getLong("enrolled_credits")?.toInt() ?: 0,
                        weightedAverage = doc.getDouble("weighted_average") ?: 0.0,
                        syncState = "SYNCED"
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetchUserProfile en Firestore: ${e.message}")
        }
        return null
    }

    suspend fun syncAcademicPeriod(period: AcademicPeriodEntity) {
        try {
            val map = period.toSyncMap()
            firestore.collection("users")
                .document(period.userId)
                .collection("academic_periods")
                .document(period.id)
                .set(map, SetOptions.merge())
                .await()
            Log.d(TAG, "Periodo académico sincronizado en Firestore: ${period.periodName}")
        } catch (e: Exception) {
            Log.w(TAG, "Error syncAcademicPeriod en Firestore: ${e.message}")
        }
    }

    suspend fun syncCourse(course: CourseEntity) {
        try {
            val map = course.toSyncMap()
            firestore.collection("users")
                .document(course.userId)
                .collection("courses")
                .document(course.id)
                .set(map, SetOptions.merge())
                .await()
            Log.d(TAG, "Curso sincronizado en Firestore: ${course.name} (id: ${course.id})")
        } catch (e: Exception) {
            Log.w(TAG, "Error syncCourse en Firestore: ${e.message}")
        }
    }

    suspend fun syncSession(session: ClassSessionEntity) {
        try {
            val map = session.toSyncMap()
            firestore.collection("users")
                .document(session.userId)
                .collection("class_sessions")
                .document(session.id)
                .set(map, SetOptions.merge())
                .await()
            Log.d(TAG, "Sesión de clase sincronizada en Firestore: ${session.courseName} (${session.sessionDate})")
        } catch (e: Exception) {
            Log.w(TAG, "Error syncSession en Firestore: ${e.message}")
        }
    }

    suspend fun syncTask(task: TaskEntity) {
        try {
            val map = task.toSyncMap()
            firestore.collection("users")
                .document(task.userId)
                .collection("tasks")
                .document(task.id)
                .set(map, SetOptions.merge())
                .await()
            Log.d(TAG, "Tarea sincronizada en Firestore: ${task.title}")
        } catch (e: Exception) {
            Log.w(TAG, "Error syncTask en Firestore: ${e.message}")
        }
    }

    suspend fun syncExam(exam: ExamEntity) {
        try {
            val map = exam.toSyncMap()
            firestore.collection("users")
                .document(exam.userId)
                .collection("exams")
                .document(exam.id)
                .set(map, SetOptions.merge())
                .await()
            Log.d(TAG, "Examen sincronizado en Firestore: ${exam.type} (${exam.courseName})")
        } catch (e: Exception) {
            Log.w(TAG, "Error syncExam en Firestore: ${e.message}")
        }
    }

    suspend fun restoreAllFromCloud(userId: String, db: AppDatabase) {
        if (userId.isBlank()) return
        Log.d(TAG, "Iniciando restauración de datos en la nube para el usuario: $userId")
        try {
            val userRef = firestore.collection("users").document(userId)

            // 0. Restaurar Perfil si falta o es genérico
            val localUser = db.userDao().getUserByIdOnce(userId)
            if (localUser == null || localUser.career == "General") {
                val cloudProfile = fetchUserProfile(userId, localUser?.email)
                if (cloudProfile != null) {
                    db.userDao().insertUser(cloudProfile)
                    Log.d(TAG, "Perfil restaurado de la nube: ${cloudProfile.email} (${cloudProfile.career}, ${cloudProfile.currentSemester}, ${cloudProfile.currentCycle})")
                }
            }

            // 1. Restaurar Periodos Académicos
            val periodsSnapshot = userRef.collection("academic_periods").get().await()
            val periods = periodsSnapshot.documents.mapNotNull { doc ->
                try {
                    val pName = doc.getString("name") ?: doc.getString("periodName") ?: "2026-I"
                    val sDate = doc.getString("start_date") ?: doc.getString("startDate") ?: "2026-03-16"
                    val eDate = doc.getString("end_date") ?: doc.getString("endDate") ?: "2026-07-20"
                    val isAct = doc.getBoolean("is_active") ?: doc.getBoolean("isActive") ?: true
                    AcademicPeriodEntity(
                        id = doc.id,
                        userId = userId,
                        periodName = pName,
                        startDate = sDate,
                        endDate = eDate,
                        isActive = isAct
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (periods.isNotEmpty()) {
                db.academicPeriodDao().upsertPeriods(periods)
                Log.d(TAG, "Restaurados ${periods.size} periodos académicos desde la nube")
            }

            // 2. Restaurar Cursos
            val coursesSnapshot = userRef.collection("courses").get().await()
            val courses = coursesSnapshot.documents.mapNotNull { doc ->
                try {
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val periodId = doc.getString("period_id") ?: doc.getString("periodId") ?: ""
                    val teacher = doc.getString("teacher_name") ?: doc.getString("teacherName")
                    val colorHex = doc.getString("color_hex") ?: doc.getString("colorHex") ?: "#0E7490"
                    val code = doc.getString("code") ?: "INF101"
                    val credits = doc.getLong("credits")?.toInt() ?: 3
                    val isDeleted = doc.getBoolean("is_deleted") ?: false
                    CourseEntity(
                        id = doc.id,
                        userId = userId,
                        periodId = periodId,
                        name = name,
                        code = code,
                        teacherName = teacher,
                        colorHex = colorHex,
                        credits = credits,
                        isDeleted = isDeleted
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (courses.isNotEmpty()) {
                db.courseDao().upsertCourses(courses)
                Log.d(TAG, "Restaurados ${courses.size} cursos desde la nube")
            }

            // 3. Restaurar Sesiones de Clase
            val sessionsSnapshot = userRef.collection("class_sessions").get().await()
            val sessions = sessionsSnapshot.documents.mapNotNull { doc ->
                try {
                    val courseId = doc.getString("course_id") ?: doc.getString("courseId") ?: ""
                    val sessionDate = doc.getString("session_date") ?: doc.getString("sessionDate") ?: ""
                    val startTime = (doc.getString("start_time") ?: doc.getString("startTime") ?: "08:00").take(5)
                    val endTime = (doc.getString("end_time") ?: doc.getString("endTime") ?: "10:00").take(5)
                    val modalityRaw = doc.getString("modality") ?: "PRESENTIAL"
                    val modality = if (modalityRaw.equals("VIRTUAL", ignoreCase = true)) "Virtual" else "Presencial"
                    val location = doc.getString("location_room") ?: doc.getString("locationRoom")
                    val virtualUrl = doc.getString("virtual_url") ?: doc.getString("virtualUrl")
                    val docente = doc.getString("notes") ?: doc.getString("docente")
                    val courseName = courses.firstOrNull { it.id == courseId }?.name
                        ?: doc.getString("course_name")
                        ?: doc.getString("courseName")
                        ?: "Clase"
                    val isDeleted = doc.getBoolean("is_deleted") ?: false

                    ClassSessionEntity(
                        id = doc.id,
                        userId = userId,
                        courseId = courseId,
                        courseName = courseName,
                        sessionDate = sessionDate,
                        startTime = startTime,
                        endTime = endTime,
                        modality = modality,
                        locationRoom = location,
                        virtualUrl = virtualUrl,
                        docente = docente,
                        color = "#0E7490",
                        isDeleted = isDeleted
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (sessions.isNotEmpty()) {
                db.sessionDao().upsertSessions(sessions)
                Log.d(TAG, "Restauradas ${sessions.size} sesiones de clase desde la nube")
            }

            // 4. Restaurar Tareas
            val tasksSnapshot = userRef.collection("tasks").get().await()
            val tasks = tasksSnapshot.documents.mapNotNull { doc ->
                try {
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val courseId = doc.getString("course_id") ?: doc.getString("courseId") ?: ""
                    val dueDate = doc.getString("due_date") ?: doc.getString("dueDate") ?: ""
                    val dueTime = doc.getString("due_time") ?: doc.getString("dueTime") ?: "23:59"
                    val prio = doc.getString("priority") ?: doc.getString("urgency") ?: "Baja"
                    val urgency = when (prio.uppercase()) {
                        "ALTA" -> "Alta"
                        "MEDIA" -> "Media"
                        else -> "Baja"
                    }
                    val delMode = doc.getString("delivery_mode") ?: "INDIVIDUAL"
                    val modality = if (delMode.contains("GRUP", true)) "Grupal" else "Individual"
                    val link = doc.getString("submission_url") ?: doc.getString("link")
                    val isCompleted = doc.getBoolean("is_completed") ?: false
                    val status = if (isCompleted) "Completado" else "Pendiente"
                    val grade = doc.getDouble("grade") ?: doc.getLong("grade")?.toDouble()
                    val courseName = courses.firstOrNull { it.id == courseId }?.name
                        ?: doc.getString("course_name")
                        ?: doc.getString("courseName")
                        ?: "General"
                    val isDeleted = doc.getBoolean("is_deleted") ?: false

                    TaskEntity(
                        id = doc.id,
                        userId = userId,
                        courseId = courseId,
                        courseName = courseName,
                        title = title,
                        dueDate = dueDate,
                        dueTime = dueTime,
                        modality = modality,
                        urgency = urgency,
                        link = link,
                        status = status,
                        grade = grade,
                        isDeleted = isDeleted
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (tasks.isNotEmpty()) {
                db.evaluationDao().upsertTasks(tasks)
                Log.d(TAG, "Restauradas ${tasks.size} tareas desde la nube")
            }

            // 5. Restaurar Exámenes
            val examsSnapshot = userRef.collection("exams").get().await()
            val exams = examsSnapshot.documents.mapNotNull { doc ->
                try {
                    val examType = doc.getString("exam_type") ?: doc.getString("type") ?: "Examen Parcial"
                    val courseId = doc.getString("course_id") ?: doc.getString("courseId") ?: ""
                    val examDate = doc.getString("exam_date") ?: doc.getString("examDate") ?: ""
                    val startTime = (doc.getString("start_time") ?: "08:00").take(5)
                    val endTime = (doc.getString("end_time") ?: "10:00").take(5)
                    val examTime = "$startTime - $endTime"
                    val modalityRaw = doc.getString("modality") ?: "PRESENTIAL"
                    val modality = if (modalityRaw.equals("VIRTUAL", ignoreCase = true)) "Virtual" else "Presencial"
                    val location = doc.getString("location_or_url") ?: doc.getString("location")
                    val weight = doc.getString("weight_percentage") ?: doc.getString("weight") ?: "20%"
                    val isCompleted = doc.getBoolean("is_completed") ?: false
                    val status = if (isCompleted) "Completado" else "Pendiente"
                    val grade = doc.getDouble("grade") ?: doc.getLong("grade")?.toDouble()
                    val courseName = courses.firstOrNull { it.id == courseId }?.name
                        ?: doc.getString("course_name")
                        ?: doc.getString("courseName")
                        ?: "General"
                    val isDeleted = doc.getBoolean("is_deleted") ?: false

                    ExamEntity(
                        id = doc.id,
                        userId = userId,
                        courseId = courseId,
                        courseName = courseName,
                        type = examType,
                        examDate = examDate,
                        examTime = examTime,
                        modality = modality,
                        location = location,
                        weight = weight,
                        status = status,
                        grade = grade,
                        isDeleted = isDeleted
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (exams.isNotEmpty()) {
                db.evaluationDao().upsertExams(exams)
                Log.d(TAG, "Restaurados ${exams.size} exámenes desde la nube")
            }

            Log.d(TAG, "Sincronización completa desde la nube terminada con éxito.")
        } catch (e: Exception) {
            Log.e(TAG, "Error restaurando datos desde la nube: ${e.message}")
        }
    }
}
