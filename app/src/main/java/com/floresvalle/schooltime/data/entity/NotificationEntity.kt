package com.floresvalle.schooltime.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val description: String,
    val category: String, // "Clases", "Tareas", "Exámenes"
    val isRead: Boolean = false,
    val isDismissed: Boolean = false,
    val virtualUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val eventDate: String? = null // yyyy-MM-dd
)
