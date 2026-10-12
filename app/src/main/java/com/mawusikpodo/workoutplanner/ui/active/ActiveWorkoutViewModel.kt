package com.mawusikpodo.workoutplanner.ui.active

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawusikpodo.workoutplanner.CompleteKey
import com.mawusikpodo.workoutplanner.data.WorkoutRepository
import com.mawusikpodo.workoutplanner.model.Exercise
import com.mawusikpodo.workoutplanner.model.Step
import com.mawusikpodo.workoutplanner.model.Workout
import com.mawusikpodo.workoutplanner.model.WorkoutItem
import com.mawusikpodo.workoutplanner.model.buildSteps
import kotlinx.coroutines.Job
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
  val soundEnabled: Boolean = true,
)

class ActiveWorkoutViewModel(
  repository: WorkoutRepository,
  workoutId: String,
  private val now: () -> Long = System::currentTimeMillis,
  private val elapsedRealtime: () -> Long = {
    try {
      SystemClock.elapsedRealtime()
    } catch (_: Exception) {
      System.currentTimeMillis()
    }
  },
) : ViewModel() {
  private val _state = MutableStateFlow(PlayerUiState())
  val state: StateFlow<PlayerUiState> = _state.asStateFlow()

  private val startedAt = now()
  private lateinit var workout: Workout
  private var steps: List<Step> = emptyList()
  private var completedMoves = 0

  // Wall-clock precision timer tracking
  private var stepTotalSec: Int? = null
  private var stepElapsedMs: Long = 0L
  private var lastTickTime: Long = 0L
  private var totalWorkoutElapsedMs: Long = 0L

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
      startClock()
    }
  }

  private var clockJob: Job? = null

  private fun startClock() {
    clockJob?.cancel()
    clockJob = viewModelScope.launch { runClock() }
  }

  fun stopClock() {
    clockJob?.cancel()
    clockJob = null
  }

  public override fun onCleared() {
    super.onCleared()
    stopClock()
  }

  private suspend fun runClock() {
    lastTickTime = elapsedRealtime()
    while (viewModelScope.isActive) {
      delay(200)
      val s = _state.value
      if (s.paused || s.finished != null) {
        lastTickTime = elapsedRealtime()
        continue
      }
      val currentRealtime = elapsedRealtime()
      val deltaMs = (currentRealtime - lastTickTime).coerceAtLeast(0)
      lastTickTime = currentRealtime

      stepElapsedMs += deltaMs
      totalWorkoutElapsedMs += deltaMs
      val currentElapsedSec = (totalWorkoutElapsedMs / 1000).toInt()

      val totalSec = stepTotalSec
      if (totalSec != null) {
        val remaining = ((totalSec * 1000L - stepElapsedMs + 999) / 1000).toInt().coerceAtLeast(0)
        _state.value = s.copy(elapsedSec = currentElapsedSec, remainingSec = remaining)
        if (remaining <= 0) {
          advance(countAsDone = true)
        }
      } else {
        _state.value = s.copy(elapsedSec = currentElapsedSec)
      }
    }
  }

  fun togglePause() {
    val willBePaused = !_state.value.paused
    lastTickTime = elapsedRealtime()
    _state.value = _state.value.copy(paused = willBePaused)
  }

  fun toggleSound() {
    _state.value = _state.value.copy(soundEnabled = !_state.value.soundEnabled)
  }

  fun addRest(seconds: Int = 15) {
    val s = _state.value
    if (s.step is Step.Rest) {
      val newTotal = (stepTotalSec ?: 0) + seconds
      stepTotalSec = newTotal
      val remaining = ((newTotal * 1000L - stepElapsedMs + 999) / 1000).toInt().coerceAtLeast(0)
      _state.value = s.copy(remainingSec = remaining)
    }
  }

  fun skipRest() {
    if (_state.value.step is Step.Rest) {
      advance(countAsDone = false)
    }
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

  /** Swap the current active exercise on the fly (for injury or equipment adaptations). */
  fun swapCurrentExercise(newExercise: Exercise) {
    val currentStep = _state.value.step ?: return
    val currentExercise =
      when (currentStep) {
        is Step.Move -> currentStep.item.exercise
        is Step.GetReady -> currentStep.next.exercise
        is Step.Rest -> currentStep.next.exercise
      }
    val oldExerciseId = currentExercise.id

    // Update all steps across the remaining workout that match the old exercise
    steps =
      steps.map { step ->
        when (step) {
          is Step.Move -> {
            if (step.item.exercise.id == oldExerciseId) {
              val newItem = step.item.copy(exercise = newExercise)
              step.copy(item = newItem)
            } else step
          }
          is Step.Rest -> {
            if (step.next.exercise.id == oldExerciseId) {
              val nextItem = step.next.copy(exercise = newExercise)
              step.copy(next = nextItem)
            } else step
          }
          is Step.GetReady -> {
            if (step.next.exercise.id == oldExerciseId) {
              val nextItem = step.next.copy(exercise = newExercise)
              step.copy(next = nextItem)
            } else step
          }
        }
      }

    // Refresh current step with new exercise
    goTo(_state.value.stepIndex)
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
    stepTotalSec = step.durationSec
    stepElapsedMs = 0L
    lastTickTime = elapsedRealtime()

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
            durationSec = s.elapsedSec,
            exercisesCompleted = completedMoves,
          ),
      )
  }
}
