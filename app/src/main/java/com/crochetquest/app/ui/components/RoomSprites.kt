package com.crochetquest.app.ui.components

import androidx.compose.ui.graphics.Color

/**
 * The cozy room, drawn on a 48 x 36 pixel grid ([GRID_WIDTH] x [GRID_HEIGHT]). Every piece has a
 * fixed spot, a day palette and a night palette (the window shows stars, the lamps glow).
 * Pieces were generated from small drawing routines, then checked by rendering the whole room.
 */
object RoomSprites {
    const val GRID_WIDTH = 48
    const val GRID_HEIGHT = 36

    /** Rows above this are wall, rows from here down are floor. */
    const val FLOOR_Y = 24

    /** Where the cat sits (its 16 x 20 canvas), so its feet land on the rug. */
    const val CAT_X = 16
    const val CAT_Y = 12

    /** One decoration: where it goes, plus how it looks by day and by night. */
    class Art(val x: Int, val y: Int, private val day: PixelSprite, private val night: PixelSprite) {
        fun sprite(isNight: Boolean): PixelSprite = if (isNight) night else day
    }

    private val DAY: Map<Char, Color> = mapOf(
        'w' to Color(0xFFC4915F),
        'h' to Color(0xFFD9AE82),
        'd' to Color(0xFF9A6B42),
        'f' to Color(0xFFE6C9A0),
        'b' to Color(0xFFE6C9A0),
        'r' to Color(0xFFD89BB5),
        'l' to Color(0xFFB8A6DB),
        'g' to Color(0xFF7FB069),
        'G' to Color(0xFF5E8F52),
        'y' to Color(0xFFF2C94C),
        'p' to Color(0xFFF4A6C3),
        'c' to Color(0xFFFFF8EF),
        't' to Color(0xFFC48B6B),
        'o' to Color(0xFF73594B),
        's' to Color(0xFFA8D8F0),
        'x' to Color(0xFFFFFFFF),
        'L' to Color(0xFFF8EBC8),
        'a' to Color(0xFFEBD9C4),
        'A' to Color(0xFFD89BB5),
        'n' to Color(0xFF3D3432),
        'm' to Color(0xFFF4DDE5),
        'e' to Color(0xFF8B6F5E),
        'u' to Color(0xFFE8836F),
        'i' to Color(0xFF9CC5E3)
    )

    private val NIGHT: Map<Char, Color> = mapOf(
        'w' to Color(0xFFC4915F),
        'h' to Color(0xFFD9AE82),
        'd' to Color(0xFF9A6B42),
        'f' to Color(0xFFE6C9A0),
        'b' to Color(0xFFE6C9A0),
        'r' to Color(0xFFD89BB5),
        'l' to Color(0xFFB8A6DB),
        'g' to Color(0xFF7FB069),
        'G' to Color(0xFF5E8F52),
        'y' to Color(0xFFF2C94C),
        'p' to Color(0xFFF4A6C3),
        'c' to Color(0xFFFFF8EF),
        't' to Color(0xFFC48B6B),
        'o' to Color(0xFF73594B),
        's' to Color(0xFF2B3A67),
        'x' to Color(0xFFF7E7A0),
        'L' to Color(0xFFFFD875),
        'a' to Color(0xFFEBD9C4),
        'A' to Color(0xFFD89BB5),
        'n' to Color(0xFF3D3432),
        'm' to Color(0xFFF4DDE5),
        'e' to Color(0xFF8B6F5E),
        'u' to Color(0xFFE8836F),
        'i' to Color(0xFF9CC5E3)
    )

    private fun art(x: Int, y: Int, rows: List<String>) =
        Art(x, y, PixelSprite(rows, DAY), PixelSprite(rows, NIGHT))

