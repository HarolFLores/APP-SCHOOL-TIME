package com.floresvalle.schooltime.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.floresvalle.schooltime.data.AppDatabase
import com.floresvalle.schooltime.scheduler.AlarmScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.firstOrNull

class EvaluationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = Firebase.auth.currentUser?.uid ?: return Result.success()
        val db = AppDatabase.getDatabase(context)
        val scheduler = AlarmScheduler(context)

        try {
            val sessions = db.sessionDao().getAllActiveSessionsSync(userId)
            sessions.forEach { scheduler.scheduleClassReminders(it) }

            val tasks = db.evaluationDao().getTasks(userId).firstOrNull() ?: emptyList()
            tasks.filter { it.status == "Pendiente" }.forEach { scheduler.scheduleTaskReminders(it) }

            val exams = db.evaluationDao().getExams(userId).firstOrNull() ?: emptyList()
            exams.filter { it.status == "Pendiente" }.forEach { scheduler.scheduleExamReminders(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Result.success()
    }
}
