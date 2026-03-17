package com.example.myapplication.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.*

/**
 * FAB 菜单组件
 * 基于 app重构界面.html 的设计
 */
@Composable
fun FABMenu(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(end = AppSpacing.ScreenPadding, bottom = 90.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 子按钮
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FABOption(
                        label = "导入数据",
                        icon = Icons.Default.Add,
                        onClick = {
                            onImport()
                            onToggle()
                        }
                    )
                }
            }
            
            // 主按钮
            val rotation by animateFloatAsState(
                targetValue = if (isExpanded) 45f else 0f,
                label = "rotation"
            )
            
            FloatingActionButton(
                onClick = onToggle,
                containerColor = Primary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}

@Composable
fun FABOption(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = AppShapes.ButtonRadiusSmall,
            color = Color.White,
            shadowElevation = AppElevation.Medium
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        
        FloatingActionButton(
            onClick = onClick,
            containerColor = Primary,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
