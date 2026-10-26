package com.crochetquest.app.domain.game

import com.crochetquest.app.domain.backup.BackupCodec
import com.crochetquest.app.domain.backup.BackupFormat
import com.crochetquest.app.domain.backup.BackupSnapshot
import com.crochetquest.app.domain.backup.BackupFormatException
import com.crochetquest.app.domain.projects.ProjectCategory
import com.crochetquest.app.domain.projects.ProjectDraft
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupRestoreTest {

    /** Plays a little: profile, session, stitch, project (finished), color, purchases, room. */
    private suspend fun playSomething(service: GameService, store: FakeGameStore) {
        service.createProfile("Mochi")
        service.logSession(durationMinutes = 30, newStitchName = "Shell")
        val id = service.saveProject(ProjectDraft(name = "Bunny", category = ProjectCategory.GIFTS))!!.projectId
        service.completeProject(id)
        val yarnId = service.saveStashYarn(null, "Sage", 0xFF9DBA94.toInt(), 2)!!
        service.setProjectYarns(id, setOf(yarnId))
        store.profile = store.profile!!.copy(yarnBalance = 300, totalXp = 300)
        service.purchaseItem("pink_bow")
        service.equipItem("pink_bow")
        service.purchaseRoomItem("room_rug_round")
        service.placeRoomItem("room_rug_round")
    }

    @Test
    fun `backup, wipe, restore gives back the exact same game (JSON and CSV)`() = runTest {
        for (format in BackupFormat.entries) {
            val store = FakeGameStore()
            val service = GameService(store, TestClock())
            playSomething(service, store)
            val before = store.snapshot()

            val text = BackupCodec.encode(service.createBackup(), format, 1L)
            service.resetProgress()
            assertNull(store.profile)

            service.restoreBackup(BackupCodec.decode(text).snapshot)
            assertEquals("format=$format", before, store.snapshot())
        }
    }

    @Test
    fun `restoring replaces the current game instead of merging with it`() = runTest {
        val first = FakeGameStore()
        val firstService = GameService(first, TestClock())
        playSomething(firstService, first)
        val backup = firstService.createBackup()

        val store = FakeGameStore()
        val service = GameService(store, TestClock())
        service.createProfile("Someone else")
        service.saveProject(ProjectDraft(name = "Will be replaced"))
        service.saveStashYarn(null, "Also replaced", 1, 1)

        service.restoreBackup(backup)

        assertEquals("Mochi", store.profile!!.catName)
        assertTrue(store.projects.values.none { it.name == "Will be replaced" })
        assertTrue(store.stash.none { it.name == "Also replaced" })
        assertEquals(backup, store.snapshot())
    }

    @Test
    fun `the game keeps working after a restore without id clashes or double rewards`() = runTest {
        val store = FakeGameStore()
        val service = GameService(store, TestClock())
        playSomething(service, store)
        val backup = service.createBackup()
        service.resetProgress()
        service.restoreBackup(backup)

        // new records get fresh ids
        val newProject = service.saveProject(ProjectDraft(name = "New"))!!.projectId
        assertTrue(store.projects.keys.size == 2 && newProject !in backup.projects.map { it.id })
        val newYarn = service.saveStashYarn(null, "Fresh", 1, 1)!!
        assertTrue(newYarn !in backup.stash.map { it.id })

        // the finished project stays rewarded: reopening and finishing again pays nothing
        val finished = backup.projects.first { it.completionRewardGranted }.id
        val xp = store.profile!!.totalXp
        service.reopenProject(finished)
        assertEquals(0, service.completeProject(finished)!!.xpEarned)
        assertEquals(xp, store.profile!!.totalXp)

        // owned items stay owned and are not charged again
        val yarn = store.profile!!.yarnBalance
        assertEquals(com.crochetquest.app.domain.cosmetics.PurchaseResult.ALREADY_OWNED, service.purchaseItem("pink_bow"))
        assertEquals(yarn, store.profile!!.yarnBalance)
    }

    @Test
    fun `a profile-less snapshot cannot be restored and changes nothing`() = runTest {
        val store = FakeGameStore()
        val service = GameService(store, TestClock())
        playSomething(service, store)
        val before = store.snapshot()
        try {
            service.restoreBackup(BackupSnapshot(profile = null))
            fail("should be refused")
        } catch (e: BackupFormatException) {
            // expected
        }
        assertEquals(before, store.snapshot())
    }

    @Test
    fun `a bad file is refused before anything is touched`() = runTest {
        val store = FakeGameStore()
        val service = GameService(store, TestClock())
        playSomething(service, store)
        val before = store.snapshot()
        try {
            BackupCodec.decode("{\"app\":\"crochet-quest\",\"formatVersion\":1,\"tables\":{\"profile\":[{\"catName\":\"x\"}]}}")
            fail("missing numbers should be refused")
        } catch (e: BackupFormatException) {
            assertNotNull(e.message)
        }
        assertEquals(before, store.snapshot())
    }
}
