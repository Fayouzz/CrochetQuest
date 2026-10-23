package com.crochetquest.app.domain.game

import com.crochetquest.app.data.local.AchievementUnlock
import com.crochetquest.app.data.local.CrochetSession
import com.crochetquest.app.data.local.PlayerProfile
import com.crochetquest.app.data.local.Project
import com.crochetquest.app.data.local.ProjectUpdate
import com.crochetquest.app.data.local.QuestCompletion
import com.crochetquest.app.data.local.StitchRecord
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Starts on Thursday 2026-01-01 12:00 UTC. Its week began Monday 2025-12-29. */
class TestClock(
    var millis: Long = Instant.parse("2026-01-01T12:00:00Z").toEpochMilli()
) : GameClock {
    override fun nowMillis(): Long = millis
    override val zone: ZoneId = ZoneOffset.UTC
    fun advanceDays(days: Long) {
        millis += days * 24 * 60 * 60 * 1000
    }
}

class FakeGameStore : GameStore {
    var profile: PlayerProfile? = null
    val sessions = mutableListOf<CrochetSession>()
    val stitches = mutableListOf<StitchRecord>()
    val projects = mutableMapOf<Long, Project>()
    val projectUpdates = mutableListOf<ProjectUpdate>()
    val completions = mutableListOf<QuestCompletion>()
    val unlocks = mutableListOf<AchievementUnlock>()
    private var nextProjectId = 1L

    override suspend fun <T> inTransaction(block: suspend () -> T): T = block()

    override suspend fun getProfile(): PlayerProfile? = profile

    override suspend fun insertProfileIfAbsent(profile: PlayerProfile): Boolean {
        if (this.profile != null) return false
        this.profile = profile
        return true
    }

    override suspend fun updateProfile(profile: PlayerProfile) {
        this.profile = profile
    }

    override suspend fun countRewardedSessionsOnDay(dayKey: String): Int =
        sessions.count { it.dayKey == dayKey && it.rewarded }

    override suspend fun insertSession(session: CrochetSession) {
        sessions += session
    }

    override suspend fun countSessionsBetween(startMillis: Long, endMillis: Long): Int =
        sessions.count { it.loggedAt >= startMillis && it.loggedAt < endMillis }

    override suspend fun insertStitchIfNew(record: StitchRecord): Boolean {
        if (stitches.any { it.stitchKey == record.stitchKey }) return false
        stitches += record
        return true
    }

    override suspend fun countStitches(): Int = stitches.size

    override suspend fun countStitchesBetween(startMillis: Long, endMillis: Long): Int =
        stitches.count { it.firstRecordedAt >= startMillis && it.firstRecordedAt < endMillis }

    override suspend fun insertProject(project: Project): Long {
        val id = nextProjectId++
        projects[id] = project.copy(id = id)
        return id
    }

    override suspend fun updateProject(project: Project) {
        projects[project.id] = project
    }

    override suspend fun getProject(id: Long): Project? = projects[id]

    override suspend fun deleteProject(id: Long) {
        projects.remove(id)
        projectUpdates.removeAll { it.projectId == id }
    }

    override suspend fun countProjects(): Int = projects.size
    override suspend fun countFinishedProjects(): Int = projects.values.count { it.completionRewardGranted }
    override suspend fun countFinishedProjectsInCategory(category: String): Int =
        projects.values.count { it.completionRewardGranted && it.category == category }

    override suspend fun countProjectsWithMaterialNotes(): Int =
        projects.values.count { it.materialNotes.isNotEmpty() }

    override suspend fun insertProjectUpdate(update: ProjectUpdate) {
        projectUpdates += update
    }

    override suspend fun countProjectUpdatesBetween(startMillis: Long, endMillis: Long): Int =
        projectUpdates.count { it.loggedAt >= startMillis && it.loggedAt < endMillis }

    override suspend fun getQuestCompletion(questKey: String, periodKey: String): QuestCompletion? =
        completions.firstOrNull { it.questKey == questKey && it.periodKey == periodKey }

    override suspend fun allQuestCompletions(): List<QuestCompletion> = completions.toList()
    override suspend fun countQuestCompletions(): Int = completions.size

    override suspend fun insertQuestCompletionIfAbsent(completion: QuestCompletion): Boolean {
        if (getQuestCompletion(completion.questKey, completion.periodKey) != null) return false
        completions += completion
        return true
    }

    override suspend fun markQuestClaimed(questKey: String, periodKey: String, claimedAt: Long): Boolean {
        val index = completions.indexOfFirst {
            it.questKey == questKey && it.periodKey == periodKey && it.claimedAt == null
        }
        if (index < 0) return false
        completions[index] = completions[index].copy(claimedAt = claimedAt)
        return true
    }

    override suspend fun allAchievementUnlocks(): List<AchievementUnlock> = unlocks.toList()

    override suspend fun insertAchievementIfAbsent(unlock: AchievementUnlock): Boolean {
        if (unlocks.any { it.achievementKey == unlock.achievementKey }) return false
        unlocks += unlock
        return true
    }
}
