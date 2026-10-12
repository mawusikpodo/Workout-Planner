package com.mawusikpodo.workoutplanner.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mawusikpodo.workoutplanner.MainActivity
import com.mawusikpodo.workoutplanner.R

class WorkoutPlayerService : Service() {

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val action = intent?.action
    if (action == ACTION_STOP) {
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
      return START_NOT_STICKY
    }

    val workoutName = intent?.getStringExtra(EXTRA_WORKOUT_NAME) ?: "Workout"
    val statusText = intent?.getStringExtra(EXTRA_STATUS_TEXT) ?: "Session in progress"

    createChannelIfNeeded()
    val notification = buildNotification(workoutName, statusText)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val serviceType =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
          ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
          0
        }
      startForeground(NOTIFICATION_ID, notification, serviceType)
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }

    return START_STICKY
  }

  private fun createChannelIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          "Active Workout",
          NotificationManager.IMPORTANCE_LOW,
        ).apply {
          description = "Displays the current active workout and progress"
          setShowBadge(false)
        }
      manager?.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(workoutName: String, statusText: String): Notification {
    val openAppIntent =
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val pendingIntent =
      PendingIntent.getActivity(
        this,
        0,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(workoutName)
      .setContentText(statusText)
      .setSmallIcon(R.drawable.ic_flame)
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .setCategory(NotificationCompat.CATEGORY_WORKOUT)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
  }

  companion object {
    const val CHANNEL_ID = "active_workout_channel"
    const val NOTIFICATION_ID = 1001

    const val ACTION_START_OR_UPDATE = "com.mawusikpodo.workoutplanner.START_OR_UPDATE"
    const val ACTION_STOP = "com.mawusikpodo.workoutplanner.STOP"
    const val EXTRA_WORKOUT_NAME = "extra_workout_name"
    const val EXTRA_STATUS_TEXT = "extra_status_text"

    fun startOrUpdate(context: Context, workoutName: String, statusText: String) {
      val intent =
        Intent(context, WorkoutPlayerService::class.java).apply {
          action = ACTION_START_OR_UPDATE
          putExtra(EXTRA_WORKOUT_NAME, workoutName)
          putExtra(EXTRA_STATUS_TEXT, statusText)
        }
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
      } catch (_: Exception) {}
    }

    fun stop(context: Context) {
      val intent =
        Intent(context, WorkoutPlayerService::class.java).apply {
          action = ACTION_STOP
        }
      try {
        context.startService(intent)
      } catch (_: Exception) {}
    }
  }
}
