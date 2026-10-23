package com.crochetquest.app.domain.achievements

import com.crochetquest.app.domain.projects.ProjectCategory
import com.crochetquest.app.domain.quests.IconKey

sealed interface AchievementRule {
    data class SessionsLogged(val count: Int) : AchievementRule
    data class QuestsCompleted(val count: Int) : AchievementRule
    data class ProjectsFinished(val count: Int) : AchievementRule
    data class StitchesRecorded(val count: Int) : AchievementRule
    data class FinishedInCategory(val category: ProjectCategory) : AchievementRule
    data class YarnEarned(val total: Int) : AchievementRule
    data class ReachLevel(val level: Int) : AchievementRule
}

data class AchievementDefinition(
    val key: String,
    val name: String,
    val description: String,
    val icon: IconKey,
    val rule: AchievementRule
)

/** An achievement as shown in the collection. [unlockedAt] is null while still locked. */
data class AchievementItem(val definition: AchievementDefinition, val unlockedAt: Long?) {
    val isUnlocked: Boolean get() = unlockedAt != null
}

object AchievementCatalog {
    val all: List<AchievementDefinition> = listOf(
        AchievementDefinition(
            "first_stitch", "First Stitch", "Log your first crochet session.",
            IconKey.STITCH, AchievementRule.SessionsLogged(1)
        ),
        AchievementDefinition(
            "quest_starter", "Quest Starter", "Complete your first quest.",
            IconKey.STAR, AchievementRule.QuestsCompleted(1)
        ),
        AchievementDefinition(
            "first_creation", "First Creation", "Finish your first project.",
            IconKey.TROPHY, AchievementRule.ProjectsFinished(1)
        ),
        AchievementDefinition(
            "stitch_explorer", "Stitch Explorer", "Record three different stitches.",
            IconKey.STITCH, AchievementRule.StitchesRecorded(3)
        ),
        AchievementDefinition(
            "tiny_but_mighty", "Tiny but Mighty", "Finish a project in the Tiny creations category.",
            IconKey.FLOWER, AchievementRule.FinishedInCategory(ProjectCategory.TINY)
        ),
        AchievementDefinition(
            "gift_giver", "Gift Giver", "Finish a project in the Gifts category.",
            IconKey.GIFT, AchievementRule.FinishedInCategory(ProjectCategory.GIFTS)
        ),
        AchievementDefinition(
            "threes_a_charm", "Three's a Charm", "Finish three projects.",
            IconKey.HEART, AchievementRule.ProjectsFinished(3)
        ),
        AchievementDefinition(
            "yarn_collector", "Yarn Collector", "Earn 100 yarn balls in total.",
            IconKey.YARN, AchievementRule.YarnEarned(100)
        ),
        AchievementDefinition(
            "cozy_creator", "Cozy Creator", "Reach level 4.",
            IconKey.TROPHY, AchievementRule.ReachLevel(4)
        )
    )
}
