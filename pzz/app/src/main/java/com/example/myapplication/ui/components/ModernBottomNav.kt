package com.example.myapplication.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.*

enum class BottomNavTab {
    Dashboard,
    Streamer,
    Settings
}

/**
 * 现代化的底部导航栏
 * 基于 app重构界面.html 的设计
 */
@Composable
fun ModernBottomNav(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = AppElevation.Large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppSpacing.ScreenPadding,
                    vertical = 12.dp
                )
                .padding(bottom = 24.dp), // 安全区域
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "数据看板",
                isSelected = selectedTab == BottomNavTab.Dashboard,
                onClick = { onTabSelected(BottomNavTab.Dashboard) }
            )
            BottomNavItem(
                icon = Icons.Default.Person,
                label = "主播管理",
                isSelected = selectedTab == BottomNavTab.Streamer,
                onClick = { onTabSelected(BottomNavTab.Streamer) }
            )
            BottomNavItem(
                icon = Icons.Default.Settings,
                label = "设置",
                isSelected = selectedTab == BottomNavTab.Settings,
                onClick = { onTabSelected(BottomNavTab.Settings) }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color by animateColorAsState(
        targetValue = if (isSelected) Primary else TextSecondary,
        label = "color"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
