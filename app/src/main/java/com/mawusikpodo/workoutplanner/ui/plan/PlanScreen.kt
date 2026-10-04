package com.mawusikpodo.workoutplanner.ui.plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawusikpodo.workoutplanner.LocalAppContainer
import com.mawusikpodo.workoutplanner.R
import com.mawusikpodo.workoutplanner.data.WorkoutRepository
import com.mawusikpodo.workoutplanner.model.Workout
import com.mawusikpodo.workoutplanner.model.startOfWeek
import com.mawusikpodo.workoutplanner.theme.Ink
import com.mawusikpodo.workoutplanner.theme.Muted
import com.mawusikpodo.workoutplanner.theme.Outline
import com.mawusikpodo.workoutplanner.theme.Surface1
import com.mawusikpodo.workoutplanner.theme.Surface2
import com.mawusikpodo.workoutplanner.ui.components.Overline
import com.mawusikpodo.workoutplanner.ui.components.Pill
import com.mawusikpodo.workoutplanner.ui.components.ScreenTitle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanDay(
  val date: LocalDate,
  val isToday: Boolean,
  /** Null when nothing is planned. */
  val planned: PlannedState?,
  val done: Boolean,
  /** Name of the workout completed that day, if any. */
  val doneWorkoutName: String? = null,
)

sealed interface PlannedState {
  data class Train(val workout: Workout) : PlannedState

  data object Rest : PlannedState
}

data class PlanUiState(
  val weekStart: LocalDate,
  val days: List<PlanDay> = emptyList(),
  val workouts: List<Workout> = emptyList(),
  val plannedCount: Int = 0,
  val plannedMinutes: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModel(private val repository: WorkoutRepository) : ViewModel() {
  private val today = repository.today()
  private val weekStart = MutableStateFlow(today.startOfWeek())

  val uiState: StateFlow<PlanUiState> =
    weekStart
      .flatMapLatest { start ->
        combine(repository.workouts, repository.sessions, repository.plan(start, start.plusDays(6))) { workouts, sessions, plan ->
          val byId = workouts.associateBy { it.id }
          val sessionDays = sessions.map { it.epochDay }.toSet()
          val days =
            (0L until 7L).map { offset ->
              val date = start.plusDays(offset)
              val entry = plan.firstOrNull { it.epochDay == date.toEpochDay() }
              PlanDay(
                date = date,
                isToday = date == today,
                planned =
                  when {
                    entry == null -> null
                    entry.workoutId == null -> PlannedState.Rest
                    else -> byId[entry.workoutId]?.let { PlannedState.Train(it) }
                  },
                done = date.toEpochDay() in sessionDays,
                doneWorkoutName = sessions.firstOrNull { it.epochDay == date.toEpochDay() }?.workoutName,
              )
            }
          val trained = days.mapNotNull { (it.planned as? PlannedState.Train)?.workout }
          PlanUiState(start, days, workouts, trained.size, trained.sumOf { it.estimatedMinutes })
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState(today.startOfWeek()))

  fun previousWeek() {
    weekStart.value = weekStart.value.minusWeeks(1)
  }

  fun nextWeek() {
    weekStart.value = weekStart.value.plusWeeks(1)
  }

  fun assign(date: LocalDate, workoutId: String?) = viewModelScope.launch { repository.setPlan(date, workoutId) }

  fun clear(date: LocalDate) = viewModelScope.launch { repository.clearPlan(date) }

  fun autoBuild() = viewModelScope.launch { repository.autoBuildWeek(weekStart.value, uiState.value.workouts) }
}

@Composable
fun PlanScreen(onOpenWorkout: (String) -> Unit, modifier: Modifier = Modifier) {
  val repository = LocalAppContainer.current.repository
  val viewModel: PlanViewModel = viewModel { PlanViewModel(repository) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  var editing by remember { mutableStateOf<PlanDay?>(null) }

  PlanContent(
    state = state,
    onPreviousWeek = viewModel::previousWeek,
    onNextWeek = viewModel::nextWeek,
    onDayClick = { editing = it },
    onAutoBuild = { viewModel.autoBuild() },
    modifier = modifier,
  )

  editing?.let { day ->
    DayPickerSheet(
      day = day,
      workouts = state.workouts,
      onPick = { workoutId -> viewModel.assign(day.date, workoutId); editing = null },
      onClear = { viewModel.clear(day.date); editing = null },
      onOpen = { id -> editing = null; onOpenWorkout(id) },
      onDismiss = { editing = null },
    )
  }
}

@Composable
private fun PlanContent(
  state: PlanUiState,
  onPreviousWeek: () -> Unit,
  onNextWeek: () -> Unit,
  onDayClick: (PlanDay) -> Unit,
  onAutoBuild: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val rangeFormat = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
  LazyColumn(
    modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item {
      Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Overline("${state.weekStart.format(rangeFormat)} – ${state.weekStart.plusDays(6).format(rangeFormat)}")
          ScreenTitle("Your week")
        }
        val navColors = IconButtonDefaults.filledIconButtonColors(containerColor = Surface1, contentColor = Ink)
        FilledIconButton(onClick = onPreviousWeek, colors = navColors) {
          Icon(painterResource(R.drawable.ic_back), contentDescription = "Previous week", modifier = Modifier.size(20.dp))
        }
        FilledIconButton(onClick = onNextWeek, colors = navColors) {
          Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "Next week", modifier = Modifier.size(20.dp))
        }
      }
    }
    item {
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        Pill("Goal: ${WorkoutRepository.DEFAULT_WEEKLY_GOAL} workouts")
        Pill("${state.plannedCount} planned")
        Pill("~${state.plannedMinutes} min")
      }
    }
    items(state.days, key = { it.date.toEpochDay() }) { day -> DayRow(day, onClick = { onDayClick(day) }) }
    item {
      OutlinedButton(
        onClick = onAutoBuild,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 52.dp),
        shape = CircleShape,
        border = BorderStroke(1.5.dp, Outline),
      ) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
        Box(Modifier.width(8.dp))
        Text("Auto-build this week", color = Ink, style = MaterialTheme.typography.titleMedium)
      }
    }
  }
}

