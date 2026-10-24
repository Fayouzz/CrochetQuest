package com.crochetquest.app.domain.game

import com.crochetquest.app.domain.cosmetics.AccessorySlot
import com.crochetquest.app.domain.cosmetics.CosmeticCatalog
import com.crochetquest.app.domain.cosmetics.PurchaseResult
import com.crochetquest.app.domain.projects.ProjectCategory
import com.crochetquest.app.domain.projects.ProjectDraft
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CosmeticsRulesTest {
    private lateinit var store: FakeGameStore
    private lateinit var clock: TestClock
    private lateinit var service: GameService

    @Before
    fun setUp() {
        store = FakeGameStore()
        clock = TestClock()
        service = GameService(store, clock)
    }

    private val yarn get() = store.profile!!.yarnBalance

    /** Gives the player [amount] more yarn by editing the profile directly (test setup only). */
    private fun grantYarn(amount: Int) {
        store.profile = store.profile!!.copy(yarnBalance = yarn + amount)
    }

    private suspend fun ownedKeys() = service.wardrobe().filter { it.owned }.map { it.item.key }.toSet()
    private suspend fun equippedKeys() = service.wardrobe().filter { it.equipped }.map { it.item.key }.toSet()

    // ---- catalog ----

    @Test
    fun `catalog matches the spec prices and has unique keys`() {
        val keys = CosmeticCatalog.all.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertEquals(20, CosmeticCatalog.find("pink_bow")!!.cost)
        assertEquals(35, CosmeticCatalog.find("flower_crown")!!.cost)
        assertEquals(40, CosmeticCatalog.find("cozy_scarf")!!.cost)
        assertEquals(50, CosmeticCatalog.find("wizard_hat")!!.cost)
        assertEquals(60, CosmeticCatalog.find("small_sweater")!!.cost)
    }

    @Test
    fun `one cheap item is reachable after a little play`() {
        val cheapest = CosmeticCatalog.all.minOf { it.cost }
        val oneSession = RewardConfig.STARTING_YARN + RewardConfig.LOG_SESSION.yarn
        assertTrue(cheapest <= oneSession)
        assertTrue(cheapest > RewardConfig.STARTING_YARN) // but not free from the very start
    }

    @Test
    fun `the wardrobe starts with nothing owned or worn (default appearance)`() = runTest {
        service.createProfile("Mochi")
        val wardrobe = service.wardrobe()
        assertEquals(CosmeticCatalog.all.size, wardrobe.size)
        assertTrue(wardrobe.none { it.owned || it.equipped })
    }

    // ---- purchasing ----

    @Test
    fun `purchase is rejected when funds are insufficient and changes nothing`() = runTest {
        service.createProfile("Mochi")
        val before = store.profile
        assertEquals(PurchaseResult.NOT_ENOUGH_YARN, service.purchaseItem("wizard_hat"))
        assertEquals(before, store.profile)
        assertTrue(store.inventory.isEmpty())
    }

    @Test
    fun `a successful purchase deducts the cost and grants permanent ownership`() = runTest {
        service.createProfile("Mochi")
        grantYarn(50)
        val before = yarn
        assertEquals(PurchaseResult.SUCCESS, service.purchaseItem("pink_bow"))
        assertEquals(before - 20, yarn)
        assertTrue("pink_bow" in ownedKeys())
        assertEquals(clock.nowMillis(), store.inventory.single().purchasedAt)
    }

    @Test
    fun `buying the same item twice never charges twice`() = runTest {
        service.createProfile("Mochi")
        grantYarn(100)
        service.purchaseItem("pink_bow")
        val afterFirst = yarn
        assertEquals(PurchaseResult.ALREADY_OWNED, service.purchaseItem("pink_bow"))
        assertEquals(PurchaseResult.ALREADY_OWNED, service.purchaseItem("pink_bow"))
        assertEquals(afterFirst, yarn)
        assertEquals(1, store.inventory.size)
    }

    @Test
    fun `an exact balance can buy and leaves zero, never negative`() = runTest {
        service.createProfile("Mochi")
        grantYarn(5) // 10 + 5 = 15 = bell collar
        assertEquals(PurchaseResult.SUCCESS, service.purchaseItem("bell_collar"))
        assertEquals(0, yarn)
        assertEquals(PurchaseResult.NOT_ENOUGH_YARN, service.purchaseItem("pink_bow"))
        assertEquals(0, yarn)
    }

    @Test
    fun `unknown items and a missing profile are rejected`() = runTest {
        assertEquals(PurchaseResult.UNKNOWN_ITEM, service.purchaseItem("nope"))
        assertEquals(PurchaseResult.NO_PROFILE, service.purchaseItem("pink_bow"))
        service.createProfile("Mochi")
        assertEquals(PurchaseResult.UNKNOWN_ITEM, service.purchaseItem("nope"))
    }

    @Test
    fun `shopping never changes xp, level or yarn earned`() = runTest {
        service.createProfile("Mochi")
        service.logSession()
        grantYarn(100)
        val profile = store.profile!!
        service.purchaseItem("cozy_scarf")
        val after = store.profile!!
        assertEquals(profile.totalXp, after.totalXp)
        assertEquals(profile.lifetimeYarnEarned, after.lifetimeYarnEarned)
    }

    // ---- equipping ----

    @Test
    fun `only owned items can be worn`() = runTest {
        service.createProfile("Mochi")
        assertFalse(service.equipItem("pink_bow"))
        assertFalse(service.equipItem("nope"))
        assertTrue(store.equipped.isEmpty())
    }

    @Test
    fun `equipping and unequipping works and is free`() = runTest {
        service.createProfile("Mochi")
        grantYarn(100)
        service.purchaseItem("pink_bow")
        val balance = yarn
        assertTrue(service.equipItem("pink_bow"))
        assertEquals(setOf("pink_bow"), equippedKeys())
        assertTrue(service.unequipSlot(AccessorySlot.HEAD))
        assertTrue(equippedKeys().isEmpty())
        assertTrue("pink_bow" in ownedKeys()) // still owned
        assertEquals(balance, yarn)
        assertFalse(service.unequipSlot(AccessorySlot.HEAD)) // nothing to remove
    }

    @Test
    fun `switching between owned items never charges again`() = runTest {
        service.createProfile("Mochi")
        grantYarn(200)
        service.purchaseItem("pink_bow")
        service.purchaseItem("flower_crown")
        val balance = yarn
        repeat(3) {
            service.equipItem("pink_bow")
            service.equipItem("flower_crown")
        }
        assertEquals(balance, yarn)
        assertEquals(setOf("flower_crown"), equippedKeys())
    }

    @Test
    fun `one item per slot, but different slots combine`() = runTest {
        service.createProfile("Mochi")
        grantYarn(300)
        listOf("pink_bow", "wizard_hat", "cozy_scarf", "round_glasses", "small_sweater")
            .forEach { service.purchaseItem(it) }
        service.equipItem("pink_bow")
        service.equipItem("wizard_hat") // replaces the bow (both HEAD)
        service.equipItem("cozy_scarf")
        service.equipItem("round_glasses")
        service.equipItem("small_sweater")
        assertEquals(
            setOf("wizard_hat", "cozy_scarf", "round_glasses", "small_sweater"),
            equippedKeys()
        )
    }

    @Test
    fun `unequipAll returns to the default look but keeps ownership`() = runTest {
        service.createProfile("Mochi")
        grantYarn(100)
        service.purchaseItem("pink_bow")
        service.purchaseItem("bell_collar")
        service.equipItem("pink_bow")
        service.equipItem("bell_collar")
        service.unequipAll()
        assertTrue(equippedKeys().isEmpty())
        assertEquals(setOf("pink_bow", "bell_collar"), ownedKeys())
    }

    // ---- reset ----

    @Test
    fun `reset deletes everything and the game can start fresh`() = runTest {
        service.createProfile("Mochi")
        service.logSession(newStitchName = "Shell")
        val id = service.saveProject(ProjectDraft(name = "Hat", category = ProjectCategory.TINY))!!.projectId
        service.completeProject(id)
        grantYarn(100)
        service.purchaseItem("pink_bow")
        service.equipItem("pink_bow")

        service.resetProgress()

        assertNull(store.profile)
        assertTrue(store.sessions.isEmpty() && store.stitches.isEmpty() && store.projects.isEmpty())
        assertTrue(store.completions.isEmpty() && store.unlocks.isEmpty())
        assertTrue(store.inventory.isEmpty() && store.equipped.isEmpty())

        val fresh = service.createProfile("Biscuit")
        assertEquals(0, fresh.totalXp)
        assertEquals(RewardConfig.STARTING_YARN, fresh.yarnBalance)
        assertTrue(service.achievements().none { it.isUnlocked })
        assertTrue(service.wardrobe().none { it.owned })
    }
}
