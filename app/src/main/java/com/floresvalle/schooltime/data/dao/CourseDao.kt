package com.floresvalle.schooltime.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.floresvalle.schooltime.data.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses WHERE userId = :userId AND isDeleted = 0")
    fun getAllCourses(userId: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE name = :name AND userId = :userId AND isDeleted = 0 LIMIT 1")
    suspend fun getCourseByName(name: String, userId: String): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourse(course: CourseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourses(courses: List<CourseEntity>): List<Long>
}
