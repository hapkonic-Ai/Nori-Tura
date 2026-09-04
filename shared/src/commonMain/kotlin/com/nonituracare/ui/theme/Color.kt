package com.nonituracare.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette derived from the Noni Tura shield logo: teal shield/figures, coral
 * girl figure, muted lavender suture/heart accent. Token names are kept
 * stable (PrimaryBlue, AccentGreen, etc.) even though the underlying hues
 * moved away from literal blue/green, since they're referenced by name
 * across the whole app — renaming them would mean touching every screen.
 */
object NorituraColors {
    val PrimaryBlue = Color(0xFF119089)
    val PrimaryBlueLight = Color(0xFFE0F5F3)
    val AccentGreen = Color(0xFF3CB6A0)
    val AccentGreenLight = Color(0xFFE3F7F2)
    val Warning = Color(0xFFF2795F)
    val WarningLight = Color(0xFFFDEAE6)
    val Error = Color(0xFFD5453A)
    val ErrorLight = Color(0xFFFBEAE8)
    val Success = Color(0xFF3CB6A0)
    val Info = Color(0xFF119089)

    /** The logo's muted lavender suture/heart accent — for decorative use
     * (connectors, subtle highlights), not for status semantics. */
    val AccentLavender = Color(0xFF8D88B0)
    val AccentLavenderLight = Color(0xFFEEEDF5)

    val PreOp = PrimaryBlue
    val InOt = Warning
    val PostOp = AccentGreen
    val Stable = AccentGreen
    val Urgent = Error

    val Background = Color(0xFFF9FAFC)
    val Surface = Color.White
    val SurfaceVariant = Color(0xFFF1F3F9)
    val TextPrimary = Color(0xFF1A1C29)
    val TextSecondary = Color(0xFF5A5E72)
    val TextTertiary = Color(0xFF8E93A6)
    val Divider = Color(0xFFE8EAF2)
    val Outline = Color(0xFFD7DAE6)
}
