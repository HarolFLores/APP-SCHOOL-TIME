package com.floresvalle.schooltime.data.sync

import android.content.Context
import com.floresvalle.schooltime.data.AppDatabase
import com.floresvalle.schooltime.data.entity.*
import com.floresvalle.schooltime.data.network.toSyncMap
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CloudSyncManager(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val firestore: FirebaseFirestore by lazy { Firebase.firestore }

    suspend fun syncUserProfile(user: UserEntity) = withContext(Dispatchers.IO) {
        try {
            val map = mapOf(
                "id" to user.id,
                "email" to user.email,
                "first_name" to user.firstName,
                "last_name" to user.lastName,
                "career" to user.career,
                "current_semester" to user.currentSemester,
                "current_cycle" to user.currentCycle,
                "phone" to user.phone,
                "photo_uri" to user.photoUri,
                "enrolled_credits" to user.enrolledCredits,
                "weighted_average" to user.weightedAverage
            )
            firestore.collection("users").document(user.id).set(map, SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchUserProfile(userId: String): UserEntity? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val data = doc.data ?: return@withContext null
                UserEntity(
                    id = userId,
                    email = data["email"] as? String ?: "",
                    firstName = data["first_name"] as? String ?: "",
                    lastName = data["last_name"] as? String ?: "",
                    career = data["career"] as? String ?: "",
                    currentSemester = data["current_semester"] as? String ?: "",
                    currentCycle = data["current_cycle"] as? String ?: "",
                    phone = data["phone"] as? String ?: "",
                    photoUri = data["photo_uri"] as? String,
                    enrolledCredits = (data["enrolled_credits"] as? Number)?.toInt() ?: 0,
                    weightedAverage = (data["weighted_average"] as? Number)?.toDouble() ?: 0.0
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun syncSession(session: ClassSessionEntity) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(session.userId)
                .collection("sessions").document(session.id)
                .set(session.toSyncMap(), SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(task.userId)
                .collection("tasks").document(task.id)
                .set(task.toSyncMap(), SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncExam(exam: ExamEntity) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(exam.userId)
                .collection("exams").document(exam.id)
                .set(exam.toSyncMap(), SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncCourse(course: CourseEntity) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(course.userId)
                .collection("courses").document(course.id)
                .set(course.toSyncMap(), SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncAcademicPeriod(period: AcademicPeriodEntity) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(period.userId)
                .collection("academic_periods").document(period.id)
                .set(period.toSyncMap(), SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun restoreAllFromCloud(userId: String) = withContext(Dispatchers.IO) {
        try {
            val user = fetchUserProfile(userId)
            if (user != null) {
                db.userDao().insertOrUpdate(user)
            }
            val periodDocs = firestore.collection("users").document(userId)
                .collection("academic_periods").get().await()
            for (doc in periodDocs) {
                val d = doc.data
                db.academicPeriodDao().upsertPeriod(
                    AcademicPeriodEntity(
                        id = doc.id,
                        userId = userId,
                        periodName = d["period_name"] as? String ?: "2025-I",
                        startDate = d["start_date"] as? String ?: "2025-03-01",
                        endDate = d["end_date"] as? String ?: "2025-07-15",
                        isActive = d["is_active"] as? Boolean ?: true
                    )
                )
            }
            val courseDocs = firestore.collection("users").document(userId)
                .collection("courses").whereEqualTo("is_deleted", false).get().await()
            for (doc in courseDocs) {
                val d = doc.data
                db.courseDao().upsertCourse(
                    CourseEntity(
                        id = doc.id,
                        userId = userId,
                        periodId = d["period_id"] as? String ?: "",
                        name = d["name"] as? String ?: "",
                        code = d["code"] as? String,
                        teacherName = d["teacher_name"] as? String,
                        colorHex = d["color_hex"] as? String ?: "#0E7490",
                        credits = (d["credits"] as? Number)?.toInt() ?: 0,
                        updatedAt = (d["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                )
            }
            val sessionDocs = firestore.collection("users").document(userId)
                .collection("sessions").whereEqualTo("is_deleted", false).get().await()
            for (doc in sessionDocs) {
                val d = doc.data
                db.sessionDao().upsertSession(
                    ClassSessionEntity(
                        id = doc.id,
                        userId = userId,
                        courseId = d["course_id"] as? String ?: "",
                        courseName = d["course_name"] as? String ?: "",
                        sessionDate = d["session_date"] as? String ?: "",
                        startTime = d["start_time"] as? String ?: "",
                        endTime = d["end_time"] as? String ?: "",
                        modality = d["modality"] as? String ?: "Presencial",
                        locationRoom = d["location_room"] as? String,
                        virtualUrl = d["virtual_url"] as? String,
                        docente = d["docente"] as? String,
                        color = d["color"] as? String,
                        status = d["status"] as? String ?: "Pendiente",
                        notes = d["notes"] as? String,
                        updatedAt = (d["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                )
            }
            val taskDocs = firestore.collection("users").document(userId)
                .collection("tasks").whereEqualTo("is_deleted", false).get().await()
            for (doc in taskDocs) {
                val d = doc.data
                db.evaluationDao().upsertTask(
                    TaskEntity(
                        id = doc.id,
                        userId = userId,
                        courseId = d["course_id"] as? String ?: "",
                        courseName = d["course_name"] as? String ?: "",
                        title = d["title"] as? String ?: "",
                        dueDate = d["due_date"] as? String ?: "",
                        dueTime = d["due_time"] as? String ?: "",
                        modality = d["modality"] as? String ?: "Individual",
                        urgency = d["urgency"] as? String ?: "Media",
                        link = d["link"] as? String,
                        status = d["status"] as? String ?: "Pendiente",
                        grade = (d["grade"] as? Number)?.toDouble(),
                        updatedAt = (d["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                )
            }
            val examDocs = firestore.collection("users").document(userId)
                .collection("exams").whereEqualTo("is_deleted", false).get().await()
            for (doc in examDocs) {
                val d = doc.data
                db.evaluationDao().upsertExam(
                    ExamEntity(
                        id = doc.id,
                        userId = userId,
                        courseId = d["course_id"] as? String ?: "",
                        courseName = d["course_name"] as? String ?: "",
                        type = d["type"] as? String ?: "Parcial",
                        examDate = d["exam_date"] as? String ?: "",
                        examTime = d["exam_time"] as? String ?: "",
                        modality = d["modality"] as? String ?: "Presencial",
                        location = d["location"] as? String,
                        weight = d["weight"] as? String,
                        status = d["status"] as? String ?: "Pendiente",
                        grade = (d["grade"] as? Number)?.toDouble(),
                        updatedAt = (d["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
