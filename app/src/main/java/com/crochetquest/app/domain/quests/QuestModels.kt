package com.crochetquest.app.domain.quests

import com.crochetquest.app.domain.game.RewardConfig
import com.crochetquest.app.domain.projects.ProjectCategory

/** Small icon identifiers; the UI maps them to pixel-art sprites. */
enum class IconKey { YARN, STAR, GIFT, FLOWER, HEART, TROPHY, STITCH }

enum class QuestCategory(val label: String) {
    COZY("Cozy Quests"),
    CREATIVE("Creative Quests"),
    MILESTONE("Project Milestones")
}

/** How often a quest instance resets. Completion rows are per (quest, period). */
enum class RepeatPolicy(val label: String) {
    ONCE("One-time"),
    DAILY("Resets daily"),
    WEEKLY("Resets weekly")
}

enum class QuestStatus(val label: String) {
    AVAILABLE("Available"),
    IN_PROGRESS("In progress"),
    COMPLETED("Ready to claim"),
    CLAIMED("Claimed")
}

/** Deterministic, local eligibility rules. Counts come from persisted records. */
sealed interface QuestRule {
    val target: Int

    data class SessionsInPeriod(override val target: Int) : QuestRule
    data class ProjectProgressInPeriod(override val target: Int) : QuestRule
    data class NewStitchesInPeriod(override val target: Int) : QuestRule
    data class ProjectsExist(override val target: Int) : QuestRule
    data class ProjectsFinished(override val target: Int) : QuestRule
    data class FinishedInCategory(val category: ProjectCategory) : QuestRule {
        override val target: Int = 1
    }
    data object ColorNotesAdded : QuestRule {
        override val target: Int = 1
    }
    /** Self-reported: the player taps "I did this" (for things the app cannot observe). */
    data object Manual : QuestRule {
        override val target: Int = 1
    }
}

data class QuestDefinition(
    val key: String,
    val title: String,
    val description: String,
    val category: QuestCategory,
    val icon: IconKey,
    val reward: RewardConfig.Reward,
    val repeat: RepeatPolicy,
    val rule: QuestRule
)

/** A quest as shown on the board. [periodKey] identifies the instance to claim. */
data class QuestItem(
    val definition: QuestDefinition,
    val status: QuestStatus,
    val current: Int,
    val target: Int,
    val periodKey: String
)

