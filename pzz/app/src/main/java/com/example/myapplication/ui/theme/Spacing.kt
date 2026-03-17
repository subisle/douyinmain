package com.example.myapplication.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 应用间距规范
 * 基于 app重构界面.html 的设计
 */
object AppSpacing {
    // 基础间距
    val None: Dp = 0.dp
    val ExtraSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Large: Dp = 16.dp
    val ExtraLarge: Dp = 20.dp
    val XXLarge: Dp = 24.dp
    val XXXLarge: Dp = 32.dp
    
    // 特定用途间距
    val ScreenPadding: Dp = 20.dp
    val CardPadding: Dp = 16.dp
    val ListItemSpacing: Dp = 12.dp
    val IconSpacing: Dp = 8.dp
}

/**
 * 应用阴影规范
 */
object AppElevation {
    val None: Dp = 0.dp
    val Small: Dp = 2.dp
    val Medium: Dp = 4.dp
    val Large: Dp = 8.dp
    val ExtraLarge: Dp = 16.dp
    
    // 特定组件阴影
    val Card: Dp = 2.dp
    val Header: Dp = 8.dp
    val FAB: Dp = 6.dp
    val Dialog: Dp = 16.dp
}
