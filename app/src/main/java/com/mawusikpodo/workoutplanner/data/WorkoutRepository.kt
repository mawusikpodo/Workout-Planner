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
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

open class WorkoutRepository(
  private val db: AppDatabase? = null,
  context: Context? = null,
  private val today: () -> LocalDate = { LocalDate.now() },
  private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
  private val database: AppDatabase
    get() = checkNotNull(db) { "AppDatabase is required for database operations" }

  fun today(): LocalDate = today.invoke()

  private val prefs = context?.getSharedPreferences("workout_planner_prefs", Context.MODE_PRIVATE)
  private val _bookmarks = MutableStateFlow<Set<String>>(prefs?.getStringSet("bookmarked_exercises", emptySet()) ?: emptySet())
  val bookmarks: StateFlow<Set<String>> = _bookmarks.asStateFlow()

  fun toggleBookmark(exerciseId: String) {
    val current = _bookmarks.value.toMutableSet()
    if (exerciseId in current) {
      current.remove(exerciseId)
    } else {
      current.add(exerciseId)
    }
    prefs?.edit()?.putStringSet("bookmarked_exercises", current)?.apply()
    _bookmarks.value = current
  }

  /** Inserts or refreshes the built-in library. Safe to call on every launch. */
  suspend fun ensureSeeded() {
    database.exerciseDao().upsertAll(SeedData.exercises)
    database.workoutDao().upsertWorkouts(SeedData.workouts)
    database.workoutDao().upsertItems(SeedData.workoutItems)
  }

  val exercises: Flow<List<Exercise>>
    get() = database.exerciseDao().observeAll().map { list -> list.map { it.toModel() } }

  val workouts: Flow<List<Workout>>
    get() = database.workoutDao().observeAll().map { list -> list.map { it.toModel() } }

  open fun workout(id: String): Flow<Workout?> = database.workoutDao().observe(id).map { it?.toModel() }

  suspend fun saveCustomWorkout(
    id: String = "custom_${System.currentTimeMillis()}",
    name: String,
    level: String,
    equipment: Equipment,
    focus: String,
    rounds: Int,
    restSec: Int,
    items: List<WorkoutItem>,
  ): String {
    val workoutEntity =
      com.mawusikpodo.workoutplanner.data.db.WorkoutEntity(
        id = id,
        name = name,
        level = level,
        equipment = equipment,
        focus = focus,
        rounds = rounds,
        restSec = restSec,
        isCustom = true,
      )
    val itemEntities =
      items.mapIndexed { index, item ->
        com.mawusikpodo.workoutplanner.data.db.WorkoutItemEntity(
          workoutId = id,
          position = index,
          section = item.section,
          exerciseId = item.exercise.id,
          durationSec = item.durationSec,
          reps = item.reps,
          perSide = item.perSide,
        )
      }
    database.workoutDao().upsertCustomWorkout(workoutEntity, itemEntities)
    return id
  }

  suspend fun deleteCustomWorkout(id: String) {
    database.workoutDao().deleteWorkout(id)
    database.workoutDao().deleteWorkoutItems(id)
  }

  val sessions: Flow<List<Session>>
    get() = database.sessionDao().observeAll().map { list -> list.map { it.toModel() } }

  fun plan(from: LocalDate, to: LocalDate): Flow<List<PlanEntry>> =
    database.planDao().observeRange(from.toEpochDay(), to.toEpochDay()).map { list -> list.map { PlanEntry(it.epochDay, it.workoutId) } }

  suspend fun saveSession(
    workoutId: String,
    workoutName: String,
    startedAtMillis: Long,
    durationSec: Int,
    exercisesCompleted: Int,
    feeling: Feeling?,
  ): Long {
    val day = Instant.ofEpochMilli(startedAtMillis).atZone(zone()).toLocalDate().toEpochDay()
    return database.sessionDao()
      .insert(SessionEntity(0, workoutId, workoutName, startedAtMillis, day, durationSec, exercisesCompleted, feeling))
  }

  /** [workoutId] null marks a rest day. */
  suspend fun setPlan(day: LocalDate, workoutId: String?) = database.planDao().upsert(PlanEntryEntity(day.toEpochDay(), workoutId))

  suspend fun clearPlan(day: LocalDate) = database.planDao().delete(day.toEpochDay())

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
    database.planDao().upsertAll(entries)
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
    isCustom = workout.isCustom,
  )

private fun SessionEntity.toModel() =
  Session(id, workoutId, workoutName, startedAtMillis, epochDay, durationSec, exercisesCompleted, feeling)