/** Bundled quest set. Stable keys identify quests; titles can change freely. */
object QuestCatalog {
    val all: List<QuestDefinition> = listOf(
        // ---- Cozy ----
        QuestDefinition(
            key = "cozy_daily_session",
            title = "Pick up your hook",
            description = "Spend a little time on any crochet project. Even a few stitches count.",
            category = QuestCategory.COZY, icon = IconKey.YARN,
            reward = RewardConfig.Reward(xp = 10, yarn = 5),
            repeat = RepeatPolicy.DAILY, rule = QuestRule.SessionsInPeriod(1)
        ),
        QuestDefinition(
            key = "cozy_weekly_sessions",
            title = "A cozy week",
            description = "Log three crochet sessions this week, whenever suits you.",
            category = QuestCategory.COZY, icon = IconKey.HEART,
            reward = RewardConfig.WEEKLY_QUEST,
            repeat = RepeatPolicy.WEEKLY, rule = QuestRule.SessionsInPeriod(3)
        ),
        QuestDefinition(
            key = "cozy_weekly_progress",
            title = "Nudge a project forward",
            description = "Record progress on any work-in-progress project this week.",
            category = QuestCategory.COZY, icon = IconKey.STAR,
            reward = RewardConfig.Reward(xp = 10, yarn = 5),
            repeat = RepeatPolicy.WEEKLY, rule = QuestRule.ProjectProgressInPeriod(1)
        ),
        // ---- Creative ----
        QuestDefinition(
            key = "creative_new_stitch",
            title = "Try a new stitch",
            description = "Record a stitch you have never logged before.",
            category = QuestCategory.CREATIVE, icon = IconKey.STITCH,
            reward = RewardConfig.Reward(xp = 10, yarn = 5),
            repeat = RepeatPolicy.WEEKLY, rule = QuestRule.NewStitchesInPeriod(1)
        ),
        QuestDefinition(
            key = "creative_color_combo",
            title = "Dream up a color combination",
            description = "Write a color combination in a project's yarn notes, or link two yarn colors to a project.",
            category = QuestCategory.CREATIVE, icon = IconKey.YARN,
            reward = RewardConfig.Reward(xp = 10, yarn = 5),
            repeat = RepeatPolicy.ONCE, rule = QuestRule.ColorNotesAdded
        ),
        QuestDefinition(
            key = "creative_tiny",
            title = "Make something tiny",
            description = "Finish a project in the Tiny creations category.",
            category = QuestCategory.CREATIVE, icon = IconKey.FLOWER,
            reward = RewardConfig.Reward(xp = 15, yarn = 10),
            repeat = RepeatPolicy.ONCE, rule = QuestRule.FinishedInCategory(ProjectCategory.TINY)
        ),
        QuestDefinition(
            key = "creative_technique",
            title = "Learn a new technique",
            description = "Try a technique you have not used before, then tap \"I did this\".",
            category = QuestCategory.CREATIVE, icon = IconKey.STITCH,
            reward = RewardConfig.Reward(xp = 15, yarn = 10),
            repeat = RepeatPolicy.WEEKLY, rule = QuestRule.Manual
        ),
        // ---- Milestones ----
        QuestDefinition(
            key = "milestone_start_project",
            title = "Start a new project",
            description = "Add a project to your tracker.",
            category = QuestCategory.MILESTONE, icon = IconKey.STAR,
            reward = RewardConfig.Reward(xp = 10, yarn = 5),
            repeat = RepeatPolicy.ONCE, rule = QuestRule.ProjectsExist(1)
        ),
        QuestDefinition(
            key = "milestone_first_finish",
            title = "Finish your first project",
            description = "Mark a project as finished.",
            category = QuestCategory.MILESTONE, icon = IconKey.TROPHY,
            reward = RewardConfig.Reward(xp = 20, yarn = 15),
            repeat = RepeatPolicy.ONCE, rule = QuestRule.ProjectsFinished(1)
        ),
        QuestDefinition(
            key = "milestone_finish_three",
            title = "Finish three projects",
            description = "Three finished makes a lovely collection.",
            category = QuestCategory.MILESTONE, icon = IconKey.TROPHY,
            reward = RewardConfig.Reward(xp = 30, yarn = 20),
            repeat = RepeatPolicy.ONCE, rule = QuestRule.ProjectsFinished(3)
        ),
        QuestDefinition(
            key = "milestone_handmade_gift",
            title = "Make a handmade gift",
            description = "Finish a project in the Gifts category.",
            category = QuestCategory.MILESTONE, icon = IconKey.GIFT,
            reward = RewardConfig.HANDMADE_GIFT_BONUS,
            repeat = RepeatPolicy.ONCE, rule = QuestRule.FinishedInCategory(ProjectCategory.GIFTS)
        )
    )

    private val byKey = all.associateBy { it.key }
    fun find(key: String): QuestDefinition? = byKey[key]
}

/** Home screen pick: something to claim, else something underway, else a fresh daily quest. */
fun pickFeaturedQuest(items: List<QuestItem>): QuestItem? =
    items.firstOrNull { it.status == QuestStatus.COMPLETED }
        ?: items.firstOrNull { it.status == QuestStatus.IN_PROGRESS }
        ?: items.firstOrNull { it.status == QuestStatus.AVAILABLE && it.definition.repeat == RepeatPolicy.DAILY }
        ?: items.firstOrNull { it.status == QuestStatus.AVAILABLE }
