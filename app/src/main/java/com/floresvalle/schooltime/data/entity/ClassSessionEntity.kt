package com.floresvalle.schooltime.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sessions")
data class ClassSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val courseId: String = UUID.randomUUID().toString(),
    val courseName: String,
    val sessionDate: String, // yyyy-MM-dd
    val startTime: String,   // HH:mm
    val endTime: String,     // HH:mm
    val modality: String,    // Presencial / Virtual
    val locationRoom: String? = null,
    val virtualUrl: String? = null,
    val docente: String? = null,
    val color: String? = null,
    val status: String = "SCHEDULED",
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncState: String = "PENDING_UPLOAD"
)
