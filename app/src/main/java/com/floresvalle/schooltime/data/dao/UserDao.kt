package com.floresvalle.schooltime.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.floresvalle.schooltime.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getFirstUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserByIdFlow(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT COUNT(*) > 0 FROM users WHERE LOWER(email) = LOWER(:email)")
    suspend fun existsByEmail(email: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserEntity): Long

    @Query("UPDATE users SET photoUri = :photoUri, syncState = 'PENDING_UPLOAD' WHERE id = :id")
    suspend fun updatePhotoUri(id: String, photoUri: String): Int

    @Query("UPDATE users SET career = :career, email = :email, syncState = 'PENDING_UPLOAD' WHERE id = :id")
    suspend fun updateRestrictedProfile(id: String, career: String, email: String): Int

    @Query("UPDATE users SET weightedAverage = :average, syncState = 'PENDING_UPLOAD' WHERE id = :id")
    suspend fun updateWeightedAverage(id: String, average: Double): Int
}
