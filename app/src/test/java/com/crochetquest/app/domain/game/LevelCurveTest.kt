package com.crochetquest.app.domain.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelCurveTest {

    @Test
    fun `level thresholds match the spec table`() {
        assertEquals(0L, LevelCurve.xpRequiredForLevel(1))
        assertEquals(100L, LevelCurve.xpRequiredForLevel(2))
        assertEquals(250L, LevelCurve.xpRequiredForLevel(3))
        assertEquals(450L, LevelCurve.xpRequiredForLevel(4))
        assertEquals(700L, LevelCurve.xpRequiredForLevel(5))
        assertEquals(1000L, LevelCurve.xpRequiredForLevel(6))
    }

    @Test
    fun `level changes exactly at the boundaries`() {
        val cases = listOf(
            0 to 1, 99 to 1, 100 to 2, 249 to 2, 250 to 3,
            449 to 3, 450 to 4, 699 to 4, 700 to 5, 999 to 5, 1000 to 6
        )
        for ((xp, level) in cases) {
            assertEquals("xp=$xp", level, LevelCurve.levelForXp(xp))
        }
    }

    @Test
    fun `negative xp is treated as level 1`() {
        assertEquals(1, LevelCurve.levelForXp(-50))
    }

    @Test
    fun `very large xp does not overflow`() {
        val level = LevelCurve.levelForXp(Int.MAX_VALUE)
        assertTrue(level > 1000)
        assertTrue(LevelCurve.xpRequiredForLevel(level) <= Int.MAX_VALUE)
        assertTrue(LevelCurve.xpRequiredForLevel(level + 1) > Int.MAX_VALUE)
    }

    @Test
    fun `curve is strictly increasing`() {
        for (level in 1..200) {
            assertTrue(LevelCurve.xpRequiredForLevel(level + 1) > LevelCurve.xpRequiredForLevel(level))
        }
    }

    @Test
    fun `titles follow the spec and cap at Master Maker`() {
        assertEquals("Tiny Beginner", LevelCurve.titleForLevel(1))
        assertEquals("Yarn Curious", LevelCurve.titleForLevel(2))
        assertEquals("Little Stitcher", LevelCurve.titleForLevel(3))
        assertEquals("Cozy Creator", LevelCurve.titleForLevel(4))
        assertEquals("Stitch Explorer", LevelCurve.titleForLevel(5))
        assertEquals("Master Maker", LevelCurve.titleForLevel(6))
        assertEquals("Master Maker", LevelCurve.titleForLevel(40))
    }

    @Test
    fun `info reports progress inside the current level`() {
        val info = LevelCurve.infoFor(120)
        assertEquals(2, info.level)
        assertEquals("Yarn Curious", info.title)
        assertEquals(20, info.xpIntoLevel)
        assertEquals(150, info.xpNeededForNext)
        assertEquals(20f / 150f, info.progress, 0.0001f)
    }

    @Test
    fun `info at an exact threshold starts a fresh level`() {
        val info = LevelCurve.infoFor(250)
        assertEquals(3, info.level)
        assertEquals(0, info.xpIntoLevel)
        assertEquals(0f, info.progress, 0.0001f)
    }
}
