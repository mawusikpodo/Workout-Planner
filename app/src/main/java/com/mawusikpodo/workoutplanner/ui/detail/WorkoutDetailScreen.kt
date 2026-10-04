package com.mawusikpodo.workoutplanner.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawusikpodo.workoutplanner.LocalAppContainer
import com.mawusikpodo.workoutplanner.R
import com.mawusikpodo.workoutplanner.data.WorkoutRepository
import com.mawusikpodo.workoutplanner.model.Section
import com.mawusikpodo.workoutplanner.model.Workout
import com.mawusikpodo.workoutplanner.theme.Muted
import com.mawusikpodo.workoutplanner.theme.Surface1
import com.mawusikpodo.workoutplanner.ui.components.ExerciseThumbnail
import com.mawusikpodo.workoutplanner.ui.components.Overline
import com.mawusikpodo.workoutplanner.ui.components.Pill
import com.mawusikpodo.workoutplanner.ui.components.PrimaryButton
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class WorkoutDetailViewModel(repository: WorkoutRepository, workoutId: String) : ViewModel() {
  val workout: StateFlow<Workout?> =
    repository.workout(workoutId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun WorkoutDetailScreen(
  workoutId: String,
  onBack: () -> Unit,
  onStart: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val repository = LocalAppContainer.current.repository
  val viewModel: WorkoutDetailViewModel = viewModel { WorkoutDetailViewModel(repository, workoutId) }
  val workout by viewModel.workout.collectAsStateWithLifecycle()
  workout?.let { WorkoutDetailContent(it, onBack, onStart, modifier) }
}

@Composable
private fun WorkoutDetailContent(workout: Workout, onBack: () -> Unit, onStart: (String) -> Unit, modifier: Modifier = Modifier) {
  // Only one thumbnail animates at a time; the first circuit exercise plays by default.
  var playingIndex by rememberSaveable { mutableStateOf(workout.items.indexOfFirst { it.section == Section.CIRCUIT }) }

  Column(modifier.fillMaxSize()) {
    Row(Modifier.padding(start = 4.dp, top = 8.dp)) {
      IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_back), contentDescription = "Back") }
    }
    LazyColumn(
      Modifier.weight(1f),
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
          Text(workout.name.uppercase(), style = MaterialTheme.typography.displaySmall)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("${workout.estimatedMinutes} min")
            Pill(workout.level)
            Pill(workout.equipment.label)
            Pill(workout.focus)
          }
        }
      }
      Section.entries.forEach { section ->
        val sectionItems = workout.items.withIndex().filter { it.value.section == section }
        if (sectionItems.isEmpty()) return@forEach
        item(key = section.name) {
          val meta =
            when (section) {
              Section.CIRCUIT ->
                buildString {
                  append("${workout.rounds} round${if (workout.rounds > 1) "s" else ""}")
                  if (workout.restSec > 0) append(" · ${workout.restSec} s rest")
                }
              else -> "${(sectionItems.sumOf { it.value.estimatedSec } + 59) / 60} min"
            }
          Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Overline(section.label, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            Overline(meta)
          }
        }
        sectionItems.forEach { (index, item) ->
          item(key = "item-$index") {
            Surface(color = Surface1, shape = RoundedCornerShape(14.dp)) {
              Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExerciseThumbnail(
                  exercise = item.exercise,
                  playing = playingIndex == index,
                  onToggle = { playingIndex = if (playingIndex == index) -1 else index },
                )
                Column(Modifier.weight(1f)) {
                  Text(item.exercise.name, style = MaterialTheme.typography.titleMedium)
                  Text(item.exercise.muscles, style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Text(item.doseLabel, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(end = 6.dp))
              }
            }
          }
        }
      }
    }
    HorizontalDivider(color = Surface1)
    Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)) {
      PrimaryButton("Start · ${workout.estimatedMinutes} min", onClick = { onStart(workout.id) }, icon = R.drawable.ic_play)
    }
  }
}
