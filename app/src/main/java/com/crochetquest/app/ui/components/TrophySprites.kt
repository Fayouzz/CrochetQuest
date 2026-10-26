package com.crochetquest.app.ui.components

import androidx.compose.ui.graphics.Color
import com.crochetquest.app.domain.projects.ProjectCategory

/** Little pixel keepsakes for the trophy shelf: one per project category. */
object TrophySprites {
    private val palette: Map<Char, Color> = mapOf(
        'b' to Color(0xFFB98B5E),
        'e' to Color(0xFF3D3432),
        'c' to Color(0xFFF6D3A8),
        'n' to Color(0xFF73594B),
        's' to Color(0xFFD89BB5),
        'l' to Color(0xFFF4DDE5),
        'h' to Color(0xFFB8A6DB),
        'q' to Color(0xFFF4A6C3),
        'd' to Color(0xFF9A6B42),
        'w' to Color(0xFFC4915F)
    )

    private val bear = PixelSprite(listOf(".bb..bb.", "bbbbbbbb", "bbebbebb", "bbccccbb", "bbcnncbb", ".bbbbbb.", "..bbbb..", "..b..b.."), palette)
    private val sweater = PixelSprite(listOf("..ssss..", ".ssllss.", "ssssssss", "s.ssss.s", "s.llll.s", "s.ssss.s", "..llll..", "........"), palette)
    private val beanie = PixelSprite(listOf("...qq...", "..qqqq..", ".hhhhhh.", "hhhhhhhh", "hhhhhhhh", "dddddddd", "dddddddd", "........"), palette)
    private val basket = PixelSprite(listOf("..dddd..", ".d....d.", "dddddddd", "wdwdwdwd", "dwdwdwdw", "wdwdwdwd", "dwdwdwdw", ".dddddd."), palette)

    fun forCategory(category: ProjectCategory): PixelSprite = when (category) {
        ProjectCategory.AMIGURUMI -> bear
        ProjectCategory.CLOTHING -> sweater
        ProjectCategory.ACCESSORIES -> beanie
        ProjectCategory.HOME_DECOR -> basket
        ProjectCategory.FLOWERS -> Sprites.flower
        ProjectCategory.GIFTS -> Sprites.gift
        ProjectCategory.TINY -> Sprites.yarnBall
        ProjectCategory.OTHER -> Sprites.star
    }
}
