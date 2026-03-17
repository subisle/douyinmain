package com.example.myapplication.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.*

/**
 * 现代化的头部组件
 * 基于 app重构界面.html 的设计
 */
@Composable
fun ModernHeader(
    selectedDate: String,
    totalSound: String,
    liveCount: String,
    isDurationMode: Boolean = false,
    selectedGender: Gender? = null,
    onGenderChange: ((Gender) -> Unit)? = null,
    onDateClick: () -> Unit,
    onDatePrevious: (() -> Unit)? = null,
    onDateNext: (() -> Unit)? = null,
    onPhoneClick: () -> Unit,
    onShareClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val headerColor by animateColorAsState(
        targetValue = if (isDurationMode) Secondary else Primary,
        label = "headerColor"
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = headerColor,
        shape = AppShapes.HeaderRadius,
        shadowElevation = AppElevation.Header
    ) {
        Column(
            modifier = Modifier.padding(
                top = 40.dp,
                start = AppSpacing.ScreenPadding,
                end = AppSpacing.ScreenPadding,
                bottom = AppSpacing.ScreenPadding
            )
        ) {
            // 顶部栏：Logo + 标题 + 图标按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：Logo + 标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Logo圆圈
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "鹏",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    // 标题组
                    Column {
                        Text(
                            text = "鹏仔数据",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "主播管理系统",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // 右侧：图标按钮组
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.IconSpacing)) {
                    HeaderIconButton(
                        icon = Icons.Default.Phone,
                        onClick = onPhoneClick
                    )
                    HeaderIconButton(
                        icon = Icons.Default.Share,
                        onClick = onShareClick
                    )
                    HeaderIconButton(
                        icon = Icons.Default.Settings,
                        onClick = onSettingsClick
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(AppSpacing.ScreenPadding))
            
            // 日期选择器
            DatePickerChip(
                date = selectedDate,
                onClick = onDateClick,
                onPrevious = onDatePrevious,
                onNext = onDateNext
            )
            
            // 性别筛选器（仅在时长模式显示）
            if (isDurationMode && selectedGender != null && onGenderChange != null) {
                Spacer(modifier = Modifier.height(8.dp))
                GenderFilterInHeader(
                    selectedGender = selectedGender,
                    onGenderChange = onGenderChange
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // 统计卡片网格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = if (isDurationMode) "今日总时长" else "今日总音浪",
                    value = totalSound,
                    trend = "+12.5%",
                    isPositive = true
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = if (isDurationMode) "人员" else "今日开播/总数",
                    value = liveCount,
                    trend = "85% 开播率",
                    isPositive = true
                )
            }
        }
    }
}

@Composable
fun HeaderIconButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun DatePickerChip(
    date: String,
    onClick: () -> Unit,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左箭头按钮（仅在音浪模式显示）
        if (onPrevious != null) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "前一天",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // 日期选择器
        Surface(
            onClick = onClick,
            shape = AppShapes.ChipRadius,
            color = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.IconSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        
        // 右箭头按钮（仅在音浪模式显示）
        if (onNext != null) {
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "后一天",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    trend: String,
    isPositive: Boolean
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.CardRadiusSmall,
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPositive) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isPositive) Color(0xFFD1FAE5) else Color(0xFFFECACA),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = trend,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPositive) Color(0xFFD1FAE5) else Color(0xFFFECACA)
                )
            }
        }
    }
}


/**
 * 头部内的性别筛选器（白色半透明样式）
 */
@Composable
fun GenderFilterInHeader(
    selectedGender: Gender,
    onGenderChange: (Gender) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Gender.values().forEach { gender ->
            val isSelected = selectedGender == gender
            Surface(
                onClick = { onGenderChange(gender) },
                shape = AppShapes.ChipRadius,
                color = if (isSelected) {
                    Color.White.copy(alpha = 0.3f)
                } else {
                    Color.White.copy(alpha = 0.1f)
                },
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (gender) {
                            Gender.All -> "全部"
                            Gender.Male -> "男"
                            Gender.Female -> "女"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White
                    )
                }
            }
        }
    }
}
