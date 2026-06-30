/*
© Жиляков Д.Э., 2026. Все права защищены.
*/

package com.example.sborkapc.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppTheme {
    val DarkBg = Color(0xFF080C14)
    val SurfaceDim = Color(0xFF0D1424)
    val CardBg = Color(0xFF111827)
    val CardBgElevated = Color(0xFF1A2236)
    val Divider = Color(0xFF1E2D45)

    val NeonCyan = Color(0xFF00D4FF)
    val NeonBlue = Color(0xFF3B82F6)
    val NeonPurple = Color(0xFF8B5CF6)
    val NeonGreen = Color(0xFF10B981)
    val NeonRed = Color(0xFFEF4444)
    val NeonAmber = Color(0xFFF59E0B)
    val NeonYellow = Color(0xFFFFD700)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFF3D5070)

    val GradientCyan = Brush.horizontalGradient(listOf(NeonCyan, NeonBlue))
    val GradientPurple = Brush.horizontalGradient(listOf(NeonBlue, NeonPurple))
    val GradientSuccess = Brush.horizontalGradient(listOf(NeonGreen, NeonCyan))
    val GradientCard = Brush.verticalGradient(listOf(CardBgElevated, CardBg))

    val GlassBorder = Color.White.copy(alpha = 0.06f)
    val GlassBorderActive = Color.White.copy(alpha = 0.15f)
}
