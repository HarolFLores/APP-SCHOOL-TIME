package com.floresvalle.schooltime.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val courseId: String = "",
    val courseName: String,
    val type: String,
    val examDate: String,
    val examTime: String,
    val modality: String = "Presencial",
    val location: String? = null,
    val weight: String? = null,
    val status: String = "Pendiente",
    val grade: Double? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncState: String = "SYNCED"
)
