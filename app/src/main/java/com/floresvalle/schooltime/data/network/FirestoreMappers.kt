package com.floresvalle.schooltime.data.network

import com.floresvalle.schooltime.data.entity.*

fun CourseEntity.toSyncMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "user_id" to userId,
    "period_id" to periodId,
    "name" to name,
    "code" to (code ?: "INF101"),
    "teacher_name" to teacherName,
    "color_hex" to colorHex,
    "credits" to credits,
    "updated_at" to updatedAt,
    "is_deleted" to isDeleted
)

fun ClassSessionEntity.toSyncMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "user_id" to userId,
    "course_id" to courseId,
    "course_name" to courseName,
    "session_date" to sessionDate,
    "start_time" to startTime,
    "end_time" to endTime,
    "modality" to modality,
    "location_room" to locationRoom,
    "virtual_url" to virtualUrl,
    "docente" to docente,
    "color" to color,
    "status" to status,
    "notes" to notes,
    "updated_at" to updatedAt,
    "is_deleted" to isDeleted
)

fun TaskEntity.toSyncMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "user_id" to userId,
    "course_id" to courseId,
    "course_name" to courseName,
    "title" to title,
    "due_date" to dueDate,
    "due_time" to dueTime,
    "modality" to modality,
    "urgency" to urgency,
    "link" to link,
    "status" to status,
    "grade" to grade,
    "updated_at" to updatedAt,
    "is_deleted" to isDeleted
)

fun ExamEntity.toSyncMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "user_id" to userId,
    "course_id" to courseId,
    "course_name" to courseName,
    "type" to type,
    "exam_date" to examDate,
    "exam_time" to examTime,
    "modality" to modality,
    "location" to location,
    "weight" to weight,
    "status" to status,
    "grade" to grade,
    "updated_at" to updatedAt,
    "is_deleted" to isDeleted
)

fun AcademicPeriodEntity.toSyncMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "user_id" to userId,
    "period_name" to periodName,
    "start_date" to startDate,
    "end_date" to endDate,
    "is_active" to isActive
)

fun NotificationEntity.toSyncMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "user_id" to userId,
    "title" to title,
    "description" to description,
    "category" to category,
    "is_read" to isRead,
    "is_dismissed" to isDismissed,
    "virtual_url" to virtualUrl,
    "timestamp" to timestamp,
    "event_date" to eventDate
)
