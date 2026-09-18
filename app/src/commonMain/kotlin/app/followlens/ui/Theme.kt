package app.followlens.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Dark, brass-accented theme. Ground is warm near-black (never pure black); a vivid gold is the
 * one accent, reserved for the primary action and headline figures — everything else stays quiet.
 * The Galivo brand mark shown in [ScreenHeader] uses its own, brighter gold plus a glow — see
 * [GalivoMark] — since FollowLens keeps its own identity distinct from Galivo's own indigo/cyan
 * corporate palette (that's the parent company's brand, not this app's).
 */
object FollowLensColors {
    val background = Color(0xFF0C0B09)
    val surface = Color(0xFF17140F)
    val surfaceRaised = Color(0xFF211C15)
    val surfaceSunken = Color(0xFF100D09)
    val outline = Color(0xFF2C2620)

    val textPrimary = Color(0xFFF3EEE3)
    val textSecondary = Color(0xFFB4A996)
    val textTertiary = Color(0xFF7D7362)

    val accent = Color(0xFFE3B268)
    val accentStrong = Color(0xFFF2CB8C)
    val accentInk = Color(0xFF241A0C)

    val seriesFollowers = Color(0xFF3A9BC9)
    val good = Color(0xFF5FA878)
    val critical = Color(0xFFC96A5B)
}

val FollowLensDarkColorScheme = darkColorScheme(
    background = FollowLensColors.background,
    onBackground = FollowLensColors.textPrimary,
    surface = FollowLensColors.surface,
    onSurface = FollowLensColors.textPrimary,
    surfaceVariant = FollowLensColors.surfaceRaised,
    onSurfaceVariant = FollowLensColors.textSecondary,
    primary = FollowLensColors.accent,
    onPrimary = FollowLensColors.accentInk,
    primaryContainer = FollowLensColors.surfaceRaised,
    onPrimaryContainer = FollowLensColors.accentStrong,
    secondary = FollowLensColors.seriesFollowers,
    onSecondary = FollowLensColors.background,
    error = FollowLensColors.critical,
    onError = FollowLensColors.background,
    outline = FollowLensColors.outline,
    outlineVariant = FollowLensColors.outline,
)

val FollowLensShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
