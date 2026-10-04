package com.mawusikpodo.workoutplanner.ui.active

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawusikpodo.workoutplanner.CompleteKey
import com.mawusikpodo.workoutplanner.LocalAppContainer
import com.mawusikpodo.workoutplanner.R
import com.mawusikpodo.workoutplanner.model.Section
import com.mawusikpodo.workoutplanner.model.Step
import com.mawusikpodo.workoutplanner.model.WorkoutItem
import com.mawusikpodo.workoutplanner.theme.Ground
import com.mawusikpodo.workoutplanner.theme.Ink
import com.mawusikpodo.workoutplanner.theme.Muted
import com.mawusikpodo.workoutplanner.theme.Outline
import com.mawusikpodo.workoutplanner.theme.Surface1
import com.mawusikpodo.workoutplanner.theme.Surface2
import com.mawusikpodo.workoutplanner.ui.components.ExerciseFigure
import com.mawusikpodo.workoutplanner.ui.components.Overline

@Composable
fun ActiveWorkoutScreen(
  workoutId: String,
  onFinished: (CompleteKey) -> Unit,
  onQuit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val repository = LocalAppContainer.current.repository
  val viewModel: ActiveWorkoutViewModel = viewModel { ActiveWorkoutViewModel(repository, workoutId) }
  val state by viewModel.state.collectAsStateWithLifecycle()
  var confirmQuit by remember { mutableStateOf(false) }
  // Remember whether the timer was running so "Keep going" resumes it.
  var resumeAfterDialog by remember { mutableStateOf(false) }
  fun askToQuit() {
    resumeAfterDialog = !state.paused
    if (!state.paused) viewModel.togglePause()
    confirmQuit = true
  }
  fun keepGoing() {
    confirmQuit = false
    if (resumeAfterDialog) viewModel.togglePause()
  }

  KeepScreenOn()
  BackHandler { askToQuit() }

  val haptics = LocalHapticFeedback.current
  LaunchedEffect(state.stepIndex) { if (!state.loading) haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
  LaunchedEffect(state.finished) { state.finished?.let(onFinished) }

  if (confirmQuit) {
    AlertDialog(
      onDismissRequest = ::keepGoing,
      title = { Text("End workout?") },
      text = { Text("You can save what you've done so far or discard this session.") },
      confirmButton = {
        TextButton(onClick = { confirmQuit = false; viewModel.finishEarly() }) { Text("Finish & save") }
      },
      dismissButton = {
        Row {
          TextButton(onClick = onQuit) { Text("Discard") }
          TextButton(onClick = ::keepGoing) { Text("Keep going") }
        }
      },
      containerColor = Surface1,
    )
  }

  if (!state.loading) {
    PlayerContent(
      state = state,
      onClose = ::askToQuit,
      onTogglePause = viewModel::togglePause,
      onNext = viewModel::next,
      onPrevious = viewModel::previous,
      modifier = modifier,
    )
  }
}

@Composable
private fun KeepScreenOn() {
  val view = LocalView.current
  DisposableEffect(view) {
    view.keepScreenOn = true
    onDispose { view.keepScreenOn = false }
  }
}

@Composable
private fun PlayerContent(
  state: PlayerUiState,
  onClose: () -> Unit,
  onTogglePause: () -> Unit,
  onNext: () -> Unit,
  onPrevious: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val step = state.step ?: return
  val focusItem: WorkoutItem =
    when (step) {
      is Step.GetReady -> step.next
      is Step.Rest -> step.next
      is Step.Move -> step.item
    }
  val title =
    when (step) {
      is Step.GetReady -> "Get ready"
      is Step.Rest -> "Rest"
      is Step.Move ->
        if (step.round != null) "Round ${step.round} of ${step.totalRounds}" else step.item.section.label
    }

  Column(
    modifier.fillMaxSize().background(Ground).safeDrawingPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onClose, modifier = Modifier.padding(start = 0.dp)) {
        Icon(painterResource(R.drawable.ic_close), contentDescription = "End workout")
      }
      Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
      Text(formatClock(state.elapsedSec), style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
    }

    LinearProgressIndicator(
      progress = { state.progress },
      modifier = Modifier.fillMaxWidth().height(4.dp),
      trackColor = Surface2,
      strokeCap = StrokeCap.Round,
      gapSize = 0.dp,
      drawStopIndicator = {},
    )

    DemoCard(item = focusItem, isRest = step is Step.Rest, modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 140.dp))

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
      val overline =
        when (step) {
          is Step.Move -> "Exercise ${step.number} of ${state.totalMoves}"
          is Step.Rest -> "Up next"
          is Step.GetReady -> "First up"
        }
      Overline(overline, color = MaterialTheme.colorScheme.primary)
      Text(focusItem.exercise.name.uppercase(), style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
    }

    TimerRing(
      remainingSec = state.remainingSec,
      totalSec = step.durationSec,
      repsLabel = if (step is Step.Move && step.durationSec == null) step.item.doseLabel else null,
      modifier = Modifier.size(180.dp).align(Alignment.CenterHorizontally),
    )

    state.upNext?.takeIf { step !is Step.Rest }?.let { next ->
      Surface(color = Surface1, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
          Overline("Next", modifier = Modifier.padding(end = 12.dp))
          Text(next.exercise.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
          Text(next.doseLabel, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
      }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
      FilledIconButton(onClick = onPrevious, modifier = Modifier.size(56.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Surface1, contentColor = Ink)) {
        Icon(painterResource(R.drawable.ic_skip_previous), contentDescription = "Previous exercise")
      }
      FilledIconButton(onClick = onTogglePause, modifier = Modifier.size(80.dp)) {
        Icon(
          painterResource(if (state.paused) R.drawable.ic_play else R.drawable.ic_pause),
          contentDescription = if (state.paused) "Resume" else "Pause",
          modifier = Modifier.size(30.dp),
        )
      }
      val isReps = step is Step.Move && step.durationSec == null
      FilledIconButton(
        onClick = onNext,
        modifier = Modifier.size(56.dp),
        colors =
          if (isReps) IconButtonDefaults.filledIconButtonColors()
          else IconButtonDefaults.filledIconButtonColors(containerColor = Surface1, contentColor = Ink),
      ) {
        Icon(
          painterResource(if (isReps) R.drawable.ic_check else R.drawable.ic_skip_next),
          contentDescription = if (isReps) "Done, next exercise" else "Skip",
        )
      }
    }
  }
}

/** Looping demo with its own pause and half-speed controls, independent of the workout timer. */
@Composable
private fun DemoCard(item: WorkoutItem, isRest: Boolean, modifier: Modifier = Modifier) {
  var demoPlaying by rememberSaveable { mutableStateOf(true) }
  var slow by rememberSaveable { mutableStateOf(false) }
  Surface(modifier, color = Surface1, shape = RoundedCornerShape(24.dp)) {
    Box {
      ExerciseFigure(
        motion = item.exercise.motion,
        playing = demoPlaying,
        slowMotion = slow,
        contentDescription = "${item.exercise.name} demonstration",
        modifier = Modifier.fillMaxSize().padding(top = 16.dp, bottom = 56.dp),
      )
      if (isRest) {
        Text(
          "REST",
          style = MaterialTheme.typography.labelSmall,
          color = Ground,
          modifier = Modifier.align(Alignment.TopStart).padding(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(horizontal = 10.dp, vertical = 6.dp),
        )
      }
      IconButton(
        onClick = { demoPlaying = !demoPlaying },
        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
        colors = IconButtonDefaults.iconButtonColors(containerColor = Ground, contentColor = Ink),
      ) {
        Icon(
          painterResource(if (demoPlaying) R.drawable.ic_pause else R.drawable.ic_play),
          contentDescription = if (demoPlaying) "Pause demo animation" else "Play demo animation",
          modifier = Modifier.size(16.dp),
        )
      }
      Text(
        item.exercise.formCue,
        style = MaterialTheme.typography.bodySmall,
        color = Muted,
        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp).background(Ground, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp).fillMaxWidth(0.65f),
      )
      OutlinedButton(
        onClick = { slow = !slow },
        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).height(40.dp),
        border = BorderStroke(1.5.dp, if (slow) MaterialTheme.colorScheme.primary else Outline),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Ground, contentColor = Ink),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
      ) {
        Text(if (slow) "0.5×" else "1×", style = MaterialTheme.typography.labelMedium)
      }
    }
  }
}

@Composable
private fun TimerRing(remainingSec: Int?, totalSec: Int?, repsLabel: String?, modifier: Modifier = Modifier) {
  val accent = MaterialTheme.colorScheme.primary
  val fraction = if (remainingSec != null && totalSec != null && totalSec > 0) remainingSec.toFloat() / totalSec else 1f
  Box(modifier, contentAlignment = Alignment.Center) {
    Canvas(Modifier.fillMaxSize()) {
      val stroke = 12.dp.toPx()
      val inset = stroke / 2
      val arcSize = Size(size.width - stroke, size.height - stroke)
      drawArc(Surface2, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
      drawArc(accent, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
      if (remainingSec != null) {
        Text(formatClock(remainingSec), style = MaterialTheme.typography.displayLarge)
        if (totalSec != null) Text("of ${formatClock(totalSec)}", style = MaterialTheme.typography.bodySmall, color = Muted)
      } else {
        Text(repsLabel.orEmpty(), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Text("Tap ✓ when done", style = MaterialTheme.typography.bodySmall, color = Muted)
      }
    }
  }
}

fun formatClock(totalSec: Int): String = "%d:%02d".format(totalSec / 60, totalSec % 60)
