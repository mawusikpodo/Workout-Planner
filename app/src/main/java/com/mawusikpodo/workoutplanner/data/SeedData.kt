package com.mawusikpodo.workoutplanner.data

import com.mawusikpodo.workoutplanner.data.db.ExerciseEntity
import com.mawusikpodo.workoutplanner.data.db.WorkoutEntity
import com.mawusikpodo.workoutplanner.data.db.WorkoutItemEntity
import com.mawusikpodo.workoutplanner.model.Equipment
import com.mawusikpodo.workoutplanner.model.Equipment.BANDS
import com.mawusikpodo.workoutplanner.model.Equipment.BODYWEIGHT
import com.mawusikpodo.workoutplanner.model.Equipment.DUMBBELLS
import com.mawusikpodo.workoutplanner.model.Motion
import com.mawusikpodo.workoutplanner.model.MuscleGroup
import com.mawusikpodo.workoutplanner.model.MuscleGroup.CARDIO
import com.mawusikpodo.workoutplanner.model.MuscleGroup.CORE
import com.mawusikpodo.workoutplanner.model.MuscleGroup.LEGS
import com.mawusikpodo.workoutplanner.model.MuscleGroup.MOBILITY
import com.mawusikpodo.workoutplanner.model.MuscleGroup.UPPER
import com.mawusikpodo.workoutplanner.model.Section
import com.mawusikpodo.workoutplanner.model.Section.CIRCUIT
import com.mawusikpodo.workoutplanner.model.Section.COOLDOWN
import com.mawusikpodo.workoutplanner.model.Section.WARMUP

/** Built-in exercise library and starter workouts, upserted on every launch. */
object SeedData {
  val exercises: List<ExerciseEntity> =
    listOf(
      ex("jumping_jacks", "Jumping jacks", "Full body · Cardio", CARDIO, BODYWEIGHT, 1, Motion.JACK, "Land softly on the balls of your feet"),
      ex("arm_circles", "Arm circles", "Shoulders", MOBILITY, BODYWEIGHT, 1, Motion.PRESS, "Start small, grow the circles slowly"),
      ex("squats", "Bodyweight squats", "Legs · Glutes", LEGS, BODYWEIGHT, 1, Motion.SQUAT, "Chest up, push your hips back"),
      ex("jump_squats", "Jump squats", "Legs · Glutes", LEGS, BODYWEIGHT, 2, Motion.JUMP_SQUAT, "Knees over toes · land softly"),
      ex("reverse_lunges", "Reverse lunges", "Legs · Glutes", LEGS, BODYWEIGHT, 2, Motion.LUNGE, "Front knee stays over your ankle"),
      ex("hip_flexor_stretch", "Hip flexor stretch", "Hips", MOBILITY, BODYWEIGHT, 1, Motion.LUNGE, "Tuck your pelvis, breathe slowly"),
      ex("glute_bridge", "Glute bridge", "Glutes · Hamstrings", LEGS, BODYWEIGHT, 1, Motion.BRIDGE, "Squeeze your glutes at the top"),
      ex("push_ups", "Push-ups", "Chest · Triceps", UPPER, BODYWEIGHT, 2, Motion.PUSH, "Keep your body in one straight line"),
      ex("knee_push_ups", "Knee push-ups", "Chest · Triceps", UPPER, BODYWEIGHT, 1, Motion.PUSH, "Hands under shoulders, hips in line"),
      ex("pike_push_ups", "Pike push-ups", "Shoulders", UPPER, BODYWEIGHT, 3, Motion.PUSH, "Hips high, lower your head between your hands"),
      ex("plank", "Plank", "Core", CORE, BODYWEIGHT, 1, Motion.PLANK, "Brace your abs, don't let your hips sag"),
      ex("plank_shoulder_taps", "Plank shoulder taps", "Core · Shoulders", CORE, BODYWEIGHT, 2, Motion.PLANK, "Keep your hips still as you tap"),
      ex("mountain_climbers", "Mountain climbers", "Core · Cardio", CARDIO, BODYWEIGHT, 2, Motion.CLIMBER, "Drive your knees, keep your hips low"),
      ex("burpees", "Burpees", "Full body · Cardio", CARDIO, BODYWEIGHT, 3, Motion.BURPEE, "Chest to the floor, then jump tall"),
      ex("crunches", "Crunches", "Core", CORE, BODYWEIGHT, 1, Motion.CRUNCH, "Lift with your abs, not your neck"),
      ex("dead_bug", "Dead bug", "Core", CORE, BODYWEIGHT, 1, Motion.CRUNCH, "Lower back stays pressed to the floor"),
      ex("hamstring_stretch", "Hamstring stretch", "Hamstrings", MOBILITY, BODYWEIGHT, 1, Motion.HINGE, "Hinge at the hips, keep knees soft"),
      ex("goblet_squat", "Goblet squat", "Legs · Glutes", LEGS, DUMBBELLS, 2, Motion.SQUAT, "Hold the weight close to your chest"),
      ex("romanian_deadlift", "Romanian deadlift", "Hamstrings · Glutes", LEGS, DUMBBELLS, 2, Motion.HINGE, "Flat back, weights slide down your thighs"),
      ex("shoulder_press", "Shoulder press", "Shoulders · Triceps", UPPER, DUMBBELLS, 2, Motion.PRESS, "Ribs down, press straight overhead"),
      ex("bent_over_row", "Bent-over row", "Back · Biceps", UPPER, DUMBBELLS, 2, Motion.HINGE, "Pull your elbows back toward your hips"),
      ex("band_squat", "Banded squat", "Legs · Glutes", LEGS, BANDS, 1, Motion.SQUAT, "Push your knees out against the band"),
      ex("band_pull_apart", "Band pull-apart", "Upper back", UPPER, BANDS, 1, Motion.PRESS, "Squeeze your shoulder blades together"),
      ex("band_good_morning", "Band good morning", "Hamstrings · Lower back", LEGS, BANDS, 2, Motion.HINGE, "Hinge slowly, keep a neutral spine"),
    )

