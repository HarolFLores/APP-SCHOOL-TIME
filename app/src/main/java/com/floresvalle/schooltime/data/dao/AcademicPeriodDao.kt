package com.floresvalle.schooltime.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.floresvalle.schooltime.data.entity.AcademicPeriodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicPeriodDao {
    @Query("SELECT * FROM academic_periods WHERE userId = :userId AND isActive = 1 LIMIT 1")
    fun getActivePeriod(userId: String): Flow<AcademicPeriodEntity?>

    @Query("SELECT * FROM academic_periods WHERE userId = :userId AND isActive = 1 LIMIT 1")
    suspend fun getActivePeriodSync(userId: String): AcademicPeriodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPeriod(period: AcademicPeriodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPeriods(periods: List<AcademicPeriodEntity>): List<Long>

    @Query("DELETE FROM academic_periods WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String): Int

    @Query("DELETE FROM academic_periods")
    suspend fun clearAllPeriods(): Int
}
