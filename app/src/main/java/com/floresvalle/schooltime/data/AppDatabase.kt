package com.floresvalle.schooltime.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.floresvalle.schooltime.data.dao.AcademicPeriodDao
import com.floresvalle.schooltime.data.dao.ClassSessionDao
import com.floresvalle.schooltime.data.dao.CourseDao
import com.floresvalle.schooltime.data.dao.EvaluationDao
import com.floresvalle.schooltime.data.dao.NotificationDao
import com.floresvalle.schooltime.data.dao.UserDao
import com.floresvalle.schooltime.data.entity.AcademicPeriodEntity
import com.floresvalle.schooltime.data.entity.ClassSessionEntity
import com.floresvalle.schooltime.data.entity.CourseEntity
import com.floresvalle.schooltime.data.entity.ExamEntity
import com.floresvalle.schooltime.data.entity.NotificationEntity
import com.floresvalle.schooltime.data.entity.TaskEntity
import com.floresvalle.schooltime.data.entity.UserEntity

@Database(
    entities = [
        ClassSessionEntity::class,
        TaskEntity::class,
        ExamEntity::class,
        UserEntity::class,
        NotificationEntity::class,
        AcademicPeriodEntity::class,
        CourseEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): ClassSessionDao
    abstract fun evaluationDao(): EvaluationDao
    abstract fun userDao(): UserDao
    abstract fun notificationDao(): NotificationDao
    abstract fun academicPeriodDao(): AcademicPeriodDao
    abstract fun courseDao(): CourseDao

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
