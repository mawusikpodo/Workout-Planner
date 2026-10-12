package com.mawusikpodo.workoutplanner.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
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
import com.mawusikpodo.workoutplanner.model.Exercise
import com.mawusikpodo.workoutplanner.model.Section
import com.mawusikpodo.workoutplanner.model.Workout
import com.mawusikpodo.workoutplanner.theme.Hairline
import com.mawusikpodo.workoutplanner.theme.HyperCoral
import com.mawusikpodo.workoutplanner.theme.Ink
import com.mawusikpodo.workoutplanner.theme.Muted
import com.mawusikpodo.workoutplanner.theme.Surface1
import com.mawusikpodo.workoutplanner.ui.components.ExerciseSheet
import com.mawusikpodo.workoutplanner.ui.components.ExerciseThumbnail
import com.mawusikpodo.workoutplanner.ui.components.Overline
import com.mawusikpodo.workoutplanner.ui.components.Pill
import com.mawusikpodo.workoutplanner.ui.components.PrimaryButton
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutDetailViewModel(repository: WorkoutRepository, workoutId: String) : ViewModel() {
  val workout: StateFlow<Workout?> =
    repository.workout(workoutId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun WorkoutDetailScreen(
  workoutId: String,
  onBack: () -> Unit,
  onStart: (String) -> Unit,
  onEdit: (String) -> Unit,
  onDuplicate: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val repository = LocalAppContainer.current.repository
  val viewModel: WorkoutDetailViewModel = viewModel { WorkoutDetailViewModel(repository, workoutId) }
  val workout by viewModel.workout.collectAsStateWithLifecycle()
  workout?.let {
    WorkoutDetailContent(
      workout = it,
      onBack = onBack,
      onStart = onStart,
      onEdit = onEdit,
      onDuplicate = onDuplicate,
      modifier = modifier,
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutDetailContent(
  workout: Workout,
  onBack: () -> Unit,
  onStart: (String) -> Unit,
  onEdit: (String) -> Unit,
  onDuplicate: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val repository = LocalAppContainer.current.repository
  val bookmarks by repository.bookmarks.collectAsStateWithLifecycle(initialValue = emptySet())
  val scope = rememberCoroutineScope()
  var detailExercise by rememberSaveable { mutableStateOf<Exercise?>(null) }
  var confirmDelete by rememberSaveable { mutableStateOf(false) }
  // Only one thumbnail animates at a time; the first circuit exercise plays by default.
  var playingIndex by rememberSaveable { mutableIntStateOf(workout.items.indexOfFirst { it.section == Section.CIRCUIT }) }

  if (confirmDelete) {
    AlertDialog(
      onDismissRequest = { confirmDelete = false },
      title = { Text("Delete Routine?") },
      text = { Text("Are you sure you want to delete '${workout.name}'? This action cannot be undone.") },
      confirmButton = {
        TextButton(
          onClick = {
            confirmDelete = false
            scope.launch {
              repository.deleteCustomWorkout(workout.id)
              onBack()
            }
          },
        ) {
          Text("Delete", color = HyperCoral)
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
      },
      containerColor = Surface1,
    )
  }

  Column(modifier.fillMaxSize()) {
    // Top Bar
    Row(
      Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_back), contentDescription = "Back") }
        Overline(if (workout.isCustom) "Custom Routine" else "Workout Overview", color = MaterialTheme.colorScheme.primary)
      }

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (workout.isCustom) {
          IconButton(onClick = { onEdit(workout.id) }) {
            Icon(painterResource(R.drawable.ic_edit), contentDescription = "Edit workout", tint = Ink, modifier = Modifier.size(20.dp))
          }
          IconButton(onClick = { confirmDelete = true }) {
            Icon(painterResource(R.drawable.ic_delete), contentDescription = "Delete workout", tint = HyperCoral, modifier = Modifier.size(20.dp))
          }
        } else {
          OutlinedButton(
            onClick = { onDuplicate(workout.id) },
            border = BorderStroke(1.dp, Hairline),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
          ) {
            Text("Customize", style = MaterialTheme.typography.labelSmall, color = Ink)
          }
        }
      }
    }

    LazyColumn(
      Modifier.weight(1f),
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      item {
        Surface(
          modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
          color = Surface1,
          shape = RoundedCornerShape(20.dp),
          border = BorderStroke(1.dp, Hairline),
        ) {
          Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(workout.name.uppercase(), style = MaterialTheme.typography.displaySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Pill("${workout.estimatedMinutes} min")
              Pill(workout.level)
              Pill(workout.equipment.label)
              Pill(workout.focus)
              Pill("~${workout.estimatedMinutes * 8} kcal")
              if (workout.isCustom) Pill("Custom", color = MaterialTheme.colorScheme.primary)
            }
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
            Surface(
              onClick = { detailExercise = item.exercise },
              color = Surface1,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, Hairline),
            ) {
              Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExerciseThumbnail(
                  exercise = item.exercise,
                  playing = playingIndex == index,
                  onToggle = { playingIndex = if (playingIndex == index) -1 else index },
                )
                Column(Modifier.weight(1f)) {
                  Text(item.exercise.name, style = MaterialTheme.typography.titleMedium)
                  Text(item.exercise.muscles, style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Pill(item.doseLabel)
              }
            }
          }
        }
      }
    }
    HorizontalDivider(color = Hairline)
    Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp)) {
      PrimaryButton("Start workout · ${workout.estimatedMinutes} min", onClick = { onStart(workout.id) }, icon = R.drawable.ic_play, height = 54.dp)
    }
  }

  detailExercise?.let { exercise ->
    ExerciseSheet(
      exercise = exercise,
      isBookmarked = exercise.id in bookmarks,
      onToggleBookmark = { repository.toggleBookmark(exercise.id) },
      onDismiss = { detailExercise = null },
    )
  }
}
