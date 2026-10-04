package com.mawusikpodo.workoutplanner.data

import com.mawusikpodo.workoutplanner.data.db.AppDatabase
import com.mawusikpodo.workoutplanner.data.db.ExerciseEntity
import com.mawusikpodo.workoutplanner.data.db.PlanEntryEntity
import com.mawusikpodo.workoutplanner.data.db.SessionEntity
import com.mawusikpodo.workoutplanner.data.db.WorkoutWithItems
import com.mawusikpodo.workoutplanner.model.Equipment
import com.mawusikpodo.workoutplanner.model.Exercise
import com.mawusikpodo.workoutplanner.model.Feeling
import com.mawusikpodo.workoutplanner.model.PlanEntry
import com.mawusikpodo.workoutplanner.model.Session
import com.mawusikpodo.workoutplanner.model.Workout
import com.mawusikpodo.workoutplanner.model.WorkoutItem
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class WorkoutRepository(
  private val db: AppDatabase,
  private val today: () -> LocalDate = { LocalDate.now() },
  private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
  fun today(): LocalDate = today.invoke()

  /** Inserts or refreshes the built-in library. Safe to call on every launch. */
  suspend fun ensureSeeded() {
    db.exerciseDao().upsertAll(SeedData.exercises)
    db.workoutDao().upsertWorkouts(SeedData.workouts)
    db.workoutDao().upsertItems(SeedData.workoutItems)
  }

  val exercises: Flow<List<Exercise>> = db.exerciseDao().observeAll().map { list -> list.map { it.toModel() } }

  val workouts: Flow<List<Workout>> = db.workoutDao().observeAll().map { list -> list.map { it.toModel() } }

  fun workout(id: String): Flow<Workout?> = db.workoutDao().observe(id).map { it?.toModel() }

  val sessions: Flow<List<Session>> = db.sessionDao().observeAll().map { list -> list.map { it.toModel() } }

  fun plan(from: LocalDate, to: LocalDate): Flow<List<PlanEntry>> =
    db.planDao().observeRange(from.toEpochDay(), to.toEpochDay()).map { list -> list.map { PlanEntry(it.epochDay, it.workoutId) } }

  suspend fun saveSession(
    workoutId: String,
    workoutName: String,
    startedAtMillis: Long,
    durationSec: Int,
    exercisesCompleted: Int,
    feeling: Feeling?,
  ): Long {
    val day = Instant.ofEpochMilli(startedAtMillis).atZone(zone()).toLocalDate().toEpochDay()
    return db.sessionDao()
      .insert(SessionEntity(0, workoutId, workoutName, startedAtMillis, day, durationSec, exercisesCompleted, feeling))
  }

  /** [workoutId] null marks a rest day. */
  suspend fun setPlan(day: LocalDate, workoutId: String?) = db.planDao().upsert(PlanEntryEntity(day.toEpochDay(), workoutId))

  suspend fun clearPlan(day: LocalDate) = db.planDao().delete(day.toEpochDay())

  /**
   * Fills a Monday-based week with [goal] workouts spread across the week and rest days in between,
   * rotating through bodyweight workouts so no workout repeats back to back. Past days and days that
   * already have a plan are left alone.
   */
  suspend fun autoBuildWeek(weekStart: LocalDate, workouts: List<Workout>, goal: Int = DEFAULT_WEEKLY_GOAL) {
    // No equipment setting yet, so only plan workouts that need nothing.
    val candidates = workouts.filter { it.equipment == Equipment.BODYWEIGHT }
    if (candidates.isEmpty()) return
    val existing = plan(weekStart, weekStart.plusDays(6)).first().map { it.epochDay }.toSet()
    val trainingDays = trainingDaysFor(goal)
    val startIndex = (weekStart.toEpochDay() / 7).toInt()
    var next = 0
    val entries =
      (0 until 7).mapNotNull { offset ->
        val day = weekStart.plusDays(offset.toLong())
        val isTraining = day.dayOfWeek in trainingDays
        val workoutId = if (isTraining) candidates[(startIndex + next++).mod(candidates.size)].id else null
        if (day < today() || day.toEpochDay() in existing) null else PlanEntryEntity(day.toEpochDay(), workoutId)
      }
    db.planDao().upsertAll(entries)
  }

  companion object {
    const val DEFAULT_WEEKLY_GOAL = 4

    fun trainingDaysFor(goal: Int): Set<DayOfWeek> =
      when (goal.coerceIn(1, 7)) {
        1 -> setOf(DayOfWeek.WEDNESDAY)
        2 -> setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY)
        3 -> setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        4 -> setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)
        5 -> setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
        6 -> DayOfWeek.entries.toSet() - DayOfWeek.SUNDAY
        else -> DayOfWeek.entries.toSet()
      }
  }
}

private fun ExerciseEntity.toModel() = Exercise(id, name, muscles, muscleGroup, equipment, difficulty, motion, formCue)

private fun WorkoutWithItems.toModel() =
  Workout(
    id = workout.id,
    name = workout.name,
    level = workout.level,
    equipment = workout.equipment,
    focus = workout.focus,
    rounds = workout.rounds,
    restSec = workout.restSec,
    items =
      items
        .sortedBy { it.item.position }
        .map { WorkoutItem(it.item.section, it.exercise.toModel(), it.item.durationSec, it.item.reps, it.item.perSide) },
  )

private fun SessionEntity.toModel() =
  Session(id, workoutId, workoutName, startedAtMillis, epochDay, durationSec, exercisesCompleted, feeling)
