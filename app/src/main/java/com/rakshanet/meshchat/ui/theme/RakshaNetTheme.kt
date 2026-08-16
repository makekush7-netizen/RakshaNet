package com.rakshanet.meshchat.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF171A18)
val MutedInk = Color(0xFF66706A)
val RakshaGreen = Color(0xFF37A86B)
val RakshaGreenDark = Color(0xFF147A48)
val Mint = Color(0xFFDCEEDC)
val MintSoft = Color(0xFFF0F6EF)
val Sun = Color(0xFFF2C94C)
val EmergencyRed = Color(0xFFED4B2F)
val AppBackground = Color(0xFFF7F6F1)
val CardBorder = Color(0xFFE3E6E1)
val TextPrimary = Ink
val TextSecondary = MutedInk

// Compatibility aliases while legacy diagnostic surfaces are migrated.
val Navy = Ink
val Teal = RakshaGreenDark
val CourseTint = MintSoft
val ConnectTint = Mint
val NavySoft = MutedInk

private val colors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    secondary = RakshaGreen,
    onSecondary = Color.White,
    tertiary = Sun,
    error = EmergencyRed,
    onError = Color.White,
    background = AppBackground,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = MintSoft,
    onSurfaceVariant = MutedInk,
    outline = CardBorder,
    outlineVariant = CardBorder,
)

private val typography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 33.sp, color = Ink),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 28.sp, color = Ink),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 25.sp, color = Ink),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 23.sp, color = Ink),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, color = Ink),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp, color = Ink),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = MutedInk),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp, color = MutedInk),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun RakshaNetTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