  val workouts: List<WorkoutEntity> =
    listOf(
      WorkoutEntity("full_body_burn", "Full-Body Burn", "Beginner", BODYWEIGHT, "Full body", rounds = 3, restSec = 20),
      WorkoutEntity("core_7", "7-Min Core", "Beginner", BODYWEIGHT, "Core", rounds = 2, restSec = 10),
      WorkoutEntity("mobility_flow", "Mobility Flow", "Beginner", BODYWEIGHT, "Stretch", rounds = 2, restSec = 0),
      WorkoutEntity("legs_glutes", "Legs & Glutes", "Intermediate", BODYWEIGHT, "Lower body", rounds = 3, restSec = 20),
      WorkoutEntity("cardio_blast", "Cardio Blast", "Advanced", BODYWEIGHT, "Cardio", rounds = 4, restSec = 15),
      WorkoutEntity("upper_push", "Upper Body Push", "Intermediate", DUMBBELLS, "Upper body", rounds = 3, restSec = 30),
      WorkoutEntity("db_strength", "Dumbbell Strength", "Intermediate", DUMBBELLS, "Full body", rounds = 3, restSec = 30),
      WorkoutEntity("band_basics", "Band Basics", "Beginner", BANDS, "Full body", rounds = 2, restSec = 20),
    )

