package com.floresvalle.schooltime.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import com.floresvalle.schooltime.R

object NotificationChannels {
    const val CHANNEL_CLASSES = "school_classes_channel"
    const val CHANNEL_EVALUATIONS = "school_evaluations_channel"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val soundUri = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.school_chime)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .build()

        val classesChannel = NotificationChannel(CHANNEL_CLASSES, "Clases y Horarios", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Recordatorios de sesiones y cátedras universitarias (2 horas y 15 min antes)"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            setSound(soundUri, audioAttributes)
        }

        val evalChannel = NotificationChannel(CHANNEL_EVALUATIONS, "Tareas y Exámenes", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Recordatorios de entregas de proyectos, tareas y fechas de exámenes"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            setSound(soundUri, audioAttributes)
        }

        manager.createNotificationChannel(classesChannel)
        manager.createNotificationChannel(evalChannel)
    }
}
