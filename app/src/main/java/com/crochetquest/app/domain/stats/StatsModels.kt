package com.crochetquest.app.domain.stats

import com.crochetquest.app.data.local.AchievementUnlock
import com.crochetquest.app.data.local.CrochetSession
import com.crochetquest.app.data.local.Project
import com.crochetquest.app.data.local.StashYarn
import com.crochetquest.app.data.local.StitchRecord
import com.crochetquest.app.domain.projects.ProjectCategory
import com.crochetquest.app.domain.projects.ProjectStatus
import com.crochetquest.app.domain.projects.categoryEnum
import com.crochetquest.app.domain.projects.statusEnum
import java.time.Instant
import java.time.Month
import java.time.ZoneId

/** Friendly totals shown on the stats screen. Nothing here is a target or a streak. */
data class GentleStats(
    val totalMinutes: Int,
    val totalSessions: Int,
    val projectsFinishedThisYear: Int,
    val projectsFinishedAllTime: Int,
    val stashColors: Int,
    val stashSkeins: Int
)

/** A "year in yarn" summary for one calendar year. */
data class YearRecap(
    val year: Int,
    val sessionCount: Int,
    val totalMinutes: Int,
    val longestSessionMinutes: Int,
    val activeDays: Int,
    val busiestMonth: Month?,
    val projectsFinished: Int,
    val topCategory: ProjectCategory?,
    val stitchesDiscovered: Int,
    val achievementsUnlocked: Int
) {
    val isEmpty: Boolean get() = sessionCount == 0 && projectsFinished == 0 && stitchesDiscovered == 0
}

data class StatsBundle(
    val gentle: GentleStats,
    /** Years that have any activity (plus the current year), newest first. */
    val years: List<Int>,
    val recaps: Map<Int, YearRecap>
)

/** Pure calculations, so every number on the stats screen is unit-tested. */
object StatsCalculator {

    fun build(
        nowMillis: Long,
        zone: ZoneId,
        sessions: List<CrochetSession>,
        projects: List<Project>,
        stitches: List<StitchRecord>,
        unlocks: List<AchievementUnlock>,
        stash: List<StashYarn>
    ): StatsBundle {
        val currentYear = yearOf(nowMillis, zone)
        val finished = projects.filter { it.statusEnum == ProjectStatus.COMPLETED && it.completedAt != null }

        val years = (sessions.map { yearOf(it.loggedAt, zone) } +
            finished.map { yearOf(it.completedAt!!, zone) } +
            stitches.map { yearOf(it.firstRecordedAt, zone) } +
            currentYear).distinct().sortedDescending()

        val gentle = GentleStats(
            totalMinutes = sessions.sumOf { it.durationMinutes ?: 0 },
            totalSessions = sessions.size,
            projectsFinishedThisYear = finished.count { yearOf(it.completedAt!!, zone) == currentYear },
            projectsFinishedAllTime = finished.size,
            stashColors = stash.size,
            stashSkeins = stash.sumOf { it.amount }
        )
        val recaps = years.associateWith { recap(it, zone, sessions, finished, stitches, unlocks) }
        return StatsBundle(gentle, years, recaps)
    }

    fun recap(
        year: Int,
        zone: ZoneId,
        sessions: List<CrochetSession>,
        finishedProjects: List<Project>,
        stitches: List<StitchRecord>,
        unlocks: List<AchievementUnlock>
    ): YearRecap {
        val yearSessions = sessions.filter { yearOf(it.loggedAt, zone) == year }
        val yearFinished = finishedProjects.filter { it.completedAt != null && yearOf(it.completedAt, zone) == year }

        // Busiest month: most minutes, falling back to most sessions when no durations were recorded.
        val byMonth = yearSessions.groupBy { monthOf(it.loggedAt, zone) }
        val busiest = byMonth.maxWithOrNull(
            compareBy<Map.Entry<Month, List<CrochetSession>>>(
                { entry -> entry.value.sumOf { it.durationMinutes ?: 0 } },
                { entry -> entry.value.size },
                { entry -> -entry.key.value } // earlier month wins ties
            )
        )?.key

        val topCategory = yearFinished
            .groupingBy { it.categoryEnum }
            .eachCount()
            .maxWithOrNull(compareBy<Map.Entry<ProjectCategory, Int>>({ it.value }, { -it.key.ordinal }))
            ?.key

        return YearRecap(
            year = year,
            sessionCount = yearSessions.size,
            totalMinutes = yearSessions.sumOf { it.durationMinutes ?: 0 },
            longestSessionMinutes = yearSessions.maxOfOrNull { it.durationMinutes ?: 0 } ?: 0,
            activeDays = yearSessions.map { it.dayKey }.distinct().size,
            busiestMonth = busiest,
            projectsFinished = yearFinished.size,
            topCategory = topCategory,
            stitchesDiscovered = stitches.count { yearOf(it.firstRecordedAt, zone) == year },
            achievementsUnlocked = unlocks.count { yearOf(it.unlockedAt, zone) == year }
        )
    }

    private fun yearOf(millis: Long, zone: ZoneId): Int = Instant.ofEpochMilli(millis).atZone(zone).year
    private fun monthOf(millis: Long, zone: ZoneId): Month = Instant.ofEpochMilli(millis).atZone(zone).month
}

/** "2 h 5 min", "45 min", "0 min". */
fun formatMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0 -> "$minutes min"
        minutes == 0 -> "$hours h"
        else -> "$hours h $minutes min"
    }
}
