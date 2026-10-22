package com.crochetquest.app.domain.game

import com.crochetquest.app.data.local.PlayerProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameServiceTest {
    private lateinit var store: FakeGameStore
    private lateinit var clock: TestClock
    private lateinit var service: GameService

    @Before
    fun setUp() {
        store = FakeGameStore()
        clock = TestClock()
        service = GameService(store, clock)
    }

    private val profile: PlayerProfile get() = store.profile!!

    // ---- profile ----

    @Test
    fun `new profile starts at zero xp with starting yarn`() = runTest {
        val created = service.createProfile("Mochi")
        assertEquals("Mochi", created.catName)
        assertEquals(0, created.totalXp)
        assertEquals(RewardConfig.STARTING_YARN, created.yarnBalance)
        assertEquals(1, LevelCurve.levelForXp(created.totalXp))
    }

    @Test
    fun `createProfile is idempotent and never resets progress`() = runTest {
        service.createProfile("Mochi")
        service.logSession()
        val again = service.createProfile("Other")
        assertEquals("Mochi", again.catName)
        assertEquals(RewardConfig.LOG_SESSION.xp, profile.totalXp)
    }

    @Test
    fun `cat name is trimmed, defaulted and length limited`() = runTest {
        assertEquals("Biscuit", service.createProfile("  Biscuit  ").catName)

        val other = FakeGameStore()
        val blank = GameService(other, clock).createProfile("   ")
        assertEquals(RewardConfig.DEFAULT_CAT_NAME, blank.catName)

        val long = FakeGameStore()
        val truncated = GameService(long, clock).createProfile("x".repeat(60))
        assertEquals(RewardConfig.MAX_CAT_NAME_LENGTH, truncated.catName.length)
    }

    @Test
    fun `renameCat rejects blank names and keeps the old one`() = runTest {
        service.createProfile("Mochi")
        assertFalse(service.renameCat("   "))
        assertEquals("Mochi", profile.catName)
        assertTrue(service.renameCat("  Noodle "))
        assertEquals("Noodle", profile.catName)
    }

    @Test
    fun `logging a session without a profile returns null`() = runTest {
        assertNull(service.logSession())
        assertTrue(store.sessions.isEmpty())
    }

    // ---- XP and yarn ----

    @Test
    fun `a session awards the configured xp and yarn`() = runTest {
        service.createProfile("Mochi")
        val result = service.logSession(durationMinutes = 30)!!
        assertTrue(result.sessionRewarded)
        assertEquals(RewardConfig.LOG_SESSION.xp, result.xpEarned)
        assertEquals(RewardConfig.LOG_SESSION.yarn, result.yarnEarned)
        assertEquals(RewardConfig.LOG_SESSION.xp, profile.totalXp)
        assertEquals(RewardConfig.STARTING_YARN + RewardConfig.LOG_SESSION.yarn, profile.yarnBalance)
        assertEquals(30, store.sessions.single().durationMinutes)
    }

    @Test
    fun `invalid durations are not stored`() = runTest {
        service.createProfile("Mochi")
        service.logSession(durationMinutes = -5)
        service.logSession(durationMinutes = 99999)
        assertTrue(store.sessions.all { it.durationMinutes == null })
    }

    @Test
    fun `sessions beyond the daily limit are saved but earn nothing`() = runTest {
        service.createProfile("Mochi")
        val results = (1..5).map { service.logSession()!! }

        val rewarded = RewardConfig.MAX_REWARDED_SESSIONS_PER_DAY
        assertEquals(rewarded, results.count { it.sessionRewarded })
        assertFalse(results.last().sessionRewarded)
        assertEquals(0, results.last().xpEarned)
        assertEquals(rewarded * RewardConfig.LOG_SESSION.xp, profile.totalXp)
        assertEquals(5, store.sessions.size)
        assertEquals(rewarded, service.rewardedSessionsToday())
    }

    @Test
    fun `the daily limit resets on the next day`() = runTest {
        service.createProfile("Mochi")
        repeat(RewardConfig.MAX_REWARDED_SESSIONS_PER_DAY + 1) { service.logSession() }
        clock.advanceDays(1)
        val next = service.logSession()!!
        assertTrue(next.sessionRewarded)
        assertEquals(1, service.rewardedSessionsToday())
    }

    @Test
    fun `level up is reported exactly when total xp crosses a threshold`() = runTest {
        service.createProfile("Mochi")
        val results = (0 until 10).map { i ->
            if (i > 0 && i % RewardConfig.MAX_REWARDED_SESSIONS_PER_DAY == 0) clock.advanceDays(1)
            service.logSession()!!
        }
        assertFalse(results[8].leveledUp) // 90 XP
        assertTrue(results[9].leveledUp)  // 100 XP
        assertEquals(2, results[9].levelAfter)
        assertEquals(100, profile.totalXp)
    }

    // ---- stitches ----

    @Test
    fun `a new stitch earns a one-time bonus`() = runTest {
        service.createProfile("Mochi")
        val result = service.logSession(newStitchName = "Granny Square")!!
        assertEquals("Granny Square", result.newStitchName)
        assertEquals(RewardConfig.LOG_SESSION.xp + RewardConfig.NEW_STITCH.xp, result.xpEarned)
        assertEquals(RewardConfig.LOG_SESSION.yarn + RewardConfig.NEW_STITCH.yarn, result.yarnEarned)
        assertEquals(1, store.stitches.size)
    }

    @Test
    fun `the same stitch is never rewarded twice, ignoring case and spacing`() = runTest {
        service.createProfile("Mochi")
        service.logSession(newStitchName = "Granny Square")
        val xpBefore = profile.totalXp
        val again = service.logSession(newStitchName = "  granny    SQUARE ")!!

        assertNull(again.newStitchName)
        assertNotNull(again.knownStitchName)
        assertEquals(RewardConfig.LOG_SESSION.xp, again.xpEarned)
        assertEquals(xpBefore + RewardConfig.LOG_SESSION.xp, profile.totalXp)
        assertEquals(1, store.stitches.size)
    }

    @Test
    fun `blank stitch names are ignored`() = runTest {
        service.createProfile("Mochi")
        val result = service.logSession(newStitchName = "   ")!!
        assertNull(result.newStitchName)
        assertNull(result.knownStitchName)
        assertTrue(store.stitches.isEmpty())
    }

    @Test
    fun `a new stitch still earns its bonus after the daily session limit`() = runTest {
        service.createProfile("Mochi")
        repeat(RewardConfig.MAX_REWARDED_SESSIONS_PER_DAY) { service.logSession() }
        val result = service.logSession(newStitchName = "Magic Ring")!!
        assertFalse(result.sessionRewarded)
        assertEquals(RewardConfig.NEW_STITCH.xp, result.xpEarned)
    }

    // ---- spending ----

    @Test
    fun `spending yarn deducts the cost`() = runTest {
        service.createProfile("Mochi")
        val before = profile.yarnBalance
        assertTrue(service.spendYarn(4))
        assertEquals(before - 4, profile.yarnBalance)
    }

    @Test
    fun `spending more than the balance is rejected and changes nothing`() = runTest {
        service.createProfile("Mochi")
        val before = profile
        assertFalse(service.spendYarn(before.yarnBalance + 1))
        assertEquals(before, profile)
    }

    @Test
    fun `balance can reach zero but never go negative`() = runTest {
        service.createProfile("Mochi")
        assertTrue(service.spendYarn(profile.yarnBalance))
        assertEquals(0, profile.yarnBalance)
        assertFalse(service.spendYarn(1))
        assertEquals(0, profile.yarnBalance)
    }

    @Test
    fun `zero or negative costs are rejected`() = runTest {
        service.createProfile("Mochi")
        val before = profile
        assertFalse(service.spendYarn(0))
        assertFalse(service.spendYarn(-10))
        assertEquals(before, profile)
    }

    @Test
    fun `xp is unaffected by spending yarn`() = runTest {
        service.createProfile("Mochi")
        service.logSession()
        val xp = profile.totalXp
        service.spendYarn(5)
        assertEquals(xp, profile.totalXp)
        assertEquals(1, LevelCurve.levelForXp(profile.totalXp))
    }
}
