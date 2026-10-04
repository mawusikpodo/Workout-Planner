package com.mawusikpodo.workoutplanner.ui.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawusikpodo.workoutplanner.CompleteKey
import com.mawusikpodo.workoutplanner.data.WorkoutRepository
import com.mawusikpodo.workoutplanner.model.Step
import com.mawusikpodo.workoutplanner.model.Workout
import com.mawusikpodo.workoutplanner.model.WorkoutItem
import com.mawusikpodo.workoutplanner.model.buildSteps
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
  val loading: Boolean = true,
  val workoutName: String = "",
  val stepIndex: Int = 0,
  val step: Step? = null,
  /** Seconds left in a timed step; null for rep-based steps. */
  val remainingSec: Int? = null,
  val paused: Boolean = false,
  val elapsedSec: Int = 0,
  val progress: Float = 0f,
  val totalMoves: Int = 0,
  val upNext: WorkoutItem? = null,
  /** Set once the last step ends or the user finishes early. */
  val finished: CompleteKey? = null,
)

class ActiveWorkoutViewModel(
  repository: WorkoutRepository,
  workoutId: String,
  private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
  private val _state = MutableStateFlow(PlayerUiState())
  val state: StateFlow<PlayerUiState> = _state.asStateFlow()

  private val startedAt = now()
  private lateinit var workout: Workout
  private var steps: List<Step> = emptyList()
  private var completedMoves = 0

  init {
    viewModelScope.launch {
      workout = repository.workout(workoutId).filterNotNull().first()
      steps = buildSteps(workout)
      _state.value =
        PlayerUiState(
          loading = false,
          workoutName = workout.name,
          totalMoves = steps.count { it is Step.Move },
        )
      if (steps.isEmpty()) finish() else goTo(0)
      runClock()
    }
  }

  private suspend fun runClock() {
    while (viewModelScope.isActive) {
      delay(1_000)
      val s = _state.value
      if (s.paused || s.finished != null) continue
      val remaining = s.remainingSec?.minus(1)
      _state.value = s.copy(elapsedSec = s.elapsedSec + 1, remainingSec = remaining)
      if (remaining != null && remaining <= 0) advance(countAsDone = true)
    }
  }

  fun togglePause() {
    _state.value = _state.value.let { it.copy(paused = !it.paused) }
  }

  /** Skip for timed steps (not counted as done), Done for rep-based steps. */
  fun next() {
    val step = _state.value.step
    advance(countAsDone = step is Step.Move && step.durationSec == null)
  }

  fun previous() {
    val current = _state.value.stepIndex
    val target = (current - 1 downTo 0).firstOrNull { steps[it] is Step.Move } ?: 0
    goTo(target)
  }

  /** Ends the workout now, keeping whatever was completed. */
  fun finishEarly() = finish()

  private fun advance(countAsDone: Boolean) {
    val s = _state.value
    if (s.finished != null) return
    if (countAsDone && s.step is Step.Move) completedMoves++
    if (s.stepIndex + 1 >= steps.size) finish() else goTo(s.stepIndex + 1)
  }

  private fun goTo(index: Int) {
    val step = steps[index]
    val upNext =
      when (step) {
        is Step.GetReady -> steps.drop(index + 1).filterIsInstance<Step.Move>().drop(1).firstOrNull()?.item
        is Step.Rest -> step.next
        is Step.Move -> steps.drop(index + 1).filterIsInstance<Step.Move>().firstOrNull()?.item
      }
    _state.value =
      _state.value.copy(
        stepIndex = index,
        step = step,
        remainingSec = step.durationSec,
        progress = index.toFloat() / steps.size,
        upNext = upNext,
      )
  }

  private fun finish() {
    val s = _state.value
    if (s.finished != null) return
    _state.value =
      s.copy(
        progress = 1f,
        finished =
          CompleteKey(
            workoutId = workout.id,
            workoutName = workout.name,
            startedAtMillis = startedAt,
            // Active time only: paused time isn't counted.
            durationSec = s.elapsedSec,
            exercisesCompleted = completedMoves,
          ),
      )
  }
}
