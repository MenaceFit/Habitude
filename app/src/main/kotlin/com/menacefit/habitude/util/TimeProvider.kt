package com.menacefit.habitude.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The single source of "now" for the whole app. Routing every "today" /
 * "now" through here (instead of scattering `LocalDate.now()` calls) means
 * the streak/XP/quest engines stay driven by one consistent clock per
 * request, and this seam can be swapped out in a test.
 */
interface TimeProvider {
    fun todayLocalDate(): LocalDate
    fun nowInstant(): Instant
    fun zoneId(): ZoneId
}

class SystemTimeProvider : TimeProvider {
    override fun todayLocalDate(): LocalDate = LocalDate.now()
    override fun nowInstant(): Instant = Instant.now()
    override fun zoneId(): ZoneId = ZoneId.systemDefault()
}

fun TimeProvider.localTimeOf(instant: Instant): LocalTime = instant.atZone(zoneId()).toLocalTime()

