package com.mawusikpodo.workoutplanner

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.mawusikpodo.workoutplanner.theme.Ink
import com.mawusikpodo.workoutplanner.theme.Muted
import com.mawusikpodo.workoutplanner.theme.NavBar
import com.mawusikpodo.workoutplanner.theme.Surface2
import com.mawusikpodo.workoutplanner.ui.active.ActiveWorkoutScreen
import com.mawusikpodo.workoutplanner.ui.complete.CompleteScreen
import com.mawusikpodo.workoutplanner.ui.detail.WorkoutDetailScreen
import com.mawusikpodo.workoutplanner.ui.library.LibraryScreen
import com.mawusikpodo.workoutplanner.ui.plan.PlanScreen
import com.mawusikpodo.workoutplanner.ui.progress.ProgressScreen
import com.mawusikpodo.workoutplanner.ui.today.TodayScreen

@Composable
fun MainNavigation() {
  val navigator = remember { TopLevelBackStack(TodayKey) }
  val current = navigator.backStack.lastOrNull()
  val showBottomBar = current !is ActiveWorkoutKey && current !is CompleteKey

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
      if (showBottomBar) {
        NavigationBar(containerColor = NavBar) {
          TopLevelKeys.forEach { key ->
            NavigationBarItem(
              selected = key == navigator.topLevelKey,
              onClick = { navigator.selectTab(key) },
              icon = { Icon(painterResource(key.icon), contentDescription = null) },
              label = { Text(key.label) },
              colors =
                NavigationBarItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  indicatorColor = Surface2,
                  unselectedIconColor = Muted,
                  unselectedTextColor = Muted,
                ),
            )
          }
        }
      }
    },
  ) { padding ->
    val content = Modifier.padding(padding).consumeWindowInsets(padding)
    NavDisplay(
      backStack = navigator.backStack,
      onBack = { navigator.removeLast() },
      entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
      entryProvider =
        entryProvider {
          entry<TodayKey> { TodayScreen(onOpenWorkout = { navigator.add(WorkoutDetailKey(it)) }, modifier = content) }
          entry<PlanKey> { PlanScreen(onOpenWorkout = { navigator.add(WorkoutDetailKey(it)) }, modifier = content) }
          entry<LibraryKey> { LibraryScreen(modifier = content) }
          entry<ProgressKey> { ProgressScreen(modifier = content) }
          entry<WorkoutDetailKey> { key ->
            WorkoutDetailScreen(
              workoutId = key.workoutId,
              onBack = { navigator.removeLast() },
              onStart = { navigator.add(ActiveWorkoutKey(it)) },
              modifier = content,
            )
          }
          entry<ActiveWorkoutKey> { key ->
            ActiveWorkoutScreen(
              workoutId = key.workoutId,
              onFinished = { result -> navigator.replaceLast(result) },
              onQuit = { navigator.removeLast() },
            )
          }
          entry<CompleteKey> { key ->
            CompleteScreen(result = key, onDone = { navigator.resetTo(TodayKey) })
          }
        },
    )
  }
}

/**
 * One back stack per bottom-bar tab, flattened for [NavDisplay]. Based on the Navigation 3
 * "common UI" recipe. Tabs other than Today are dropped when you back out of them.
 */
class TopLevelBackStack(private val startKey: TopLevelKey) {
  private val stacks: LinkedHashMap<TopLevelKey, SnapshotStateList<NavKey>> = linkedMapOf(startKey to mutableStateListOf(startKey))

  var topLevelKey: TopLevelKey by mutableStateOf(startKey)
    private set

  val backStack: SnapshotStateList<NavKey> = mutableStateListOf(startKey)

  private fun sync() {
    backStack.clear()
    backStack.addAll(stacks.values.flatten())
  }

  fun selectTab(key: TopLevelKey) {
    val existing = stacks.remove(key)
    if (existing != null && key == topLevelKey) {
      // Re-selecting the current tab pops it back to its root.
      stacks[key] = mutableStateListOf(key)
    } else {
      stacks[key] = existing ?: mutableStateListOf(key)
    }
    topLevelKey = key
    sync()
  }

  fun add(key: NavKey) {
    stacks.getValue(topLevelKey).add(key)
    sync()
  }

  fun replaceLast(key: NavKey) {
    val stack = stacks.getValue(topLevelKey)
    stack.removeAt(stack.lastIndex)
    stack.add(key)
    sync()
  }

  fun removeLast() {
    val stack = stacks.getValue(topLevelKey)
    if (stack.size > 1) {
      stack.removeAt(stack.lastIndex)
    } else if (stacks.size > 1) {
      stacks.remove(topLevelKey)
      topLevelKey = stacks.keys.last()
    }
    sync()
  }

  /** Clears every tab and shows [key]; used after finishing a workout. */
  fun resetTo(key: TopLevelKey) {
    stacks.clear()
    stacks[startKey] = mutableStateListOf(startKey)
    if (key != startKey) stacks[key] = mutableStateListOf(key)
    topLevelKey = key
    sync()
  }
}
