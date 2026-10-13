package com.floresvalle.schooltime.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.floresvalle.schooltime.data.dao.*
import com.floresvalle.schooltime.data.entity.*

@Database(
    entities = [
        UserEntity::class,
        AcademicPeriodEntity::class,
        CourseEntity::class,
        ClassSessionEntity::class,
        TaskEntity::class,
        ExamEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun academicPeriodDao(): AcademicPeriodDao
    abstract fun courseDao(): CourseDao
    abstract fun sessionDao(): ClassSessionDao
    abstract fun evaluationDao(): EvaluationDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "schooltime_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun clearAllData(context: Context) {
            getDatabase(context).clearAllTables()
        }
    }
}
