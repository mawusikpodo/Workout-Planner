package com.mawusikpodo.workoutplanner.model

import com.mawusikpodo.workoutplanner.data.WorkoutRepository
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutLogicTest {
  private fun exercise(id: String) =
    Exercise(id, id, "", MuscleGroup.LEGS, Equipment.BODYWEIGHT, 1, Motion.SQUAT, "")

  private val workout =
    Workout(
      id = "w",
      name = "Test",
      level = "Beginner",
      equipment = Equipment.BODYWEIGHT,
      focus = "Full body",
      rounds = 2,
      restSec = 15,
      items =
        listOf(
          WorkoutItem(Section.WARMUP, exercise("warm"), 60, null, false),
          WorkoutItem(Section.CIRCUIT, exercise("a"), 40, null, false),
          WorkoutItem(Section.CIRCUIT, exercise("b"), null, 10, false),
          WorkoutItem(Section.COOLDOWN, exercise("cool"), 30, null, false),
        ),
    )

  @Test
  fun buildSteps_ordersWarmupRoundsWithRestThenCooldown() {
    val steps = buildSteps(workout)
    val shape =
      steps.map {
        when (it) {
          is Step.GetReady -> "ready"
          is Step.Move -> it.item.exercise.id + (it.round?.let { r -> "@$r" } ?: "")
          is Step.Rest -> "rest"
        }
      }
    assertEquals(listOf("ready", "warm", "a@1", "rest", "b@1", "rest", "a@2", "rest", "b@2", "cool"), shape)
  }

  @Test
  fun buildSteps_numbersMovesSequentially() {
    val numbers = buildSteps(workout).filterIsInstance<Step.Move>().map { it.number }
    assertEquals((1..6).toList(), numbers)
  }

  @Test
  fun buildSteps_restBeforeNextRoundPointsAtFirstCircuitExercise() {
    val rests = buildSteps(workout).filterIsInstance<Step.Rest>()
    assertEquals(listOf("b", "a", "b"), rests.map { it.next.exercise.id })
  }

  @Test
  fun estimatedSec_countsRoundsRestsAndRepTime() {
    // warm 60 + (40 + 10 reps * 3 s) * 2 rounds + 3 rests * 15 + cool 30
    assertEquals(60 + 140 + 45 + 30, workout.estimatedSec)
    assertEquals(5, workout.estimatedMinutes)
  }

  @Test
  fun currentStreak_countsBackFromTodayOrYesterday() {
    val today = LocalDate.of(2026, 10, 4)
    val d = today.toEpochDay()
    assertEquals(3, currentStreak(setOf(d, d - 1, d - 2, d - 5), today))
    assertEquals(2, currentStreak(setOf(d - 1, d - 2), today))
    assertEquals(0, currentStreak(setOf(d - 2), today))
  }

  @Test
  fun bestStreak_findsLongestRun() {
    assertEquals(4, bestStreak(setOf(1, 2, 5, 6, 7, 8, 10)))
    assertEquals(0, bestStreak(emptySet()))
  }

  @Test
  fun weeklyMinutes_bucketsSessionsByMondayWeek() {
    val today = LocalDate.of(2026, 10, 7) // Wednesday
    fun session(date: LocalDate, minutes: Int) = Session(0, "w", "W", 0, date.toEpochDay(), minutes * 60, 0, null)
    val result =
      weeklyMinutes(
        listOf(
          session(LocalDate.of(2026, 10, 5), 20), // this week (Monday)
          session(LocalDate.of(2026, 10, 7), 10), // this week
          session(LocalDate.of(2026, 10, 4), 30), // last week (Sunday)
          session(LocalDate.of(2026, 1, 1), 99), // outside the window
        ),
        today,
        weeks = 3,
      )
    assertEquals(listOf(0, 30, 30), result)
  }

  @Test
  fun trainingDays_matchGoalSize() {
    for (goal in 1..7) assertEquals(goal, WorkoutRepository.trainingDaysFor(goal).size)
    assertTrue(DayOfWeek.SUNDAY !in WorkoutRepository.trainingDaysFor(4))
  }
}
