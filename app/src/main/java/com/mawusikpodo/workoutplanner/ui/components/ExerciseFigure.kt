package com.mawusikpodo.workoutplanner.ui.components

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.mawusikpodo.workoutplanner.model.Motion
import com.mawusikpodo.workoutplanner.theme.Ink
import com.mawusikpodo.workoutplanner.theme.Muted
import com.mawusikpodo.workoutplanner.theme.Outline
import com.mawusikpodo.workoutplanner.theme.Shirt
import kotlin.math.floor

/**
 * Looping demonstration of an exercise, drawn as a simple figure that eases between key poses.
 * Stands in for real GIF/video demos; swap the body of this composable for an image loader later.
 */
@Composable
fun ExerciseFigure(
  motion: Motion,
  playing: Boolean,
  modifier: Modifier = Modifier,
  slowMotion: Boolean = false,
  contentDescription: String? = null,
) {
  val poses = remember(motion) { posesFor(motion) }
  val animate = playing && rememberAnimationsEnabled() && poses.size > 1
  val limbColor = if (playing) Ink else Muted
  val torsoColor = if (playing) Shirt else Muted.copy(alpha = 0.7f)

  val progress: State<Float> =
    if (animate) {
      val perPose = (if (motion == Motion.PLANK) 1200 else 650) * if (slowMotion) 2 else 1
      val transition = rememberInfiniteTransition(label = "figure")
      transition.animateFloat(
        initialValue = 0f,
        targetValue = poses.size.toFloat(),
        animationSpec = infiniteRepeatable(tween(perPose * poses.size, easing = LinearEasing), RepeatMode.Restart),
        label = "pose",
      )
    } else remember { mutableFloatStateOf(0f) }

  Canvas(modifier.semantics { if (contentDescription != null) this.contentDescription = contentDescription }) {
    val t = progress.value
    val index = floor(t).toInt().mod(poses.size)
    // Hold each pose briefly, then ease into the next one.
    val raw = t - floor(t)
    val eased = FastOutSlowInEasing.transform(((raw - HOLD) / (1 - HOLD)).coerceIn(0f, 1f))
    val pose = poses[index].lerpTo(poses[(index + 1) % poses.size], eased)
    drawFigure(pose, limbColor, torsoColor)
  }
}

private const val HOLD = 0.35f

@Composable
private fun rememberAnimationsEnabled(): Boolean {
  val resolver = LocalContext.current.contentResolver
  return remember { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f }
}

/** Joints in a 60×60 box; the ground is at y = 55. */
private class Pose(val points: List<Offset>) {
  fun lerpTo(other: Pose, fraction: Float) = Pose(points.zip(other.points) { a, b -> lerp(a, b, fraction) })

  operator fun get(joint: Int) = points[joint]
}

private const val HEAD = 0
private const val NECK = 1
private const val HIP = 2
private const val L_ELBOW = 3
private const val L_HAND = 4
private const val R_ELBOW = 5
private const val R_HAND = 6
private const val L_KNEE = 7
private const val L_FOOT = 8
private const val R_KNEE = 9
private const val R_FOOT = 10

private fun pose(vararg xy: Int): Pose {
  require(xy.size == 22)
  return Pose((0 until 11).map { Offset(xy[it * 2].toFloat(), xy[it * 2 + 1].toFloat()) })
}

private fun Pose.with(vararg overrides: Pair<Int, Offset>): Pose {
  val list = points.toMutableList()
  overrides.forEach { (joint, offset) -> list[joint] = offset }
  return Pose(list)
}

private fun o(x: Int, y: Int) = Offset(x.toFloat(), y.toFloat())

