package com.crochetquest.app.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Accessory layers, drawn on the same 16x20 grid as the cat ([CatCanvas]) so they line up pixel
 * for pixel. Symmetric items are authored as a left half (8 wide) and mirrored.
 * Rows not listed are transparent. An unknown item key simply has no layer (the cat stays cute).
 */
object CosmeticSprites {
    private const val BLANK_HALF = "........"
    private const val BLANK_FULL = "................"

    private fun halfLayer(rows: Map<Int, String>, palette: Map<Char, Color>): PixelSprite =
        PixelSprite.mirrored(List(CatCanvas.HEIGHT) { rows[it] ?: BLANK_HALF }, palette)

    private fun fullLayer(rows: Map<Int, String>, palette: Map<Char, Color>): PixelSprite =
        PixelSprite(List(CatCanvas.HEIGHT) { rows[it] ?: BLANK_FULL }, palette)

    private val gold = Color(0xFFF2C94C)

    /** Closed eyes: paints the eye pixels body-colored for a moment. */
    val blink: PixelSprite = halfLayer(
        mapOf(10 to "....c..."),
        mapOf('c' to Color(0xFFF6D3A8))
    )

    /** Drifting "z z" above a napping cat. */
    val zzz: PixelSprite = fullLayer(
        mapOf(
            0 to ".............zzz",
            1 to "..............z.",
            2 to ".............zzz",
            4 to "...........zzz..",
            5 to "............z...",
            6 to "...........zzz.."
        ),
        mapOf('z' to Color(0xFF8E9AD9))
    )

    private val pinkBow = halfLayer(
        mapOf(5 to ".....bb.", 6 to ".....bbd"),
        mapOf('b' to Color(0xFFF27BA8), 'd' to Color(0xFFC25783))
    )

    private val flowerCrown = halfLayer(
        mapOf(6 to "....p.y.", 7 to "...gggpg"),
        mapOf('p' to Color(0xFFF4A6C3), 'y' to gold, 'g' to Color(0xFF7FB069))
    )

    private val wizardHat = halfLayer(
        mapOf(
            0 to ".......h",
            1 to ".......h",
            2 to "......hh",
            3 to ".....hhh",
            4 to ".....hyh",
            5 to "....hhhh",
            6 to "...ddddd",
            7 to "..hhhhhh"
        ),
        mapOf('h' to Color(0xFF7B68B5), 'd' to Color(0xFF5A4A96), 'y' to gold)
    )

    private val roundGlasses = halfLayer(
        mapOf(9 to "...fff..", 10 to "...f.fff", 11 to "...fff.."),
        mapOf('f' to Color(0xFFD9A441))
    )

    private val bellCollar = halfLayer(
        mapOf(14 to "..rrrrrr", 15 to ".......y"),
        mapOf('r' to Color(0xFFE06666), 'y' to gold)
    )

    /** Asymmetric: a striped wrap with a tail hanging down the cat's left side. */
    private val cozyScarf = fullLayer(
        mapOf(
            14 to "..ssssssssssss..",
            15 to "..swswswswswsw..",
            16 to "...........ss...",
            17 to "...........sw...",
            18 to "...........ss..."
        ),
        mapOf('s' to Color(0xFFC77DA3), 'w' to Color(0xFFFFF8EF))
    )

    private val smallSweater = halfLayer(
        mapOf(14 to "...kkkkk", 15 to "...lllll", 16 to "...kkkkk", 17 to "...lllll"),
        mapOf('k' to Color(0xFFD89BB5), 'l' to Color(0xFFF4DDE5))
    )

    private val sprites: Map<String, PixelSprite> = mapOf(
        "pink_bow" to pinkBow,
        "flower_crown" to flowerCrown,
        "wizard_hat" to wizardHat,
        "round_glasses" to roundGlasses,
        "bell_collar" to bellCollar,
        "cozy_scarf" to cozyScarf,
        "small_sweater" to smallSweater
    )

    fun forItem(itemKey: String): PixelSprite? = sprites[itemKey]
}
