package com.scnu.schedule.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Claude：衬线标题 + 人文无衬线正文（系统字体近似 Copernicus/Inter） */
val ClaudeTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 28.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
)

/** OpenCode：全等宽（JetBrains Mono 近似 Berkeley Mono） */
val OpenCodeTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 11.sp),
)

/** 复古报刊：全衬线（系统衬线近似 Georgia/宋体） */
val RetroTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)
