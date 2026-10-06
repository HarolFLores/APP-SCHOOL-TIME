package com.floresvalle.schooltime.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.floresvalle.schooltime.R

object NotificationChannels {
    const val CHANNEL_CLASSES = "school_classes_channel_v3"
    const val CHANNEL_EVALUATIONS = "school_evaluations_channel_v3"

    fun getSoundUri(context: Context): Uri {
        return Uri.parse(
            ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.packageName + "/" + R.raw.school_chime
        )
    }

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val soundUri = getSoundUri(context)

            val classesChannel = NotificationChannel(
                CHANNEL_CLASSES,
                "Clases y Horarios Académicos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios de clases y cátedras (2h y 15 min antes)"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 200, 100, 300)
                enableLights(true)
                lightColor = android.graphics.Color.CYAN
                setSound(soundUri, audioAttributes)
            }

            val evaluationsChannel = NotificationChannel(
                CHANNEL_EVALUATIONS,
                "Tareas y Exámenes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios de entregas de trabajos y exámenes"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 120, 200, 120, 400)
                enableLights(true)
                lightColor = android.graphics.Color.YELLOW
                setSound(soundUri, audioAttributes)
            }

            manager.createNotificationChannel(classesChannel)
            manager.createNotificationChannel(evaluationsChannel)
        }
    }
}

