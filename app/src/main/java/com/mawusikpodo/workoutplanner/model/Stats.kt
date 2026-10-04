package com.mawusikpodo.workoutplanner.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

fun LocalDate.startOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * Consecutive days with at least one session, counting back from [today]. A streak that ended
 * yesterday is still alive (today isn't over yet).
 */
fun currentStreak(sessionDays: Set<Long>, today: LocalDate): Int {
  var day = today.toEpochDay()
  if (day !in sessionDays) day -= 1
  var streak = 0
  while (day in sessionDays) {
    streak++
    day--
  }
  return streak
}

fun bestStreak(sessionDays: Set<Long>): Int {
  var best = 0
  var run = 0
  var previous: Long? = null
  for (day in sessionDays.sorted()) {
    run = if (previous != null && day == previous + 1) run + 1 else 1
    best = maxOf(best, run)
    previous = day
  }
  return best
}

/** Active minutes per week for the [weeks] weeks ending with the week containing [today], oldest first. */
fun weeklyMinutes(sessions: List<Session>, today: LocalDate, weeks: Int = 8): List<Int> {
  val currentWeek = today.startOfWeek().toEpochDay()
  val totals = IntArray(weeks)
  for (s in sessions) {
    val weekStart = LocalDate.ofEpochDay(s.epochDay).startOfWeek().toEpochDay()
    val weeksAgo = ((currentWeek - weekStart) / 7).toInt()
    if (weeksAgo in 0 until weeks) totals[weeks - 1 - weeksAgo] += s.durationSec
  }
  return totals.map { (it + 30) / 60 }
}

/** Rough energy estimate for bodyweight circuit training (~7 kcal/min). Shown as an estimate only. */
fun estimatedKcal(durationSec: Int): Int = (durationSec / 60.0 * 7).toInt()
