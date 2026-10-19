package com.floresvalle.schooltime.data.network

import com.floresvalle.schooltime.data.entity.AcademicPeriodEntity
import com.floresvalle.schooltime.data.entity.ClassSessionEntity
import com.floresvalle.schooltime.data.entity.CourseEntity
import com.floresvalle.schooltime.data.entity.ExamEntity
import com.floresvalle.schooltime.data.entity.NotificationEntity
import com.floresvalle.schooltime.data.entity.TaskEntity

// Helpers para serialización de entidades hacia Cloud Firestore
fun CourseEntity.toSyncMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "user_id" to userId,
        "period_id" to periodId,
        "name" to name,
        "code" to (code ?: "INF101"),
        "teacher_name" to teacherName,
        "color_hex" to (colorHex ?: "#0E7490"),
        "credits" to credits,
        "course_type" to "CURRICULAR",
        "updated_at" to updatedAt,
        "is_deleted" to isDeleted
    )
}

fun ClassSessionEntity.toSyncMap(): Map<String, Any?> {
    val modUpper = if (modality.equals("Virtual", true)) "VIRTUAL" else "PRESENTIAL"
    return mapOf(
        "id" to id,
        "course_id" to courseId,
        "session_date" to sessionDate,
        "start_time" to (if (startTime.length == 5) "$startTime:00" else startTime),
        "end_time" to (if (endTime.length == 5) "$endTime:00" else endTime),
        "modality" to modUpper,
        "location_room" to locationRoom,
        "virtual_url" to virtualUrl,
        "status" to "SCHEDULED",
        "notes" to docente,
        "updated_at" to updatedAt,
        "is_deleted" to isDeleted
    )
}

fun TaskEntity.toSyncMap(): Map<String, Any?> {
    val delMode = if (modality.contains("Grupo", true)) "GRUPAL" else "INDIVIDUAL"
    val prio = when (urgency.uppercase()) {
        "ALTA" -> "ALTA"
        "BAJA" -> "BAJA"
        else -> "MEDIA"
    }
    val timeFormatted = if (dueTime.isBlank()) "23:59:00" else if (dueTime.length == 5) "$dueTime:00" else dueTime

    return mapOf(
        "id" to id,
        "user_id" to userId,
        "course_id" to courseId,
        "title" to title,
        "description" to title,
        "due_date" to dueDate,
        "due_time" to timeFormatted,
        "delivery_mode" to delMode,
        "priority" to prio,
        "submission_url" to link,
        "is_completed" to (status == "Completado"),
        "grade" to grade,
        "updated_at" to updatedAt,
        "is_deleted" to isDeleted
    )
}

fun ExamEntity.toSyncMap(): Map<String, Any?> {
    val times = examTime.split("-").map { it.trim() }
    val sTime = if (times.isNotEmpty() && times[0].isNotBlank()) (if (times[0].length == 5) "${times[0]}:00" else times[0]) else "08:00:00"
    val eTime = if (times.size > 1 && times[1].isNotBlank()) (if (times[1].length == 5) "${times[1]}:00" else times[1]) else "10:00:00"
    val modUpper = if (modality.equals("Virtual", true)) "VIRTUAL" else "PRESENTIAL"

    return mapOf(
        "id" to id,
        "user_id" to userId,
        "course_id" to courseId,
        "exam_type" to type,
        "exam_date" to examDate,
        "start_time" to sTime,
        "end_time" to eTime,
        "modality" to modUpper,
        "location_or_url" to (location ?: "Aula por definir"),
        "weight_percentage" to (weight ?: "20%"),
        "is_completed" to (status == "Completado"),
        "grade" to grade,
        "updated_at" to updatedAt,
        "is_deleted" to isDeleted
    )
}

fun AcademicPeriodEntity.toSyncMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "user_id" to userId,
        "name" to periodName,
        "start_date" to startDate,
        "end_date" to endDate,
        "is_active" to isActive,
        "updated_at" to System.currentTimeMillis(),
        "is_deleted" to false
    )
}

fun NotificationEntity.toSyncMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "user_id" to userId,
        "title" to title,
        "message" to description,
        "description" to description,
        "type" to category,
        "category" to category,
        "is_read" to isRead,
        "created_at" to timestamp
    )
}
