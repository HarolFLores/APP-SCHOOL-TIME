package com.floresvalle.schooltime.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "academic_periods")
data class AcademicPeriodEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val periodName: String = "2025-I",
    val startDate: String, // yyyy-MM-dd
    val endDate: String,   // yyyy-MM-dd
    val isActive: Boolean = true,
    val syncState: String = "PENDING_UPLOAD"
)
