package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================================
// FocusLock Context-Based Color Psychology & Semantic Color System
// ============================================================================
//
// Semantic Color Rules:
// - RED (Error / Destructive / Blocked / Expired):
//   Errors, critical warnings, failed actions, expired limits, revoked permissions, destructive actions.
// - GREEN (Success / Completed / Granted):
//   Successful operations, completed focus sessions, earned achievements, granted permissions, positive confirmations.
// - AMBER / ORANGE (Warning / Approaching Limit / Pending):
//   Cautions, approaching time limits, pending actions, permissions needing setup, gentle reminders.
// - BLUE / TEAL (Info / Active Focus / Normal Interactive):
//   Active focus sessions, informational messages, normal progress indicators, normal interactive elements.
// - PURPLE (Premium / Exclusive / Special Achievement):
//   Premium features, exclusive content, special achievements, deep work mastery.
// - NEUTRAL (General Text / Surfaces / Inactive):
//   General text, backgrounds, descriptions, inactive elements, secondary information.
// ============================================================================

enum class SemanticTone {
    INFO,       // Blue / Teal: Active focus sessions, normal interactive elements, normal progress
    SUCCESS,    // Green: Completed sessions, granted permissions, earned achievements, confirmations
    WARNING,    // Amber / Orange: Approaching limits, pending setup, cautions, gentle reminders
    ERROR,      // Red: Expired limits, blocked apps, failed permission checks, critical warnings, destructive actions
    PREMIUM,    // Purple: Premium features, exclusive content, special achievements
    NEUTRAL;    // Neutral: General text, backgrounds, descriptions, secondary/inactive elements

    companion object {
        val BLUE = INFO
        val TEAL = INFO
        val GREEN = SUCCESS
        val AMBER = WARNING
        val ORANGE = WARNING
        val RED = ERROR
        val PURPLE = PREMIUM
    }
}

@Immutable
data class SemanticRoleColors(
    val primary: Color,
    val onPrimary: Color,
    val text: Color,
    val container: Color,
    val containerElevated: Color,
    val border: Color,
    val iconTint: Color,
    val gradientStart: Color,
    val gradientEnd: Color
) {
    val brush: Brush
        get() = Brush.horizontalGradient(listOf(gradientStart, gradientEnd))

    val accent: Color
        get() = primary

    val icon: Color
        get() = iconTint

    val onContainer: Color
        get() = text
}

@Immutable
data class SemanticColorSystem(
    val isDark: Boolean,
    val info: SemanticRoleColors,
    val success: SemanticRoleColors,
    val warning: SemanticRoleColors,
    val error: SemanticRoleColors,
    val premium: SemanticRoleColors,
    val neutral: SemanticRoleColors,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val surfaceBase: Color,
    val surfaceElevated: Color,
    val surfaceRecessed: Color,
    val outlineSubtle: Color
) {
    val blue: SemanticRoleColors get() = info
    val teal: SemanticRoleColors get() = info
    val green: SemanticRoleColors get() = success
    val amber: SemanticRoleColors get() = warning
    val red: SemanticRoleColors get() = error
    val purple: SemanticRoleColors get() = premium

    fun forTone(tone: SemanticTone): SemanticRoleColors = when (tone) {
        SemanticTone.INFO -> info
        SemanticTone.SUCCESS -> success
        SemanticTone.WARNING -> warning
        SemanticTone.ERROR -> error
        SemanticTone.PREMIUM -> premium
        SemanticTone.NEUTRAL -> neutral
    }
}

