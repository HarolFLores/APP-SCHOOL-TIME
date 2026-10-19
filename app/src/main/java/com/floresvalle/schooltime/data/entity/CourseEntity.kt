package com.floresvalle.schooltime.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val periodId: String,
    val name: String,
    val code: String? = null,
    val teacherName: String? = null,
    val colorHex: String? = "#0E7490",
    val credits: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
