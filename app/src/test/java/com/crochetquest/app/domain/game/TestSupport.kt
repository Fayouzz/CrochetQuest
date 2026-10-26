package com.crochetquest.app.domain.game

import com.crochetquest.app.data.local.AchievementUnlock
import com.crochetquest.app.data.local.CrochetSession
import com.crochetquest.app.data.local.EquippedCosmetic
import com.crochetquest.app.data.local.InventoryItem
import com.crochetquest.app.data.local.PlayerProfile
import com.crochetquest.app.data.local.Project
import com.crochetquest.app.data.local.ProjectUpdate
import com.crochetquest.app.data.local.ProjectYarnLink
import com.crochetquest.app.data.local.QuestCompletion
import com.crochetquest.app.data.local.StashYarn
import com.crochetquest.app.data.local.StitchRecord
import com.crochetquest.app.domain.backup.BackupSnapshot
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
    val inventory = mutableListOf<InventoryItem>()
    val equipped = mutableMapOf<String, String>()
    val stash = mutableListOf<StashYarn>()
    val links = mutableListOf<ProjectYarnLink>()
    private var nextProjectId = 1L
    private var nextStashId = 1L
    /** Like the real database, every inserted row gets a unique id. */
    private var nextRowId = 1L

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
        sessions += session.copy(id = nextRowId++)
    }

    override suspend fun countSessionsBetween(startMillis: Long, endMillis: Long): Int =
        sessions.count { it.loggedAt >= startMillis && it.loggedAt < endMillis }

    override suspend fun insertStitchIfNew(record: StitchRecord): Boolean {
        if (stitches.any { it.stitchKey == record.stitchKey }) return false
        stitches += record.copy(id = nextRowId++)
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
        links.removeAll { it.projectId == id }
    }

    override suspend fun countProjects(): Int = projects.size
    override suspend fun countFinishedProjects(): Int = projects.values.count { it.completionRewardGranted }
    override suspend fun countFinishedProjectsInCategory(category: String): Int =
        projects.values.count { it.completionRewardGranted && it.category == category }

    override suspend fun countProjectsWithMaterialNotes(): Int =
        projects.values.count { it.materialNotes.isNotEmpty() }

    override suspend fun insertProjectUpdate(update: ProjectUpdate) {
        projectUpdates += update.copy(id = nextRowId++)
    }

    override suspend fun countProjectUpdatesBetween(startMillis: Long, endMillis: Long): Int =
        projectUpdates.count { it.loggedAt >= startMillis && it.loggedAt < endMillis }

    override suspend fun getQuestCompletion(questKey: String, periodKey: String): QuestCompletion? =
        completions.firstOrNull { it.questKey == questKey && it.periodKey == periodKey }

    override suspend fun allQuestCompletions(): List<QuestCompletion> = completions.toList()
    override suspend fun countQuestCompletions(): Int = completions.size

    override suspend fun insertQuestCompletionIfAbsent(completion: QuestCompletion): Boolean {
        if (getQuestCompletion(completion.questKey, completion.periodKey) != null) return false
        completions += completion.copy(id = nextRowId++)
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
        unlocks += unlock.copy(id = nextRowId++)
        return true
    }

    override suspend fun allInventory(): List<InventoryItem> = inventory.toList()

    override suspend fun insertInventoryItemIfAbsent(item: InventoryItem): Boolean {
        if (inventory.any { it.itemKey == item.itemKey }) return false
        inventory += item.copy(id = nextRowId++)
        return true
    }

    override suspend fun allEquipped(): List<EquippedCosmetic> =
        equipped.map { (slot, key) -> EquippedCosmetic(slot, key) }

    override suspend fun setEquipped(slot: String, itemKey: String) {
        equipped[slot] = itemKey
    }

    override suspend fun clearEquipped(slot: String) {
        equipped.remove(slot)
    }

    override suspend fun clearAllEquipped() {
        equipped.clear()
    }

    override suspend fun resetAllData() {
        profile = null
        sessions.clear(); stitches.clear(); projects.clear(); projectUpdates.clear()
        completions.clear(); unlocks.clear(); inventory.clear(); equipped.clear()
        stash.clear(); links.clear()
    }

    override suspend fun allSessions(): List<CrochetSession> = sessions.toList()
    override suspend fun allStitches(): List<StitchRecord> = stitches.toList()
    override suspend fun allProjects(): List<Project> = projects.values.toList()

    override suspend fun insertStashYarn(yarn: StashYarn): Long {
        val id = nextStashId++
        stash += yarn.copy(id = id)
        return id
    }

    override suspend fun updateStashYarn(yarn: StashYarn) {
        val i = stash.indexOfFirst { it.id == yarn.id }
        if (i >= 0) stash[i] = yarn
    }

    override suspend fun getStashYarn(id: Long): StashYarn? = stash.firstOrNull { it.id == id }

    override suspend fun deleteStashYarn(id: Long) {
        stash.removeAll { it.id == id }
        links.removeAll { it.yarnId == id }
    }

    override suspend fun allStashYarn(): List<StashYarn> = stash.toList()
    override suspend fun allProjectYarnLinks(): List<ProjectYarnLink> = links.toList()

    override suspend fun insertProjectYarnLink(link: ProjectYarnLink) {
        if (link !in links) links += link
    }

    override suspend fun deleteProjectYarnLink(projectId: Long, yarnId: Long) {
        links.removeAll { it.projectId == projectId && it.yarnId == yarnId }
    }

    override suspend fun countProjectsWithColors(min: Int): Int =
        links.groupBy { it.projectId }.count { it.value.size >= min }

    override suspend fun snapshot(): BackupSnapshot = BackupSnapshot(
        profile = profile,
        sessions = sessions.toList(),
        stitches = stitches.toList(),
        projects = projects.values.toList(),
        projectUpdates = projectUpdates.toList(),
        questCompletions = completions.toList(),
        achievements = unlocks.toList(),
        inventory = inventory.toList(),
        equipped = equipped.map { (slot, key) -> EquippedCosmetic(slot, key) },
        stash = stash.toList(),
        projectYarn = links.toList()
    )

    override suspend fun replaceAll(snapshot: BackupSnapshot) {
        resetAllData()
        profile = snapshot.profile
        sessions += snapshot.sessions
        stitches += snapshot.stitches
        snapshot.projects.forEach { projects[it.id] = it }
        projectUpdates += snapshot.projectUpdates
        completions += snapshot.questCompletions
        unlocks += snapshot.achievements
        inventory += snapshot.inventory
        snapshot.equipped.forEach { equipped[it.accessorySlot] = it.itemKey }
        stash += snapshot.stash
        links += snapshot.projectYarn
        nextProjectId = (projects.keys.maxOrNull() ?: 0L) + 1
        nextStashId = (stash.maxOfOrNull { it.id } ?: 0L) + 1
        nextRowId = listOf(
            sessions.maxOfOrNull { it.id }, stitches.maxOfOrNull { it.id }, projectUpdates.maxOfOrNull { it.id },
            completions.maxOfOrNull { it.id }, unlocks.maxOfOrNull { it.id }, inventory.maxOfOrNull { it.id }
        ).filterNotNull().maxOrNull()?.plus(1) ?: 1L
    }
}
