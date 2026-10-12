package com.mawusikpodo.workoutplanner.model

import kotlin.math.ceil

/** How the exercise figure moves; each value maps to a pose sequence in `ExerciseFigure`. */
enum class Motion { SQUAT, JUMP_SQUAT, PUSH, JACK, LUNGE, CLIMBER, PLANK, HINGE, BRIDGE, BURPEE, CRUNCH, PRESS }

enum class Equipment(val label: String) {
  BODYWEIGHT("Bodyweight"),
  DUMBBELLS("Dumbbells"),
  BANDS("Bands"),
}

enum class MuscleGroup(val label: String) {
  LEGS("Legs"),
  CORE("Core"),
  UPPER("Upper"),
  CARDIO("Cardio"),
  MOBILITY("Mobility"),
}

enum class Section(val label: String) {
  WARMUP("Warm-up"),
  CIRCUIT("Circuit"),
  COOLDOWN("Cool-down"),
}

enum class Feeling(val label: String) {
  EASY("Easy"),
  JUST_RIGHT("Just right"),
  TOUGH("Tough"),
}

data class Exercise(
  val id: String,
  val name: String,
  val muscles: String,
  val group: MuscleGroup,
  val equipment: Equipment,
  /** 1 = beginner, 2 = intermediate, 3 = advanced. */
  val difficulty: Int,
  val motion: Motion,
  val formCue: String,
)

data class WorkoutItem(
  val section: Section,
  val exercise: Exercise,
  val durationSec: Int?,
  val reps: Int?,
  val perSide: Boolean,
) {
  val doseLabel: String
    get() =
      when {
        durationSec != null -> "$durationSec s"
        perSide -> "$reps / side"
        else -> "$reps reps"
      }

  /** Rough time for rep-based items: ~3 s per rep. */
  val estimatedSec: Int
    get() = durationSec ?: ((reps ?: 10) * 3 * if (perSide) 2 else 1)
}

data class Workout(
  val id: String,
  val name: String,
  val level: String,
  val equipment: Equipment,
  val focus: String,
  val rounds: Int,
  val restSec: Int,
  val items: List<WorkoutItem>,
  val isCustom: Boolean = false,
) {
  fun section(section: Section) = items.filter { it.section == section }

  val estimatedSec: Int
    get() {
      val circuit = section(Section.CIRCUIT)
      val rests = (circuit.size * rounds - 1).coerceAtLeast(0)
      return section(Section.WARMUP).sumOf { it.estimatedSec } +
        circuit.sumOf { it.estimatedSec } * rounds +
        rests * restSec +
        section(Section.COOLDOWN).sumOf { it.estimatedSec }
    }

  val estimatedMinutes: Int
    get() = ceil(estimatedSec / 60.0).toInt()

  val exerciseCount: Int
    get() = items.size
}

data class Session(
  val id: Long,
  val workoutId: String,
  val workoutName: String,
  val startedAtMillis: Long,
  val epochDay: Long,
  val durationSec: Int,
  val exercisesCompleted: Int,
  val feeling: Feeling?,
)

/** A day in the plan: [workoutId] null means a planned rest day. */
data class PlanEntry(val epochDay: Long, val workoutId: String?)
