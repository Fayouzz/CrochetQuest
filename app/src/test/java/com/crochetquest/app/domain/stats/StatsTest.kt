package com.crochetquest.app.domain.stats

import com.crochetquest.app.data.local.AchievementUnlock
import com.crochetquest.app.data.local.CrochetSession
import com.crochetquest.app.data.local.Project
import com.crochetquest.app.data.local.StashYarn
import com.crochetquest.app.data.local.StitchRecord
import com.crochetquest.app.domain.projects.ProjectCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.Month
import java.time.ZoneOffset

class StatsTest {
    private val utc = ZoneOffset.UTC
    private fun at(iso: String) = Instant.parse(iso).toEpochMilli()
    private val now = at("2026-06-15T12:00:00Z")

    private fun session(id: Long, iso: String, minutes: Int?) = CrochetSession(
        id = id, loggedAt = at(iso), dayKey = iso.substring(0, 10), durationMinutes = minutes, rewarded = true
    )

    private fun project(
        id: Long, category: ProjectCategory, completedIso: String?, status: String = "COMPLETED"
    ) = Project(
        id = id, name = "P$id", description = "", category = category.name, status = status,
        progressPercent = 100, targetDateEpochDay = null, hookSize = "", materialNotes = "", stitchNotes = "",
        createdAt = at("2025-01-01T00:00:00Z"), completedAt = completedIso?.let { at(it) },
        completionRewardGranted = true, lastRewardedProgress = 100
    )

    private fun bundle(
        sessions: List<CrochetSession> = emptyList(),
        projects: List<Project> = emptyList(),
        stitches: List<StitchRecord> = emptyList(),
        unlocks: List<AchievementUnlock> = emptyList(),
        stash: List<StashYarn> = emptyList()
    ) = StatsCalculator.build(now, utc, sessions, projects, stitches, unlocks, stash)

    @Test
    fun `total time sums durations and ignores sessions without one`() {
        val b = bundle(
            sessions = listOf(
                session(1, "2026-01-02T10:00:00Z", 30),
                session(2, "2026-02-02T10:00:00Z", null),
                session(3, "2025-12-02T10:00:00Z", 45)
            )
        )
        assertEquals(75, b.gentle.totalMinutes)
        assertEquals(3, b.gentle.totalSessions)
    }

    @Test
    fun `projects finished this year only counts this calendar year`() {
        val b = bundle(
            projects = listOf(
                project(1, ProjectCategory.GIFTS, "2026-03-01T10:00:00Z"),
                project(2, ProjectCategory.GIFTS, "2026-05-01T10:00:00Z"),
                project(3, ProjectCategory.GIFTS, "2025-12-31T23:00:00Z"),
                project(4, ProjectCategory.GIFTS, null, status = "IN_PROGRESS") // reopened, not finished
            )
        )
        assertEquals(2, b.gentle.projectsFinishedThisYear)
        assertEquals(3, b.gentle.projectsFinishedAllTime)
    }

    @Test
    fun `stash totals`() {
        val b = bundle(
            stash = listOf(
                StashYarn(1, "A", 0, 3, 0),
                StashYarn(2, "B", 0, 4, 0)
            )
        )
        assertEquals(2, b.gentle.stashColors)
        assertEquals(7, b.gentle.stashSkeins)
    }

    @Test
    fun `years list is newest first, includes this year even with no activity`() {
        val empty = bundle()
        assertEquals(listOf(2026), empty.years)
        val b = bundle(
            sessions = listOf(session(1, "2024-01-02T10:00:00Z", 10)),
            projects = listOf(project(1, ProjectCategory.OTHER, "2025-03-01T10:00:00Z"))
        )
        assertEquals(listOf(2026, 2025, 2024), b.years)
    }

