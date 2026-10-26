package com.crochetquest.app.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class DayCycleTest {
    private val utc = ZoneOffset.UTC
    private fun at(iso: String): Long = Instant.parse(iso).toEpochMilli()

    @Test
    fun `night runs from 20_00 to 07_00`() {
        assertFalse(DayCycle.isNight(at("2026-03-10T19:59:59Z"), utc))
        assertTrue(DayCycle.isNight(at("2026-03-10T20:00:00Z"), utc))
        assertTrue(DayCycle.isNight(at("2026-03-10T23:59:00Z"), utc))
        assertTrue(DayCycle.isNight(at("2026-03-11T00:00:00Z"), utc))
        assertTrue(DayCycle.isNight(at("2026-03-11T06:59:59Z"), utc))
        assertFalse(DayCycle.isNight(at("2026-03-11T07:00:00Z"), utc))
        assertFalse(DayCycle.isNight(at("2026-03-11T12:00:00Z"), utc))
    }

    @Test
    fun `next boundary is the following 07_00, 20_00 or midnight`() {
        assertEquals(at("2026-03-10T20:00:00Z"), DayCycle.nextBoundary(at("2026-03-10T12:00:00Z"), utc))
        assertEquals(at("2026-03-11T00:00:00Z"), DayCycle.nextBoundary(at("2026-03-10T20:30:00Z"), utc))
        assertEquals(at("2026-03-11T07:00:00Z"), DayCycle.nextBoundary(at("2026-03-11T00:10:00Z"), utc))
        assertEquals(at("2026-03-11T00:00:00Z"), DayCycle.nextBoundary(at("2026-03-10T23:59:59Z"), utc))
    }

    @Test
    fun `a boundary is strictly in the future even when now is exactly on one`() {
        val boundary = at("2026-03-10T20:00:00Z")
        assertEquals(at("2026-03-11T00:00:00Z"), DayCycle.nextBoundary(boundary, utc))
        val midnight = at("2026-03-11T00:00:00Z")
        assertEquals(at("2026-03-11T07:00:00Z"), DayCycle.nextBoundary(midnight, utc))
    }

    @Test
    fun `other time zones use their own local hours`() {
        val tokyo = java.time.ZoneId.of("Asia/Tokyo") // UTC+9
        assertTrue(DayCycle.isNight(at("2026-03-10T12:00:00Z"), tokyo)) // 21:00 local
        assertFalse(DayCycle.isNight(at("2026-03-10T03:00:00Z"), tokyo)) // 12:00 local
    }

    @Test
    fun `timer minutes round to the nearest minute within 1 to 1440`() {
        assertEquals(1, minutesForTimer(0, 0))
        assertEquals(1, minutesForTimer(0, 29_000))
        assertEquals(1, minutesForTimer(0, 89_000))
        assertEquals(2, minutesForTimer(0, 90_000))
        assertEquals(25, minutesForTimer(0, 25 * 60_000L))
        assertEquals(1440, minutesForTimer(0, 100L * 60 * 60_000))
        assertEquals(1, minutesForTimer(5_000, 1_000)) // clock moved backwards
    }
}
