package com.mawusikpodo.workoutplanner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.mawusikpodo.workoutplanner.R
import com.mawusikpodo.workoutplanner.model.Exercise
import com.mawusikpodo.workoutplanner.theme.ElectricCyan
import com.mawusikpodo.workoutplanner.theme.Hairline
import com.mawusikpodo.workoutplanner.theme.Muted
import com.mawusikpodo.workoutplanner.theme.Surface1
import com.mawusikpodo.workoutplanner.theme.Surface2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSwapSheet(
  currentExercise: Exercise,
  allExercises: List<Exercise>,
  onSelect: (Exercise) -> Unit,
  onDismiss: () -> Unit,
) {
  var query by rememberSaveable { mutableStateOf("") }

  val recommended =
    allExercises.filter {
      it.id != currentExercise.id &&
        it.group == currentExercise.group &&
        (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) || it.muscles.contains(query.trim(), ignoreCase = true))
    }

  val others =
    allExercises.filter {
      it.id != currentExercise.id &&
        it.group != currentExercise.group &&
        (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) || it.muscles.contains(query.trim(), ignoreCase = true))
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = Surface1,
    dragHandle = null,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Overline("Exercise Swap", color = ElectricCyan)
          Text("Replace ${currentExercise.name}", style = MaterialTheme.typography.titleLarge)
        }
        IconButton(onClick = onDismiss) {
          Icon(painterResource(R.drawable.ic_close), contentDescription = "Close", tint = Muted)
        }
      }

      TextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("Search alternative exercise...") },
        leadingIcon = {
          Icon(
            painterResource(R.drawable.ic_search),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Muted,
          )
        },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { query = "" }) {
              Icon(painterResource(R.drawable.ic_close), contentDescription = "Clear search", tint = Muted)
            }
          }
        },
        singleLine = true,
        shape = CircleShape,
        colors =
          TextFieldDefaults.colors(
            focusedContainerColor = Surface2,
            unfocusedContainerColor = Surface2,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
          ),
        modifier = Modifier.fillMaxWidth(),
      )

      LazyColumn(
        modifier = Modifier.fillMaxWidth().height(380.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (recommended.isNotEmpty()) {
          item {
            Text(
              "Recommended (${currentExercise.group.label} Focus)",
              style = MaterialTheme.typography.labelMedium,
              color = ElectricCyan,
              modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
          }
          items(recommended, key = { it.id }) { exercise ->
            SwapExerciseRow(exercise = exercise, onClick = { onSelect(exercise) })
          }
        }

        if (others.isNotEmpty()) {
          item {
            Text(
              "Other Exercises",
              style = MaterialTheme.typography.labelMedium,
              color = Muted,
              modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
            )
          }
          items(others, key = { it.id }) { exercise ->
            SwapExerciseRow(exercise = exercise, onClick = { onSelect(exercise) })
          }
        }

        if (recommended.isEmpty() && others.isEmpty()) {
          item {
            EmptyState(
              text = "No exercises found. Try searching for a different move or muscle group.",
              modifier = Modifier.padding(top = 24.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SwapExerciseRow(exercise: Exercise, onClick: () -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    color = Surface2,
    border = BorderStroke(1.dp, Hairline),
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      ExerciseThumbnail(exercise = exercise, playing = false, onToggle = {})
      Column(Modifier.weight(1f)) {
        Text(exercise.name, style = MaterialTheme.typography.titleSmall)
        Text(exercise.muscles, style = MaterialTheme.typography.bodySmall, color = Muted)
      }
      Pill(exercise.equipment.label)
    }
  }
}
