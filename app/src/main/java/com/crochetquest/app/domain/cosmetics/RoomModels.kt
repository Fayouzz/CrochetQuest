package com.crochetquest.app.domain.cosmetics

/** Spots in the cozy room. One item can be placed per spot. */
enum class RoomSlot(val label: String) {
    WINDOW("Window"),
    SHELF("Shelf"),
    RUG("Rug"),
    LAMP("Lamp"),
    PLANT("Plant");

    /** Equipped-table key. The "ROOM_" prefix keeps these apart from the cat's accessory slots. */
    val storageKey: String get() = "ROOM_$name"
}

data class RoomItem(
    val key: String,
    val name: String,
    val description: String,
    val cost: Int,
    /** Player level needed before this can be bought, so the room fills up as you level. */
    val requiredLevel: Int,
    val slot: RoomSlot
)

/** An item as shown in the room shop. */
data class RoomEntry(
    val item: RoomItem,
    val owned: Boolean,
    val placed: Boolean,
    val unlocked: Boolean
)

data class RoomState(val entries: List<RoomEntry>, val level: Int) {
    val placedKeys: Set<String> get() = entries.filter { it.placed }.map { it.item.key }.toSet()
}

/** Free decorations that simply appear as the player levels up. Nothing to buy. */
data class AutoDecor(val key: String, val name: String, val level: Int)

object RoomAutoDecor {
    const val BUNTING = "auto_bunting"
    const val PICTURE = "auto_picture"
    const val STAR_LIGHTS = "auto_star_lights"

    val all: List<AutoDecor> = listOf(
        AutoDecor(BUNTING, "Cozy bunting", 2),
        AutoDecor(PICTURE, "Little framed picture", 4),
        AutoDecor(STAR_LIGHTS, "Star lights", 6)
    )

    fun unlockedAt(level: Int): List<AutoDecor> = all.filter { level >= it.level }
    fun nextAfter(level: Int): AutoDecor? = all.firstOrNull { it.level > level }
}

/** Bundled room shop. Keys are unique across the whole app (they share the inventory table). */
object RoomCatalog {
    val all: List<RoomItem> = listOf(
        RoomItem("room_rug_round", "Round rug", "Soft, cream and perfectly circular.", 25, 1, RoomSlot.RUG),
        RoomItem("room_plant_sprout", "Little sprout", "A tiny green friend in a pot.", 20, 1, RoomSlot.PLANT),
        RoomItem("room_window_round", "Round window", "A porthole to the sky outside.", 30, 1, RoomSlot.WINDOW),
        RoomItem("room_shelf_wood", "Wooden shelf", "A neat little shelf for odds and ends.", 35, 2, RoomSlot.SHELF),
        RoomItem("room_lamp_paper", "Paper lamp", "Glows warm when evening comes.", 30, 2, RoomSlot.LAMP),
        RoomItem("room_plant_fern", "Fern", "Leafy and calm.", 35, 2, RoomSlot.PLANT),
        RoomItem("room_rug_striped", "Striped rug", "Hand-woven look, in rosy stripes.", 50, 3, RoomSlot.RUG),
        RoomItem("room_window_garden", "Garden window", "With a flower box on the sill.", 60, 3, RoomSlot.WINDOW),
        RoomItem("room_shelf_yarn", "Yarn shelf", "Shows off your favorite yarn balls.", 65, 4, RoomSlot.SHELF),
        RoomItem("room_plant_flowers", "Flower pot", "Pink and yellow blooms.", 45, 4, RoomSlot.PLANT),
        RoomItem("room_lamp_star", "Star lamp", "A little star that lights the room.", 55, 5, RoomSlot.LAMP)
    )

    private val byKey = all.associateBy { it.key }
    fun find(key: String): RoomItem? = byKey[key]
}
