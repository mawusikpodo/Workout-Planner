package com.mawusikpodo.workoutplanner.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mawusikpodo.workoutplanner.LocalAppContainer
import com.mawusikpodo.workoutplanner.R
import com.mawusikpodo.workoutplanner.model.Equipment
import com.mawusikpodo.workoutplanner.model.Exercise
import com.mawusikpodo.workoutplanner.model.MuscleGroup
import com.mawusikpodo.workoutplanner.theme.Ground
import com.mawusikpodo.workoutplanner.theme.Ink
import com.mawusikpodo.workoutplanner.theme.Muted
import com.mawusikpodo.workoutplanner.theme.Surface1
import com.mawusikpodo.workoutplanner.theme.Surface2
import com.mawusikpodo.workoutplanner.ui.components.DifficultyBars
import com.mawusikpodo.workoutplanner.ui.components.EmptyState
import com.mawusikpodo.workoutplanner.ui.components.ExerciseFigure
import com.mawusikpodo.workoutplanner.ui.components.ExerciseThumbnail
import com.mawusikpodo.workoutplanner.ui.components.Overline
import com.mawusikpodo.workoutplanner.ui.components.ScreenTitle

/** Filtering is simple enough to live in the screen; the list comes straight from the repository. */
@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
  val repository = LocalAppContainer.current.repository
  val exercises by repository.exercises.collectAsStateWithLifecycle(initialValue = emptyList())
  var query by rememberSaveable { mutableStateOf("") }
  var group by rememberSaveable { mutableStateOf<MuscleGroup?>(null) }
  var equipment by rememberSaveable { mutableStateOf(Equipment.BODYWEIGHT) }
  var playingId by rememberSaveable { mutableStateOf<String?>(null) }
  var detailId by rememberSaveable { mutableStateOf<String?>(null) }

  val filtered =
    exercises.filter {
      it.equipment == equipment &&
        (group == null || it.group == group) &&
        (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) || it.muscles.contains(query.trim(), ignoreCase = true))
    }

  LazyColumn(
    modifier.fillMaxSize(),
    contentPadding = PaddingValues(top = 28.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item { ScreenTitle("Library", Modifier.padding(horizontal = 20.dp)) }
    item {
      TextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("Search ${exercises.size} exercises") },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null, modifier = Modifier.size(20.dp)) },
        singleLine = true,
        shape = CircleShape,
        colors =
          TextFieldDefaults.colors(
            focusedContainerColor = Surface1,
            unfocusedContainerColor = Surface1,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
          ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
      )
    }
    item {
      LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { GroupChip("All", group == null) { group = null } }
        items(MuscleGroup.entries) { g -> GroupChip(g.label, group == g) { group = if (group == g) null else g } }
      }
    }
    item {
      SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Equipment.entries.forEachIndexed { i, e ->
          SegmentedButton(
            selected = equipment == e,
            onClick = { equipment = e },
            shape = SegmentedButtonDefaults.itemShape(i, Equipment.entries.size),
            colors =
              SegmentedButtonDefaults.colors(
                activeContainerColor = Surface2,
                activeContentColor = Ink,
                inactiveContainerColor = Surface1,
                inactiveContentColor = Muted,
              ),
            icon = {},
          ) {
            Text(e.label, style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }
    if (filtered.isEmpty() && exercises.isNotEmpty()) {
      item { EmptyState("No exercises match these filters.", Modifier.padding(horizontal = 20.dp)) }
    }
    items(filtered, key = { it.id }) { exercise ->
      Surface(
        onClick = { detailId = exercise.id },
        color = Surface1,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(horizontal = 20.dp),
      ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          ExerciseThumbnail(
            exercise = exercise,
            playing = playingId == exercise.id,
            onToggle = { playingId = if (playingId == exercise.id) null else exercise.id },
            size = 56.dp,
          )
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
            Text(exercise.muscles, style = MaterialTheme.typography.bodySmall, color = Muted)
          }
          DifficultyBars(exercise.difficulty, Modifier.padding(end = 8.dp))
        }
      }
    }
  }

  exercises.firstOrNull { it.id == detailId }?.let { ExerciseSheet(it, onDismiss = { detailId = null }) }
}

@Composable
private fun GroupChip(label: String, selected: Boolean, onClick: () -> Unit) {
  FilterChip(
    selected = selected,
    onClick = onClick,
    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
    shape = CircleShape,
    border = null,
    colors =
      FilterChipDefaults.filterChipColors(
        containerColor = Surface1,
        labelColor = Ink,
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = Ground,
      ),
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseSheet(exercise: Exercise, onDismiss: () -> Unit) {
  ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Surface1) {
    Column(
      Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(Modifier.fillMaxWidth().height(220.dp)) {
        Surface(color = Surface2, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxSize()) {
          ExerciseFigure(exercise.motion, playing = true, contentDescription = "${exercise.name} demonstration", modifier = Modifier.padding(12.dp))
        }
      }
      Overline(exercise.muscles, color = MaterialTheme.colorScheme.primary)
      Text(exercise.name.uppercase(), style = MaterialTheme.typography.displaySmall)
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DifficultyBars(exercise.difficulty)
        Text(listOf("Beginner", "Intermediate", "Advanced")[exercise.difficulty - 1] + " · " + exercise.equipment.label, style = MaterialTheme.typography.bodyMedium, color = Muted)
      }
      Text("Form tip: ${exercise.formCue}", style = MaterialTheme.typography.bodyLarge)
    }
  }
}