val DarkSemanticColors = SemanticColorSystem(
    isDark = true,
    info = SemanticRoleColors(
        primary = Color(0xFF00E5FF),
        onPrimary = Color(0xFF04151F),
        text = Color(0xFF24DFEC),
        container = Color(0xE60E2439),
        containerElevated = Color(0xEB132E47),
        border = Color(0x7500E5FF),
        iconTint = Color(0xFF00E5FF),
        gradientStart = Color(0xFF00E5FF),
        gradientEnd = Color(0xFF2979FF)
    ),
    success = SemanticRoleColors(
        primary = Color(0xFF10B981),
        onPrimary = Color(0xFF032217),
        text = Color(0xFF34D399),
        container = Color(0xE60B2922),
        containerElevated = Color(0xEB0F352B),
        border = Color(0x8010B981),
        iconTint = Color(0xFF34D399),
        gradientStart = Color(0xFF10B981),
        gradientEnd = Color(0xFF34D399)
    ),
    warning = SemanticRoleColors(
        primary = Color(0xFFFFAB00),
        onPrimary = Color(0xFF1F1200),
        text = Color(0xFFFFB74D),
        container = Color(0xE62B1E10),
        containerElevated = Color(0xEB362512),
        border = Color(0x88FFA726),
        iconTint = Color(0xFFFFB300),
        gradientStart = Color(0xFFFFAB00),
        gradientEnd = Color(0xFFFF6D00)
    ),
    error = SemanticRoleColors(
        primary = Color(0xFFFF5252),
        onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFFFF6E6E),
        container = Color(0xE62D131B),
        containerElevated = Color(0xEB381721),
        border = Color(0x88FF5252),
        iconTint = Color(0xFFFF5252),
        gradientStart = Color(0xFFFF5252),
        gradientEnd = Color(0xFFEF4444)
    ),
    premium = SemanticRoleColors(
        primary = Color(0xFFA855F7),
        onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFFC084FC),
        container = Color(0xE6211336),
        containerElevated = Color(0xEB2B1846),
        border = Color(0x80A855F7),
        iconTint = Color(0xFFC084FC),
        gradientStart = Color(0xFF9D4EDD),
        gradientEnd = Color(0xFF7C3AED)
    ),
    neutral = SemanticRoleColors(
        primary = Color(0xFF94A3B8),
        onPrimary = Color(0xFF0F172A),
        text = Color(0xFFE2E8F0),
        container = Color(0xE6101B2D),
        containerElevated = Color(0xEB152338),
        border = Color(0x29FFFFFF),
        iconTint = Color(0xFFCBD5E1),
        gradientStart = Color(0xFF475569),
        gradientEnd = Color(0xFF334155)
    ),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFFB0C0D4),
    textMuted = Color(0xFF7E92AA),
    surfaceBase = Color(0xFF101A2B),
    surfaceElevated = Color(0xFF162438),
    surfaceRecessed = Color(0xFF09111E),
    outlineSubtle = Color(0x29FFFFFF)
)

val LightSemanticColors = SemanticColorSystem(
    isDark = false,
    info = SemanticRoleColors(
        primary = Color(0xFF00ACC1),
        onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF00838F),
        container = Color(0xFFE0F7FA),
        containerElevated = Color(0xFFB2EBF2),
        border = Color(0xFF00BCD4),
        iconTint = Color(0xFF0097A7),
        gradientStart = Color(0xFF00B8D4),
        gradientEnd = Color(0xFF0288D1)
    ),
    success = SemanticRoleColors(
        primary = Color(0xFF059669),
        onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF047857),
        container = Color(0xFFD1FAE5),
        containerElevated = Color(0xFFA7F3D0),
        border = Color(0xFF10B981),
        iconTint = Color(0xFF059669),
        gradientStart = Color(0xFF059669),
        gradientEnd = Color(0xFF10B981)
    ),
    warning = SemanticRoleColors(
        primary = Color(0xFFD97706),
        onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFFB45309),
        container = Color(0xFFFEF3C7),
        containerElevated = Color(0xFFFDE68A),
        border = Color(0xFFF59E0B),
        iconTint = Color(0xFFD97706),
        gradientStart = Color(0xFFF59E0B),
        gradientEnd = Color(0xFFD97706)
    ),
    error = SemanticRoleColors(
        primary = Color(0xFFDC2626),
        onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFFB91C1C),
        container = Color(0xFFFEE2E2),
        containerElevated = Color(0xFFFECACA),
        border = Color(0xFFEF4444),
        iconTint = Color(0xFFDC2626),
        gradientStart = Color(0xFFEF4444),
        gradientEnd = Color(0xFFDC2626)
    ),
    premium = SemanticRoleColors(
        primary = Color(0xFF7C3AED),
        onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF6D28D9),
        container = Color(0xFFF3E8FF),
        containerElevated = Color(0xFFE9D5FF),
        border = Color(0xFF8B5CF6),
        iconTint = Color(0xFF7C3AED),
        gradientStart = Color(0xFF8B5CF6),
        gradientEnd = Color(0xFF6D28D9)
    ),
    neutral = SemanticRoleColors(
        primary = Color(0xFF475569),
        onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF1E293B),
        container = Color(0xFFF1F5F9),
        containerElevated = Color(0xFFE2E8F0),
        border = Color(0xFFCBD5E1),
        iconTint = Color(0xFF475569),
        gradientStart = Color(0xFF64748B),
        gradientEnd = Color(0xFF475569)
    ),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    surfaceBase = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF8FAFC),
    surfaceRecessed = Color(0xFFE2E8F0),
    outlineSubtle = Color(0xFFCBD5E1)
)

