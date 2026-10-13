package com.floresvalle.schooltime.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.floresvalle.schooltime.data.entity.ClassSessionEntity
import com.floresvalle.schooltime.data.entity.ExamEntity
import com.floresvalle.schooltime.data.entity.TaskEntity
import com.floresvalle.schooltime.receiver.AlarmReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AlarmScheduler(private val context: Context) {
    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleClassReminders(session: ClassSessionEntity) {
        try {
            val sessionDate = LocalDate.parse(session.sessionDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val startTime = LocalTime.parse(session.startTime, DateTimeFormatter.ofPattern("HH:mm"))
            val sessionDateTime = LocalDateTime.of(sessionDate, startTime)
            val zoneId = ZoneId.systemDefault()

            val reminder2Hours = sessionDateTime.minusHours(2)
            if (reminder2Hours.isAfter(LocalDateTime.now())) {
                val epoch = reminder2Hours.atZone(zoneId).toInstant().toEpochMilli()
                scheduleSingleAlarm(
                    epoch,
                    "Clase en 2 horas: ${session.courseName}",
                    "Tu clase inicia a las ${session.startTime}. Aula: ${session.locationRoom ?: "Virtual"}",
                    session.virtualUrl,
                    ("C2H_" + session.id).hashCode(),
                    "Clases",
                    session.userId,
                    session.sessionDate
                )
            }

            val reminder15Min = sessionDateTime.minusMinutes(15)
            if (reminder15Min.isAfter(LocalDateTime.now())) {
                val epoch = reminder15Min.atZone(zoneId).toInstant().toEpochMilli()
                scheduleSingleAlarm(
                    epoch,
                    "¡Clase en 15 minutos! ${session.courseName}",
                    "Prepara tus materiales. Inicio: ${session.startTime}",
                    session.virtualUrl,
                    ("C15M_" + session.id).hashCode(),
                    "Clases",
                    session.userId,
                    session.sessionDate
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scheduleTaskReminders(task: TaskEntity) {
        try {
            val dueLocalDate = LocalDate.parse(task.dueDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val zoneId = ZoneId.systemDefault()

            val date2Days = dueLocalDate.minusDays(2).atTime(9, 0)
            if (date2Days.isAfter(LocalDateTime.now())) {
                val epoch = date2Days.atZone(zoneId).toInstant().toEpochMilli()
                scheduleSingleAlarm(
                    epoch,
                    "Entrega Próxima: ${task.courseName}",
                    "Tienes pendiente la entrega de \"${task.title}\" en 2 días (${task.dueDate}).",
                    task.link,
                    ("T2D_" + task.id).hashCode(),
                    "Tareas",
                    task.userId,
                    task.dueDate
                )
            }

            val date1Day = dueLocalDate.minusDays(1).atTime(20, 0)
            if (date1Day.isAfter(LocalDateTime.now())) {
                val epoch = date1Day.atZone(zoneId).toInstant().toEpochMilli()
                scheduleSingleAlarm(
                    epoch,
                    "¡Atención! Tarea Próxima a Vencer",
                    "Mañana vence la tarea \"${task.title}\" de ${task.courseName}.",
                    task.link,
                    ("T1D_" + task.id).hashCode(),
                    "Tareas",
                    task.userId,
                    task.dueDate
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scheduleExamReminders(exam: ExamEntity) {
        try {
            val examLocalDate = LocalDate.parse(exam.examDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val zoneId = ZoneId.systemDefault()

            val date3Days = examLocalDate.minusDays(3).atTime(9, 0)
            if (date3Days.isAfter(LocalDateTime.now())) {
                val epoch = date3Days.atZone(zoneId).toInstant().toEpochMilli()
                scheduleSingleAlarm(
                    epoch,
                    "Examen en 3 días: ${exam.courseName}",
                    "Repasa los temas para el ${exam.type} (${exam.examDate} a las ${exam.examTime}).",
                    null,
                    ("E3D_" + exam.id).hashCode(),
                    "Exámenes",
                    exam.userId,
                    exam.examDate
                )
            }

            val date1Day = examLocalDate.minusDays(1).atTime(18, 0)
            if (date1Day.isAfter(LocalDateTime.now())) {
                val epoch = date1Day.atZone(zoneId).toInstant().toEpochMilli()
                scheduleSingleAlarm(
                    epoch,
                    "¡Mañana es tu evaluación! ${exam.courseName}",
                    "Hora: ${exam.examTime}. Lugar: ${exam.location ?: "Aula asignada"}",
                    null,
                    ("E1D_" + exam.id).hashCode(),
                    "Exámenes",
                    exam.userId,
                    exam.examDate
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scheduleSingleAlarm(
        triggerEpochMillis: Long,
        title: String,
        message: String,
        virtualUrl: String?,
        notificationId: Int,
        category: String,
        userId: String,
        eventDate: String?
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_MESSAGE, message)
            putExtra(AlarmReceiver.EXTRA_URL, virtualUrl)
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(AlarmReceiver.EXTRA_CATEGORY, category)
            putExtra(AlarmReceiver.EXTRA_USER_ID, userId)
            putExtra(AlarmReceiver.EXTRA_EVENT_DATE, eventDate)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        if (Build.VERSION.SDK_INT >= 31) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
        }
    }
}
