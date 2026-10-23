package com.crochetquest.app.domain.game

import com.crochetquest.app.domain.projects.ProjectCategory
import com.crochetquest.app.domain.projects.ProjectDraft
import com.crochetquest.app.domain.quests.QuestCatalog
import com.crochetquest.app.domain.quests.QuestItem
import com.crochetquest.app.domain.quests.QuestStatus
import com.crochetquest.app.domain.quests.RepeatPolicy
import com.crochetquest.app.domain.quests.pickFeaturedQuest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuestRulesTest {
    private lateinit var store: FakeGameStore
    private lateinit var clock: TestClock
    private lateinit var service: GameService

    @Before
    fun setUp() {
        store = FakeGameStore()
        clock = TestClock()
        service = GameService(store, clock)
    }

    private suspend fun board(): Map<String, QuestItem> =
        service.questBoard().associateBy { it.definition.key }

    private suspend fun item(key: String): QuestItem = board().getValue(key)

    private val xp get() = store.profile!!.totalXp
    private val yarn get() = store.profile!!.yarnBalance

    private suspend fun finishProject(category: ProjectCategory): Long {
        val id = service.saveProject(ProjectDraft(name = "P", category = category))!!.projectId
        service.completeProject(id)
        return id
    }

    @Test
    fun `catalog keys are unique and every category has quests`() {
        val keys = QuestCatalog.all.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertEquals(3, QuestCatalog.all.map { it.category }.toSet().size)
        assertTrue(QuestCatalog.all.any { it.repeat == RepeatPolicy.ONCE })
        assertTrue(QuestCatalog.all.any { it.repeat == RepeatPolicy.DAILY })
        assertTrue(QuestCatalog.all.any { it.repeat == RepeatPolicy.WEEKLY })
    }

    @Test
    fun `quests start available`() = runTest {
        service.createProfile("Mochi")
        assertTrue(service.questBoard().all { it.status == QuestStatus.AVAILABLE })
    }

    @Test
    fun `logging a session completes the daily quest but does not claim it`() = runTest {
        service.createProfile("Mochi")
        val result = service.logSession()!!
        assertTrue(result.checks.completedQuestTitles.contains("Pick up your hook"))
        assertEquals(QuestStatus.COMPLETED, item("cozy_daily_session").status)
        // only the plain session reward was paid so far
        assertEquals(RewardConfig.LOG_SESSION.xp, xp)
    }

    @Test
    fun `claiming pays the quest reward exactly once`() = runTest {
        service.createProfile("Mochi")
        service.logSession()
        val xpBefore = xp
        val yarnBefore = yarn
        val quest = item("cozy_daily_session")

        val claim = service.claimQuest(quest.definition.key, quest.periodKey)
        assertNotNull(claim)
        assertEquals(xpBefore + quest.definition.reward.xp, xp)
        assertEquals(yarnBefore + quest.definition.reward.yarn, yarn)
        assertEquals(QuestStatus.CLAIMED, item("cozy_daily_session").status)

        // double tap / repeated event
        assertNull(service.claimQuest(quest.definition.key, quest.periodKey))
        assertEquals(xpBefore + quest.definition.reward.xp, xp)
        assertEquals(yarnBefore + quest.definition.reward.yarn, yarn)
    }

    @Test
    fun `building the board repeatedly never grants anything`() = runTest {
        service.createProfile("Mochi")
        service.logSession()
        val xpBefore = xp
        val completionsBefore = store.completions.size
        repeat(5) { service.questBoard() }
        assertEquals(xpBefore, xp)
        assertEquals(completionsBefore, store.completions.size)
    }

    @Test
    fun `claiming an incomplete or unknown quest is rejected`() = runTest {
        service.createProfile("Mochi")
        val xpBefore = xp
        assertNull(service.claimQuest("cozy_daily_session", "2026-01-01"))
        assertNull(service.claimQuest("does_not_exist", "once"))
        assertEquals(xpBefore, xp)
    }

    @Test
    fun `claiming with the wrong period is rejected`() = runTest {
        service.createProfile("Mochi")
        service.logSession()
        assertNull(service.claimQuest("cozy_daily_session", "1999-01-01"))
    }

    @Test
    fun `a daily quest resets the next day and can be completed again`() = runTest {
        service.createProfile("Mochi")
        service.logSession()
        val first = item("cozy_daily_session")
        service.claimQuest(first.definition.key, first.periodKey)

        clock.advanceDays(1)
        assertEquals(QuestStatus.AVAILABLE, item("cozy_daily_session").status)
        service.logSession()
        val second = item("cozy_daily_session")
        assertEquals(QuestStatus.COMPLETED, second.status)
        assertFalse(first.periodKey == second.periodKey)
        assertNotNull(service.claimQuest(second.definition.key, second.periodKey))
    }

    @Test
    fun `an unclaimed reward from an earlier day stays claimable`() = runTest {
        service.createProfile("Mochi")
        service.logSession()
        val earlier = item("cozy_daily_session")
        clock.advanceDays(3)

        val later = item("cozy_daily_session")
        assertEquals(QuestStatus.COMPLETED, later.status)
        assertEquals(earlier.periodKey, later.periodKey)
        assertNotNull(service.claimQuest(later.definition.key, later.periodKey))
        assertEquals(QuestStatus.AVAILABLE, item("cozy_daily_session").status)
    }

    @Test
    fun `weekly session quest shows progress and completes at three`() = runTest {
        service.createProfile("Mochi")
        service.logSession()
        service.logSession()
        val partial = item("cozy_weekly_sessions")
        assertEquals(QuestStatus.IN_PROGRESS, partial.status)
        assertEquals(2, partial.current)
        assertEquals(3, partial.target)

        service.logSession()
        assertEquals(QuestStatus.COMPLETED, item("cozy_weekly_sessions").status)
    }

    @Test
    fun `weekly quests reset on Monday`() = runTest {
        service.createProfile("Mochi")
        repeat(3) { service.logSession() }
        val week1 = item("cozy_weekly_sessions")
        assertEquals(QuestStatus.COMPLETED, week1.status)
        service.claimQuest(week1.definition.key, week1.periodKey)

        clock.advanceDays(2) // Saturday, same week
        assertEquals(QuestStatus.CLAIMED, item("cozy_weekly_sessions").status)
        clock.advanceDays(2) // Monday
        val week2 = item("cozy_weekly_sessions")
        assertEquals(QuestStatus.AVAILABLE, week2.status)
        assertFalse(week1.periodKey == week2.periodKey)
    }

    @Test
    fun `new stitch quest completes only when a new stitch is recorded`() = runTest {
        service.createProfile("Mochi")
        service.logSession()
        assertEquals(QuestStatus.AVAILABLE, item("creative_new_stitch").status)
        service.logSession(newStitchName = "Puff stitch")
        assertEquals(QuestStatus.COMPLETED, item("creative_new_stitch").status)
    }

    @Test
    fun `manual quests complete only on the explicit action`() = runTest {
        service.createProfile("Mochi")
        service.logSession()
        assertEquals(QuestStatus.AVAILABLE, item("creative_technique").status)

        assertNotNull(service.markManualQuestDone("creative_technique"))
        assertEquals(QuestStatus.COMPLETED, item("creative_technique").status)
        assertNull(service.markManualQuestDone("creative_technique")) // already done this week

        val quest = item("creative_technique")
        assertNotNull(service.claimQuest(quest.definition.key, quest.periodKey))
        assertNull(service.markManualQuestDone("creative_technique"))
    }

    @Test
    fun `non-manual quests cannot be marked done by hand`() = runTest {
        service.createProfile("Mochi")
        assertNull(service.markManualQuestDone("cozy_daily_session"))
        assertNull(service.markManualQuestDone("nope"))
        assertTrue(store.completions.isEmpty())
    }

    // ---- project-linked quests ----

    @Test
    fun `creating a project completes the start-a-project milestone`() = runTest {
        service.createProfile("Mochi")
        assertEquals(QuestStatus.AVAILABLE, item("milestone_start_project").status)
        service.saveProject(ProjectDraft(name = "Hat"))
        assertEquals(QuestStatus.COMPLETED, item("milestone_start_project").status)
    }

    @Test
    fun `progress on a project completes the weekly progress quest`() = runTest {
        service.createProfile("Mochi")
        val id = service.saveProject(ProjectDraft(name = "Hat"))!!.projectId
        assertEquals(QuestStatus.AVAILABLE, item("cozy_weekly_progress").status)
        service.setProjectProgress(id, 5) // small, unrewarded, but still progress
        assertEquals(QuestStatus.COMPLETED, item("cozy_weekly_progress").status)
    }

    @Test
    fun `color notes complete the color combination quest`() = runTest {
        service.createProfile("Mochi")
        service.saveProject(ProjectDraft(name = "Hat"))
        assertEquals(QuestStatus.AVAILABLE, item("creative_color_combo").status)
        service.saveProject(ProjectDraft(name = "Hat 2", materialNotes = "sage + cream"))
        assertEquals(QuestStatus.COMPLETED, item("creative_color_combo").status)
    }

    @Test
    fun `finishing projects drives the milestone quests`() = runTest {
        service.createProfile("Mochi")
        finishProject(ProjectCategory.OTHER)
        assertEquals(QuestStatus.COMPLETED, item("milestone_first_finish").status)
        assertEquals(QuestStatus.IN_PROGRESS, item("milestone_finish_three").status)
        assertEquals(1, item("milestone_finish_three").current)
        finishProject(ProjectCategory.OTHER)
        finishProject(ProjectCategory.OTHER)
        assertEquals(QuestStatus.COMPLETED, item("milestone_finish_three").status)
    }

    @Test
    fun `tiny and gift quests need the matching category`() = runTest {
        service.createProfile("Mochi")
        finishProject(ProjectCategory.OTHER)
        assertEquals(QuestStatus.AVAILABLE, item("creative_tiny").status)
        assertEquals(QuestStatus.AVAILABLE, item("milestone_handmade_gift").status)
        finishProject(ProjectCategory.TINY)
        finishProject(ProjectCategory.GIFTS)
        assertEquals(QuestStatus.COMPLETED, item("creative_tiny").status)
        assertEquals(QuestStatus.COMPLETED, item("milestone_handmade_gift").status)
    }

    @Test
    fun `reopening a project does not create duplicate quest completions`() = runTest {
        service.createProfile("Mochi")
        val id = finishProject(ProjectCategory.OTHER)
        val count = store.completions.size
        service.reopenProject(id)
        service.completeProject(id)
        assertEquals(count, store.completions.size)
        val quest = item("milestone_first_finish")
        assertNotNull(service.claimQuest(quest.definition.key, quest.periodKey))
        assertNull(service.claimQuest(quest.definition.key, quest.periodKey))
    }

    @Test
    fun `a claim can level the player up`() = runTest {
        service.createProfile("Mochi")
        // 9 sessions over 3 days = 90 XP, then claim a 10 XP quest to reach exactly 100
        repeat(9) { i ->
            if (i > 0 && i % RewardConfig.MAX_REWARDED_SESSIONS_PER_DAY == 0) clock.advanceDays(1)
            service.logSession()
        }
        assertEquals(90, xp)
        val quest = item("cozy_daily_session")
        assertEquals(QuestStatus.COMPLETED, quest.status)
        val claim = service.claimQuest(quest.definition.key, quest.periodKey)!!
        assertTrue(claim.leveledUp)
        assertEquals(2, claim.levelAfter)
    }

    @Test
    fun `featured quest prefers claimable then in progress then daily`() = runTest {
        service.createProfile("Mochi")
        assertEquals("cozy_daily_session", pickFeaturedQuest(service.questBoard())!!.definition.key)

        service.logSession(newStitchName = "Shell") // completes the daily + new-stitch quests
        val featured = pickFeaturedQuest(service.questBoard())!!
        assertEquals(QuestStatus.COMPLETED, featured.status)
    }

    @Test
    fun `featured quest is null when nothing is left`() {
        assertNull(pickFeaturedQuest(emptyList()))
    }
}
