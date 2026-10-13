package com.floresvalle.schooltime.data.repository

import android.content.Context
import com.floresvalle.schooltime.data.AppDatabase
import com.floresvalle.schooltime.data.entity.AcademicPeriodEntity
import com.floresvalle.schooltime.data.entity.UserEntity
import com.floresvalle.schooltime.data.sync.CloudSyncManager
import com.floresvalle.schooltime.util.AuthPreferences
import com.floresvalle.schooltime.util.AuthValidation
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context.applicationContext)
    private val userDao = db.userDao()
    private val academicPeriodDao = db.academicPeriodDao()
    private val cloudSync = CloudSyncManager(context.applicationContext)
    private val auth: FirebaseAuth get() = Firebase.auth

    suspend fun loginWithEmail(email: String, pass: String, rememberMe: Boolean): AuthResult<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val normalizedEmail = AuthValidation.normalizeEmail(email)
            val authResult = auth.signInWithEmailAndPassword(normalizedEmail, pass).await()
            val firebaseUser = authResult.user ?: return@withContext AuthResult.Error("No se pudo autenticar usuario")
            
            var user = userDao.getUserByEmail(normalizedEmail)
            if (user == null) {
                user = cloudSync.fetchUserProfile(firebaseUser.uid)
                if (user != null) {
                    userDao.insertOrUpdate(user)
                    cloudSync.restoreAllFromCloud(firebaseUser.uid)
                } else {
                    user = UserEntity(
                        id = firebaseUser.uid,
                        email = normalizedEmail,
                        firstName = firebaseUser.displayName?.split(" ")?.firstOrNull() ?: "Estudiante",
                        lastName = firebaseUser.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: "",
                        career = "Ingeniería de Sistemas",
                        currentSemester = "2025-I",
                        currentCycle = "V",
                        phone = "999999999",
                        photoUri = firebaseUser.photoUrl?.toString()
                    )
                    userDao.insertOrUpdate(user)
                    cloudSync.syncUserProfile(user)
                }
            }
            AuthPreferences.saveLoginSession(context, user.id, user.email, rememberMe)
            AuthResult.Success(user)
        } catch (e: Exception) {
            val localUser = userDao.getUserByEmail(AuthValidation.normalizeEmail(email))
            if (localUser != null && localUser.password == pass) {
                AuthPreferences.saveLoginSession(context, localUser.id, localUser.email, rememberMe)
                AuthResult.Success(localUser)
            } else {
                AuthResult.Error(AuthValidation.mapFirebaseAuthError(e), isNetworkError = true)
            }
        }
    }

    suspend fun checkAndSyncGoogleUser(uid: String, email: String, name: String, photoUrl: String?, rememberMe: Boolean): AuthResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val normalized = AuthValidation.normalizeEmail(email)
            var user = userDao.getUserByEmail(normalized)
            if (user == null) {
                user = cloudSync.fetchUserProfile(uid)
            }
            if (user != null) {
                userDao.insertOrUpdate(user)
                cloudSync.restoreAllFromCloud(uid)
                AuthPreferences.saveLoginSession(context, user.id, user.email, rememberMe)
                AuthResult.Success(true)
            } else {
                AuthResult.Success(false)
            }
        } catch (e: Exception) {
            AuthResult.Error(AuthValidation.mapFirebaseAuthError(e))
        }
    }

    suspend fun syncExistingRegisteredUserByEmail(email: String, name: String, photoUrl: String?, rememberMe: Boolean): AuthResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val normalized = AuthValidation.normalizeEmail(email)
            val user = userDao.getUserByEmail(normalized)
            if (user != null) {
                AuthPreferences.saveLoginSession(context, user.id, user.email, rememberMe)
                AuthResult.Success(true)
            } else {
                AuthResult.Success(false)
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Error al sincronizar")
        }
    }

    suspend fun saveAndSyncUserRegistration(userEntity: UserEntity, periodEntity: AcademicPeriodEntity, provider: String): AuthResult<Unit> = withContext(Dispatchers.IO) {
        try {
            userDao.insertOrUpdate(userEntity)
            academicPeriodDao.upsertPeriod(periodEntity)
            cloudSync.syncUserProfile(userEntity)
            cloudSync.syncAcademicPeriod(periodEntity)
            AuthPreferences.saveLoginSession(context, userEntity.id, userEntity.email, true)
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Error al registrar")
        }
    }

    suspend fun checkEmailAvailable(email: String): Boolean = withContext(Dispatchers.IO) {
        !userDao.existsByEmail(AuthValidation.normalizeEmail(email))
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        auth.signOut()
        AuthPreferences.clearSession(context)
    }

    suspend fun clearAllRoomData() = withContext(Dispatchers.IO) {
        AppDatabase.clearAllData(context)
        AuthPreferences.clearSession(context)
    }
}
