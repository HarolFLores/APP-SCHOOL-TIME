package com.floresvalle.schooltime.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.floresvalle.schooltime.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users LIMIT 1")
    fun getUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserByIdOnce(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT COUNT(*) > 0 FROM users WHERE LOWER(email) = LOWER(:email)")
    suspend fun existsByEmail(email: String): Boolean

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String): Int

    @Query("DELETE FROM users")
    suspend fun clearAllUsers(): Int

    @Query("UPDATE users SET career = :career, email = :email, syncState = 'PENDING_UPLOAD' WHERE id = :userId")
    suspend fun updateUserProfile(userId: String, career: String, email: String): Int

    @Query("UPDATE users SET photoUri = :photoUri, syncState = 'PENDING_UPLOAD' WHERE id = :userId")
    suspend fun updatePhotoUri(userId: String, photoUri: String?): Int

    @Query("UPDATE users SET weightedAverage = :avg, syncState = 'PENDING_UPLOAD' WHERE id = :userId")
    suspend fun updateWeightedAverage(userId: String, avg: Double): Int
}
