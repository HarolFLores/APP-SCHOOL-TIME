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
    fun getAllActiveTasks(userId: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>): List<Long>

    @Query("SELECT * FROM exams WHERE userId = :userId AND isDeleted = 0 ORDER BY examDate ASC")
    fun getAllActiveExams(userId: String): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExam(exam: ExamEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExams(exams: List<ExamEntity>): List<Long>

    @Query("UPDATE tasks SET status = :status, grade = :grade, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun updateTaskStatusAndGrade(taskId: String, status: String, grade: Double?, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE exams SET status = :status, grade = :grade, updatedAt = :timestamp WHERE id = :examId")
    suspend fun updateExamStatusAndGrade(examId: String, status: String, grade: Double?, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE tasks SET dueDate = :dueDate, dueTime = :dueTime, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun updateTaskDateTime(taskId: String, dueDate: String, dueTime: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE tasks SET title = :title, dueDate = :dueDate, dueTime = :dueTime, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun updateTaskDetails(taskId: String, title: String, dueDate: String, dueTime: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun softDeleteTask(taskId: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE exams SET examDate = :examDate, examTime = :examTime, updatedAt = :timestamp WHERE id = :examId")
    suspend fun updateExamDateTime(examId: String, examDate: String, examTime: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE exams SET type = :type, examDate = :examDate, examTime = :examTime, updatedAt = :timestamp WHERE id = :examId")
    suspend fun updateExamDetails(examId: String, type: String, examDate: String, examTime: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE exams SET isDeleted = 1, updatedAt = :timestamp WHERE id = :examId")
    suspend fun softDeleteExam(examId: String, timestamp: Long = System.currentTimeMillis()): Int
}
