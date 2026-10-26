package com.crochetquest.app.domain.game

import com.crochetquest.app.data.local.CrochetSession
import com.crochetquest.app.data.local.InventoryItem
import com.crochetquest.app.data.local.PlayerProfile
import com.crochetquest.app.data.local.Project
import com.crochetquest.app.data.local.ProjectUpdate
import com.crochetquest.app.data.local.ProjectYarnLink
import com.crochetquest.app.data.local.QuestCompletion
import com.crochetquest.app.data.local.StashYarn
import com.crochetquest.app.data.local.StitchRecord
import com.crochetquest.app.domain.achievements.AchievementCatalog
import com.crochetquest.app.domain.achievements.AchievementItem
import com.crochetquest.app.domain.backup.BackupFormatException
import com.crochetquest.app.domain.backup.BackupSnapshot
import com.crochetquest.app.domain.cosmetics.AccessorySlot
import com.crochetquest.app.domain.cosmetics.CosmeticCatalog
import com.crochetquest.app.domain.cosmetics.PurchaseResult
import com.crochetquest.app.domain.cosmetics.RoomCatalog
import com.crochetquest.app.domain.cosmetics.RoomEntry
import com.crochetquest.app.domain.cosmetics.RoomSlot
import com.crochetquest.app.domain.cosmetics.RoomState
import com.crochetquest.app.domain.cosmetics.WardrobeItem
import com.crochetquest.app.domain.projects.ProjectDraft
import com.crochetquest.app.domain.projects.ProjectStatus
import com.crochetquest.app.domain.projects.statusEnum
import com.crochetquest.app.domain.quests.QuestCatalog
import com.crochetquest.app.domain.quests.QuestItem
import com.crochetquest.app.domain.quests.QuestRule
import com.crochetquest.app.domain.quests.QuestStatus
import com.crochetquest.app.domain.quests.periodFor
import com.crochetquest.app.domain.stash.StashItem
import com.crochetquest.app.domain.stash.StashLimits
import com.crochetquest.app.domain.stats.StatsBundle
import com.crochetquest.app.domain.stats.StatsCalculator

data class SessionResult(
    val xpEarned: Int,
    val yarnEarned: Int,
    /** False when the daily reward limit was already reached (the session is still saved). */
    val sessionRewarded: Boolean,
    /** Set when the player recorded a stitch for the first time. */
    val newStitchName: String?,
    /** Set when the player typed a stitch they had already recorded (no reward). */
    val knownStitchName: String?,
    val levelBefore: Int,
    val levelAfter: Int,
    val checks: ChecksResult = ChecksResult.NONE
) {
    val leveledUp: Boolean get() = levelAfter > levelBefore
}

data class ProjectResult(
    val projectId: Long,
    val levelBefore: Int,
    val levelAfter: Int,
    val xpEarned: Int = 0,
    val yarnEarned: Int = 0,
    val completedNow: Boolean = false,
    val rewardedUpdate: Boolean = false,
    val checks: ChecksResult = ChecksResult.NONE
) {
    val leveledUp: Boolean get() = levelAfter > levelBefore
}

data class ClaimResult(
    val questTitle: String,
    val xpEarned: Int,
    val yarnEarned: Int,
    val levelBefore: Int,
    val levelAfter: Int,
    val checks: ChecksResult = ChecksResult.NONE
) {
    val leveledUp: Boolean get() = levelAfter > levelBefore
}

/**
 * Single source of truth for XP, yarn, levels, projects, quests and achievements.
 * UI code never touches reward math. Every mutating operation runs in one transaction, and
 * ends by running the [ProgressEngine] so quests and achievements follow real events only.
 */
