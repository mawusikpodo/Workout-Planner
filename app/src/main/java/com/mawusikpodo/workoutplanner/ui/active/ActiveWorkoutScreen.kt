package com.mawusikpodo.workoutplanner.ui.active

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
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
import com.mawusikpodo.workoutplanner.model.Exercise
import com.mawusikpodo.workoutplanner.model.Section
import com.mawusikpodo.workoutplanner.model.Step
import com.mawusikpodo.workoutplanner.model.WorkoutItem
import com.mawusikpodo.workoutplanner.service.WorkoutPlayerService
import com.mawusikpodo.workoutplanner.theme.ElectricCyan
import com.mawusikpodo.workoutplanner.theme.Ground
import com.mawusikpodo.workoutplanner.theme.Hairline
import com.mawusikpodo.workoutplanner.theme.Ink
import com.mawusikpodo.workoutplanner.theme.Muted
import com.mawusikpodo.workoutplanner.theme.Outline
import com.mawusikpodo.workoutplanner.theme.Surface1
import com.mawusikpodo.workoutplanner.theme.Surface2
import com.mawusikpodo.workoutplanner.ui.components.ExerciseFigure
import com.mawusikpodo.workoutplanner.ui.components.ExerciseSwapSheet
import com.mawusikpodo.workoutplanner.ui.components.ExerciseThumbnail
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
  val allExercises by repository.exercises.collectAsStateWithLifecycle(initialValue = emptyList())
  var confirmQuit by remember { mutableStateOf(false) }
  var exerciseToSwap by remember { mutableStateOf<Exercise?>(null) }
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
  val context = LocalContext.current
  val audioCoach = remember { AudioCoach(context) }
  DisposableEffect(audioCoach) {
    onDispose { audioCoach.release() }
  }

  // Manage foreground notification lifecycle
  DisposableEffect(Unit) {
    onDispose { WorkoutPlayerService.stop(context) }
  }

  LaunchedEffect(state.stepIndex, state.remainingSec, state.paused, state.finished) {
    if (!state.loading && state.finished == null) {
      val status =
        when (val s = state.step) {
          is Step.Move -> {
            val dose = state.remainingSec?.let { "${it}s" } ?: s.item.doseLabel
            "${s.item.exercise.name} ($dose)"
          }
          is Step.Rest -> "Recovery rest (${state.remainingSec ?: 0}s) · Next: ${s.next.exercise.name}"
          is Step.GetReady -> "Get ready · ${s.next.exercise.name}"
          null -> "Session in progress"
        }
      val fullStatus = if (state.paused) "Paused · $status" else status
      WorkoutPlayerService.startOrUpdate(context, state.workoutName, fullStatus)
    } else if (state.finished != null) {
      WorkoutPlayerService.stop(context)
    }
  }

  LaunchedEffect(state.stepIndex) {
    if (!state.loading) {
      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
      if (state.soundEnabled) {
        when (val step = state.step) {
          is Step.GetReady -> audioCoach.speak("Get ready for ${step.next.exercise.name}")
          is Step.Move -> audioCoach.speak("${step.item.exercise.name}. ${step.item.doseLabel}")
          is Step.Rest -> audioCoach.speak("Rest for ${step.durationSec} seconds. Next up: ${step.next.exercise.name}")
          null -> {}
        }
      }
    }
  }

  LaunchedEffect(state.remainingSec, state.paused) {
    val remaining = state.remainingSec
    if (remaining != null && !state.paused) {
      if (remaining in 1..3) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (state.soundEnabled) {
          audioCoach.beep()
        }
      } else if (remaining == 0) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (state.soundEnabled) {
          audioCoach.chime()
        }
      }
    }
  }

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

  exerciseToSwap?.let { currentEx ->
    ExerciseSwapSheet(
      currentExercise = currentEx,
      allExercises = allExercises,
      onSelect = { newEx ->
        viewModel.swapCurrentExercise(newEx)
        audioCoach.speak("Swapped to ${newEx.name}")
        exerciseToSwap = null
      },
      onDismiss = { exerciseToSwap = null },
    )
  }

  if (!state.loading) {
    PlayerContent(
      state = state,
      onClose = ::askToQuit,
      onTogglePause = viewModel::togglePause,
      onToggleSound = viewModel::toggleSound,
      onNext = viewModel::next,
      onPrevious = viewModel::previous,
      onAddRest15 = { viewModel.addRest(15) },
      onAddRest30 = { viewModel.addRest(30) },
      onSkipRest = viewModel::skipRest,
      onOpenSwap = {
        val current =
          when (val s = state.step) {
            is Step.Move -> s.item.exercise
            is Step.GetReady -> s.next.exercise
            is Step.Rest -> s.next.exercise
            null -> null
          }
        exerciseToSwap = current
      },
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
  onToggleSound: () -> Unit,
  onNext: () -> Unit,
  onPrevious: () -> Unit,
  onAddRest15: () -> Unit,
  onAddRest30: () -> Unit,
  onSkipRest: () -> Unit,
  onOpenSwap: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val step = state.step ?: return
  val isRest = step is Step.Rest
  val accentColor = if (isRest) ElectricCyan else MaterialTheme.colorScheme.primary

  val focusItem: WorkoutItem =
    when (step) {
      is Step.GetReady -> step.next
      is Step.Rest -> step.next
      is Step.Move -> step.item
    }
  val title =
    when (step) {
      is Step.GetReady -> "GET READY"
      is Step.Rest -> "RECOVERY REST"
      is Step.Move ->
        if (step.round != null) "ROUND ${step.round} OF ${step.totalRounds}" else step.item.section.label.uppercase()
    }

  Column(
    modifier = modifier.fillMaxSize().background(Ground).safeDrawingPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Header Bar
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onClose, modifier = Modifier.padding(start = 0.dp)) {
        Icon(painterResource(R.drawable.ic_close), contentDescription = "End workout", tint = Ink)
      }
      Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
      IconButton(onClick = onToggleSound) {
        Icon(
          painterResource(if (state.soundEnabled) R.drawable.ic_volume_up else R.drawable.ic_volume_off),
          contentDescription = if (state.soundEnabled) "Mute audio" else "Unmute audio",
          tint = if (state.soundEnabled) accentColor else Muted,
          modifier = Modifier.size(20.dp),
        )
      }
      Text(
        formatClock(state.elapsedSec),
        style = MaterialTheme.typography.bodyMedium,
        color = Muted,
        modifier = Modifier.width(48.dp),
        textAlign = TextAlign.End,
      )
    }

    // Segmented / Continuous Progress Bar
    LinearProgressIndicator(
      progress = { state.progress },
      modifier = Modifier.fillMaxWidth().height(4.dp),
      color = accentColor,
      trackColor = Surface2,
      strokeCap = StrokeCap.Round,
      gapSize = 0.dp,
      drawStopIndicator = {},
    )

    if (step is Step.Rest) {
      // DEDICATED REST MODE (Recovery & Breathing)
      Column(
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Overline("Rest & Catch Your Breath", color = ElectricCyan)
          Text("RECOVER", style = MaterialTheme.typography.headlineMedium)
        }

        TimerRing(
          remainingSec = state.remainingSec,
          totalSec = step.durationSec,
          repsLabel = null,
          accentColor = ElectricCyan,
          modifier = Modifier.size(170.dp),
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = onAddRest15,
            shape = CircleShape,
            border = BorderStroke(1.dp, Hairline),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Surface1),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
          ) {
            Text("+15s", style = MaterialTheme.typography.labelMedium, color = Ink)
          }
          OutlinedButton(
            onClick = onAddRest30,
            shape = CircleShape,
            border = BorderStroke(1.dp, Hairline),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Surface1),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
          ) {
            Text("+30s", style = MaterialTheme.typography.labelMedium, color = Ink)
          }
          Button(
            onClick = onSkipRest,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Ground),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
          ) {
            Text("Skip rest", style = MaterialTheme.typography.labelMedium)
          }
        }

        // Preview of Next Move with Swap Option
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = Surface1,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, Hairline),
        ) {
          Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            ExerciseThumbnail(exercise = step.next.exercise, playing = true, onToggle = {})
            Column(Modifier.weight(1f)) {
              Overline("Coming up next", color = ElectricCyan)
              Text(step.next.exercise.name, style = MaterialTheme.typography.titleMedium)
              Text(step.next.doseLabel, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            IconButton(
              onClick = onOpenSwap,
              modifier = Modifier.size(36.dp).background(Surface2, CircleShape).border(1.dp, Hairline, CircleShape),
            ) {
              Icon(painterResource(R.drawable.ic_swap), contentDescription = "Swap next exercise", tint = ElectricCyan, modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    } else {
      // DEDICATED WORK MODE (Active Movement)
      DemoCard(item = focusItem, modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 140.dp))

      // Clean Form Coaching Strip below the demo
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text("💡", style = MaterialTheme.typography.bodyMedium)
        Text(focusItem.exercise.formCue, style = MaterialTheme.typography.bodySmall, color = Ink, modifier = Modifier.weight(1f))
      }

      Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val overline =
          when (step) {
            is Step.Move -> "Exercise ${step.number} of ${state.totalMoves}"
            else -> "First up"
          }
        Overline(overline, color = MaterialTheme.colorScheme.primary)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(focusItem.exercise.name.uppercase(), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
          IconButton(
            onClick = onOpenSwap,
            modifier = Modifier.size(32.dp).background(Surface1, CircleShape).border(1.dp, Hairline, CircleShape),
          ) {
            Icon(painterResource(R.drawable.ic_swap), contentDescription = "Swap exercise", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
          }
        }
      }

      TimerRing(
        remainingSec = state.remainingSec,
        totalSec = step.durationSec,
        repsLabel = if (step is Step.Move && step.durationSec == null) step.item.doseLabel else null,
        accentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(160.dp).align(Alignment.CenterHorizontally),
      )

      state.upNext?.let { next ->
        Surface(color = Surface1, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Hairline)) {
          Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Overline("Next", modifier = Modifier.padding(end = 10.dp))
            Text(next.exercise.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(next.doseLabel, style = MaterialTheme.typography.bodySmall, color = Muted)
          }
        }
      }
    }

    // Media Controls
    Row(
      Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FilledIconButton(
        onClick = onPrevious,
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Surface1, contentColor = Ink),
      ) {
        Icon(painterResource(R.drawable.ic_skip_previous), contentDescription = "Previous exercise")
      }
      FilledIconButton(
        onClick = onTogglePause,
        modifier = Modifier.size(76.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = accentColor, contentColor = Ground),
      ) {
        Icon(
          painterResource(if (state.paused) R.drawable.ic_play else R.drawable.ic_pause),
          contentDescription = if (state.paused) "Resume" else "Pause",
          modifier = Modifier.size(32.dp),
          tint = Ground,
        )
      }
      val isReps = step is Step.Move && step.durationSec == null
      FilledIconButton(
        onClick = onNext,
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        colors =
          if (isReps) IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Ground)
          else IconButtonDefaults.filledIconButtonColors(containerColor = Surface1, contentColor = Ink),
      ) {
        Icon(
          painterResource(if (isReps) R.drawable.ic_check else R.drawable.ic_skip_next),
          contentDescription = if (isReps) "Done, next exercise" else "Skip",
          tint = if (isReps) Ground else Ink,
        )
      }
    }
  }
}

