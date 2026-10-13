package com.floresvalle.schooltime.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val courseId: String = "",
    val courseName: String,
    val title: String,
    val dueDate: String,
    val dueTime: String,
    val modality: String = "Individual",
    val urgency: String = "Media",
    val link: String? = null,
    val status: String = "Pendiente",
    val grade: Double? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncState: String = "SYNCED"
)
