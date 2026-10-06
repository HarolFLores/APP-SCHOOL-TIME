package com.floresvalle.schooltime.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.floresvalle.schooltime.worker.EvaluationWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationChannels.createChannels(context)

            val workRequest = OneTimeWorkRequestBuilder<EvaluationWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
