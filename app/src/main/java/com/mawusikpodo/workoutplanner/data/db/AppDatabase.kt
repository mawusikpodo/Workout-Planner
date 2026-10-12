package com.mawusikpodo.workoutplanner.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [ExerciseEntity::class, WorkoutEntity::class, WorkoutItemEntity::class, SessionEntity::class, PlanEntryEntity::class],
  version = 2,
  exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun exerciseDao(): ExerciseDao

  abstract fun workoutDao(): WorkoutDao

  abstract fun sessionDao(): SessionDao

  abstract fun planDao(): PlanDao

  companion object {
    val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE workouts ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")
        }
      }

    fun create(context: Context): AppDatabase =
      Room.databaseBuilder(context, AppDatabase::class.java, "workout-planner.db")
        .addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
  }
}
