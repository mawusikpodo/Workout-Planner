package com.mawusikpodo.workoutplanner.ui.active

import com.mawusikpodo.workoutplanner.data.WorkoutRepository
import com.mawusikpodo.workoutplanner.model.Equipment
import com.mawusikpodo.workoutplanner.model.Exercise
import com.mawusikpodo.workoutplanner.model.Motion
import com.mawusikpodo.workoutplanner.model.MuscleGroup
import com.mawusikpodo.workoutplanner.model.Section
import com.mawusikpodo.workoutplanner.model.Step
import com.mawusikpodo.workoutplanner.model.Workout
import com.mawusikpodo.workoutplanner.model.WorkoutItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModelTest {
  private val testDispatcher = StandardTestDispatcher()

  private fun exercise(id: String, name: String = id) =
    Exercise(id, name, "Legs", MuscleGroup.LEGS, Equipment.BODYWEIGHT, 1, Motion.SQUAT, "Form cue")

  private val testWorkout =
    Workout(
      id = "w1",
      name = "Test Routine",
      level = "Beginner",
      equipment = Equipment.BODYWEIGHT,
      focus = "Full body",
      rounds = 2,
      restSec = 15,
      items =
        listOf(
          WorkoutItem(Section.WARMUP, exercise("warm", "Jumping jacks"), 30, null, false),
          WorkoutItem(Section.CIRCUIT, exercise("move_a", "Push-ups"), 40, null, false),
          WorkoutItem(Section.CIRCUIT, exercise("move_b", "Squats"), null, 12, false),
          WorkoutItem(Section.COOLDOWN, exercise("cool", "Stretch"), 30, null, false),
        ),
      isCustom = true,
    )

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun swapCurrentExercise_updatesCurrentAndSubsequentRounds() = runTest {
    var currentTime = 10_000L
    val fakeRepo = object : WorkoutRepository() {
      override fun workout(id: String): Flow<Workout?> = flowOf(testWorkout)
    }

    val viewModel =
      ActiveWorkoutViewModel(
        repository = fakeRepo,
        workoutId = "w1",
        now = { 10_000L },
        elapsedRealtime = { currentTime },
      )

    runCurrent()

    // Step 0 is GetReady
    assertEquals(0, viewModel.state.value.stepIndex)
    assertTrue(viewModel.state.value.step is Step.GetReady)

    // Advance to step 1 (Warmup: warm)
    viewModel.next()
    runCurrent()
    assertEquals("warm", (viewModel.state.value.step as Step.Move).item.exercise.id)

    // Advance to step 2 (Circuit round 1: move_a "Push-ups")
    viewModel.next()
    runCurrent()
    assertEquals("move_a", (viewModel.state.value.step as Step.Move).item.exercise.id)

    // Swap move_a to knee push-ups
    val kneePushUps = exercise("knee_push_ups", "Knee push-ups")
    viewModel.swapCurrentExercise(kneePushUps)
    runCurrent()

    // Current step should now be knee push-ups
    val currentStep = viewModel.state.value.step as Step.Move
    assertEquals("knee_push_ups", currentStep.item.exercise.id)
    assertEquals("Knee push-ups", currentStep.item.exercise.name)
    viewModel.stopClock()
  }

  @Test
  fun togglePause_pausesAndResumesTimer() = runTest {
    var currentTime = 10_000L
    val fakeRepo = object : WorkoutRepository() {
      override fun workout(id: String): Flow<Workout?> = flowOf(testWorkout)
    }

    val viewModel =
      ActiveWorkoutViewModel(
        repository = fakeRepo,
        workoutId = "w1",
        now = { 10_000L },
        elapsedRealtime = { currentTime },
      )

    runCurrent()

    assertEquals(false, viewModel.state.value.paused)
    viewModel.togglePause()
    assertEquals(true, viewModel.state.value.paused)
    viewModel.togglePause()
    assertEquals(false, viewModel.state.value.paused)
    viewModel.stopClock()
  }
}
