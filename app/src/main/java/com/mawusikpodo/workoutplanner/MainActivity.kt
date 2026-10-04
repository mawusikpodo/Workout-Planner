package com.mawusikpodo.workoutplanner

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.mawusikpodo.workoutplanner.theme.WorkoutPlannerTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    // The app is always dark, so system bar icons stay light regardless of the device theme.
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    super.onCreate(savedInstanceState)

    val container = (application as WorkoutPlannerApp).container
    setContent {
      CompositionLocalProvider(LocalAppContainer provides container) { WorkoutPlannerTheme { MainNavigation() } }
    }
  }
}
