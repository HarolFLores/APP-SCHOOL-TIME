package com.floresvalle.schooltime.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val courseId: String = UUID.randomUUID().toString(),
    val courseName: String,
    val type: String, // Examen Parcial / Final / Práctica Calificada
    val examDate: String,
    val examTime: String,
    val modality: String, // Presencial / Virtual
    val location: String?,
    val weight: String?,
    val status: String = "Pendiente", // Pendiente / Completado / No entregado
    val grade: Double? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncState: String = "PENDING_UPLOAD"
)