class GameService(
    private val store: GameStore,
    private val clock: GameClock = SystemGameClock
) {
    private val engine = ProgressEngine(store, clock)

    // ------------------------------------------------------------------ profile

    /** Idempotent: if a profile already exists it is returned unchanged. */
    suspend fun createProfile(catName: String): PlayerProfile = store.inTransaction {
        store.getProfile() ?: run {
            val profile = PlayerProfile(
                catName = cleanCatName(catName) ?: RewardConfig.DEFAULT_CAT_NAME,
                totalXp = 0,
                yarnBalance = RewardConfig.STARTING_YARN,
                createdAt = clock.nowMillis()
            )
            store.insertProfileIfAbsent(profile)
            profile
        }
    }

    /** Returns false if there is no profile or the name is blank. */
    suspend fun renameCat(newName: String): Boolean = store.inTransaction {
        val profile = store.getProfile() ?: return@inTransaction false
        val cleaned = cleanCatName(newName) ?: return@inTransaction false
        store.updateProfile(profile.copy(catName = cleaned))
        true
    }

    suspend fun rewardedSessionsToday(): Int = store.countRewardedSessionsOnDay(clock.todayKey())
    suspend fun sessionTotal(): Int = store.countSessionsBetween(0L, Long.MAX_VALUE)
    suspend fun stitchTotal(): Int = store.countStitches()

    // ------------------------------------------------------------------ sessions

    /**
     * Logs a crochet session. Rules:
     *  - the first MAX_REWARDED_SESSIONS_PER_DAY sessions each local day earn the session reward;
     *    later ones are still recorded but earn nothing (no pressure, no farming);
     *  - an optional stitch name earns the new-stitch reward only the first time that stitch
     *    (case/whitespace-insensitive) is ever recorded.
     * Returns null when there is no profile yet.
     */
    suspend fun logSession(
        durationMinutes: Int? = null,
        newStitchName: String? = null
    ): SessionResult? = store.inTransaction {
        val profile = store.getProfile() ?: return@inTransaction null
        val now = clock.nowMillis()
        val dayKey = clock.dayKeyFor(now)

        var xp = 0
        var yarn = 0

        val sessionRewarded =
            store.countRewardedSessionsOnDay(dayKey) < RewardConfig.MAX_REWARDED_SESSIONS_PER_DAY
        if (sessionRewarded) {
            xp += RewardConfig.LOG_SESSION.xp
            yarn += RewardConfig.LOG_SESSION.yarn
        }
        store.insertSession(
            CrochetSession(
                loggedAt = now,
                dayKey = dayKey,
                durationMinutes = durationMinutes?.takeIf { it in 1..1440 },
                rewarded = sessionRewarded
            )
        )

        var rewardedStitch: String? = null
        var knownStitch: String? = null
        val stitchName = cleanStitchName(newStitchName)
        if (stitchName != null) {
            val isNew = store.insertStitchIfNew(
                StitchRecord(
                    stitchKey = stitchName.lowercase(),
                    stitchName = stitchName,
                    firstRecordedAt = now
                )
            )
            if (isNew) {
                rewardedStitch = stitchName
                xp += RewardConfig.NEW_STITCH.xp
                yarn += RewardConfig.NEW_STITCH.yarn
            } else {
                knownStitch = stitchName
            }
        }

        val updated = profile.withReward(xp, yarn)
        if (updated != profile) store.updateProfile(updated)
        val checks = engine.runChecks(updated)

        SessionResult(
            xpEarned = xp,
            yarnEarned = yarn,
            sessionRewarded = sessionRewarded,
            newStitchName = rewardedStitch,
            knownStitchName = knownStitch,
            levelBefore = LevelCurve.levelForXp(profile.totalXp),
            levelAfter = LevelCurve.levelForXp(updated.totalXp),
            checks = checks
        )
    }

    /** Deducts yarn if affordable. Used by the shop in a later stage. Never goes negative. */
    suspend fun spendYarn(cost: Int): Boolean = store.inTransaction {
        val profile = store.getProfile() ?: return@inTransaction false
        val updated = profile.trySpendYarn(cost) ?: return@inTransaction false
        store.updateProfile(updated)
        true
    }

    // ------------------------------------------------------------------ projects

    /**
     * Creates a project, or edits the details of an existing one. Editing never changes status,
     * progress or rewards. Returns null for a blank name, a missing profile or an unknown id.
     */
    suspend fun saveProject(draft: ProjectDraft): ProjectResult? = store.inTransaction {
        val profile = store.getProfile() ?: return@inTransaction null
        val name = cleanText(draft.name, RewardConfig.MAX_PROJECT_NAME_LENGTH) ?: return@inTransaction null
        val now = clock.nowMillis()
        val description = draft.description.limited(RewardConfig.MAX_PROJECT_NOTE_LENGTH)
        val hook = draft.hookSize.limited(RewardConfig.MAX_HOOK_SIZE_LENGTH)
        val materials = draft.materialNotes.limited(RewardConfig.MAX_PROJECT_NOTE_LENGTH)
        val stitches = draft.stitchNotes.limited(RewardConfig.MAX_PROJECT_NOTE_LENGTH)

        val projectId: Long
        if (draft.id == null) {
            // New projects start Planned or In progress; finishing always goes through completeProject.
            val status = if (draft.status == ProjectStatus.PLANNED) ProjectStatus.PLANNED else ProjectStatus.IN_PROGRESS
            projectId = store.insertProject(
                Project(
                    name = name,
                    description = description,
                    category = draft.category.name,
                    status = status.name,
                    progressPercent = 0,
                    targetDateEpochDay = draft.targetDateEpochDay,
                    hookSize = hook,
                    materialNotes = materials,
                    stitchNotes = stitches,
                    createdAt = now,
                    completedAt = null,
                    completionRewardGranted = false,
                    lastRewardedProgress = 0
                )
            )
        } else {
            val existing = store.getProject(draft.id) ?: return@inTransaction null
            store.updateProject(
                existing.copy(
                    name = name,
                    description = description,
                    category = draft.category.name,
                    targetDateEpochDay = draft.targetDateEpochDay,
                    hookSize = hook,
                    materialNotes = materials,
                    stitchNotes = stitches
                )
            )
            projectId = existing.id
        }

        val level = LevelCurve.levelForXp(profile.totalXp)
        ProjectResult(projectId, level, level, checks = engine.runChecks(profile))
    }

    /** Moves a Planned project to In progress. No reward. */
    suspend fun startProject(id: Long): Boolean = store.inTransaction {
        val project = store.getProject(id) ?: return@inTransaction false
        if (project.statusEnum != ProjectStatus.PLANNED) return@inTransaction false
        store.updateProject(project.copy(status = ProjectStatus.IN_PROGRESS.name))
        true
    }

    /**
     * Sets progress (0-100). Rules:
     *  - completed projects must be reopened first;
     *  - moving a Planned project forward makes it In progress;
     *  - forward moves are logged to the project history;
     *  - a reward is given only when progress passes the best previously rewarded value by
     *    MEANINGFUL_PROGRESS_STEP points. Moving back and forth never earns twice.
     */
    suspend fun setProjectProgress(id: Long, percent: Int): ProjectResult? = store.inTransaction {
        val profile = store.getProfile() ?: return@inTransaction null
        val project = store.getProject(id) ?: return@inTransaction null
        if (project.statusEnum == ProjectStatus.COMPLETED) return@inTransaction null

        val now = clock.nowMillis()
        val newPercent = percent.coerceIn(0, 100)
        val level = LevelCurve.levelForXp(profile.totalXp)
        if (newPercent == project.progressPercent) {
            return@inTransaction ProjectResult(id, level, level)
        }

        var updated = project.copy(progressPercent = newPercent)
        if (updated.statusEnum == ProjectStatus.PLANNED && newPercent > 0) {
            updated = updated.copy(status = ProjectStatus.IN_PROGRESS.name)
        }

        var rewarded = false
        if (newPercent > project.progressPercent) {
            rewarded = newPercent - project.lastRewardedProgress >= RewardConfig.MEANINGFUL_PROGRESS_STEP
            if (rewarded) updated = updated.copy(lastRewardedProgress = newPercent)
            store.insertProjectUpdate(
                ProjectUpdate(
                    projectId = id,
                    loggedAt = now,
                    fromPercent = project.progressPercent,
                    toPercent = newPercent,
                    rewarded = rewarded
                )
            )
        }
        store.updateProject(updated)

        val reward = if (rewarded) RewardConfig.PROJECT_UPDATE else RewardConfig.Reward(0, 0)
        val newProfile = profile.withReward(reward.xp, reward.yarn)
        if (newProfile != profile) store.updateProfile(newProfile)
        ProjectResult(
            projectId = id,
            levelBefore = level,
            levelAfter = LevelCurve.levelForXp(newProfile.totalXp),
            xpEarned = reward.xp,
            yarnEarned = reward.yarn,
            rewardedUpdate = rewarded,
            checks = engine.runChecks(newProfile)
        )
    }

    /**
     * Marks a project finished. The completion reward is granted only if this project has never
     * been rewarded before, so reopening and finishing again (or any edit) cannot pay twice.
     * Returns null if the project is missing or already completed.
     */
    suspend fun completeProject(id: Long): ProjectResult? = store.inTransaction {
        val profile = store.getProfile() ?: return@inTransaction null
        val project = store.getProject(id) ?: return@inTransaction null
        if (project.statusEnum == ProjectStatus.COMPLETED) return@inTransaction null

        val now = clock.nowMillis()
        var updated = project.copy(
            status = ProjectStatus.COMPLETED.name,
            progressPercent = 100,
            completedAt = now,
            lastRewardedProgress = 100
        )
        if (project.progressPercent < 100) {
            store.insertProjectUpdate(
                ProjectUpdate(
                    projectId = id,
                    loggedAt = now,
                    fromPercent = project.progressPercent,
                    toPercent = 100,
                    rewarded = false
                )
            )
        }

        var xp = 0
        var yarn = 0
        if (!project.completionRewardGranted) {
            xp = RewardConfig.PROJECT_COMPLETE.xp
            yarn = RewardConfig.PROJECT_COMPLETE.yarn
            updated = updated.copy(completionRewardGranted = true)
        }
        store.updateProject(updated)

        val newProfile = profile.withReward(xp, yarn)
        if (newProfile != profile) store.updateProfile(newProfile)
        ProjectResult(
            projectId = id,
            levelBefore = LevelCurve.levelForXp(profile.totalXp),
            levelAfter = LevelCurve.levelForXp(newProfile.totalXp),
            xpEarned = xp,
            yarnEarned = yarn,
            completedNow = true,
            checks = engine.runChecks(newProfile)
        )
    }

    /** Reopens a finished project as In progress. Never grants or removes anything. */
    suspend fun reopenProject(id: Long): Boolean = store.inTransaction {
        val project = store.getProject(id) ?: return@inTransaction false
        if (project.statusEnum != ProjectStatus.COMPLETED) return@inTransaction false
        store.updateProject(
            project.copy(status = ProjectStatus.IN_PROGRESS.name, completedAt = null)
        )
        true
    }

    suspend fun deleteProject(id: Long): Boolean = store.inTransaction {
        if (store.getProject(id) == null) return@inTransaction false
        store.deleteProject(id)
        true
    }

    // ------------------------------------------------------------------ quests

    /**
     * The quest board. An unclaimed completion from an earlier period stays claimable (progress
     * is never reset unexpectedly); otherwise the current period's state is shown.
     */
    suspend fun questBoard(): List<QuestItem> {
        val now = clock.nowMillis()
        val completions = store.allQuestCompletions()
        return QuestCatalog.all.map { definition ->
            val period = clock.periodFor(definition.repeat, now)
            val target = definition.rule.target
            val unclaimed = completions
                .filter { it.questKey == definition.key && it.claimedAt == null }
                .minByOrNull { it.completedAt }
            val thisPeriod = completions.firstOrNull {
                it.questKey == definition.key && it.periodKey == period.key
            }
            when {
                unclaimed != null ->
                    QuestItem(definition, QuestStatus.COMPLETED, target, target, unclaimed.periodKey)
                thisPeriod != null ->
                    QuestItem(definition, QuestStatus.CLAIMED, target, target, period.key)
                else -> {
                    val (current, _) = engine.questProgress(definition, period)
                    val status = if (current > 0) QuestStatus.IN_PROGRESS else QuestStatus.AVAILABLE
                    QuestItem(definition, status, current, target, period.key)
                }
            }
        }
    }

    /**
     * Claims the reward of a completed quest instance. It works exactly once: the claim flips
     * claimedAt in a single guarded UPDATE, so double taps and repeated events cannot pay twice.
     */
    suspend fun claimQuest(questKey: String, periodKey: String): ClaimResult? = store.inTransaction {
        val definition = QuestCatalog.find(questKey) ?: return@inTransaction null
        val profile = store.getProfile() ?: return@inTransaction null
        val completion = store.getQuestCompletion(questKey, periodKey) ?: return@inTransaction null
        if (completion.claimedAt != null) return@inTransaction null
        if (!store.markQuestClaimed(questKey, periodKey, clock.nowMillis())) return@inTransaction null

        val updated = profile.withReward(definition.reward.xp, definition.reward.yarn)
        store.updateProfile(updated)
        ClaimResult(
            questTitle = definition.title,
            xpEarned = definition.reward.xp,
            yarnEarned = definition.reward.yarn,
            levelBefore = LevelCurve.levelForXp(profile.totalXp),
            levelAfter = LevelCurve.levelForXp(updated.totalXp),
            checks = engine.runChecks(updated)
        )
    }

    /**
     * For self-reported quests ("I did this"). Marks the current instance completed (not yet
     * claimed). Returns null if the quest is not a manual one or is already done this period.
     */
    suspend fun markManualQuestDone(questKey: String): ChecksResult? = store.inTransaction {
        val definition = QuestCatalog.find(questKey) ?: return@inTransaction null
        if (definition.rule != QuestRule.Manual) return@inTransaction null
        val profile = store.getProfile() ?: return@inTransaction null
        val now = clock.nowMillis()
        val period = clock.periodFor(definition.repeat, now)
        val inserted = store.insertQuestCompletionIfAbsent(
            QuestCompletion(
                questKey = definition.key,
                periodKey = period.key,
                completedAt = now,
                claimedAt = null,
                associatedProjectId = null
            )
        )
        if (!inserted) return@inTransaction null
        engine.runChecks(profile)
    }

    // ------------------------------------------------------------------ achievements

    suspend fun achievements(): List<AchievementItem> {
        val unlocked = store.allAchievementUnlocks().associateBy { it.achievementKey }
        return AchievementCatalog.all.map { AchievementItem(it, unlocked[it.key]?.unlockedAt) }
    }

    // ------------------------------------------------------------------ wardrobe

    /**
     * Buys a cosmetic for the cat. See [buy] for the safety rules.
     */
    suspend fun purchaseItem(itemKey: String): PurchaseResult {
        val item = CosmeticCatalog.find(itemKey) ?: return PurchaseResult.UNKNOWN_ITEM
        return buy(item.key, item.cost, requiredLevel = 1)
    }

    /**
     * Shared purchase path for everything that costs yarn balls. Order matters (there is no
     * rollback in tests, and none is needed): every check happens before anything is written,
     * and the unique inventory index is the final guard, so a second purchase of the same item
     * can never charge twice.
     */
    private suspend fun buy(itemKey: String, cost: Int, requiredLevel: Int): PurchaseResult =
        store.inTransaction {
            val profile = store.getProfile() ?: return@inTransaction PurchaseResult.NO_PROFILE
            if (LevelCurve.levelForXp(profile.totalXp) < requiredLevel) {
                return@inTransaction PurchaseResult.LEVEL_TOO_LOW
            }
            if (store.allInventory().any { it.itemKey == itemKey }) {
                return@inTransaction PurchaseResult.ALREADY_OWNED
            }
            val updated = profile.trySpendYarn(cost)
                ?: return@inTransaction PurchaseResult.NOT_ENOUGH_YARN
            val inserted = store.insertInventoryItemIfAbsent(
                InventoryItem(itemKey = itemKey, purchasedAt = clock.nowMillis())
            )
            if (!inserted) return@inTransaction PurchaseResult.ALREADY_OWNED
            store.updateProfile(updated)
            PurchaseResult.SUCCESS
        }

    /** Wears an owned item, replacing whatever the cat wore in that slot. Free to repeat. */
    suspend fun equipItem(itemKey: String): Boolean = store.inTransaction {
        val item = CosmeticCatalog.find(itemKey) ?: return@inTransaction false
        if (store.allInventory().none { it.itemKey == itemKey }) return@inTransaction false
        store.setEquipped(item.slot.name, item.key)
        true
    }

    suspend fun unequipSlot(slot: AccessorySlot): Boolean = store.inTransaction {
        if (store.allEquipped().none { it.accessorySlot == slot.name }) return@inTransaction false
        store.clearEquipped(slot.name)
        true
    }

    /** Back to the default appearance. Owned items stay owned. */
    suspend fun unequipAll() = store.inTransaction {
        AccessorySlot.entries.forEach { store.clearEquipped(it.name) }
    }

    suspend fun wardrobe(): List<WardrobeItem> {
        val owned = store.allInventory().map { it.itemKey }.toSet()
        val worn = store.allEquipped().map { it.itemKey }.toSet()
        return CosmeticCatalog.all.map { item ->
            val isOwned = item.key in owned
            WardrobeItem(item, owned = isOwned, equipped = isOwned && item.key in worn)
        }
    }

    // ------------------------------------------------------------------ cozy room

    /** Buys a room decoration. Needs enough yarn balls AND the item's player level. */
    suspend fun purchaseRoomItem(itemKey: String): PurchaseResult {
        val item = RoomCatalog.find(itemKey) ?: return PurchaseResult.UNKNOWN_ITEM
        return buy(item.key, item.cost, item.requiredLevel)
    }

    /** Places an owned item in its spot, replacing whatever was there. Free to repeat. */
    suspend fun placeRoomItem(itemKey: String): Boolean = store.inTransaction {
        val item = RoomCatalog.find(itemKey) ?: return@inTransaction false
        if (store.allInventory().none { it.itemKey == itemKey }) return@inTransaction false
        store.setEquipped(item.slot.storageKey, item.key)
        true
    }

    suspend fun removeRoomSlot(slot: RoomSlot): Boolean = store.inTransaction {
        if (store.allEquipped().none { it.accessorySlot == slot.storageKey }) return@inTransaction false
        store.clearEquipped(slot.storageKey)
        true
    }

    suspend fun roomState(): RoomState {
        val level = store.getProfile()?.let { LevelCurve.levelForXp(it.totalXp) } ?: 1
        val owned = store.allInventory().map { it.itemKey }.toSet()
        val placed = store.allEquipped().map { it.itemKey }.toSet()
        val entries = RoomCatalog.all.map { item ->
            val isOwned = item.key in owned
            RoomEntry(
                item = item,
                owned = isOwned,
                placed = isOwned && item.key in placed,
                unlocked = level >= item.requiredLevel
            )
        }
        return RoomState(entries, level)
    }

    // ------------------------------------------------------------------ yarn stash

    /**
     * Adds a yarn color ([id] null) or edits one. The name is required; the amount is kept
     * between 0 and [StashLimits.MAX_AMOUNT]. Returns the color's id, or null if invalid.
     */
    suspend fun saveStashYarn(id: Long?, name: String, colorArgb: Int, amount: Int): Long? =
        store.inTransaction {
            val cleanName = cleanText(name, StashLimits.MAX_NAME_LENGTH) ?: return@inTransaction null
            val cleanAmount = amount.coerceIn(0, StashLimits.MAX_AMOUNT)
            if (id == null) {
                store.insertStashYarn(
                    StashYarn(
                        name = cleanName, colorArgb = colorArgb, amount = cleanAmount,
                        createdAt = clock.nowMillis()
                    )
                )
            } else {
                val existing = store.getStashYarn(id) ?: return@inTransaction null
                store.updateStashYarn(existing.copy(name = cleanName, colorArgb = colorArgb, amount = cleanAmount))
                id
            }
        }

    /** Adds or removes skeins (never below 0 or above the maximum). */
    suspend fun adjustStashAmount(id: Long, delta: Int): Boolean = store.inTransaction {
        val yarn = store.getStashYarn(id) ?: return@inTransaction false
        store.updateStashYarn(yarn.copy(amount = (yarn.amount.toLong() + delta).coerceIn(0L, StashLimits.MAX_AMOUNT.toLong()).toInt()))
        true
    }

    suspend fun deleteStashYarn(id: Long): Boolean = store.inTransaction {
        if (store.getStashYarn(id) == null) return@inTransaction false
        store.deleteStashYarn(id)
        true
    }

    /**
     * Makes the project's linked colors exactly [yarnIds] (unknown colors are ignored).
     * Returns null if the project doesn't exist. Linking two colors can complete a quest.
     */
    suspend fun setProjectYarns(projectId: Long, yarnIds: Set<Long>): ChecksResult? = store.inTransaction {
        val profile = store.getProfile() ?: return@inTransaction null
        if (store.getProject(projectId) == null) return@inTransaction null
        val existingColors = store.allStashYarn().map { it.id }.toSet()
        val target = yarnIds.filter { it in existingColors }.toSet()
        val current = store.allProjectYarnLinks().filter { it.projectId == projectId }.map { it.yarnId }.toSet()
        (current - target).forEach { store.deleteProjectYarnLink(projectId, it) }
        (target - current).forEach { store.insertProjectYarnLink(ProjectYarnLink(projectId, it)) }
        engine.runChecks(profile)
    }

    suspend fun stash(): List<StashItem> {
        val links = store.allProjectYarnLinks()
        return store.allStashYarn().map { yarn ->
            StashItem(yarn, links.filter { it.yarnId == yarn.id }.map { it.projectId }.sorted())
        }
    }

    // ------------------------------------------------------------------ stats

    suspend fun statsBundle(): StatsBundle = StatsCalculator.build(
        nowMillis = clock.nowMillis(),
        zone = clock.zone,
        sessions = store.allSessions(),
        projects = store.allProjects(),
        stitches = store.allStitches(),
        unlocks = store.allAchievementUnlocks(),
        stash = store.allStashYarn()
    )

    // ------------------------------------------------------------------ backup and restore

    suspend fun createBackup(): BackupSnapshot = store.inTransaction { store.snapshot() }

    /**
     * Replaces everything with [snapshot] in one transaction. The snapshot must already have
     * passed BackupCodec.validate. A final check guards against an empty or profile-less restore.
     */
    suspend fun restoreBackup(snapshot: BackupSnapshot) {
        if (snapshot.profile == null) throw BackupFormatException("This backup has no player profile in it.")
        store.inTransaction { store.replaceAll(snapshot) }
    }

    // ------------------------------------------------------------------ reset

    /** Deletes everything (after the UI has asked for explicit confirmation). */
    suspend fun resetProgress() = store.inTransaction { store.resetAllData() }

    // ------------------------------------------------------------------ helpers

    private fun cleanCatName(raw: String): String? = cleanText(raw, RewardConfig.MAX_CAT_NAME_LENGTH)

    private fun cleanStitchName(raw: String?): String? =
        raw?.let { cleanText(it, RewardConfig.MAX_STITCH_NAME_LENGTH) }

    private fun cleanText(raw: String, maxLength: Int): String? =
        raw.trim().replace(WHITESPACE, " ").take(maxLength).trim().ifEmpty { null }

    private fun String.limited(maxLength: Int): String = trim().take(maxLength)

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