    private val byKey: Map<String, Art> = mapOf(
        "room_window_round" to art(7, 4, listOf(
            "....ffff....",
            "..fffbbfff..",
            ".ffxsbbssff.",
            ".fxxsbbsssf.",
            "ffsssbbsssff",
            "fbbbbbbbbbbf",
            "fbbbbbbbbbbf",
            "ffsssbbsssff",
            ".fsssbbsxxf.",
            ".ffssbbssff.",
            "..fffbbfff..",
            "....ffff...."
        )),
        "room_window_garden" to art(6, 4, listOf(
            "ffffffffffffff",
            "fsssssbbsssssf",
            "fsxxssbbsssssf",
            "fsssssbbsssssf",
            "fbbbbbbbbbbbbf",
            "fbbbbbbbbbbbbf",
            "fsssssbbsssssf",
            "fssxssbbssxxsf",
            "fsssssbbsssssf",
            "ffffffffffffff",
            "gpgygpgpygpgyg",
            "dddddddddddddd"
        )),
        "room_shelf_wood" to art(32, 7, listOf(
            "................",
            ".Arr............",
            ".Arrll.....ccc..",
            ".Arrll.....uuuu.",
            ".Arrll.....uuu..",
            "hhhhhhhhhhhhhhhh",
            "wwwwwwwwwwwwwwww",
            "..d..........d.."
        )),
        "room_shelf_yarn" to art(32, 7, listOf(
            "................",
            "..AAA.llll.yyy..",
            ".AAcAAlcllyycyy.",
            ".AAAAlllllyyyyy.",
            ".AAAAAllllyyyyy.",
            "hhhhhhhhhhhhhhhh",
            "wwwwwwwwwwwwwwww",
            "..d..........d.."
        )),
        "room_lamp_paper" to art(40, 16, listOf(
            "oooooo",
            "LLLLLL",
            "LLLLLL",
            "LLLLLL",
            "LLLLLL",
            ".LLLL.",
            "..dd..",
            "..dd..",
            "..dd..",
            "..dd..",
            "..dd..",
            "..dd..",
            "oooooo",
            "oooooo"
        )),
        "room_lamp_star" to art(40, 16, listOf(
            "...L...",
            "..LLL..",
            "LLLLLLL",
            ".LLLLL.",
            "..LLL..",
            ".LL.LL.",
            "L.....L",
            "...d...",
            "...d...",
            "...d...",
            "...d...",
            "...d...",
            ".ooooo.",
            ".ooooo."
        )),
        "room_plant_sprout" to art(3, 20, listOf(
            "........",
            ".....ggg",
            "GGG..gg.",
            ".GGgg...",
            "...gg...",
            "...gg...",
            ".dddddd.",
            ".eeeeee.",
            ".tttttt.",
            ".tttttt."
        )),
        "room_plant_fern" to art(3, 18, listOf(
            "....gg....",
            "....GG....",
            "..ggGGgg..",
            ".g..GG..g.",
            "..ggGGgg..",
            ".g..GG..g.",
            "G.ggGGgg.G",
            "....GG....",
            ".dddddddd.",
            ".eeeeeeee.",
            ".tttttttt.",
            ".tttttttt."
        )),
        "room_plant_flowers" to art(3, 18, listOf(
            ".....u....",
            "..p.uyu...",
            ".pyp.u.p..",
            "..p..gpyp.",
            "..g..g.p..",
            "..g..ggg..",
            "..gg.g.g..",
            "..g..g.g..",
            ".dddddddd.",
            ".eeeeeeee.",
            ".tttttttt.",
            ".tttttttt."
        )),
        "room_rug_round" to art(14, 28, listOf(
            "....AAAAAAAAAAAA....",
            ".AAAaaaaaaaaaaaaAAA.",
            "AAaaacccAAAAcccaaaAA",
            "AAaaacccAAAAcccaaaAA",
            ".AAAaaaaaaaaaaaaAAA.",
            "....AAAAAAAAAAAA...."
        )),
        "room_rug_striped" to art(13, 28, listOf(
            ".....AAAAAAAAAAAA.....",
            ".mmmmmmmmmmmmmmmmmmmm.",
            "AAAAAAAAAAAAAAAAAAAAAA",
            "mmmmmmmmmmmmmmmmmmmmmm",
            ".AAAAAAAAAAAAAAAAAAAA.",
            ".....mmmmmmmmmmmm....."
        )),
        "auto_bunting" to art(2, 0, listOf(
            "oooooooooooooooooooooooooooooooooooooooooooo",
            ".rrr.lll.yyy.ggg.rrr.lll.yyy.ggg.rrr.lll.yyy",
            "..r...l...y...g...r...l...y...g...r...l...y."
        )),
        "auto_picture" to art(20, 4, listOf(
            "dddddddd",
            "dssssysd",
            "dssssxsd",
            "dgGggggd",
            "dggggggd",
            "dddddddd"
        )),
        "auto_star_lights" to art(0, 2, listOf(
            "..o..",
            "..y..",
            ".ycy.",
            "..y..",
            "..o..",
            "..y..",
            ".ycy.",
            "..y..",
            "..o..",
            "..y..",
            ".ycy.",
            "..y..",
            "..o..",
            "..y..",
            ".ycy.",
            "..y.."
        ))
    )

    /** The art for a room item key or a free decoration key, or null if there is none. */
    fun forKey(key: String): Art? = byKey[key]
}
