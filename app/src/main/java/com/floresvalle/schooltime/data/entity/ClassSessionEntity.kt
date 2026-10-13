package com.floresvalle.schooltime.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sessions")
data class ClassSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val courseId: String = "",
    val courseName: String,
    val sessionDate: String,
    val startTime: String,
    val endTime: String,
    val modality: String = "Presencial",
    val locationRoom: String? = null,
    val virtualUrl: String? = null,
    val docente: String? = null,
    val color: String? = null,
    val status: String = "Pendiente",
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncState: String = "SYNCED"
)
