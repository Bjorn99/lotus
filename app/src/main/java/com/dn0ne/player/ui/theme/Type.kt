package com.dn0ne.player.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dn0ne.player.R

val InterFont = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp,
        fontWeight = FontWeight.Bold, fontFamily = InterFont,
    ),
    displayMedium = TextStyle(
        fontSize = 45.sp, lineHeight = 52.sp,
        fontWeight = FontWeight.Bold, fontFamily = InterFont,
    ),
    displaySmall = TextStyle(
        fontSize = 36.sp, lineHeight = 44.sp,
        fontWeight = FontWeight.Bold, fontFamily = InterFont,
    ),
    headlineLarge = TextStyle(
        fontSize = 32.sp, lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold, fontFamily = InterFont,
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp, lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold, fontFamily = InterFont,
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp, lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold, fontFamily = InterFont,
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp, lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold, fontFamily = InterFont,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp,
        fontWeight = FontWeight.Medium, fontFamily = InterFont,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
        fontWeight = FontWeight.Medium, fontFamily = InterFont,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp,
        fontWeight = FontWeight.Normal, fontFamily = InterFont,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp,
        fontWeight = FontWeight.Normal, fontFamily = InterFont,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
        fontWeight = FontWeight.Normal, fontFamily = InterFont,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
        fontWeight = FontWeight.Medium, fontFamily = InterFont,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
        fontWeight = FontWeight.Medium, fontFamily = InterFont,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
        fontWeight = FontWeight.Medium, fontFamily = InterFont,
    ),
)