/** Looping demo with its own pause and half-speed controls, independent of the workout timer. */
@Composable
private fun DemoCard(item: WorkoutItem, modifier: Modifier = Modifier) {
  var demoPlaying by rememberSaveable { mutableStateOf(true) }
  var slow by rememberSaveable { mutableStateOf(false) }
  Surface(
    modifier,
    color = Surface1,
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, Hairline),
  ) {
    Box {
      ExerciseFigure(
        motion = item.exercise.motion,
        exerciseId = item.exercise.id,
        playing = demoPlaying,
        slowMotion = slow,
        contentDescription = "${item.exercise.name} demonstration",
        modifier = Modifier.fillMaxSize().padding(12.dp),
      )
      Row(
        Modifier.align(Alignment.TopEnd).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        OutlinedButton(
          onClick = { slow = !slow },
          modifier = Modifier.height(32.dp),
          shape = CircleShape,
          border = BorderStroke(1.dp, if (slow) MaterialTheme.colorScheme.primary else Hairline),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = Ground.copy(alpha = 0.8f), contentColor = if (slow) MaterialTheme.colorScheme.primary else Ink),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
          Text(if (slow) "0.5×" else "1×", style = MaterialTheme.typography.labelSmall)
        }
        IconButton(
          onClick = { demoPlaying = !demoPlaying },
          modifier = Modifier.size(32.dp).background(Ground.copy(alpha = 0.8f), CircleShape).border(1.dp, Hairline, CircleShape),
        ) {
          Icon(
            painterResource(if (demoPlaying) R.drawable.ic_pause else R.drawable.ic_play),
            contentDescription = if (demoPlaying) "Pause demo animation" else "Play demo animation",
            modifier = Modifier.size(14.dp),
            tint = Ink,
          )
        }
      }
    }
  }
}

@Composable
private fun TimerRing(
  remainingSec: Int?,
  totalSec: Int?,
  repsLabel: String?,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  val fraction = if (remainingSec != null && totalSec != null && totalSec > 0) remainingSec.toFloat() / totalSec else 1f
  val isCountingDown = remainingSec != null && remainingSec in 1..3
  val scale by animateFloatAsState(
    targetValue = if (isCountingDown) 1.08f else 1f,
    animationSpec = tween(durationMillis = 200),
    label = "timerScale",
  )

  Box(modifier.scale(scale), contentAlignment = Alignment.Center) {
    Canvas(Modifier.fillMaxSize()) {
      val stroke = 10.dp.toPx()
      val inset = stroke / 2
      val arcSize = Size(size.width - stroke, size.height - stroke)
      drawArc(Surface2, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
      drawArc(accentColor, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
      if (remainingSec != null) {
        Text(formatClock(remainingSec), style = MaterialTheme.typography.displayLarge, color = if (isCountingDown) accentColor else Ink)
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
