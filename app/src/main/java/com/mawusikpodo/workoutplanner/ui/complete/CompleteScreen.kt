package com.mawusikpodo.workoutplanner.ui.complete

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mawusikpodo.workoutplanner.CompleteKey
import com.mawusikpodo.workoutplanner.LocalAppContainer
import com.mawusikpodo.workoutplanner.R
import com.mawusikpodo.workoutplanner.model.Feeling
import com.mawusikpodo.workoutplanner.model.estimatedKcal
import com.mawusikpodo.workoutplanner.theme.Ground
import com.mawusikpodo.workoutplanner.theme.Ink
import com.mawusikpodo.workoutplanner.theme.Muted
import com.mawusikpodo.workoutplanner.theme.Outline
import com.mawusikpodo.workoutplanner.theme.Surface1
import com.mawusikpodo.workoutplanner.theme.Surface2
import com.mawusikpodo.workoutplanner.ui.active.formatClock
import com.mawusikpodo.workoutplanner.ui.components.PrimaryButton
import com.mawusikpodo.workoutplanner.ui.components.StatTile
import kotlinx.coroutines.launch

@Composable
fun CompleteScreen(result: CompleteKey, onDone: () -> Unit, modifier: Modifier = Modifier) {
  val container = LocalAppContainer.current
  var feeling by rememberSaveable { mutableStateOf<Feeling?>(null) }
  var saved by rememberSaveable { mutableStateOf(false) }

  fun save() {
    if (saved) return
    saved = true
    // App scope so the write finishes even though this screen leaves immediately.
    container.appScope.launch {
      container.repository.saveSession(
        workoutId = result.workoutId,
        workoutName = result.workoutName,
        startedAtMillis = result.startedAtMillis,
        durationSec = result.durationSec,
        exercisesCompleted = result.exercisesCompleted,
        feeling = feeling,
      )
    }
    onDone()
  }

  // Leaving with back still saves; discarding is an explicit choice.
  BackHandler { save() }

  Column(
    modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 32.dp),
    verticalArrangement = Arrangement.spacedBy(28.dp),
  ) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Box(Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Ground, modifier = Modifier.size(48.dp))
      }
      Text("WORKOUT\nCOMPLETE", style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
      Text(result.workoutName, style = MaterialTheme.typography.bodyLarge, color = Muted)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      StatTile("Time", formatClock(result.durationSec), Modifier.weight(1f))
      StatTile("Exercises", "${result.exercisesCompleted}", Modifier.weight(1f))
      StatTile("kcal (est.)", "~${estimatedKcal(result.durationSec)}", Modifier.weight(1f))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text("How did it feel?", style = MaterialTheme.typography.titleMedium)
      Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Feeling.entries.forEach { option ->
          val selected = feeling == option
          Surface(
            modifier =
              Modifier.weight(1f).height(48.dp).selectable(selected = selected, role = Role.RadioButton, onClick = { feeling = option }),
            shape = RoundedCornerShape(12.dp),
            color = if (selected) Surface2 else Surface1,
            border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
              RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(unselectedColor = Outline), modifier = Modifier.size(20.dp))
              Spacer(Modifier.size(6.dp))
              Text(option.label, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
      Text("Saved with this workout in your history.", style = MaterialTheme.typography.bodySmall, color = Muted)
    }

    Spacer(Modifier.weight(1f, fill = false))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      PrimaryButton("Save & finish", onClick = ::save)
      OutlinedButton(
        onClick = onDone,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = CircleShape,
        border = BorderStroke(1.5.dp, Outline),
      ) {
        Text("Discard", color = Ink, style = MaterialTheme.typography.titleMedium)
      }
    }
  }
}
