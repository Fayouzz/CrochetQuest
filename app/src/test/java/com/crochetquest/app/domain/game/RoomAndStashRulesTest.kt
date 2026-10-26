package com.crochetquest.app.domain.game

import com.crochetquest.app.domain.cosmetics.AccessorySlot
import com.crochetquest.app.domain.cosmetics.PurchaseResult
import com.crochetquest.app.domain.cosmetics.RoomAutoDecor
import com.crochetquest.app.domain.cosmetics.RoomCatalog
import com.crochetquest.app.domain.cosmetics.RoomSlot
import com.crochetquest.app.domain.projects.ProjectDraft
import com.crochetquest.app.domain.quests.QuestStatus
import com.crochetquest.app.domain.stash.StashLimits
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomAndStashRulesTest {
    private lateinit var store: FakeGameStore
    private lateinit var service: GameService

    @Before
    fun setUp() {
        store = FakeGameStore()
        service = GameService(store, TestClock())
    }

    private suspend fun start(yarn: Int = 0, xp: Int = 0) {
        service.createProfile("Mochi")
        store.profile = store.profile!!.copy(yarnBalance = yarn, totalXp = xp)
    }

    private val yarn get() = store.profile!!.yarnBalance

    // ================= room =================

    @Test
    fun `room catalog keys are unique and never clash with accessory keys`() {
        val keys = RoomCatalog.all.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        val cosmeticKeys = com.crochetquest.app.domain.cosmetics.CosmeticCatalog.all.map { it.key }
        assertTrue(keys.intersect(cosmeticKeys.toSet()).isEmpty())
        // every spot has something to buy, including at level 1
        RoomSlot.entries.forEach { slot -> assertTrue(RoomCatalog.all.any { it.slot == slot }) }
        assertTrue(RoomCatalog.all.any { it.requiredLevel == 1 })
    }

    @Test
    fun `room items are gated by player level as well as yarn`() = runTest {
        start(yarn = 500, xp = 0) // level 1, plenty of yarn
        assertEquals(PurchaseResult.LEVEL_TOO_LOW, service.purchaseRoomItem("room_rug_striped"))
        assertEquals(500, yarn)
        assertEquals(PurchaseResult.SUCCESS, service.purchaseRoomItem("room_rug_round"))
        assertEquals(475, yarn)

        store.profile = store.profile!!.copy(totalXp = 250) // level 3
        assertEquals(PurchaseResult.SUCCESS, service.purchaseRoomItem("room_rug_striped"))
    }

    @Test
    fun `room purchases never charge twice or go negative`() = runTest {
        start(yarn = 30)
        assertEquals(PurchaseResult.SUCCESS, service.purchaseRoomItem("room_window_round")) // 30
        assertEquals(0, yarn)
        assertEquals(PurchaseResult.ALREADY_OWNED, service.purchaseRoomItem("room_window_round"))
        assertEquals(PurchaseResult.NOT_ENOUGH_YARN, service.purchaseRoomItem("room_plant_sprout"))
        assertEquals(0, yarn)
        assertEquals(PurchaseResult.UNKNOWN_ITEM, service.purchaseRoomItem("nope"))
    }

    @Test
    fun `placing needs ownership, one item per spot, and removing works`() = runTest {
        start(yarn = 500, xp = 250)
        assertFalse(service.placeRoomItem("room_rug_round"))
        service.purchaseRoomItem("room_rug_round")
        service.purchaseRoomItem("room_rug_striped")

        assertTrue(service.placeRoomItem("room_rug_round"))
        assertTrue(service.placeRoomItem("room_rug_striped")) // replaces the round rug
        var state = service.roomState()
        assertEquals(setOf("room_rug_striped"), state.placedKeys)

        assertTrue(service.removeRoomSlot(RoomSlot.RUG))
        assertFalse(service.removeRoomSlot(RoomSlot.RUG))
        state = service.roomState()
        assertTrue(state.placedKeys.isEmpty())
        assertTrue(state.entries.first { it.item.key == "room_rug_round" }.owned) // still owned
    }

    @Test
    fun `room entries report unlocked by level`() = runTest {
        start(xp = 0)
        var state = service.roomState()
        assertEquals(1, state.level)
        assertTrue(state.entries.first { it.item.key == "room_rug_round" }.unlocked)
        assertFalse(state.entries.first { it.item.key == "room_lamp_star" }.unlocked)
        store.profile = store.profile!!.copy(totalXp = 700) // level 5
        state = service.roomState()
        assertTrue(state.entries.all { it.unlocked })
    }

    @Test
    fun `taking the cat's accessories off leaves the room alone, and room items stay out of the wardrobe`() = runTest {
        start(yarn = 500)
        service.purchaseRoomItem("room_plant_sprout")
        service.placeRoomItem("room_plant_sprout")
        service.purchaseItem("pink_bow")
        service.equipItem("pink_bow")

        service.unequipAll()

        assertEquals(setOf("room_plant_sprout"), service.roomState().placedKeys)
        assertTrue(service.wardrobe().none { it.equipped })
        assertEquals(com.crochetquest.app.domain.cosmetics.CosmeticCatalog.all.size, service.wardrobe().size)
    }

    @Test
    fun `free decorations appear as the player levels up`() {
        assertTrue(RoomAutoDecor.unlockedAt(1).isEmpty())
        assertEquals(listOf(RoomAutoDecor.BUNTING), RoomAutoDecor.unlockedAt(2).map { it.key })
        assertEquals(3, RoomAutoDecor.unlockedAt(6).size)
        assertEquals(RoomAutoDecor.BUNTING, RoomAutoDecor.nextAfter(1)!!.key)
        assertNull(RoomAutoDecor.nextAfter(6))
    }

    // ================= stash =================

    @Test
    fun `adding a yarn color needs a name and keeps the amount in range`() = runTest {
        start()
        assertNull(service.saveStashYarn(null, "   ", 0xFF112233.toInt(), 3))
        val id = service.saveStashYarn(null, "  Sage   green ", 0xFF9DBA94.toInt(), 5000)!!
        val yarn = store.stash.single()
        assertEquals("Sage green", yarn.name)
        assertEquals(StashLimits.MAX_AMOUNT, yarn.amount)
        assertEquals(id, yarn.id)

        service.saveStashYarn(id, "Sage", 0xFF9DBA94.toInt(), -4) // edit
        assertEquals("Sage", store.stash.single().name)
        assertEquals(0, store.stash.single().amount)
        assertNull(service.saveStashYarn(999, "Ghost", 0, 1))
    }

    @Test
    fun `amounts adjust by skeins and stay within 0 to the maximum`() = runTest {
        start()
        val id = service.saveStashYarn(null, "Cream", 0, 1)!!
        assertTrue(service.adjustStashAmount(id, 2))
        assertEquals(3, store.stash.single().amount)
        service.adjustStashAmount(id, -10)
        assertEquals(0, store.stash.single().amount)
        service.adjustStashAmount(id, 5000)
        assertEquals(StashLimits.MAX_AMOUNT, store.stash.single().amount)
        assertFalse(service.adjustStashAmount(404, 1))
    }

    @Test
    fun `colors link to projects and the link set can be changed`() = runTest {
        start()
        val project = service.saveProject(ProjectDraft(name = "Hat"))!!.projectId
        val a = service.saveStashYarn(null, "A", 1, 1)!!
        val b = service.saveStashYarn(null, "B", 2, 1)!!
        val c = service.saveStashYarn(null, "C", 3, 1)!!

        assertNotNull(service.setProjectYarns(project, setOf(a, b, 12345))) // unknown id ignored
        assertEquals(setOf(a, b), store.links.map { it.yarnId }.toSet())
        assertEquals(listOf(project), service.stash().first { it.yarn.id == a }.projectIds)

        service.setProjectYarns(project, setOf(b, c))
        assertEquals(setOf(b, c), store.links.map { it.yarnId }.toSet())
        assertTrue(service.stash().first { it.yarn.id == a }.projectIds.isEmpty())

        assertNull(service.setProjectYarns(999, setOf(a)))
    }

    @Test
    fun `linking two colors to a project completes the color combination quest`() = runTest {
        start()
        val project = service.saveProject(ProjectDraft(name = "Hat"))!!.projectId
        val a = service.saveStashYarn(null, "A", 1, 1)!!
        val b = service.saveStashYarn(null, "B", 2, 1)!!
        suspend fun status() = service.questBoard().first { it.definition.key == "creative_color_combo" }.status

        service.setProjectYarns(project, setOf(a))
        assertEquals(QuestStatus.AVAILABLE, status())
        service.setProjectYarns(project, setOf(a, b))
        assertEquals(QuestStatus.COMPLETED, status())
    }

    @Test
    fun `deleting a color or a project removes its links`() = runTest {
        start()
        val p1 = service.saveProject(ProjectDraft(name = "One"))!!.projectId
        val p2 = service.saveProject(ProjectDraft(name = "Two"))!!.projectId
        val a = service.saveStashYarn(null, "A", 1, 1)!!
        val b = service.saveStashYarn(null, "B", 2, 1)!!
        service.setProjectYarns(p1, setOf(a, b))
        service.setProjectYarns(p2, setOf(a))

        assertTrue(service.deleteStashYarn(a))
        assertEquals(setOf(b), store.links.map { it.yarnId }.toSet())
        assertFalse(service.deleteStashYarn(a))

        service.deleteProject(p1)
        assertTrue(store.links.isEmpty())
        assertEquals(1, store.stash.size) // the colors themselves are kept
    }

    @Test
    fun `reset also clears the stash`() = runTest {
        start()
        service.saveStashYarn(null, "A", 1, 1)
        service.resetProgress()
        assertTrue(store.stash.isEmpty() && store.links.isEmpty())
    }
}
