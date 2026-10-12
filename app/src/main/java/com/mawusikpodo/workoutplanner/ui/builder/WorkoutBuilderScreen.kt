package com.mawusikpodo.workoutplanner.ui.builder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawusikpodo.workoutplanner.LocalAppContainer
import com.mawusikpodo.workoutplanner.R
import com.mawusikpodo.workoutplanner.data.WorkoutRepository
import com.mawusikpodo.workoutplanner.model.Equipment
import com.mawusikpodo.workoutplanner.model.Exercise
import com.mawusikpodo.workoutplanner.model.MuscleGroup
import com.mawusikpodo.workoutplanner.model.Section
import com.mawusikpodo.workoutplanner.model.Workout
import com.mawusikpodo.workoutplanner.model.WorkoutItem
import com.mawusikpodo.workoutplanner.theme.ElectricCyan
import com.mawusikpodo.workoutplanner.theme.Ground
import com.mawusikpodo.workoutplanner.theme.Hairline
import com.mawusikpodo.workoutplanner.theme.Ink
import com.mawusikpodo.workoutplanner.theme.Muted
import com.mawusikpodo.workoutplanner.theme.Surface1
import com.mawusikpodo.workoutplanner.theme.Surface2
import com.mawusikpodo.workoutplanner.ui.components.EmptyState
import com.mawusikpodo.workoutplanner.ui.components.ExerciseThumbnail
import com.mawusikpodo.workoutplanner.ui.components.Overline
import com.mawusikpodo.workoutplanner.ui.components.Pill
import com.mawusikpodo.workoutplanner.ui.components.PrimaryButton
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class WorkoutBuilderViewModel(
  private val repository: WorkoutRepository,
  private val workoutId: String?,
  private val duplicateFromId: String?,
) : ViewModel() {
  var name by mutableStateOf("")
  var level by mutableStateOf("Beginner")
  var equipment by mutableStateOf(Equipment.BODYWEIGHT)
  var focus by mutableStateOf("Full body")
  var rounds by mutableIntStateOf(3)
  var restSec by mutableIntStateOf(20)
  val items = mutableStateListOf<WorkoutItem>()
  var isLoaded by mutableStateOf(false)

  init {
    viewModelScope.launch {
      val sourceId = workoutId ?: duplicateFromId
      if (sourceId != null) {
        val existing = repository.workout(sourceId).firstOrNull()
        if (existing != null) {
          name = if (workoutId != null) existing.name else "${existing.name} (Custom)"
          level = existing.level
          equipment = existing.equipment
          focus = existing.focus
          rounds = existing.rounds
          restSec = existing.restSec
          items.clear()
          items.addAll(existing.items)
        }
      }
      isLoaded = true
    }
  }

  suspend fun save(): String {
    val finalId = workoutId ?: "custom_${System.currentTimeMillis()}"
    val finalName = name.trim().ifEmpty { "My Custom Routine" }
    repository.saveCustomWorkout(
      id = finalId,
      name = finalName,
      level = level,
      equipment = equipment,
      focus = focus,
      rounds = rounds,
      restSec = restSec,
      items = items.toList(),
    )
    return finalId
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkoutBuilderScreen(
  workoutId: String?,
  duplicateFromId: String?,
  onBack: () -> Unit,
  onSaved: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val repository = LocalAppContainer.current.repository
  val viewModel: WorkoutBuilderViewModel =
    viewModel(key = "builder_${workoutId ?: duplicateFromId ?: "new"}") {
      WorkoutBuilderViewModel(repository, workoutId, duplicateFromId)
    }
  val allExercises by repository.exercises.collectAsStateWithLifecycle(initialValue = emptyList())
  val scope = rememberCoroutineScope()
  var showPicker by rememberSaveable { mutableStateOf(false) }
  var editingItemIndex by rememberSaveable { mutableStateOf<Int?>(null) }

  if (!viewModel.isLoaded) return

  Column(modifier = modifier.fillMaxSize().background(Ground)) {
    // Top Bar
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      IconButton(onClick = onBack) {
        Icon(painterResource(R.drawable.ic_back), contentDescription = "Back", tint = Ink)
      }
      Text(
        if (workoutId != null) "Edit Routine" else "Create Routine",
        style = MaterialTheme.typography.titleMedium,
      )
      Button(
        onClick = {
          scope.launch {
            val savedId = viewModel.save()
            onSaved(savedId)
          }
        },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Ground),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
        enabled = viewModel.items.isNotEmpty(),
      ) {
        Text("Save", style = MaterialTheme.typography.labelMedium)
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxWidth().weight(1f),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Routine Name Input
      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Overline("Routine Name")
          TextField(
            value = viewModel.name,
            onValueChange = { viewModel.name = it },
            placeholder = { Text("e.g. Morning Fat Burner") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors =
              TextFieldDefaults.colors(
                focusedContainerColor = Surface1,
                unfocusedContainerColor = Surface1,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
              ),
            modifier = Modifier.fillMaxWidth().border(1.dp, Hairline, RoundedCornerShape(14.dp)),
          )
        }
      }

      // Target Focus & Level
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Overline("Target Focus")
          val focusOptions = listOf("Full body", "Upper body", "Lower body", "Core", "Cardio", "Stretch")
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            focusOptions.forEach { option ->
              val selected = viewModel.focus == option
              FilterChip(
                selected = selected,
                onClick = { viewModel.focus = option },
                label = { Text(option) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Ground,
                    containerColor = Surface1,
                    labelColor = Ink,
                  ),
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = Hairline),
              )
            }
          }
        }
      }

      // Equipment & Level
      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Overline("Difficulty")
            val levels = listOf("Beginner", "Intermediate", "Advanced")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              levels.forEach { lvl ->
                val selected = viewModel.level == lvl
                FilterChip(
                  selected = selected,
                  onClick = { viewModel.level = lvl },
                  label = { Text(lvl, style = MaterialTheme.typography.labelSmall) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = ElectricCyan,
                      selectedLabelColor = Ground,
                      containerColor = Surface1,
                      labelColor = Ink,
                    ),
                  border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = Hairline),
                )
              }
            }
          }
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Overline("Equipment")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Equipment.entries.forEach { eq ->
                val selected = viewModel.equipment == eq
                FilterChip(
                  selected = selected,
                  onClick = { viewModel.equipment = eq },
                  label = { Text(eq.label, style = MaterialTheme.typography.labelSmall) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primary,
                      selectedLabelColor = Ground,
                      containerColor = Surface1,
                      labelColor = Ink,
                    ),
                  border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = Hairline),
                )
              }
            }
          }
        }
      }

      // Circuit Settings: Rounds & Rest
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = Surface1,
          border = BorderStroke(1.dp, Hairline),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text("Circuit Rounds", style = MaterialTheme.typography.titleSmall)
              Text("Repeats for main moves", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              IconButton(
                onClick = { if (viewModel.rounds > 1) viewModel.rounds-- },
                modifier = Modifier.size(32.dp).background(Surface2, CircleShape),
              ) {
                Text("-", style = MaterialTheme.typography.titleMedium, color = Ink)
              }
              Text("${viewModel.rounds}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(20.dp))
              IconButton(
                onClick = { if (viewModel.rounds < 8) viewModel.rounds++ },
                modifier = Modifier.size(32.dp).background(Surface2, CircleShape),
              ) {
                Text("+", style = MaterialTheme.typography.titleMedium, color = Ink)
              }
            }
          }
        }
      }

      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = Surface1,
          border = BorderStroke(1.dp, Hairline),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text("Rest Between Moves", style = MaterialTheme.typography.titleSmall)
              Text("Recovery time in circuit", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(15, 20, 30, 45).forEach { rSec ->
                val selected = viewModel.restSec == rSec
                Surface(
                  modifier = Modifier.clickable { viewModel.restSec = rSec },
                  shape = CircleShape,
                  color = if (selected) MaterialTheme.colorScheme.primary else Surface2,
                ) {
                  Text(
                    "${rSec}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Ground else Ink,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  )
                }
              }
            }
          }
        }
      }

      // Exercise List Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Overline("Exercises (${viewModel.items.size})")
            Text("Workout Flow", style = MaterialTheme.typography.titleMedium)
          }
          Button(
            onClick = { showPicker = true },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Surface1, contentColor = MaterialTheme.colorScheme.primary),
            border = BorderStroke(1.dp, Hairline),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
          ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add Move", style = MaterialTheme.typography.labelMedium)
          }
        }
      }

      // Exercise Items
      if (viewModel.items.isEmpty()) {
        item {
          EmptyState(
            text = "No exercises added yet. Tap 'Add Move' to build your workout from the exercise catalog.",
            modifier = Modifier.padding(vertical = 24.dp),
          )
        }
      } else {
        itemsIndexed(viewModel.items) { index, item ->
          BuilderExerciseRow(
            item = item,
            onEditDose = { editingItemIndex = index },
            onCycleSection = {
              val nextSection =
                when (item.section) {
                  Section.WARMUP -> Section.CIRCUIT
                  Section.CIRCUIT -> Section.COOLDOWN
                  Section.COOLDOWN -> Section.WARMUP
                }
              viewModel.items[index] = item.copy(section = nextSection)
            },
            onDelete = { viewModel.items.removeAt(index) },
          )
        }
      }
    }
  }

  // Bottom Sheet to Pick Exercises
  if (showPicker) {
    ExercisePickerSheet(
      exercises = allExercises,
      onSelect = { exercise ->
        viewModel.items.add(
          WorkoutItem(
            section = Section.CIRCUIT,
            exercise = exercise,
            durationSec = 30,
            reps = null,
            perSide = false,
          ),
        )
        showPicker = false
      },
      onDismiss = { showPicker = false },
    )
  }

  // Dose Editor Dialog
  editingItemIndex?.let { index ->
    val item = viewModel.items.getOrNull(index)
    if (item != null) {
      DoseEditorDialog(
        item = item,
        onConfirm = { updated ->
          viewModel.items[index] = updated
          editingItemIndex = null
        },
        onDismiss = { editingItemIndex = null },
      )
    }
  }
}

