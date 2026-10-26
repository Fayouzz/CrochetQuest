package com.crochetquest.app.data.repository

import androidx.room.withTransaction
import com.crochetquest.app.data.local.AchievementUnlock
import com.crochetquest.app.data.local.CrochetDatabase
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
import com.crochetquest.app.domain.game.GameStore
import kotlinx.coroutines.flow.Flow

/** Room-backed implementation of [GameStore], plus observable flows for the UI. */
class GameRepository(private val db: CrochetDatabase) : GameStore {
    private val profileDao = db.profileDao()
    private val sessionDao = db.sessionDao()
    private val stitchDao = db.stitchDao()
    private val projectDao = db.projectDao()
    private val projectUpdateDao = db.projectUpdateDao()
    private val questDao = db.questDao()
    private val achievementDao = db.achievementDao()
    private val inventoryDao = db.inventoryDao()
    private val equippedDao = db.equippedDao()
    private val resetDao = db.resetDao()
    private val stashDao = db.stashDao()
    private val backupDao = db.backupDao()

    // ---- observable state for the UI ----
    val profile: Flow<PlayerProfile?> = profileDao.observe()
    val projects: Flow<List<Project>> = projectDao.observeAll()
    val sessionCount: Flow<Int> = sessionDao.observeTotal()
    val stitchCount: Flow<Int> = stitchDao.observeCount()
    val projectUpdateCount: Flow<Int> = projectUpdateDao.observeTotal()
    val questCompletions: Flow<List<QuestCompletion>> = questDao.observeAll()
    val achievementUnlocks: Flow<List<AchievementUnlock>> = achievementDao.observeAll()
    val inventory: Flow<List<InventoryItem>> = inventoryDao.observeAll()
    val equipped: Flow<List<EquippedCosmetic>> = equippedDao.observeAll()
    val stash: Flow<List<StashYarn>> = stashDao.observeAll()
    val projectYarnLinks: Flow<List<ProjectYarnLink>> = stashDao.observeLinks()

    fun observeProjectUpdates(projectId: Long): Flow<List<ProjectUpdate>> =
        projectUpdateDao.observeForProject(projectId)

    // ---- GameStore ----
    override suspend fun <T> inTransaction(block: suspend () -> T): T =
        db.withTransaction { block() }

    override suspend fun getProfile(): PlayerProfile? = profileDao.get()

    override suspend fun insertProfileIfAbsent(profile: PlayerProfile): Boolean =
        profileDao.insertIfAbsent(profile) != -1L

    override suspend fun updateProfile(profile: PlayerProfile) = profileDao.update(profile)

    override suspend fun countRewardedSessionsOnDay(dayKey: String): Int =
        sessionDao.countRewardedOnDay(dayKey)

    override suspend fun insertSession(session: CrochetSession) {
        sessionDao.insert(session)
    }

    override suspend fun countSessionsBetween(startMillis: Long, endMillis: Long): Int =
        sessionDao.countBetween(startMillis, endMillis)

    override suspend fun insertStitchIfNew(record: StitchRecord): Boolean =
        stitchDao.insertIfNew(record) != -1L

    override suspend fun countStitches(): Int = stitchDao.count()

    override suspend fun countStitchesBetween(startMillis: Long, endMillis: Long): Int =
        stitchDao.countBetween(startMillis, endMillis)

    override suspend fun insertProject(project: Project): Long = projectDao.insert(project)

    override suspend fun updateProject(project: Project) = projectDao.update(project)

    override suspend fun getProject(id: Long): Project? = projectDao.get(id)

    override suspend fun deleteProject(id: Long) {
        projectUpdateDao.deleteForProject(id)
        stashDao.deleteLinksForProject(id)
        projectDao.delete(id)
    }

    override suspend fun countProjects(): Int = projectDao.count()
    override suspend fun countFinishedProjects(): Int = projectDao.countFinished()
    override suspend fun countFinishedProjectsInCategory(category: String): Int =
        projectDao.countFinishedInCategory(category)

    override suspend fun countProjectsWithMaterialNotes(): Int = projectDao.countWithMaterialNotes()

    override suspend fun insertProjectUpdate(update: ProjectUpdate) = projectUpdateDao.insert(update)

    override suspend fun countProjectUpdatesBetween(startMillis: Long, endMillis: Long): Int =
        projectUpdateDao.countBetween(startMillis, endMillis)

    override suspend fun getQuestCompletion(questKey: String, periodKey: String): QuestCompletion? =
        questDao.get(questKey, periodKey)

