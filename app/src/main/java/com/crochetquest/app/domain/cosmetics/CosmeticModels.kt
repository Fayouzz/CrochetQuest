package com.crochetquest.app.domain.cosmetics

/**
 * Where an accessory sits on the cat. A cat wears at most one item per slot, and the enum order
 * is also the drawing order (body first, head last) so layers stack correctly.
 */
enum class AccessorySlot(val label: String) {
    BODY("Body"),
    NECK("Neck"),
    FACE("Face"),
    HEAD("Head")
}

data class CosmeticItem(
    val key: String,
    val name: String,
    val description: String,
    val cost: Int,
    val slot: AccessorySlot
)

/** An item as shown in the wardrobe. Equipped implies owned. */
data class WardrobeItem(val item: CosmeticItem, val owned: Boolean, val equipped: Boolean)

enum class PurchaseResult { SUCCESS, NOT_ENOUGH_YARN, ALREADY_OWNED, UNKNOWN_ITEM, NO_PROFILE }

/** Bundled shop. Stable keys identify items; names and prices can change freely. */
object CosmeticCatalog {
    val all: List<CosmeticItem> = listOf(
        CosmeticItem("bell_collar", "Bell collar", "A tiny jingly bell. Quiet, of course.", 15, AccessorySlot.NECK),
        CosmeticItem("pink_bow", "Pink bow", "A sweet bow for the top of the head.", 20, AccessorySlot.HEAD),
        CosmeticItem("round_glasses", "Round glasses", "For serious pattern reading.", 25, AccessorySlot.FACE),
        CosmeticItem("flower_crown", "Flower crown", "Fresh blooms in a tiny ring.", 35, AccessorySlot.HEAD),
        CosmeticItem("cozy_scarf", "Cozy scarf", "Long, striped and very warm.", 40, AccessorySlot.NECK),
        CosmeticItem("wizard_hat", "Wizard hat", "Pointy, starry, a little magical.", 50, AccessorySlot.HEAD),
        CosmeticItem("small_sweater", "Small sweater", "Hand-knit look, naturally.", 60, AccessorySlot.BODY)
    )

    private val byKey = all.associateBy { it.key }
    fun find(key: String): CosmeticItem? = byKey[key]
}