@Composable
private fun BuilderExerciseRow(
  item: WorkoutItem,
  onEditDose: () -> Unit,
  onCycleSection: () -> Unit,
  onDelete: () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    color = Surface1,
    border = BorderStroke(1.dp, Hairline),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      ExerciseThumbnail(exercise = item.exercise, playing = false, onToggle = {})
      Column(Modifier.weight(1f)) {
        Text(item.exercise.name, style = MaterialTheme.typography.titleSmall)
        Row(
          modifier = Modifier.padding(top = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Section toggle chip
          Surface(
            modifier = Modifier.clickable(onClick = onCycleSection),
            shape = CircleShape,
            color = Surface2,
            border = BorderStroke(1.dp, Hairline),
          ) {
            Text(
              item.section.label,
              style = MaterialTheme.typography.labelSmall,
              color = if (item.section == Section.CIRCUIT) MaterialTheme.colorScheme.primary else Muted,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
          // Dose chip (tap to edit)
          Surface(
            modifier = Modifier.clickable(onClick = onEditDose),
            shape = CircleShape,
            color = Surface2,
            border = BorderStroke(1.dp, Hairline),
          ) {
            Text(
              item.doseLabel,
              style = MaterialTheme.typography.labelSmall,
              color = Ink,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }
      }
      IconButton(onClick = onDelete) {
        Icon(painterResource(R.drawable.ic_delete), contentDescription = "Delete exercise", tint = Muted, modifier = Modifier.size(20.dp))
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
  exercises: List<Exercise>,
  onSelect: (Exercise) -> Unit,
  onDismiss: () -> Unit,
) {
  var query by rememberSaveable { mutableStateOf("") }
  var selectedGroup by rememberSaveable { mutableStateOf<MuscleGroup?>(null) }

  val filtered =
    exercises.filter {
      (selectedGroup == null || it.group == selectedGroup) &&
        (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) || it.muscles.contains(query.trim(), ignoreCase = true))
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = Surface1,
    dragHandle = null,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Overline("Library")
          Text("Pick an Exercise", style = MaterialTheme.typography.titleLarge)
        }
        IconButton(onClick = onDismiss) {
          Icon(painterResource(R.drawable.ic_close), contentDescription = "Close", tint = Muted)
        }
      }

      TextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("Search by exercise or muscle...") },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null, modifier = Modifier.size(20.dp), tint = Muted) },
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

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
          selected = selectedGroup == null,
          onClick = { selectedGroup = null },
          label = { Text("All") },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Ground),
        )
        MuscleGroup.entries.forEach { mg ->
          val selected = selectedGroup == mg
          FilterChip(
            selected = selected,
            onClick = { selectedGroup = if (selected) null else mg },
            label = { Text(mg.label) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Ground),
          )
        }
      }

      LazyColumn(
        modifier = Modifier.fillMaxWidth().height(360.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(filtered, key = { it.id }) { exercise ->
          Surface(
            modifier = Modifier.fillMaxWidth().clickable { onSelect(exercise) },
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
      }
    }
  }
}