    @Test
    fun `a recap only looks at its own year`() {
        val b = bundle(
            sessions = listOf(
                session(1, "2026-01-05T10:00:00Z", 20),
                session(2, "2026-01-05T18:00:00Z", 40),
                session(3, "2026-03-10T10:00:00Z", 30),
                session(4, "2025-03-10T10:00:00Z", 999)
            )
        )
        val r = b.recaps.getValue(2026)
        assertEquals(3, r.sessionCount)
        assertEquals(90, r.totalMinutes)
        assertEquals(40, r.longestSessionMinutes)
        assertEquals(2, r.activeDays) // two sessions on the same day count once
        assertEquals(999, b.recaps.getValue(2025).totalMinutes)
    }

    @Test
    fun `busiest month is by minutes, with earlier months winning ties`() {
        val byMinutes = bundle(
            sessions = listOf(
                session(1, "2026-02-01T10:00:00Z", 10),
                session(2, "2026-02-02T10:00:00Z", 10),
                session(3, "2026-04-01T10:00:00Z", 60)
            )
        ).recaps.getValue(2026)
        assertEquals(Month.APRIL, byMinutes.busiestMonth)

        val tie = bundle(
            sessions = listOf(
                session(1, "2026-05-01T10:00:00Z", 30),
                session(2, "2026-03-01T10:00:00Z", 30)
            )
        ).recaps.getValue(2026)
        assertEquals(Month.MARCH, tie.busiestMonth)

        val noDurations = bundle(
            sessions = listOf(
                session(1, "2026-07-01T10:00:00Z", null),
                session(2, "2026-07-02T10:00:00Z", null),
                session(3, "2026-08-01T10:00:00Z", null)
            )
        ).recaps.getValue(2026)
        assertEquals(Month.JULY, noDurations.busiestMonth)
    }

    @Test
    fun `top category is the most finished, earlier category wins ties`() {
        val r = bundle(
            projects = listOf(
                project(1, ProjectCategory.GIFTS, "2026-01-01T10:00:00Z"),
                project(2, ProjectCategory.GIFTS, "2026-02-01T10:00:00Z"),
                project(3, ProjectCategory.CLOTHING, "2026-03-01T10:00:00Z"),
                project(4, ProjectCategory.CLOTHING, "2026-04-01T10:00:00Z")
            )
        ).recaps.getValue(2026)
        assertEquals(ProjectCategory.CLOTHING, r.topCategory) // CLOTHING comes before GIFTS
        assertEquals(4, r.projectsFinished)
    }

    @Test
    fun `stitches and achievements are counted by year`() {
        val r = bundle(
            stitches = listOf(
                StitchRecord(1, "a", "A", at("2026-02-01T00:00:00Z")),
                StitchRecord(2, "b", "B", at("2026-09-01T00:00:00Z")),
                StitchRecord(3, "c", "C", at("2025-09-01T00:00:00Z"))
            ),
            unlocks = listOf(
                AchievementUnlock(1, "x", at("2026-02-01T00:00:00Z")),
                AchievementUnlock(2, "y", at("2025-02-01T00:00:00Z"))
            )
        ).recaps.getValue(2026)
        assertEquals(2, r.stitchesDiscovered)
        assertEquals(1, r.achievementsUnlocked)
    }

    @Test
    fun `an empty year is flagged and has no favorites`() {
        val r = bundle().recaps.getValue(2026)
        assertTrue(r.isEmpty)
        assertNull(r.busiestMonth)
        assertNull(r.topCategory)
        assertEquals(0, r.longestSessionMinutes)
        assertFalse(bundle(sessions = listOf(session(1, "2026-01-01T10:00:00Z", 5))).recaps.getValue(2026).isEmpty)
    }

    @Test
    fun `minutes are formatted gently`() {
        assertEquals("0 min", formatMinutes(0))
        assertEquals("45 min", formatMinutes(45))
        assertEquals("2 h", formatMinutes(120))
        assertEquals("2 h 5 min", formatMinutes(125))
    }
}
