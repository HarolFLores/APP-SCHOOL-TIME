package com.floresvalle.schooltime.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.floresvalle.schooltime.data.entity.ExamEntity
import com.floresvalle.schooltime.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluationDao {
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isDeleted = 0 ORDER BY dueDate ASC")
    fun getTasks(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM exams WHERE userId = :userId AND isDeleted = 0 ORDER BY examDate ASC")
    fun getExams(userId: String): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExam(exam: ExamEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExams(exams: List<ExamEntity>): List<Long>

    @Query("UPDATE tasks SET status = :status, grade = :grade, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTaskStatusAndGrade(id: String, status: String, grade: Double?, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE exams SET status = :status, grade = :grade, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateExamStatusAndGrade(id: String, status: String, grade: Double?, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE tasks SET dueDate = :dueDate, dueTime = :dueTime, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTaskDateTime(id: String, dueDate: String, dueTime: String, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE exams SET examDate = :examDate, examTime = :examTime, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateExamDateTime(id: String, examDate: String, examTime: String, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteTask(id: String, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE exams SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteExam(id: String, updatedAt: Long = System.currentTimeMillis()): Int
}
