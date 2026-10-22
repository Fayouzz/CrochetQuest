package com.crochetquest.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object CozyPalette {
    val Cream = Color(0xFFFFF8EF)
    val PalePink = Color(0xFFF4DDE5)
    val DustyRose = Color(0xFFD89BB5)
    val Sage = Color(0xFFDCE8D5)
    val Lavender = Color(0xFFE8E1F4)
    val WarmBrown = Color(0xFF73594B)
    val DarkText = Color(0xFF3D3432)
    val MutedText = Color(0xFF897C76)
    val CardBackground = Color(0xFFFFFCF8)

    // Spec muted text (#897C76) is only ~3.8:1 on cream; this slightly deeper tone is used for
    // small body text so it stays above 4.5:1.
    val MutedTextReadable = Color(0xFF6F635D)
}

private val CozyColorScheme = lightColorScheme(
    primary = CozyPalette.DustyRose,
    onPrimary = CozyPalette.DarkText,
    primaryContainer = CozyPalette.PalePink,
    onPrimaryContainer = CozyPalette.DarkText,
    secondary = CozyPalette.WarmBrown,
    onSecondary = CozyPalette.Cream,
    secondaryContainer = CozyPalette.Sage,
    onSecondaryContainer = CozyPalette.DarkText,
    tertiary = CozyPalette.WarmBrown,
    onTertiary = CozyPalette.Cream,
    tertiaryContainer = CozyPalette.Lavender,
    onTertiaryContainer = CozyPalette.DarkText,
    background = CozyPalette.Cream,
    onBackground = CozyPalette.DarkText,
    surface = CozyPalette.CardBackground,
    onSurface = CozyPalette.DarkText,
    surfaceVariant = CozyPalette.PalePink,
    onSurfaceVariant = CozyPalette.MutedTextReadable,
    surfaceContainerLowest = CozyPalette.CardBackground,
    surfaceContainerLow = CozyPalette.CardBackground,
    surfaceContainer = CozyPalette.CardBackground,
    surfaceContainerHigh = CozyPalette.CardBackground,
    surfaceContainerHighest = CozyPalette.PalePink,
    outline = CozyPalette.WarmBrown,
    outlineVariant = CozyPalette.PalePink
)

private val baseTypography = Typography()
private val CozyTypography = baseTypography.copy(
    headlineLarge = baseTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = baseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = baseTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = baseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
)

private val CozyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CozyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CozyColorScheme,
        typography = CozyTypography,
        shapes = CozyShapes,
        content = content
    )
}
