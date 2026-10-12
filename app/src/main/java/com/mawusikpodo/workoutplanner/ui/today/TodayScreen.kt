package com.mawusikpodo.workoutplanner.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawusikpodo.workoutplanner.LocalAppContainer
import com.mawusikpodo.workoutplanner.R
import com.mawusikpodo.workoutplanner.model.Workout
import com.mawusikpodo.workoutplanner.theme.Hairline
import com.mawusikpodo.workoutplanner.theme.Ink
import com.mawusikpodo.workoutplanner.theme.MintGlow
import com.mawusikpodo.workoutplanner.theme.Muted
import com.mawusikpodo.workoutplanner.theme.Outline
import com.mawusikpodo.workoutplanner.theme.StudioFrame
import com.mawusikpodo.workoutplanner.theme.Surface1
import com.mawusikpodo.workoutplanner.theme.Surface2
import com.mawusikpodo.workoutplanner.ui.components.ExerciseFigure
import com.mawusikpodo.workoutplanner.ui.components.Overline
import com.mawusikpodo.workoutplanner.ui.components.Pill
import com.mawusikpodo.workoutplanner.ui.components.PrimaryButton
import com.mawusikpodo.workoutplanner.ui.components.ScreenTitle

@Composable
fun TodayScreen(onOpenWorkout: (String) -> Unit, modifier: Modifier = Modifier) {
  val repository = LocalAppContainer.current.repository
  val viewModel: TodayViewModel = viewModel { TodayViewModel(repository) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  TodayContent(state, onOpenWorkout, modifier)
}

@Composable
private fun TodayContent(state: TodayUiState, onOpenWorkout: (String) -> Unit, modifier: Modifier = Modifier) {
  Column(
    modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 28.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(22.dp),
  ) {
    Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Overline(state.dateLabel)
        ScreenTitle("Ready to move?")
      }
      StreakPill(state.streak)
    }

    WeekStrip(state.week, Modifier.padding(horizontal = 20.dp))

    state.featured?.let { FeaturedCard(it, state.mode, onOpenWorkout, Modifier.padding(horizontal = 20.dp)) }

    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(Modifier.fillMaxWidth()) {
        Text("Weekly goal", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text("${state.weeklyDone} of ${state.weeklyGoal} workouts", style = MaterialTheme.typography.bodyMedium, color = Muted)
      }
      LinearProgressIndicator(
        progress = { (state.weeklyDone.toFloat() / state.weeklyGoal).coerceAtMost(1f) },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        trackColor = Surface2,
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
      )
    }

    if (state.otherWorkouts.isNotEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("More workouts", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 20.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          items(state.otherWorkouts, key = { it.id }) { workout -> WorkoutChip(workout) { onOpenWorkout(workout.id) } }
        }
      }
    }
  }
}

@Composable
private fun StreakPill(streak: Int) {
  Row(
    Modifier
      .clip(CircleShape)
      .background(Surface1)
      .border(1.dp, Hairline, CircleShape)
      .padding(horizontal = 14.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Icon(painterResource(R.drawable.ic_flame), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
    Text("$streak-day streak", style = MaterialTheme.typography.labelMedium)
  }
}

@Composable
private fun WeekStrip(week: List<WeekDay>, modifier: Modifier = Modifier) {
  Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    week.forEach { day ->
      val accent = MaterialTheme.colorScheme.primary
      val isToday = day.status == DayStatus.TODAY
      Column(
        Modifier.weight(1f)
          .clip(RoundedCornerShape(14.dp))
          .background(if (isToday) Surface2 else Surface1.copy(alpha = 0.5f))
          .border(1.dp, if (isToday) accent else Hairline, RoundedCornerShape(14.dp))
          .padding(vertical = 10.dp)
          .semantics { contentDescription = "${day.label}: ${if (day.status == DayStatus.DONE) "worked out" else "no workout"}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(day.label, style = MaterialTheme.typography.bodySmall, color = if (isToday) Ink else Muted)
        Box(
          Modifier.size(12.dp)
            .clip(CircleShape)
            .background(if (day.status == DayStatus.DONE) accent else Color.Transparent)
            .border(
              2.dp,
              when (day.status) {
                DayStatus.DONE -> accent
                DayStatus.TODAY -> Ink
                DayStatus.OTHER -> Hairline
              },
              CircleShape,
            )
        )
      }
    }
  }
}

@Composable
private fun FeaturedCard(workout: Workout, mode: TodayMode, onOpenWorkout: (String) -> Unit, modifier: Modifier = Modifier) {
  val label =
    when (mode) {
      TodayMode.PLANNED -> "Today's plan"
      TodayMode.SUGGESTED -> "Suggested for today"
      TodayMode.REST_DAY -> "Rest day · optional stretch"
      TodayMode.DONE -> "Done today"
    }
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = Surface1,
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, Hairline),
  ) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Overline(label, color = MaterialTheme.colorScheme.primary)
        Pill("${workout.estimatedMinutes} min")
      }
      val circuitExercise = workout.items.firstOrNull { it.section == com.mawusikpodo.workoutplanner.model.Section.CIRCUIT }?.exercise
      Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(18.dp)).background(StudioFrame)) {
        ExerciseFigure(
          motion = circuitExercise?.motion ?: com.mawusikpodo.workoutplanner.model.Motion.SQUAT,
          exerciseId = circuitExercise?.id,
          playing = mode != TodayMode.DONE,
          modifier = Modifier.fillMaxSize(),
        )
      }
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(workout.name.uppercase(), style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Pill("${workout.exerciseCount} moves")
          Pill(workout.equipment.label)
          Pill(workout.focus)
        }
      }
      PrimaryButton(
        text = if (mode == TodayMode.DONE) "Go again" else "Start workout · ${workout.estimatedMinutes} min",
        icon = R.drawable.ic_play,
        height = 54.dp,
        onClick = { onOpenWorkout(workout.id) },
      )
    }
  }
}

@Composable
private fun WorkoutChip(workout: Workout, onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    color = Surface1,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, Hairline),
    modifier = Modifier.width(160.dp).height(96.dp),
  ) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
      Text(workout.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("${workout.estimatedMinutes} min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        Text(workout.focus, style = MaterialTheme.typography.bodySmall, color = Muted)
      }
    }
  }
}
