package com.mawusikpodo.workoutplanner.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mawusikpodo.workoutplanner.R
import com.mawusikpodo.workoutplanner.model.Exercise
import com.mawusikpodo.workoutplanner.theme.Ground
import com.mawusikpodo.workoutplanner.theme.Ink
import com.mawusikpodo.workoutplanner.theme.Muted
import com.mawusikpodo.workoutplanner.theme.Outline
import com.mawusikpodo.workoutplanner.theme.Surface1
import com.mawusikpodo.workoutplanner.theme.Surface2

/** Uppercase condensed heading used at the top of every screen. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
  Text(text.uppercase(), modifier = modifier, style = MaterialTheme.typography.headlineLarge)
}

@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = Muted) {
  Text(text.uppercase(), modifier = modifier, style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier) {
  Text(
    text,
    modifier = modifier.clip(CircleShape).background(Surface1).padding(horizontal = 12.dp, vertical = 6.dp),
    style = MaterialTheme.typography.labelMedium,
  )
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  Surface(modifier = modifier, color = Surface1, shape = RoundedCornerShape(16.dp), content = content)
}

/** Full-width volt button used for the main action on a screen. */
@Composable
fun PrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  @DrawableRes icon: Int? = null,
  height: Dp = 56.dp,
  enabled: Boolean = true,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.fillMaxWidth().height(height),
    shape = CircleShape,
    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Ground),
  ) {
    if (icon != null) {
      Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
      Box(Modifier.size(8.dp))
    }
    Text(text, style = MaterialTheme.typography.labelLarge)
  }
}

/** 1–3 bars showing exercise difficulty. */
@Composable
fun DifficultyBars(level: Int, modifier: Modifier = Modifier) {
  val label = listOf("Beginner", "Intermediate", "Advanced").getOrElse(level - 1) { "" }
  Row(
    modifier.semantics { contentDescription = "Difficulty: $label" },
    horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    repeat(3) { i ->
      Box(
        Modifier.size(width = 6.dp, height = 14.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(if (i < level) MaterialTheme.colorScheme.primary else Outline)
      )
    }
  }
}

/**
 * Exercise thumbnail that plays its animation only while [playing]; tapping toggles it. Lists keep at
 * most one thumbnail playing to save battery.
 */
@Composable
fun ExerciseThumbnail(
  exercise: Exercise,
  playing: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
) {
  val shape = RoundedCornerShape(if (size > 50.dp) 12.dp else 10.dp)
  Surface(
    onClick = onToggle,
    modifier =
      modifier.size(size).semantics {
        role = Role.Button
        contentDescription = "Preview ${exercise.name}"
        stateDescription = if (playing) "Playing" else "Paused"
      },
    shape = shape,
    color = Surface2,
    border = if (playing) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
  ) {
    Box {
      ExerciseFigure(exercise.motion, playing = playing, modifier = Modifier.matchParentSize().padding(2.dp))
      Box(
        Modifier.align(Alignment.BottomEnd).padding(3.dp).size(15.dp).clip(CircleShape).background(Ground),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
          contentDescription = null,
          tint = Ink,
          modifier = Modifier.size(8.dp),
        )
      }
    }
  }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
  Column(
    modifier.clip(RoundedCornerShape(16.dp)).background(Surface1).padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
    Text(value, style = MaterialTheme.typography.headlineMedium)
  }
}

/** Placeholder shown while data loads or when a list is empty. */
@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
  Box(
    modifier.fillMaxWidth().border(1.5.dp, Surface2, RoundedCornerShape(16.dp)).padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Muted)
  }
}
