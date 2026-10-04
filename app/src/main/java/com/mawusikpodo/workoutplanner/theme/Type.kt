package com.mawusikpodo.workoutplanner.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mawusikpodo.workoutplanner.R

/** Condensed display face for headings, numbers and timers. */
val Display =
  FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
  )

@OptIn(ExperimentalTextApi::class)
private fun dmSans(weight: FontWeight) =
  Font(R.font.dm_sans, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

val Body =
  FontFamily(
    dmSans(FontWeight.Normal),
    dmSans(FontWeight.Medium),
    dmSans(FontWeight.SemiBold),
    dmSans(FontWeight.Bold),
  )

private fun display(size: Int, lineHeight: Int = size) =
  TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = size.sp, lineHeight = lineHeight.sp)

private fun body(size: Int, weight: FontWeight = FontWeight.Normal, lineHeight: Int = (size * 1.4).toInt()) =
  TextStyle(fontFamily = Body, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp)

val Typography =
  Typography(
    displayLarge = display(64),
    displayMedium = display(48, 46),
    displaySmall = display(44, 42),
    headlineLarge = display(40),
    headlineMedium = display(30),
    headlineSmall = display(24),
    titleLarge = body(18, FontWeight.SemiBold),
    titleMedium = body(15, FontWeight.SemiBold),
    titleSmall = body(14, FontWeight.SemiBold),
    bodyLarge = body(16),
    bodyMedium = body(14),
    bodySmall = body(12),
    labelLarge = body(16, FontWeight.Bold),
    labelMedium = body(13, FontWeight.Medium),
    labelSmall = body(12, FontWeight.SemiBold).copy(letterSpacing = 0.96.sp),
  )
