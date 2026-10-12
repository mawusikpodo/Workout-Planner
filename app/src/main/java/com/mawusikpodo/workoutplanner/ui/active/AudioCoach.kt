package com.mawusikpodo.workoutplanner.ui.active

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class AudioCoach(context: Context) {
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  private var tts: TextToSpeech? = null
  private var isTtsReady = false
  private var toneGenerator: ToneGenerator? = null
  private val mainHandler = Handler(Looper.getMainLooper())
  private var focusRequest: AudioFocusRequest? = null
  private var hasAudioFocus = false

  init {
    try {
      toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
    } catch (_: Exception) {}

    tts =
      TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
          tts?.language = Locale.getDefault()
          isTtsReady = true
        }
      }

    tts?.setOnUtteranceProgressListener(
      object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}

        override fun onDone(utteranceId: String?) {
          mainHandler.post { abandonFocus() }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
          mainHandler.post { abandonFocus() }
        }
      }
    )
  }

  private fun requestDuckFocus() {
    val am = audioManager ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (focusRequest == null) {
        val attributes =
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        focusRequest =
          AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { /* Focus will be abandoned after speech finishes */ }
            .build()
      }
      focusRequest?.let {
        val result = am.requestAudioFocus(it)
        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
      }
    } else {
      @Suppress("DEPRECATION")
      val result =
        am.requestAudioFocus(
          null,
          AudioManager.STREAM_MUSIC,
          AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
        )
      hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
    }
  }

  private fun abandonFocus() {
    if (!hasAudioFocus) return
    val am = audioManager ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      focusRequest?.let { am.abandonAudioFocusRequest(it) }
    } else {
      @Suppress("DEPRECATION")
      am.abandonAudioFocus(null)
    }
    hasAudioFocus = false
  }

  fun speak(text: String) {
    if (isTtsReady && text.isNotBlank()) {
      requestDuckFocus()
      val utteranceId = "coach_${System.currentTimeMillis()}"
      tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }
  }

  fun beep() {
    try {
      requestDuckFocus()
      toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
      mainHandler.postDelayed({ abandonFocus() }, 160)
    } catch (_: Exception) {
      abandonFocus()
    }
  }

  fun chime() {
    try {
      requestDuckFocus()
      toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 250)
      mainHandler.postDelayed({ abandonFocus() }, 300)
    } catch (_: Exception) {
      abandonFocus()
    }
  }

  fun release() {
    mainHandler.removeCallbacksAndMessages(null)
    abandonFocus()
    tts?.stop()
    tts?.shutdown()
    tts = null
    toneGenerator?.release()
    toneGenerator = null
  }
}
