package com.crochetquest.app.domain.game

import com.crochetquest.app.domain.achievements.AchievementCatalog
import com.crochetquest.app.domain.projects.ProjectCategory
import com.crochetquest.app.domain.projects.ProjectDraft
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AchievementRulesTest {
    private lateinit var store: FakeGameStore
    private lateinit var clock: TestClock
    private lateinit var service: GameService

    @Before
    fun setUp() {
        store = FakeGameStore()
        clock = TestClock()
        service = GameService(store, clock)
    }

    private suspend fun unlockedKeys(): Set<String> =
        service.achievements().filter { it.isUnlocked }.map { it.definition.key }.toSet()

    private suspend fun finishProject(category: ProjectCategory = ProjectCategory.OTHER): Long {
        val id = service.saveProject(ProjectDraft(name = "P", category = category))!!.projectId
        service.completeProject(id)
        return id
    }

    @Test
    fun `catalog has unique keys`() {
        val keys = AchievementCatalog.all.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `everything starts locked`() = runTest {
        service.createProfile("Mochi")
        assertTrue(unlockedKeys().isEmpty())
        assertEquals(AchievementCatalog.all.size, service.achievements().size)
    }

    @Test
    fun `first session unlocks First Stitch with an unlock date`() = runTest {
        service.createProfile("Mochi")
        val result = service.logSession()!!
        assertTrue(result.checks.unlockedAchievementNames.contains("First Stitch"))
        val item = service.achievements().first { it.definition.key == "first_stitch" }
        assertTrue(item.isUnlocked)
        assertEquals(clock.nowMillis(), item.unlockedAt)
    }

    @Test
    fun `achievements are never awarded twice or re-dated`() = runTest {
        service.createProfile("Mochi")
        service.logSession()
        val firstDate = service.achievements().first { it.definition.key == "first_stitch" }.unlockedAt
        clock.advanceDays(2)
        val again = service.logSession()!!
        assertFalse(again.checks.unlockedAchievementNames.contains("First Stitch"))
        assertEquals(firstDate, service.achievements().first { it.definition.key == "first_stitch" }.unlockedAt)
        assertEquals(1, store.unlocks.count { it.achievementKey == "first_stitch" })
    }

    @Test
    fun `completing a quest unlocks Quest Starter`() = runTest {
        service.createProfile("Mochi")
        val result = service.logSession()!!
        assertTrue(result.checks.completedQuestTitles.isNotEmpty())
        assertTrue("quest_starter" in unlockedKeys())
    }

    @Test
    fun `Stitch Explorer needs three distinct stitches`() = runTest {
        service.createProfile("Mochi")
        service.logSession(newStitchName = "Shell")
        service.logSession(newStitchName = "shell") // duplicate
        service.logSession(newStitchName = "Puff")
        assertFalse("stitch_explorer" in unlockedKeys())
        clock.advanceDays(1)
        service.logSession(newStitchName = "Bobble")
        assertTrue("stitch_explorer" in unlockedKeys())
    }

    @Test
    fun `finishing a project unlocks First Creation`() = runTest {
        service.createProfile("Mochi")
        assertFalse("first_creation" in unlockedKeys())
        finishProject()
        assertTrue("first_creation" in unlockedKeys())
    }

    @Test
    fun `category achievements need the matching category`() = runTest {
        service.createProfile("Mochi")
        finishProject(ProjectCategory.OTHER)
        assertFalse("tiny_but_mighty" in unlockedKeys())
        assertFalse("gift_giver" in unlockedKeys())
        finishProject(ProjectCategory.TINY)
        assertTrue("tiny_but_mighty" in unlockedKeys())
        assertFalse("gift_giver" in unlockedKeys())
        finishProject(ProjectCategory.GIFTS)
        assertTrue("gift_giver" in unlockedKeys())
    }

    @Test
    fun `Three's a Charm needs three finished projects`() = runTest {
        service.createProfile("Mochi")
        finishProject()
        finishProject()
        assertFalse("threes_a_charm" in unlockedKeys())
        finishProject()
        assertTrue("threes_a_charm" in unlockedKeys())
    }

    @Test
    fun `reopening and finishing the same project does not count twice`() = runTest {
        service.createProfile("Mochi")
        val id = finishProject()
        repeat(3) {
            service.reopenProject(id)
            service.completeProject(id)
        }
        assertFalse("threes_a_charm" in unlockedKeys())
    }

    @Test
    fun `Yarn Collector uses yarn earned, not the current balance`() = runTest {
        service.createProfile("Mochi")
        // 6 days x 3 sessions x 5 yarn = 90 yarn earned, then spend most of it
        repeat(18) { i ->
            if (i > 0 && i % RewardConfig.MAX_REWARDED_SESSIONS_PER_DAY == 0) clock.advanceDays(1)
            service.logSession()
        }
        service.spendYarn(store.profile!!.yarnBalance - 1)
        assertFalse("yarn_collector" in unlockedKeys())
        assertEquals(90, store.profile!!.lifetimeYarnEarned)

        clock.advanceDays(1)
        repeat(2) { service.logSession() } // +10 -> 100 earned in total
        assertTrue("yarn_collector" in unlockedKeys())
        assertTrue(store.profile!!.yarnBalance < 100) // balance is low, achievement still earned
    }

    @Test
    fun `Cozy Creator unlocks on reaching level 4`() = runTest {
        service.createProfile("Mochi")
        // 450 XP needed; project completions are the quick way: 30 XP each + quests via claims
        var days = 0
        while (store.profile!!.totalXp < 450) {
            repeat(RewardConfig.MAX_REWARDED_SESSIONS_PER_DAY) { service.logSession() }
            clock.advanceDays(1)
            days++
            check(days < 100)
        }
        assertEquals(4, LevelCurve.levelForXp(store.profile!!.totalXp))
        assertTrue("cozy_creator" in unlockedKeys())
    }

    @Test
    fun `no achievements are granted without a profile or events`() = runTest {
        assertNull(service.logSession())
        assertTrue(unlockedKeys().isEmpty())
        assertNotNull(service.achievements())
    }
}
