package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.Primary
import com.example.myapplication.ui.theme.customColors

@Composable
fun DateNavigationBar(
    currentDate: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.customColors.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 上一天按钮 - 圆形背景
            IconButton(
                onClick = onPreviousDay,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "上一天",
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // 日期显示 - 更大更醒目
            Text(
                text = formatDateLabel(currentDate),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.customColors.textPrimary
            )
            
            // 下一天按钮 - 圆形背景
            IconButton(
                onClick = onNextDay,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "下一天",
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

fun formatDateLabel(date: String): String {
    val parts = date.split("-")
    if (parts.size != 3) return date
    val month = parts[1].toIntOrNull() ?: return date
    val day = parts[2].toIntOrNull() ?: return date
    return "${month}月${day}日"
}
