package com.mawusikpodo.workoutplanner.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawusikpodo.workoutplanner.LocalAppContainer
import com.mawusikpodo.workoutplanner.data.WorkoutRepository
import com.mawusikpodo.workoutplanner.model.Session
import com.mawusikpodo.workoutplanner.model.bestStreak
import com.mawusikpodo.workoutplanner.model.currentStreak
import com.mawusikpodo.workoutplanner.model.weeklyMinutes
import androidx.compose.foundation.BorderStroke
import com.mawusikpodo.workoutplanner.model.Feeling
import com.mawusikpodo.workoutplanner.theme.Hairline
import com.mawusikpodo.workoutplanner.theme.Muted
import com.mawusikpodo.workoutplanner.theme.Outline
import com.mawusikpodo.workoutplanner.theme.PastBar
import com.mawusikpodo.workoutplanner.theme.Surface1
import com.mawusikpodo.workoutplanner.ui.components.EmptyState
import com.mawusikpodo.workoutplanner.ui.components.ScreenTitle
import com.mawusikpodo.workoutplanner.ui.components.StatTile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProgressUiState(
  val workouts: Int = 0,
  val minutes: Int = 0,
  val streak: Int = 0,
  val bestStreak: Int = 0,
  val weekly: List<Int> = List(8) { 0 },
  val recent: List<Session> = emptyList(),
)

class ProgressViewModel(repository: WorkoutRepository) : ViewModel() {
  private val today = repository.today()

  val uiState: StateFlow<ProgressUiState> =
    repository.sessions
      .map { sessions ->
        val days = sessions.map { it.epochDay }.toSet()
        ProgressUiState(
          workouts = sessions.size,
          minutes = (sessions.sumOf { it.durationSec } + 30) / 60,
          streak = currentStreak(days, today),
          bestStreak = bestStreak(days),
          weekly = weeklyMinutes(sessions, today),
          recent = sessions.take(10),
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())
}

@Composable
fun ProgressScreen(modifier: Modifier = Modifier) {
  val repository = LocalAppContainer.current.repository
  val viewModel: ProgressViewModel = viewModel { ProgressViewModel(repository) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  LazyColumn(
    modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item { ScreenTitle("Progress", Modifier.padding(bottom = 4.dp)) }
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          StatTile("Workouts", "${state.workouts}", Modifier.weight(1f))
          StatTile("Active minutes", "${state.minutes}", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          StatTile("Current streak", days(state.streak), Modifier.weight(1f))
          StatTile("Best streak", days(state.bestStreak), Modifier.weight(1f))
        }
      }
    }
    item { WeeklyChart(state.weekly) }
    item { Text("Recent", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp)) }
    if (state.recent.isEmpty()) {
      item { EmptyState("Finish a workout and it will show up here.") }
    }
    items(state.recent, key = { it.id }) { session -> SessionRow(session) }
  }
}

private fun days(n: Int) = if (n == 1) "1 day" else "$n days"

/** Single-series bar chart: past weeks in neutral grey, the current week in the accent colour. */
@Composable
private fun WeeklyChart(minutes: List<Int>) {
  val max = (minutes.maxOrNull() ?: 0).coerceAtLeast(30)
  val peak = minutes.indexOf(minutes.max())
  Surface(
    color = Surface1,
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, Hairline),
  ) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Row(verticalAlignment = Alignment.Bottom) {
        Text("Active minutes per week", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text("Last 8 weeks", style = MaterialTheme.typography.bodySmall, color = Muted)
      }
      Row(
        Modifier.fillMaxWidth().height(130.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
      ) {
        minutes.forEachIndexed { i, value ->
          val isCurrent = i == minutes.lastIndex
          Column(
            Modifier.weight(1f).fillMaxHeight().semantics {
              contentDescription = "${if (isCurrent) "This week" else "${minutes.size - 1 - i} weeks ago"}: $value minutes"
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
          ) {
            // Label only the current week and the peak, to keep the chart quiet.
            if ((isCurrent || i == peak) && value > 0) Text("$value", style = MaterialTheme.typography.bodySmall, color = if (isCurrent) MaterialTheme.colorScheme.primary else Muted)
            Box(
              Modifier.fillMaxWidth()
                .widthIn(max = 24.dp)
                .height((100 * value / max).coerceAtLeast(if (value > 0) 4 else 0).dp)
                .background(if (isCurrent) MaterialTheme.colorScheme.primary else PastBar, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            )
          }
        }
      }
      HorizontalDivider(color = Hairline)
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        minutes.indices.forEach { i ->
          Text(
            if (i == minutes.lastIndex) "Now" else "-${minutes.lastIndex - i}w",
            style = MaterialTheme.typography.bodySmall,
            color = if (i == minutes.lastIndex) MaterialTheme.colorScheme.primary else Muted,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
          )
        }
      }
    }
  }
}

@Composable
private fun SessionRow(session: Session) {
  val date = Instant.ofEpochMilli(session.startedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
  val dateLabel =
    when (date) {
      LocalDate.now() -> "Today"
      LocalDate.now().minusDays(1) -> "Yesterday"
      else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
    }
  val feelingEmoji =
    when (session.feeling) {
      Feeling.EASY -> "😌 "
      Feeling.JUST_RIGHT -> "💪 "
      Feeling.TOUGH -> "🔥 "
      null -> ""
    }
  Surface(
    color = Surface1,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, Hairline),
  ) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(session.workoutName, style = MaterialTheme.typography.titleMedium)
        Text(
          listOfNotNull(dateLabel, session.feeling?.let { "$feelingEmoji${it.label}" }).joinToString(" · "),
          style = MaterialTheme.typography.bodySmall,
          color = Muted,
        )
      }
      Text("${(session.durationSec + 30) / 60} min", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
  }
}
