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

import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder

class WorkoutPlannerApp : Application(), ImageLoaderFactory {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
  }

  override fun newImageLoader(): ImageLoader {
    return ImageLoader.Builder(this)
      .components {
        if (Build.VERSION.SDK_INT >= 28) {
          add(ImageDecoderDecoder.Factory())
        } else {
          add(GifDecoder.Factory())
        }
      }
      .build()
  }
}

/** Manual dependency container; swap for Hilt if the object graph grows. */
class AppContainer(context: Context) {
  val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val database = AppDatabase.create(context)
  val repository = WorkoutRepository(database, context = context)

  init {
    appScope.launch { repository.ensureSeeded() }
  }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
