package com.mawusikpodo.workoutplanner.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawusikpodo.workoutplanner.data.WorkoutRepository
import com.mawusikpodo.workoutplanner.model.Workout
import com.mawusikpodo.workoutplanner.model.currentStreak
import com.mawusikpodo.workoutplanner.model.startOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class DayStatus { DONE, TODAY, OTHER }

data class WeekDay(val label: String, val status: DayStatus)

enum class TodayMode { PLANNED, SUGGESTED, REST_DAY, DONE }

data class TodayUiState(
  val loading: Boolean = true,
  val dateLabel: String = "",
  val streak: Int = 0,
  val week: List<WeekDay> = emptyList(),
  val featured: Workout? = null,
  val mode: TodayMode = TodayMode.SUGGESTED,
  val weeklyDone: Int = 0,
  val weeklyGoal: Int = WorkoutRepository.DEFAULT_WEEKLY_GOAL,
  val otherWorkouts: List<Workout> = emptyList(),
)

class TodayViewModel(repository: WorkoutRepository) : ViewModel() {
  private val today = repository.today()
  private val weekStart = today.startOfWeek()

  val uiState: StateFlow<TodayUiState> =
    combine(repository.workouts, repository.sessions, repository.plan(weekStart, weekStart.plusDays(6))) {
        workouts,
        sessions,
        plan ->
        val sessionDays = sessions.map { it.epochDay }.toSet()
        val todayDay = today.toEpochDay()
        val doneToday = todayDay in sessionDays
        val todayEntry = plan.firstOrNull { it.epochDay == todayDay }
        val plannedWorkout = todayEntry?.workoutId?.let { id -> workouts.firstOrNull { it.id == id } }
        val doneThisWeek = sessions.filter { it.epochDay >= weekStart.toEpochDay() }

        val mode =
          when {
            doneToday -> TodayMode.DONE
            plannedWorkout != null -> TodayMode.PLANNED
            todayEntry != null -> TodayMode.REST_DAY
            else -> TodayMode.SUGGESTED
          }
        val featured =
          when {
            doneToday -> workouts.firstOrNull { w -> w.id == sessions.first { it.epochDay == todayDay }.workoutId }
            plannedWorkout != null -> plannedWorkout
            todayEntry != null -> workouts.firstOrNull { it.focus == "Stretch" }
            else -> workouts.firstOrNull { w -> doneThisWeek.none { it.workoutId == w.id } } ?: workouts.firstOrNull()
          } ?: plannedWorkout

        TodayUiState(
          loading = workouts.isEmpty(),
          dateLabel =
            today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
              " · " +
              today.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())),
          streak = currentStreak(sessionDays, today),
          week =
            (0L until 7L).map { offset ->
              val day = weekStart.plusDays(offset)
              WeekDay(
                label = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                status =
                  when {
                    day.toEpochDay() in sessionDays -> DayStatus.DONE
                    day == today -> DayStatus.TODAY
                    else -> DayStatus.OTHER
                  },
              )
            },
          featured = featured,
          mode = mode,
          weeklyDone = doneThisWeek.map { it.epochDay }.distinct().size,
          otherWorkouts = workouts.filter { it.id != featured?.id }.sortedBy { it.estimatedMinutes },
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())
}
