package com.mawusikpodo.workoutplanner

import androidx.annotation.DrawableRes
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Tabs in the bottom navigation bar; each keeps its own back stack. */
@Serializable
sealed interface TopLevelKey : NavKey {
  val label: String
  @get:DrawableRes val icon: Int
}

@Serializable
data object TodayKey : TopLevelKey {
  override val label = "Today"
  override val icon = R.drawable.ic_home
}

@Serializable
data object PlanKey : TopLevelKey {
  override val label = "Plan"
  override val icon = R.drawable.ic_calendar
}

@Serializable
data object LibraryKey : TopLevelKey {
  override val label = "Library"
  override val icon = R.drawable.ic_grid
}

@Serializable
data object ProgressKey : TopLevelKey {
  override val label = "Progress"
  override val icon = R.drawable.ic_chart
}

val TopLevelKeys: List<TopLevelKey> = listOf(TodayKey, PlanKey, LibraryKey, ProgressKey)

@Serializable data class WorkoutDetailKey(val workoutId: String) : NavKey

/** Full-screen player; hides the bottom bar. */
@Serializable data class ActiveWorkoutKey(val workoutId: String) : NavKey

@Serializable
data class CompleteKey(
  val workoutId: String,
  val workoutName: String,
  val startedAtMillis: Long,
  val durationSec: Int,
  val exercisesCompleted: Int,
) : NavKey
