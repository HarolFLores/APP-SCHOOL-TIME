package com.floresvalle.schooltime.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val password: String? = null,
    val firstName: String,
    val lastName: String,
    val career: String,
    val currentSemester: String,
    val currentCycle: String,
    val phone: String = "",
    val photoUri: String? = null,
    val enrolledCredits: Int = 0,
    val weightedAverage: Double = 0.0,
    val syncState: String = "PENDING_UPLOAD"
)
