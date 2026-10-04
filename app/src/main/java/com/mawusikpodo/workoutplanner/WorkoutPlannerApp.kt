package com.mawusikpodo.workoutplanner

import android.app.Application
import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.mawusikpodo.workoutplanner.data.WorkoutRepository
import com.mawusikpodo.workoutplanner.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WorkoutPlannerApp : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
  }
}

/** Manual dependency container; swap for Hilt if the object graph grows. */
class AppContainer(context: Context) {
  val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val database = AppDatabase.create(context)
  val repository = WorkoutRepository(database)

  init {
    appScope.launch { repository.ensureSeeded() }
  }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
