package com.example.myapplication.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.*

enum class DataMode {
    Sound,
    Duration
}

/**
 * 分段控制器组件
 * 基于 app重构界面.html 的设计
 */
@Composable
fun SegmentControl(
    selectedMode: DataMode,
    onModeChange: (DataMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = AppShapes.CardRadiusSmall,
        color = Color(0xFFE5E7EB),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(4.dp)
        ) {
            SegmentButton(
                text = "音浪业绩",
                isSelected = selectedMode == DataMode.Sound,
                onClick = { onModeChange(DataMode.Sound) },
                modifier = Modifier.weight(1f)
            )
            SegmentButton(
                text = "时长考勤",
                isSelected = selectedMode == DataMode.Duration,
                onClick = { onModeChange(DataMode.Duration) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SegmentButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.Transparent,
        label = "backgroundColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Primary else Color(0xFF6B7280),
        label = "textColor"
    )
    
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = AppShapes.ButtonRadiusSmall,
        color = backgroundColor,
        shadowElevation = if (isSelected) AppElevation.Small else AppElevation.None
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}