//                   head    neck    hip     lElbow  lHand   rElbow  rHand   lKnee   lFoot   rKnee   rFoot
private val STAND = pose(30, 11, 30, 19, 30, 35, 26, 27, 25, 34, 34, 27, 35, 34, 28, 44, 27, 53, 32, 44, 33, 53)
private val SQUAT_LOW = pose(30, 23, 30, 30, 30, 43, 25, 36, 29, 33, 35, 36, 31, 33, 22, 47, 25, 53, 38, 47, 35, 53)
private val JUMP = pose(30, 6, 30, 14, 30, 30, 26, 8, 25, 2, 34, 8, 35, 2, 29, 39, 28, 48, 31, 39, 32, 48)
private val JACK_OPEN = pose(30, 11, 30, 19, 30, 35, 23, 13, 19, 6, 37, 13, 41, 6, 25, 44, 20, 53, 35, 44, 40, 53)
private val PRESS_DOWN = STAND.with(L_ELBOW to o(23, 24), L_HAND to o(25, 16), R_ELBOW to o(37, 24), R_HAND to o(35, 16))
private val PRESS_UP = STAND.with(L_ELBOW to o(25, 12), L_HAND to o(26, 4), R_ELBOW to o(35, 12), R_HAND to o(34, 4))
private val PLANK_HIGH = pose(47, 27, 42, 31, 26, 41, 42, 42, 42, 53, 42, 42, 42, 53, 18, 47, 10, 53, 18, 47, 10, 53)
private val PLANK_LOW = pose(47, 40, 42, 44, 26, 48, 35, 48, 42, 53, 35, 48, 42, 53, 18, 51, 10, 53, 18, 51, 10, 53)
private val FOREARM = pose(46, 38, 41, 42, 26, 45, 40, 53, 48, 53, 40, 53, 48, 53, 18, 49, 10, 53, 18, 49, 10, 53)
private val FOREARM_BREATH = FOREARM.with(HIP to o(26, 43), HEAD to o(46, 37), NECK to o(41, 41))
private val CLIMBER_DRIVE = PLANK_HIGH.with(L_KNEE to o(34, 45), L_FOOT to o(28, 52))
private val CLIMBER_SWITCH = PLANK_HIGH.with(R_KNEE to o(34, 45), R_FOOT to o(28, 52))
private val SIDE_STAND = pose(30, 11, 30, 19, 30, 35, 31, 27, 32, 34, 29, 27, 28, 34, 31, 44, 31, 53, 29, 44, 29, 53)
private val LUNGE_LOW = pose(30, 19, 30, 27, 30, 40, 31, 33, 32, 40, 29, 33, 28, 40, 40, 42, 40, 53, 22, 52, 12, 53)
private val HINGE_DOWN = pose(42, 36, 37, 32, 25, 32, 37, 40, 37, 47, 37, 40, 37, 47, 26, 43, 25, 53, 28, 43, 28, 53)
private val BRIDGE_DOWN = pose(10, 49, 16, 51, 32, 51, 22, 53, 28, 53, 22, 53, 28, 53, 42, 43, 46, 53, 42, 43, 46, 53)
private val BRIDGE_UP = BRIDGE_DOWN.with(HIP to o(32, 41), L_KNEE to o(42, 40), R_KNEE to o(42, 40))
private val CRUNCH_DOWN = pose(10, 49, 16, 51, 34, 51, 13, 45, 9, 47, 13, 45, 9, 47, 42, 42, 48, 53, 42, 42, 48, 53)
private val CRUNCH_UP = CRUNCH_DOWN.with(HEAD to o(16, 40), NECK to o(20, 44), L_ELBOW to o(19, 37), L_HAND to o(14, 39), R_ELBOW to o(19, 37), R_HAND to o(14, 39))

private fun posesFor(motion: Motion): List<Pose> =
  when (motion) {
    Motion.SQUAT -> listOf(STAND, SQUAT_LOW)
    Motion.JUMP_SQUAT -> listOf(STAND, SQUAT_LOW, JUMP)
    Motion.PUSH -> listOf(PLANK_HIGH, PLANK_LOW)
    Motion.JACK -> listOf(STAND, JACK_OPEN)
    Motion.LUNGE -> listOf(SIDE_STAND, LUNGE_LOW)
    Motion.CLIMBER -> listOf(CLIMBER_DRIVE, CLIMBER_SWITCH)
    Motion.PLANK -> listOf(FOREARM, FOREARM_BREATH)
    Motion.HINGE -> listOf(SIDE_STAND, HINGE_DOWN)
    Motion.BRIDGE -> listOf(BRIDGE_DOWN, BRIDGE_UP)
    Motion.BURPEE -> listOf(STAND, SQUAT_LOW, PLANK_HIGH, SQUAT_LOW, JUMP)
    Motion.CRUNCH -> listOf(CRUNCH_DOWN, CRUNCH_UP)
    Motion.PRESS -> listOf(PRESS_DOWN, PRESS_UP)
  }

private fun DrawScope.drawFigure(pose: Pose, limb: Color, torso: Color) {
  val unit = minOf(size.width, size.height) / 60f
  val origin = Offset((size.width - 60 * unit) / 2, (size.height - 60 * unit) / 2)
  fun p(joint: Int) = origin + pose[joint] * unit

  drawLine(Outline, origin + Offset(6f, 56f) * unit, origin + Offset(54f, 56f) * unit, 1.6f * unit, StrokeCap.Round)

  fun chain(color: Color, width: Float, vararg joints: Int) {
    val path = Path().apply {
      moveTo(p(joints[0]).x, p(joints[0]).y)
      joints.drop(1).forEach { lineTo(p(it).x, p(it).y) }
    }
    drawPath(path, color, style = Stroke(width * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
  }

  chain(limb, 3.4f, HIP, R_KNEE, R_FOOT)
  chain(limb, 3.4f, NECK, R_ELBOW, R_HAND)
  chain(torso, 7f, NECK, HIP)
  chain(limb, 3.4f, HIP, L_KNEE, L_FOOT)
  chain(limb, 3.4f, NECK, L_ELBOW, L_HAND)
  drawCircle(limb, radius = 4.6f * unit, center = p(HEAD))
}

@Preview(widthDp = 120, heightDp = 120, backgroundColor = 0xFF1A1C20, showBackground = true)
@Composable
private fun ExerciseFigurePreview() {
  ExerciseFigure(Motion.JUMP_SQUAT, playing = true)
}