@Composable
private fun DayRow(day: PlanDay, onClick: () -> Unit) {
  val accent = MaterialTheme.colorScheme.primary
  val isRest = day.planned == PlannedState.Rest
  val (title, meta) =
    when (val planned = day.planned) {
      is PlannedState.Train -> planned.workout.name to "${planned.workout.estimatedMinutes} min · ${planned.workout.equipment.label}"
      PlannedState.Rest ->
        if (day.doneWorkoutName != null) day.doneWorkoutName to "Done on a rest day" else "Rest day" to "Recover, or go for a walk"
      null -> if (day.doneWorkoutName != null) day.doneWorkoutName to "Completed" else "Nothing planned" to "Tap to add a workout"
    }
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = if (isRest || day.planned == null) Color.Transparent else Surface1,
    border =
      when {
        day.isToday -> BorderStroke(1.5.dp, accent)
        isRest || day.planned == null -> BorderStroke(1.5.dp, Surface2)
        else -> null
      },
  ) {
    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Overline(day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
        Text("${day.date.dayOfMonth}", style = MaterialTheme.typography.headlineSmall)
      }
      Column(Modifier.weight(1f).padding(start = 14.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = if (day.planned is PlannedState.Train || day.doneWorkoutName != null) Ink else Muted)
        Text(meta, style = MaterialTheme.typography.bodySmall, color = Muted)
      }
      if (day.done) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
          Icon(painterResource(R.drawable.ic_check), contentDescription = "Completed", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPickerSheet(
  day: PlanDay,
  workouts: List<Workout>,
  onPick: (String?) -> Unit,
  onClear: () -> Unit,
  onOpen: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Surface1) {
    LazyColumn(
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.navigationBarsPadding(),
    ) {
      item {
        Text(
          day.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())),
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(bottom = 8.dp),
        )
      }
      (day.planned as? PlannedState.Train)?.let { planned ->
        item {
          OutlinedButton(onClick = { onOpen(planned.workout.id) }, modifier = Modifier.fillMaxWidth(), shape = CircleShape, border = BorderStroke(1.5.dp, Outline)) {
            Text("Open ${planned.workout.name}", color = Ink)
          }
        }
      }
      items(workouts, key = { it.id }) { workout ->
        val selected = (day.planned as? PlannedState.Train)?.workout?.id == workout.id
        Surface(
          onClick = { onPick(workout.id) },
          shape = RoundedCornerShape(14.dp),
          color = Surface2,
          border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        ) {
          Row(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(workout.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${workout.estimatedMinutes} min · ${workout.equipment.label}", style = MaterialTheme.typography.bodySmall, color = Muted)
          }
        }
      }
      item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
          OutlinedButton(onClick = { onPick(null) }, modifier = Modifier.weight(1f), shape = CircleShape, border = BorderStroke(1.5.dp, Outline)) {
            Text("Rest day", color = Ink)
          }
          OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f), shape = CircleShape, border = BorderStroke(1.5.dp, Outline)) {
            Text("Clear", color = Ink)
          }
        }
      }
    }
  }
}
