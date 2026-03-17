package com.example.myapplication.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * 应用形状规范
 * 基于 app重构界面.html 的设计
 */
object AppShapes {
    // 头部圆角（底部圆角）
    val HeaderRadius = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    
    // 卡片圆角
    val CardRadius = RoundedCornerShape(16.dp)
    val CardRadiusLarge = RoundedCornerShape(20.dp)
    val CardRadiusSmall = RoundedCornerShape(12.dp)
    
    // 芯片/标签圆角
    val ChipRadius = RoundedCornerShape(20.dp)
    
    // 按钮圆角
    val ButtonRadius = RoundedCornerShape(12.dp)
    val ButtonRadiusSmall = RoundedCornerShape(8.dp)
    
    // 输入框圆角
    val InputRadius = RoundedCornerShape(10.dp)
    
    // 对话框圆角
    val DialogRadius = RoundedCornerShape(20.dp)
    
    // 圆形
    val Circle = CircleShape
}
