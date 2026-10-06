package com.floresvalle.schooltime.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.floresvalle.schooltime.data.entity.ClassSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassSessionDao {
    @Query("SELECT * FROM sessions WHERE sessionDate = :date AND userId = :userId AND isDeleted = 0 ORDER BY startTime ASC")
    fun getSessionsForDate(date: String, userId: String): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM sessions WHERE userId = :userId AND isDeleted = 0 ORDER BY sessionDate ASC, startTime ASC")
    fun getAllActiveSessions(userId: String): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM sessions WHERE userId = :userId AND isDeleted = 0 ORDER BY sessionDate ASC, startTime ASC")
    suspend fun getAllActiveSessionsSync(userId: String): List<ClassSessionEntity>

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): ClassSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: ClassSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessions(sessions: List<ClassSessionEntity>): List<Long>

    @Query("UPDATE sessions SET isDeleted = 1, syncState = 'PENDING_UPLOAD', updatedAt = :timestamp WHERE id = :sessionId")
    suspend fun softDeleteSession(sessionId: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE sessions SET docente = :docente, startTime = :startTime, endTime = :endTime, sessionDate = :sessionDate, updatedAt = :timestamp WHERE id = :sessionId")
    suspend fun updateSessionDetails(sessionId: String, docente: String?, startTime: String, endTime: String, sessionDate: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("SELECT * FROM sessions WHERE syncState = 'PENDING_UPLOAD'")
    suspend fun getPendingUploadSessions(): List<ClassSessionEntity>

    @Query("UPDATE sessions SET syncState = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>): Int
}
