package com.mawusikpodo.workoutplanner.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.mawusikpodo.workoutplanner.model.Equipment
import com.mawusikpodo.workoutplanner.model.Feeling
import com.mawusikpodo.workoutplanner.model.Motion
import com.mawusikpodo.workoutplanner.model.MuscleGroup
import com.mawusikpodo.workoutplanner.model.Section

@Entity(tableName = "exercises")
data class ExerciseEntity(
  @PrimaryKey val id: String,
  val name: String,
  val muscles: String,
  val muscleGroup: MuscleGroup,
  val equipment: Equipment,
  val difficulty: Int,
  val motion: Motion,
  val formCue: String,
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
  @PrimaryKey val id: String,
  val name: String,
  val level: String,
  val equipment: Equipment,
  val focus: String,
  val rounds: Int,
  val restSec: Int,
)

@Entity(
  tableName = "workout_items",
  primaryKeys = ["workoutId", "position"],
  indices = [Index("exerciseId")],
)
data class WorkoutItemEntity(
  val workoutId: String,
  val position: Int,
  val section: Section,
  val exerciseId: String,
  val durationSec: Int?,
  val reps: Int?,
  val perSide: Boolean = false,
)

@Entity(tableName = "sessions", indices = [Index("epochDay")])
data class SessionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val workoutId: String,
  val workoutName: String,
  val startedAtMillis: Long,
  val epochDay: Long,
  val durationSec: Int,
  val exercisesCompleted: Int,
  val feeling: Feeling?,
)

@Entity(tableName = "plan_entries")
data class PlanEntryEntity(@PrimaryKey val epochDay: Long, val workoutId: String?)

data class WorkoutItemWithExercise(
  @Embedded val item: WorkoutItemEntity,
  @Relation(parentColumn = "exerciseId", entityColumn = "id") val exercise: ExerciseEntity,
)

data class WorkoutWithItems(
  @Embedded val workout: WorkoutEntity,
  @Relation(entity = WorkoutItemEntity::class, parentColumn = "id", entityColumn = "workoutId")
  val items: List<WorkoutItemWithExercise>,
)
