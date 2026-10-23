package com.crochetquest.app.domain.game

import com.crochetquest.app.data.local.AchievementUnlock
import com.crochetquest.app.data.local.PlayerProfile
import com.crochetquest.app.data.local.QuestCompletion
import com.crochetquest.app.domain.achievements.AchievementCatalog
import com.crochetquest.app.domain.achievements.AchievementDefinition
import com.crochetquest.app.domain.achievements.AchievementRule
import com.crochetquest.app.domain.quests.QuestCatalog
import com.crochetquest.app.domain.quests.QuestDefinition
import com.crochetquest.app.domain.quests.QuestPeriod
import com.crochetquest.app.domain.quests.QuestRule
import com.crochetquest.app.domain.quests.periodFor

/** What a game event unlocked along the way; used for friendly messages. */
data class ChecksResult(
    val completedQuestTitles: List<String> = emptyList(),
    val unlockedAchievementNames: List<String> = emptyList()
) {
    companion object {
        val NONE = ChecksResult()
    }
}

/**
 * Detects quests that became complete and achievements that became earned.
 * It runs only after real events (a session, a project change, a claim), inside the event's
 * transaction. It is never triggered by screens being opened or recomposed.
 * Completing a quest or unlocking an achievement writes a unique row, so repeats are impossible.
 */
class ProgressEngine(private val store: GameStore, private val clock: GameClock) {

    suspend fun runChecks(profile: PlayerProfile): ChecksResult {
        val now = clock.nowMillis()
        val quests = checkQuests(now)          // first: "Quest Starter" depends on completions
        val achievements = checkAchievements(profile, now)
        return ChecksResult(quests.map { it.title }, achievements.map { it.name })
    }

    /** Current / target progress of a quest within [period]. */
    suspend fun questProgress(definition: QuestDefinition, period: QuestPeriod): Pair<Int, Int> {
        val rule = definition.rule
        val current = when (rule) {
            is QuestRule.SessionsInPeriod ->
                store.countSessionsBetween(period.startMillis, period.endMillis)
            is QuestRule.ProjectProgressInPeriod ->
                store.countProjectUpdatesBetween(period.startMillis, period.endMillis)
            is QuestRule.NewStitchesInPeriod ->
                store.countStitchesBetween(period.startMillis, period.endMillis)
            is QuestRule.ProjectsExist -> store.countProjects()
            is QuestRule.ProjectsFinished -> store.countFinishedProjects()
            is QuestRule.FinishedInCategory -> store.countFinishedProjectsInCategory(rule.category.name)
            QuestRule.ColorNotesAdded -> store.countProjectsWithMaterialNotes()
            QuestRule.Manual -> 0
        }
        return current.coerceAtMost(rule.target) to rule.target
    }

    private suspend fun checkQuests(now: Long): List<QuestDefinition> {
        val newlyCompleted = mutableListOf<QuestDefinition>()
        for (definition in QuestCatalog.all) {
            if (definition.rule == QuestRule.Manual) continue // completed by the player, explicitly
            val period = clock.periodFor(definition.repeat, now)
            if (store.getQuestCompletion(definition.key, period.key) != null) continue
            val (current, target) = questProgress(definition, period)
            if (current >= target) {
                val inserted = store.insertQuestCompletionIfAbsent(
                    QuestCompletion(
                        questKey = definition.key,
                        periodKey = period.key,
                        completedAt = now,
                        claimedAt = null,
                        associatedProjectId = null
                    )
                )
                if (inserted) newlyCompleted += definition
            }
        }
        return newlyCompleted
    }

    private suspend fun checkAchievements(profile: PlayerProfile, now: Long): List<AchievementDefinition> {
        val unlocked = store.allAchievementUnlocks().map { it.achievementKey }.toSet()
        val newlyUnlocked = mutableListOf<AchievementDefinition>()
        for (definition in AchievementCatalog.all) {
            if (definition.key in unlocked) continue
            if (isSatisfied(definition.rule, profile)) {
                val inserted = store.insertAchievementIfAbsent(
                    AchievementUnlock(achievementKey = definition.key, unlockedAt = now)
                )
                if (inserted) newlyUnlocked += definition
            }
        }
        return newlyUnlocked
    }

    private suspend fun isSatisfied(rule: AchievementRule, profile: PlayerProfile): Boolean = when (rule) {
        is AchievementRule.SessionsLogged -> store.countSessionsBetween(0L, Long.MAX_VALUE) >= rule.count
        is AchievementRule.QuestsCompleted -> store.countQuestCompletions() >= rule.count
        is AchievementRule.ProjectsFinished -> store.countFinishedProjects() >= rule.count
        is AchievementRule.StitchesRecorded -> store.countStitches() >= rule.count
        is AchievementRule.FinishedInCategory ->
            store.countFinishedProjectsInCategory(rule.category.name) >= 1
        is AchievementRule.YarnEarned -> profile.lifetimeYarnEarned >= rule.total
        is AchievementRule.ReachLevel -> LevelCurve.levelForXp(profile.totalXp) >= rule.level
    }
}