    override suspend fun allQuestCompletions(): List<QuestCompletion> = questDao.getAll()
    override suspend fun countQuestCompletions(): Int = questDao.count()

    override suspend fun insertQuestCompletionIfAbsent(completion: QuestCompletion): Boolean =
        questDao.insertIfAbsent(completion) != -1L

    override suspend fun markQuestClaimed(questKey: String, periodKey: String, claimedAt: Long): Boolean =
        questDao.markClaimed(questKey, periodKey, claimedAt) > 0

    override suspend fun allAchievementUnlocks(): List<AchievementUnlock> = achievementDao.getAll()

    override suspend fun insertAchievementIfAbsent(unlock: AchievementUnlock): Boolean =
        achievementDao.insertIfAbsent(unlock) != -1L

    override suspend fun allInventory(): List<InventoryItem> = inventoryDao.getAll()

    override suspend fun insertInventoryItemIfAbsent(item: InventoryItem): Boolean =
        inventoryDao.insertIfAbsent(item) != -1L

    override suspend fun allEquipped(): List<EquippedCosmetic> = equippedDao.getAll()

    override suspend fun setEquipped(slot: String, itemKey: String) =
        equippedDao.set(EquippedCosmetic(slot, itemKey))

    override suspend fun clearEquipped(slot: String) = equippedDao.clear(slot)
    override suspend fun clearAllEquipped() = equippedDao.clearAll()

    override suspend fun resetAllData() {
        resetDao.clearStashLinks()
        resetDao.clearStash()
        resetDao.clearEquipped()
        resetDao.clearInventory()
        resetDao.clearAchievements()
        resetDao.clearQuestCompletions()
        resetDao.clearProjectUpdates()
        resetDao.clearProjects()
        resetDao.clearStitches()
        resetDao.clearSessions()
        resetDao.clearProfile()
    }

    // ---- stats ----
    override suspend fun allSessions(): List<CrochetSession> = sessionDao.getAll()
    override suspend fun allStitches(): List<StitchRecord> = stitchDao.getAll()
    override suspend fun allProjects(): List<Project> = projectDao.getAll()

    // ---- yarn stash ----
    override suspend fun insertStashYarn(yarn: StashYarn): Long = stashDao.insert(yarn)
    override suspend fun updateStashYarn(yarn: StashYarn) = stashDao.update(yarn)
    override suspend fun getStashYarn(id: Long): StashYarn? = stashDao.get(id)

    override suspend fun deleteStashYarn(id: Long) {
        stashDao.deleteLinksForYarn(id)
        stashDao.delete(id)
    }

    override suspend fun allStashYarn(): List<StashYarn> = stashDao.getAll()
    override suspend fun allProjectYarnLinks(): List<ProjectYarnLink> = stashDao.getAllLinks()

    override suspend fun insertProjectYarnLink(link: ProjectYarnLink) {
        stashDao.insertLink(link)
    }

    override suspend fun deleteProjectYarnLink(projectId: Long, yarnId: Long) =
        stashDao.deleteLink(projectId, yarnId)

    override suspend fun countProjectsWithColors(min: Int): Int = stashDao.countProjectsWithAtLeast(min)

    // ---- backup and restore ----
    override suspend fun snapshot(): BackupSnapshot = BackupSnapshot(
        profile = profileDao.get(),
        sessions = sessionDao.getAll(),
        stitches = stitchDao.getAll(),
        projects = projectDao.getAll(),
        projectUpdates = projectUpdateDao.getAll(),
        questCompletions = questDao.getAll(),
        achievements = achievementDao.getAll(),
        inventory = inventoryDao.getAll(),
        equipped = equippedDao.getAll(),
        stash = stashDao.getAll(),
        projectYarn = stashDao.getAllLinks()
    )

    override suspend fun replaceAll(snapshot: BackupSnapshot) {
        resetAllData()
        backupDao.insertProfile(listOfNotNull(snapshot.profile))
        backupDao.insertSessions(snapshot.sessions)
        backupDao.insertStitches(snapshot.stitches)
        backupDao.insertProjects(snapshot.projects)
        backupDao.insertProjectUpdates(snapshot.projectUpdates)
        backupDao.insertQuestCompletions(snapshot.questCompletions)
        backupDao.insertAchievements(snapshot.achievements)
        backupDao.insertInventory(snapshot.inventory)
        backupDao.insertEquipped(snapshot.equipped)
        backupDao.insertStash(snapshot.stash)
        backupDao.insertLinks(snapshot.projectYarn)
    }
}