val LocalSemanticColors = staticCompositionLocalOf { DarkSemanticColors }

// Core Brand & Semantic Tokens (Backwards-compatible exports)
val FocusCyan = Color(0xFF00E5FF)
val FocusTurquoise = Color(0xFF24DFEC)
val FocusBlue = Color(0xFF2979FF)
val FocusPurple = Color(0xFFA855F7)
val FocusGreen = Color(0xFF10B981)
val FocusAmber = Color(0xFFFFAB00)
val FocusRed = Color(0xFFFF5252)

// Light Theme Tokens
val SleekPrimaryLight = Color(0xFF00ACC1)
val SleekOnPrimaryLight = Color(0xFFFFFFFF)
val SleekPrimaryContainerLight = Color(0xFFE0F7FA)
val SleekOnPrimaryContainerLight = Color(0xFF002633)

val SleekSecondaryLight = Color(0xFF059669)
val SleekOnSecondaryLight = Color(0xFFFFFFFF)
val SleekSecondaryContainerLight = Color(0xFFD1FAE5)
val SleekOnSecondaryContainerLight = Color(0xFF064E3B)

val SleekBackgroundLight = Color(0xFFF8FAFC)
val SleekOnBackgroundLight = Color(0xFF0F172A)
val SleekSurfaceLight = Color(0xFFFFFFFF)
val SleekOnSurfaceLight = Color(0xFF0F172A)
val SleekSurfaceVariantLight = Color(0xFFF1F5F9)
val SleekOnSurfaceVariantLight = Color(0xFF475569)
val SleekOutlineLight = Color(0xFFCBD5E1)

// Dark AMOLED Theme Tokens
val SleekPrimaryDark = Color(0xFF00E5FF)
val SleekOnPrimaryDark = Color(0xFF04151F)
val SleekPrimaryContainerDark = Color(0xFF0B3548)
val SleekOnPrimaryContainerDark = Color(0xFFB8F2FF)

val SleekSecondaryDark = Color(0xFF34D399)
val SleekOnSecondaryDark = Color(0xFF03281C)
val SleekSecondaryContainerDark = Color(0xFF084230)
val SleekOnSecondaryContainerDark = Color(0xFFA7F3D0)

val SleekBackgroundDark = Color(0xFF070C15)
val SleekOnBackgroundDark = Color(0xFFF1F5F9)
val SleekSurfaceDark = Color(0xFF101A2B)
val SleekOnSurfaceDark = Color(0xFFF1F5F9)
val SleekSurfaceVariantDark = Color(0xFF162438)
val SleekOnSurfaceVariantDark = Color(0xFFA8B8CC)
val SleekOutlineDark = Color(0xFF2B3D54)

// Semantic Status Colors
val SleekError = Color(0xFFFF5252)
val SleekOnError = Color(0xFFFFFFFF)
val SleekWarning = Color(0xFFFFAB00)
val SleekSuccess = Color(0xFF10B981)
val SleekPremium = Color(0xFFA855F7)
val SleekInfo = Color(0xFF24DFEC)
