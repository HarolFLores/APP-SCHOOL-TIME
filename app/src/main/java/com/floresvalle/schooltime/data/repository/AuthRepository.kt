package com.floresvalle.schooltime.data.repository

import android.content.Context
import android.util.Log
import com.floresvalle.schooltime.data.AppDatabase
import com.floresvalle.schooltime.data.entity.AcademicPeriodEntity
import com.floresvalle.schooltime.data.entity.UserEntity
import com.floresvalle.schooltime.data.sync.CloudSyncManager
import com.floresvalle.schooltime.util.AuthPreferences
import com.floresvalle.schooltime.util.AuthValidation
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

sealed class AuthResult<out T> {
    data class Success<out T>(
        val data: T,
        val isOffline: Boolean = false,
        val message: String? = null
    ) : AuthResult<T>()

    data class Error(
        val message: String,
        val canRetryOffline: Boolean = false,
        val isNetworkError: Boolean = false
    ) : AuthResult<Nothing>()
}

class AuthRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context.applicationContext)
    private val userDao = db.userDao()
    private val academicPeriodDao = db.academicPeriodDao()

    suspend fun loginWithEmail(
        email: String,
        pass: String,
        rememberMe: Boolean
    ): AuthResult<UserEntity> = withContext(Dispatchers.IO) {
        val normalizedEmail = AuthValidation.normalizeEmail(email)

        try {
            val authResult = Firebase.auth.signInWithEmailAndPassword(normalizedEmail, pass).await()
            val firebaseUser = authResult.user

            if (firebaseUser == null) {
                return@withContext AuthResult.Error("No se pudo obtener información del usuario.")
            }

            try {
                firebaseUser.reload().await()
            } catch (reloadEx: Exception) {
                Log.w("AuthRepository", "Error recargando estado de usuario: ${reloadEx.message}")
            }

            // Sync with Room and Cloud (Firebase Firestore)
            var localUser = userDao.getUserByEmail(normalizedEmail) ?: userDao.getUserByIdOnce(firebaseUser.uid)

            // 1. Consultar Firestore en la nube por el perfil registrado
            val cloudProfile = CloudSyncManager.fetchUserProfile(firebaseUser.uid, normalizedEmail)
            if (cloudProfile != null) {
                userDao.insertUser(cloudProfile)
                localUser = cloudProfile
            }

            if (localUser == null) {
                val names = (firebaseUser.displayName ?: "Estudiante").split(" ")
                val fName = names.firstOrNull() ?: "Estudiante"
                val lName = names.drop(1).joinToString(" ")
                val fallbackUser = UserEntity(
                    id = firebaseUser.uid,
                    email = normalizedEmail,
                    firstName = fName,
                    lastName = lName,
                    career = "General",
                    currentSemester = "2026-I",
                    currentCycle = "1er Ciclo",
                    photoUri = firebaseUser.photoUrl?.toString(),
                    syncState = "SYNCED"
                )
                userDao.insertUser(fallbackUser)
                localUser = fallbackUser
            }

            AuthPreferences.saveUserSession(context, firebaseUser.uid, normalizedEmail, rememberMe)
            ensureActivePeriodExists(localUser.id, localUser.currentSemester)

            // Restaurar cursos, clases, tareas y exámenes inmediatamente
            CloudSyncManager.restoreAllFromCloud(localUser.id, db)

            AuthResult.Success(localUser, isOffline = false)

        } catch (e: Exception) {
            val isOfflineError = e is FirebaseNetworkException || e is IOException ||
                    e.message?.contains("network", ignoreCase = true) == true

            if (isOfflineError) {
                val cachedUser = userDao.getUserByEmail(normalizedEmail)
                if (cachedUser != null) {
                    AuthPreferences.saveUserSession(context, cachedUser.id, normalizedEmail, rememberMe)
                    return@withContext AuthResult.Success(
                        data = cachedUser,
                        isOffline = true,
                        message = "Sin conexión: Has iniciado sesión en modo offline con tus datos guardados."
                    )
                }
                return@withContext AuthResult.Error(
                    message = "Sin conexión a internet y no se encontraron datos guardados para este correo.",
                    isNetworkError = true
                )
            }

            AuthResult.Error(AuthValidation.mapFirebaseAuthError(e, isRegister = false))
        }
    }

    suspend fun checkAndSyncGoogleUser(
        uid: String,
        email: String,
        name: String,
        photoUrl: String?,
        rememberMe: Boolean
    ): AuthResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val normalizedEmail = AuthValidation.normalizeEmail(email)

            // 1. Consultar si esta cuenta de Google (por su UID) ya tiene perfil completo en Cloud Firestore
            val cloudProfile = CloudSyncManager.fetchUserProfile(uid, normalizedEmail)
            if (cloudProfile != null && cloudProfile.career.isNotBlank() && cloudProfile.career != "General") {
                userDao.insertUser(cloudProfile)
                val resolvedSemester = cloudProfile.currentSemester.ifBlank { "2026-I" }
                ensureActivePeriodExists(cloudProfile.id, resolvedSemester)
                AuthPreferences.saveUserSession(context, cloudProfile.id, normalizedEmail, rememberMe)
                CloudSyncManager.restoreAllFromCloud(cloudProfile.id, db)
                return@withContext AuthResult.Success(true)
            }

            // 2. Consultar si ya existe en la base de datos Room local para este UID o correo
            val localUser = userDao.getUserByIdOnce(uid) ?: userDao.getUserByEmail(normalizedEmail)
            if (localUser != null && localUser.career.isNotBlank() && localUser.career != "General") {
                val resolvedSemester = localUser.currentSemester.ifBlank { "2026-I" }
                ensureActivePeriodExists(localUser.id, resolvedSemester)
                AuthPreferences.saveUserSession(context, localUser.id, normalizedEmail, rememberMe)
                CloudSyncManager.restoreAllFromCloud(localUser.id, db)
                return@withContext AuthResult.Success(true)
            }

            // NO EXISTE EN FIRESTORE NI EN ROOM PARA ESTE UID -> ES UN NUEVO USUARIO DE GOOGLE
            // Retorna false para redirigir al formulario de onboarding (carrera, ciclo, fechas, sin contraseña)
            AuthResult.Success(false)
        } catch (e: Exception) {
            AuthResult.Error(AuthValidation.mapFirebaseAuthError(e))
        }
    }

    suspend fun syncExistingRegisteredUserByEmail(
        email: String,
        name: String,
        photoUrl: String?,
        rememberMe: Boolean
    ): AuthResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val normalizedEmail = AuthValidation.normalizeEmail(email)
            var localUser = userDao.getUserByEmail(normalizedEmail)

            val cloudProfile = CloudSyncManager.fetchUserProfile("", normalizedEmail)
            if (cloudProfile != null) {
                userDao.insertUser(cloudProfile)
                localUser = cloudProfile
            }

            if (localUser != null && localUser.career.isNotBlank() && localUser.career != "General") {
                ensureActivePeriodExists(localUser.id, localUser.currentSemester)
                AuthPreferences.saveUserSession(context, localUser.id, normalizedEmail, rememberMe)
                CloudSyncManager.restoreAllFromCloud(localUser.id, db)
                return@withContext AuthResult.Success(true)
            }

            AuthResult.Success(false)
        } catch (e: Exception) {
            AuthResult.Error(AuthValidation.mapFirebaseAuthError(e))
        }
    }

    private suspend fun ensureActivePeriodExists(userId: String, semester: String) {
        try {
            val existingPeriod = academicPeriodDao.getActivePeriodSync(userId)
            if (existingPeriod == null) {
                val periodName = if (semester.isNotBlank()) semester else "2026-I"
                val defaultPeriod = AcademicPeriodEntity(
                    userId = userId,
                    periodName = periodName,
                    startDate = "2026-03-16",
                    endDate = "2026-07-20",
                    isActive = true
                )
                academicPeriodDao.upsertPeriod(defaultPeriod)
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Error asegurando periodo activo: ${e.message}")
        }
    }

    suspend fun saveAndSyncUserRegistration(
        userEntity: UserEntity,
        periodEntity: AcademicPeriodEntity,
        provider: String
    ): AuthResult<Unit> = withContext(NonCancellable + Dispatchers.IO) {
        try {
            // Inserción en Room (Offline-First)
            userDao.insertUser(userEntity)
            academicPeriodDao.upsertPeriod(periodEntity)
            AuthPreferences.saveUserSession(context, userEntity.id, userEntity.email, true)

            // Sincronización en la Nube (Firebase Firestore)
            CloudSyncManager.syncUserProfile(userEntity)
            CloudSyncManager.syncAcademicPeriod(periodEntity)

            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error("Error guardando datos del usuario: ${e.localizedMessage}")
        }
    }

    suspend fun checkEmailAvailable(email: String): Boolean = withContext(Dispatchers.IO) {
        val normalized = AuthValidation.normalizeEmail(email)
        val existing = userDao.getUserByEmail(normalized)
        existing == null || existing.career.isBlank() || existing.career == "General"
    }

    suspend fun logout(): Unit = withContext(Dispatchers.IO) {
        try {
            Firebase.auth.signOut()
            val shouldRemember = AuthPreferences.shouldRememberMe(context)
            if (!shouldRemember) {
                db.clearAllTables()
            }
            AuthPreferences.clearSession(context)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error durante logout: ${e.message}")
        }
    }

    suspend fun clearAllRoomData(): Unit = withContext(Dispatchers.IO) {
        try {
            db.clearAllTables()
            AuthPreferences.clearSession(context)
            Log.d("AuthRepository", "Base de datos Room limpiada exitosamente.")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al limpiar Room: ${e.message}")
        }
    }
}
