package com.floresvalle.schooltime.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val CHANNEL_CLASSES = "school_classes_channel"
    const val CHANNEL_EVALUATIONS = "school_evaluations_channel"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val classesChannel = NotificationChannel(
                CHANNEL_CLASSES,
                "Clases y Horarios",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios de sesiones y cátedras universitarias (2 horas y 15 min antes)"
                enableVibration(true)
            }

            val evaluationsChannel = NotificationChannel(
                CHANNEL_EVALUATIONS,
                "Tareas y Exámenes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios de entregas de proyectos, tareas y fechas de exámenes"
                enableVibration(true)
            }

            manager.createNotificationChannel(classesChannel)
            manager.createNotificationChannel(evaluationsChannel)
        }
    }
}
