package com.mawusikpodo.workoutplanner.model

/** One screen of the active-workout player. A null [durationSec] means the user taps Done (rep-based). */
sealed interface Step {
  val durationSec: Int?

  data class GetReady(val next: WorkoutItem, override val durationSec: Int = GET_READY_SEC) : Step

  data class Move(
    val item: WorkoutItem,
    /** 1-based position among all exercise steps. */
    val number: Int,
    val round: Int?,
    val totalRounds: Int?,
  ) : Step {
    override val durationSec: Int?
      get() = item.durationSec
  }

  data class Rest(override val durationSec: Int, val next: WorkoutItem) : Step

  companion object {
    const val GET_READY_SEC = 5
  }
}

/**
 * Flattens a workout into player steps: a short get-ready, the warm-up, the circuit repeated for each
 * round with rest between exercises, then the cool-down.
 */
fun buildSteps(workout: Workout): List<Step> {
  val warmup = workout.section(Section.WARMUP)
  val circuit = workout.section(Section.CIRCUIT)
  val cooldown = workout.section(Section.COOLDOWN)
  val first = warmup.firstOrNull() ?: circuit.firstOrNull() ?: cooldown.firstOrNull() ?: return emptyList()

  val steps = mutableListOf<Step>(Step.GetReady(first))
  var number = 0
  warmup.forEach { steps += Step.Move(it, ++number, null, null) }
  for (round in 1..workout.rounds) {
    circuit.forEachIndexed { i, item ->
      steps += Step.Move(item, ++number, round, workout.rounds)
      val isLast = round == workout.rounds && i == circuit.lastIndex
      if (!isLast && workout.restSec > 0) {
        val next = circuit.getOrNull(i + 1) ?: circuit.first()
        steps += Step.Rest(workout.restSec, next)
      }
    }
  }
  cooldown.forEach { steps += Step.Move(it, ++number, null, null) }
  return steps
}