  val workoutItems: List<WorkoutItemEntity> =
    items(
      "full_body_burn",
      t(WARMUP, "jumping_jacks", 60),
      t(WARMUP, "arm_circles", 45),
      t(CIRCUIT, "jump_squats", 40),
      r(CIRCUIT, "push_ups", 12),
      r(CIRCUIT, "reverse_lunges", 10, perSide = true),
      t(CIRCUIT, "plank_shoulder_taps", 40),
      t(CIRCUIT, "mountain_climbers", 30),
      t(COOLDOWN, "hamstring_stretch", 60),
      t(COOLDOWN, "hip_flexor_stretch", 60),
    ) +
      items(
        "core_7",
        t(CIRCUIT, "crunches", 30),
        t(CIRCUIT, "plank", 30),
        t(CIRCUIT, "dead_bug", 30),
        t(CIRCUIT, "mountain_climbers", 30),
        t(CIRCUIT, "plank_shoulder_taps", 30),
      ) +
      items(
        "mobility_flow",
        t(WARMUP, "arm_circles", 45),
        t(CIRCUIT, "hip_flexor_stretch", 60),
        t(CIRCUIT, "hamstring_stretch", 60),
        r(CIRCUIT, "glute_bridge", 12),
        r(CIRCUIT, "squats", 10),
        t(COOLDOWN, "dead_bug", 45),
      ) +
      items(
        "legs_glutes",
        t(WARMUP, "jumping_jacks", 60),
        r(CIRCUIT, "squats", 15),
        r(CIRCUIT, "reverse_lunges", 10, perSide = true),
        r(CIRCUIT, "glute_bridge", 15),
        t(CIRCUIT, "jump_squats", 30),
        t(COOLDOWN, "hamstring_stretch", 60),
        t(COOLDOWN, "hip_flexor_stretch", 60),
      ) +
      items(
        "cardio_blast",
        t(WARMUP, "jumping_jacks", 60),
        t(CIRCUIT, "burpees", 30),
        t(CIRCUIT, "mountain_climbers", 30),
        t(CIRCUIT, "jump_squats", 30),
        t(CIRCUIT, "jumping_jacks", 30),
        t(COOLDOWN, "hamstring_stretch", 45),
      ) +
      items(
        "upper_push",
        t(WARMUP, "arm_circles", 45),
        t(WARMUP, "jumping_jacks", 45),
        r(CIRCUIT, "push_ups", 10),
        r(CIRCUIT, "shoulder_press", 12),
        r(CIRCUIT, "bent_over_row", 12),
        r(CIRCUIT, "pike_push_ups", 8),
        t(CIRCUIT, "plank", 40),
        t(COOLDOWN, "hamstring_stretch", 45),
      ) +
      items(
        "db_strength",
        t(WARMUP, "jumping_jacks", 60),
        r(CIRCUIT, "goblet_squat", 12),
        r(CIRCUIT, "romanian_deadlift", 12),
        r(CIRCUIT, "shoulder_press", 10),
        r(CIRCUIT, "bent_over_row", 10, perSide = true),
        t(COOLDOWN, "hamstring_stretch", 60),
      ) +
      items(
        "band_basics",
        t(WARMUP, "arm_circles", 45),
        r(CIRCUIT, "band_squat", 15),
        r(CIRCUIT, "band_pull_apart", 15),
        r(CIRCUIT, "band_good_morning", 12),
        t(CIRCUIT, "plank", 30),
        t(COOLDOWN, "hamstring_stretch", 45),
      )

  private fun ex(
    id: String,
    name: String,
    muscles: String,
    group: MuscleGroup,
    equipment: Equipment,
    difficulty: Int,
    motion: Motion,
    cue: String,
  ) = ExerciseEntity(id, name, muscles, group, equipment, difficulty, motion, cue)

  private class Spec(val section: Section, val exerciseId: String, val durationSec: Int?, val reps: Int?, val perSide: Boolean)

  private fun t(section: Section, exerciseId: String, seconds: Int) = Spec(section, exerciseId, seconds, null, false)

  private fun r(section: Section, exerciseId: String, reps: Int, perSide: Boolean = false) =
    Spec(section, exerciseId, null, reps, perSide)

  private fun items(workoutId: String, vararg specs: Spec) =
    specs.mapIndexed { i, s -> WorkoutItemEntity(workoutId, i, s.section, s.exerciseId, s.durationSec, s.reps, s.perSide) }
}
