package com.floresvalle.schooltime.receiver

import android.R
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.floresvalle.schooltime.MainActivity
import com.floresvalle.schooltime.data.AppDatabase
import com.floresvalle.schooltime.data.entity.NotificationEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_CATEGORY = "extra_category" // "Clases", "Tareas", "Exámenes"
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_EVENT_DATE = "extra_event_date"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Recordatorio SchoolTime"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Tienes una actividad programada"
        val virtualUrl = intent.getStringExtra(EXTRA_URL)
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, System.currentTimeMillis().toInt())
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "Clases"
        val userId = intent.getStringExtra(EXTRA_USER_ID) ?: Firebase.auth.currentUser?.uid ?: ""
        val eventDate = intent.getStringExtra(EXTRA_EVENT_DATE)

        // Ensure notification channels are created
        NotificationChannels.createChannels(context)

        val channelId = if (category == "Clases") {
            NotificationChannels.CHANNEL_CLASSES
        } else {
            NotificationChannels.CHANNEL_EVALUATIONS
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to launch MainActivity
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(appPendingIntent)

        if (!virtualUrl.isNullOrBlank()) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(virtualUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val openLinkPendingIntent = PendingIntent.getActivity(
                context,
                notifId + 10000,
                browserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(
                R.drawable.ic_menu_view,
                "Unirse a Videollamada",
                openLinkPendingIntent
            )
        }

        notificationManager.notify(notifId, builder.build())

        // Insert into Room Database synchronously/async using goAsync()
        if (userId.isNotBlank()) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    db.notificationDao().upsertNotification(
                        NotificationEntity(
                            id = "NOTIF_${notifId}_${System.currentTimeMillis()}",
                            userId = userId,
                            title = title,
                            description = message,
                            category = category,
                            isRead = false,
                            virtualUrl = virtualUrl,
                            timestamp = System.currentTimeMillis(),
                            eventDate = eventDate
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
