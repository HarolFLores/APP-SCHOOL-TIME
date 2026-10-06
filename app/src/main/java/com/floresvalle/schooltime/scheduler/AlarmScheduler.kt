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
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleClassReminders(session: ClassSessionEntity) {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val startDateTime = LocalDateTime.parse("${session.sessionDate} ${session.startTime}", formatter)
            val zoneId = ZoneId.systemDefault()

            val locationText = if (session.modality.equals("Virtual", ignoreCase = true)) {
                "Online (Meet)"
            } else {
                session.locationRoom ?: "Aula por definir"
            }

            // Notice 1: 2 hours before
            val time2h = startDateTime.minusHours(2)
            if (time2h.isAfter(LocalDateTime.now())) {
                val epoch2h = time2h.atZone(zoneId).toInstant().toEpochMilli()
                scheduleSingleAlarm(
                    triggerEpochMillis = epoch2h,
                    title = "Clase próxima: ${session.courseName}",
                    message = "Inicia a las ${session.startTime} en $locationText",
                    virtualUrl = if (session.modality.equals("Virtual", true)) session.virtualUrl else null,
                    notificationId = ("S2H_${session.id}").hashCode(),
                    category = "Clases",
                    userId = session.userId,
                    eventDate = session.sessionDate
                )
            }

            // Notice 2: 15 minutes before
            val time15m = startDateTime.minusMinutes(15)
            if (time15m.isAfter(LocalDateTime.now())) {
                val epoch15m = time15m.atZone(zoneId).toInstant().toEpochMilli()
                scheduleSingleAlarm(
                    triggerEpochMillis = epoch15m,
                    title = "Tu clase de ${session.courseName} está por comenzar",
                    message = "Inicia en 15 minutos en $locationText. Haz clic para entrar.",
                    virtualUrl = if (session.modality.equals("Virtual", true)) session.virtualUrl else null,
                    notificationId = ("S15M_${session.id}").hashCode(),
                    category = "Clases",
                    userId = session.userId,
                    eventDate = session.sessionDate
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scheduleTaskReminders(task: TaskEntity) {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val dueLocalDate = LocalDate.parse(task.dueDate, formatter)
            val zoneId = ZoneId.systemDefault()

            // 1. Preventive: 2 days before at 09:00 AM
            val date2Days = dueLocalDate.minusDays(2).atTime(9, 0)
            if (date2Days.isAfter(LocalDateTime.now())) {
                val epoch2Days = date2Days.atZone(zoneId).toInstant().toEpochMilli()
                scheduleSingleAlarm(
                    triggerEpochMillis = epoch2Days,
                    title = "Entrega Próxima: ${task.courseName}",
                    message = "Tienes pendiente la entrega de ${task.title} en 2 días",
                    virtualUrl = task.link,
                    notificationId = ("T2D_${task.id}").hashCode(),
                    category = "Tareas",
                    userId = task.userId,
                    eventDate = task.dueDate
                )
            }

            // 2. Urgent: 1 day before at 08:00 PM
            val date1Day = dueLocalDate.minusDays(1).atTime(20, 0)
            if (date1Day.isAfter(LocalDateTime.now())) {
                val epoch1Day = date1Day.atZone(zoneId).toInstant().toEpochMilli()
                scheduleSingleAlarm(
                    triggerEpochMillis = epoch1Day,
                    title = "¡Atención! Tarea Próxima a Vencer",
                    message = "Mañana vence la tarea ${task.title} de ${task.courseName}",
                    virtualUrl = task.link,
                    notificationId = ("T1D_${task.id}").hashCode(),
                    category = "Tareas",
                    userId = task.userId,
                    eventDate = task.dueDate
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scheduleExamReminders(exam: ExamEntity) {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val examLocalDate = LocalDate.parse(exam.examDate, formatter)
            val zoneId = ZoneId.systemDefault()

            val locationInfo = exam.location ?: "Ubicación por definir"

            // 1. Preventive: 3 days before at 09:00 AM
            val date3Days = examLocalDate.minusDays(3).atTime(9, 0)
            if (date3Days.isAfter(LocalDateTime.now())) {
                val epoch3Days = date3Days.atZone(zoneId).toInstant().toEpochMilli()
                scheduleSingleAlarm(
                    triggerEpochMillis = epoch3Days,
                    title = "Examen Próximo: ${exam.courseName}",
                    message = "Examen próximo: ${exam.type} de ${exam.courseName} programado para el ${exam.examDate}",
                    virtualUrl = if (exam.modality.equals("Virtual", true)) exam.location else null,
                    notificationId = ("E3D_${exam.id}").hashCode(),
                    category = "Exámenes",
                    userId = exam.userId,
                    eventDate = exam.examDate
                )
            }

            // 2. Final: 1 day before at 08:00 PM
            val date1Day = examLocalDate.minusDays(1).atTime(20, 0)
            if (date1Day.isAfter(LocalDateTime.now())) {
                val epoch1Day = date1Day.atZone(zoneId).toInstant().toEpochMilli()
                scheduleSingleAlarm(
                    triggerEpochMillis = epoch1Day,
                    title = "¡Mañana tienes examen!",
                    message = "Mañana tienes examen de ${exam.courseName} (${exam.type}) en $locationInfo",
                    virtualUrl = if (exam.modality.equals("Virtual", true)) exam.location else null,
                    notificationId = ("E1D_${exam.id}").hashCode(),
                    category = "Exámenes",
                    userId = exam.userId,
                    eventDate = exam.examDate
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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
        }
    }
}
