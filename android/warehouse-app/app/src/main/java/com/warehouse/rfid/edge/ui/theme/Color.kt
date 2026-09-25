package com.warehouse.rfid.edge.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand — one hue (blue), expressed as a tonal ramp: light tint, vivid mid tone, deep shade.
// Same tone everywhere, just at different depths — not a second decorative color.
val Blue50 = Color(0xFFEDF2FF)
val Blue100 = Color(0xFFD6E4FF)
val Blue300 = Color(0xFF7DA6FF)
val Blue500 = Color(0xFF2F5FEE) // the one brand tone
val Blue600 = Color(0xFF2448C4)
val Blue700 = Color(0xFF1A3690)
val Blue900 = Color(0xFF0E1F5C)

val BrandPrimaryLight = Blue500
val BrandPrimaryLightHover = Blue700
val BrandOnPrimaryLight = Color(0xFFFFFFFF)
val BrandPrimaryContainerLight = Blue100
val BrandOnPrimaryContainerLight = Blue900

val BrandPrimaryDark = Blue300
val BrandOnPrimaryDark = Blue900
val BrandPrimaryContainerDark = Blue700
val BrandOnPrimaryContainerDark = Blue50

/** Soft tinted shadow color for the "glow" under primary actions — same hue, used sparingly. */
val BrandGlowLight = Blue500
val BrandGlowDark = Blue300

// Neutral scale — surfaces & text
val SurfaceLight = Color(0xFFF5F6F8)
val SurfaceContainerLight = Color(0xFFFFFFFF)
val SurfaceContainerHighLight = Color(0xFFEDEEF1)
val OnSurfaceLight = Color(0xFF15181F)
val OnSurfaceVariantLight = Color(0xFF6B7078)
val OutlineLight = Color(0xFFE3E5EA)

val SurfaceDark = Color(0xFF121317)
val SurfaceContainerDark = Color(0xFF1C1E23)
val SurfaceContainerHighDark = Color(0xFF26282E)
val OnSurfaceDark = Color(0xFFE7E8EB)
val OnSurfaceVariantDark = Color(0xFFA6ABB5)
val OutlineDark = Color(0xFF3A3D45)

// Semantic — meaning, not decoration
val SuccessLight = Color(0xFF1F8A4C)
val OnSuccessLight = Color(0xFFFFFFFF)
val SuccessContainerLight = Color(0xFFDCF3E4)
val OnSuccessContainerLight = Color(0xFF0B3D22)

val WarningLight = Color(0xFFB07A17)
val OnWarningLight = Color(0xFFFFFFFF)
val WarningContainerLight = Color(0xFFFBEBD3)
val OnWarningContainerLight = Color(0xFF4A3405)

val ErrorLight = Color(0xFFC4372E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFBDAD7)
val OnErrorContainerLight = Color(0xFF55130E)

val InfoLight = Color(0xFF2F6FE0)
val OnInfoLight = Color(0xFFFFFFFF)
val InfoContainerLight = Color(0xFFDCE6FB)
val OnInfoContainerLight = Color(0xFF0F2B57)

val SuccessDark = Color(0xFF7ED8A1)
val OnSuccessDark = Color(0xFF0B3D22)
val SuccessContainerDark = Color(0xFF165A34)
val OnSuccessContainerDark = Color(0xFFDCF3E4)

val WarningDark = Color(0xFFE8BB6E)
val OnWarningDark = Color(0xFF4A3405)
val WarningContainerDark = Color(0xFF6B4B0C)
val OnWarningContainerDark = Color(0xFFFBEBD3)

val ErrorDark = Color(0xFFEE9A93)
val OnErrorDark = Color(0xFF55130E)
val ErrorContainerDark = Color(0xFF7E241C)
val OnErrorContainerDark = Color(0xFFFBDAD7)

val InfoDark = Color(0xFF9BBBF5)
val OnInfoDark = Color(0xFF0F2B57)
val InfoContainerDark = Color(0xFF1D3F7D)
val OnInfoContainerDark = Color(0xFFDCE6FB)

val LightColorScheme = lightColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = BrandOnPrimaryLight,
    primaryContainer = BrandPrimaryContainerLight,
    onPrimaryContainer = BrandOnPrimaryContainerLight,
    secondary = BrandPrimaryLightHover,
    onSecondary = BrandOnPrimaryLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceContainerLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerHighLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = BrandOnPrimaryDark,
    primaryContainer = BrandPrimaryContainerDark,
    onPrimaryContainer = BrandOnPrimaryContainerDark,
    secondary = BrandPrimaryDark,
    onSecondary = BrandOnPrimaryDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceContainerDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerHighDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

/** Semantic colors Material3's base ColorScheme doesn't provide (success/warning/info). */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val glow: Color,
)

val LightExtendedColors = ExtendedColors(
    success = SuccessLight,
    onSuccess = OnSuccessLight,
    successContainer = SuccessContainerLight,
    onSuccessContainer = OnSuccessContainerLight,
    warning = WarningLight,
    onWarning = OnWarningLight,
    warningContainer = WarningContainerLight,
    onWarningContainer = OnWarningContainerLight,
    info = InfoLight,
    onInfo = OnInfoLight,
    infoContainer = InfoContainerLight,
    onInfoContainer = OnInfoContainerLight,
    glow = BrandGlowLight,
)

val DarkExtendedColors = ExtendedColors(
    success = SuccessDark,
    onSuccess = OnSuccessDark,
    successContainer = SuccessContainerDark,
    onSuccessContainer = OnSuccessContainerDark,
    warning = WarningDark,
    onWarning = OnWarningDark,
    warningContainer = WarningContainerDark,
    onWarningContainer = OnWarningContainerDark,
    info = InfoDark,
    onInfo = OnInfoDark,
    infoContainer = InfoContainerDark,
    onInfoContainer = OnInfoContainerDark,
    glow = BrandGlowDark,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