@Composable
private fun DoseEditorDialog(
  item: WorkoutItem,
  onConfirm: (WorkoutItem) -> Unit,
  onDismiss: () -> Unit,
) {
  var isTimed by rememberSaveable { mutableStateOf(item.durationSec != null) }
  var durationSec by rememberSaveable { mutableIntStateOf(item.durationSec ?: 30) }
  var reps by rememberSaveable { mutableIntStateOf(item.reps ?: 10) }
  var perSide by rememberSaveable { mutableStateOf(item.perSide) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Surface1,
    title = { Text("Set Dose for ${item.exercise.name}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = { isTimed = true },
            colors = ButtonDefaults.buttonColors(containerColor = if (isTimed) MaterialTheme.colorScheme.primary else Surface2, contentColor = if (isTimed) Ground else Ink),
            modifier = Modifier.weight(1f),
          ) {
            Text("Timed (Sec)")
          }
          Button(
            onClick = { isTimed = false },
            colors = ButtonDefaults.buttonColors(containerColor = if (!isTimed) MaterialTheme.colorScheme.primary else Surface2, contentColor = if (!isTimed) Ground else Ink),
            modifier = Modifier.weight(1f),
          ) {
            Text("Reps")
          }
        }

        if (isTimed) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Duration (Seconds):", style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              IconButton(onClick = { if (durationSec > 10) durationSec -= 5 }) {
                Text("-", style = MaterialTheme.typography.titleLarge, color = Ink)
              }
              Text("${durationSec}s", style = MaterialTheme.typography.titleMedium)
              IconButton(onClick = { if (durationSec < 180) durationSec += 5 }) {
                Text("+", style = MaterialTheme.typography.titleLarge, color = Ink)
              }
            }
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Rep Count:", style = MaterialTheme.typography.bodyMedium)
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { if (reps > 1) reps-- }) {
                  Text("-", style = MaterialTheme.typography.titleLarge, color = Ink)
                }
                Text("$reps", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { if (reps < 100) reps++ }) {
                  Text("+", style = MaterialTheme.typography.titleLarge, color = Ink)
                }
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth().clickable { perSide = !perSide },
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text("Per Side?", style = MaterialTheme.typography.bodyMedium)
              Pill(if (perSide) "Yes (per side)" else "No", color = if (perSide) MaterialTheme.colorScheme.primary else Muted)
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onConfirm(
            item.copy(
              durationSec = if (isTimed) durationSec else null,
              reps = if (!isTimed) reps else null,
              perSide = if (!isTimed) perSide else false,
            ),
          )
        },
      ) {
        Text("Done")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
