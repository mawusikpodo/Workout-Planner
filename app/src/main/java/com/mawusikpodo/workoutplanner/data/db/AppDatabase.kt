package com.mawusikpodo.workoutplanner.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [ExerciseEntity::class, WorkoutEntity::class, WorkoutItemEntity::class, SessionEntity::class, PlanEntryEntity::class],
  version = 1,
  exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun exerciseDao(): ExerciseDao

  abstract fun workoutDao(): WorkoutDao

  abstract fun sessionDao(): SessionDao

  abstract fun planDao(): PlanDao

  companion object {
    fun create(context: Context): AppDatabase =
      Room.databaseBuilder(context, AppDatabase::class.java, "workout-planner.db").build()
  }
}
