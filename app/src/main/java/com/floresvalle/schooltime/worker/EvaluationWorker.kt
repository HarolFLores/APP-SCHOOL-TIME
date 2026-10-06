package com.floresvalle.schooltime.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.floresvalle.schooltime.data.AppDatabase
import com.floresvalle.schooltime.scheduler.AlarmScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class EvaluationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(context)
            val userId = Firebase.auth.currentUser?.uid ?: ""

            if (userId.isNotBlank()) {
                val alarmScheduler = AlarmScheduler(context)

                // 1. Scan Tasks
                val tasks = db.evaluationDao().getAllActiveTasks(userId).firstOrNull() ?: emptyList()
                tasks.filter { it.status != "Completado" }.forEach { task ->
                    alarmScheduler.scheduleTaskReminders(task)
                }

                // 2. Scan Exams
                val exams = db.evaluationDao().getAllActiveExams(userId).firstOrNull() ?: emptyList()
                exams.filter { it.status != "Completado" }.forEach { exam ->
                    alarmScheduler.scheduleExamReminders(exam)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
