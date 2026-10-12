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
import androidx.compose.material3.IconButton
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import com.mawusikpodo.workoutplanner.theme.Ground
import com.mawusikpodo.workoutplanner.theme.Hairline
import com.mawusikpodo.workoutplanner.theme.Ink
import com.mawusikpodo.workoutplanner.theme.Muted
import com.mawusikpodo.workoutplanner.theme.Surface1
import com.mawusikpodo.workoutplanner.theme.Surface2
import com.mawusikpodo.workoutplanner.ui.components.DifficultyBars
import com.mawusikpodo.workoutplanner.ui.components.EmptyState
import com.mawusikpodo.workoutplanner.ui.components.ExerciseFigure
import com.mawusikpodo.workoutplanner.ui.components.ExerciseSheet
import com.mawusikpodo.workoutplanner.ui.components.ExerciseThumbnail
import com.mawusikpodo.workoutplanner.ui.components.Overline
import com.mawusikpodo.workoutplanner.ui.components.ScreenTitle

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.style.TextOverflow
import com.mawusikpodo.workoutplanner.ui.components.Pill

/** Filtering is simple enough to live in the screen; the list comes straight from the repository. */
@Composable
fun LibraryScreen(
  onCreateWorkout: () -> Unit = {},
  onOpenWorkout: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val repository = LocalAppContainer.current.repository
  val exercises by repository.exercises.collectAsStateWithLifecycle(initialValue = emptyList())
  val workouts by repository.workouts.collectAsStateWithLifecycle(initialValue = emptyList())
  val bookmarks by repository.bookmarks.collectAsStateWithLifecycle(initialValue = emptySet())
  var query by rememberSaveable { mutableStateOf("") }
  var group by rememberSaveable { mutableStateOf<MuscleGroup?>(null) }
  var equipment by rememberSaveable { mutableStateOf(Equipment.BODYWEIGHT) }
  var savedOnly by rememberSaveable { mutableStateOf(false) }
  var playingId by rememberSaveable { mutableStateOf<String?>(null) }
  var detailId by rememberSaveable { mutableStateOf<String?>(null) }

  val customWorkouts = workouts.filter { it.isCustom }
  val filtered =
    exercises.filter {
      (!savedOnly || it.id in bookmarks) &&
        it.equipment == equipment &&
        (group == null || it.group == group) &&
        (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) || it.muscles.contains(query.trim(), ignoreCase = true))
    }

  LazyColumn(
    modifier.fillMaxSize(),
    contentPadding = PaddingValues(top = 28.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ScreenTitle("Library")
        Button(
          onClick = onCreateWorkout,
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Ground),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        ) {
          Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("New Routine", style = MaterialTheme.typography.labelMedium)
        }
      }
    }

    if (customWorkouts.isNotEmpty()) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
          Overline("My Custom Routines", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp))
          LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(customWorkouts, key = { it.id }) { cw ->
              Surface(
                onClick = { onOpenWorkout(cw.id) },
                color = Surface1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Hairline),
                modifier = Modifier.width(180.dp).height(96.dp),
              ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                  Text(cw.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${cw.estimatedMinutes} min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Pill("Custom", color = MaterialTheme.colorScheme.primary)
                  }
                }
              }
            }
          }
        }
      }
    }
    item {
      TextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("Search ${exercises.size} exercises") },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null, modifier = Modifier.size(20.dp), tint = Muted) },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { query = "" }) {
              Icon(painterResource(R.drawable.ic_close), contentDescription = "Clear search", modifier = Modifier.size(16.dp), tint = Muted)
            }
          }
        },
        singleLine = true,
        shape = CircleShape,
        colors =
          TextFieldDefaults.colors(
            focusedContainerColor = Surface1,
            unfocusedContainerColor = Surface1,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
          ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).border(1.dp, Hairline, CircleShape),
      )
    }
    item {
      LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected = savedOnly,
            onClick = { savedOnly = !savedOnly },
            label = { Text("Saved", style = MaterialTheme.typography.labelMedium) },
            leadingIcon = {
              Icon(
                painter = painterResource(if (savedOnly) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
            shape = CircleShape,
            border = BorderStroke(1.dp, if (savedOnly) MaterialTheme.colorScheme.primary else Hairline),
            colors =
              FilterChipDefaults.filterChipColors(
                containerColor = Surface1,
                labelColor = Ink,
                iconColor = Ink,
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = Ground,
                selectedLeadingIconColor = Ground,
              ),
          )
        }
        item { GroupChip("All", group == null) { group = null } }
        items(MuscleGroup.entries) { g -> GroupChip(g.label, group == g) { group = if (group == g) null else g } }
      }
    }
    item {
      SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
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
            border = BorderStroke(1.dp, Hairline),
            icon = {},
          ) {
            Text(e.label, style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }
    if (filtered.isEmpty() && exercises.isNotEmpty()) {
      item { EmptyState(if (savedOnly) "No saved exercises yet." else "No exercises match these filters.", Modifier.padding(horizontal = 20.dp)) }
    }
    items(filtered, key = { it.id }) { exercise ->
      Surface(
        onClick = { detailId = exercise.id },
        color = Surface1,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Hairline),
        modifier = Modifier.padding(horizontal = 20.dp),
      ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
          DifficultyBars(exercise.difficulty)
          val isBookmarked = exercise.id in bookmarks
          IconButton(
            onClick = { repository.toggleBookmark(exercise.id) },
            modifier = Modifier.size(36.dp),
          ) {
            Icon(
              painter = painterResource(if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark),
              contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark exercise",
              tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Muted,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }

  exercises.firstOrNull { it.id == detailId }?.let {
    ExerciseSheet(
      exercise = it,
      isBookmarked = it.id in bookmarks,
      onToggleBookmark = { repository.toggleBookmark(it.id) },
      onDismiss = { detailId = null },
    )
  }
}

@Composable
private fun GroupChip(label: String, selected: Boolean, onClick: () -> Unit) {
  FilterChip(
    selected = selected,
    onClick = onClick,
    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
    shape = CircleShape,
    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Hairline),
    colors =
      FilterChipDefaults.filterChipColors(
        containerColor = Surface1,
        labelColor = Ink,
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = Ground,
      ),
  )
}
