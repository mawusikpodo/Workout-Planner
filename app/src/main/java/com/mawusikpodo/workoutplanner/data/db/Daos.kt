package com.mawusikpodo.workoutplanner.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
  @Query("SELECT * FROM exercises ORDER BY name") fun observeAll(): Flow<List<ExerciseEntity>>

  @Query("SELECT COUNT(*) FROM exercises") suspend fun count(): Int

  @Upsert suspend fun upsertAll(exercises: List<ExerciseEntity>)
}

@Dao
interface WorkoutDao {
  @Transaction
  @Query("SELECT * FROM workouts ORDER BY name")
  fun observeAll(): Flow<List<WorkoutWithItems>>

  @Transaction
  @Query("SELECT * FROM workouts WHERE id = :id")
  fun observe(id: String): Flow<WorkoutWithItems?>

  @Upsert suspend fun upsertWorkouts(workouts: List<WorkoutEntity>)

  @Upsert suspend fun upsertItems(items: List<WorkoutItemEntity>)
}

@Dao
interface SessionDao {
  @Query("SELECT * FROM sessions ORDER BY startedAtMillis DESC") fun observeAll(): Flow<List<SessionEntity>>

  @Insert suspend fun insert(session: SessionEntity): Long
}

@Dao
interface PlanDao {
  @Query("SELECT * FROM plan_entries WHERE epochDay BETWEEN :fromDay AND :toDay ORDER BY epochDay")
  fun observeRange(fromDay: Long, toDay: Long): Flow<List<PlanEntryEntity>>

  @Upsert suspend fun upsert(entry: PlanEntryEntity)

  @Upsert suspend fun upsertAll(entries: List<PlanEntryEntity>)

  @Query("DELETE FROM plan_entries WHERE epochDay = :epochDay") suspend fun delete(epochDay: Long)
}
