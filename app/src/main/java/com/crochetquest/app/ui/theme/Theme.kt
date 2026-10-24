package com.crochetquest.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crochetquest.app.R

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

/**
 * Nunito (SIL Open Font License, see /licenses) is a friendly rounded font. It is bundled as one
 * variable font file, so each weight is the same file with a different weight setting.
 */
@OptIn(ExperimentalTextApi::class)
private fun nunito(weight: FontWeight): Font = Font(
    resId = R.font.nunito,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

@OptIn(ExperimentalTextApi::class)
private val Nunito = FontFamily(
    nunito(FontWeight.Normal),
    nunito(FontWeight.Medium),
    nunito(FontWeight.SemiBold),
    nunito(FontWeight.Bold)
)

private fun TextStyle.rounded(weight: FontWeight? = null): TextStyle =
    copy(fontFamily = Nunito, fontWeight = weight ?: fontWeight)

private val baseTypography = Typography()
private val CozyTypography = Typography(
    displayLarge = baseTypography.displayLarge.rounded(),
    displayMedium = baseTypography.displayMedium.rounded(),
    displaySmall = baseTypography.displaySmall.rounded(),
    headlineLarge = baseTypography.headlineLarge.rounded(FontWeight.Bold),
    headlineMedium = baseTypography.headlineMedium.rounded(FontWeight.Bold),
    headlineSmall = baseTypography.headlineSmall.rounded(FontWeight.SemiBold),
    titleLarge = baseTypography.titleLarge.rounded(FontWeight.SemiBold),
    titleMedium = baseTypography.titleMedium.rounded(FontWeight.SemiBold),
    titleSmall = baseTypography.titleSmall.rounded(),
    bodyLarge = baseTypography.bodyLarge.rounded(),
    bodyMedium = baseTypography.bodyMedium.rounded(),
    bodySmall = baseTypography.bodySmall.rounded(),
    labelLarge = baseTypography.labelLarge.rounded(),
    labelMedium = baseTypography.labelMedium.rounded(),
    labelSmall = baseTypography.labelSmall.rounded()
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
