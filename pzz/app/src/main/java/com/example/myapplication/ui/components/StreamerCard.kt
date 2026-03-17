package com.example.myapplication.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.*

/**
 * 现代化的主播卡片组件
 * 基于 app重构界面.html 的设计
 */
@Composable
fun StreamerCard(
    rank: Int,
    name: String,
    value: String,
    subValue: String = "",
    isLive: Boolean = false,
    gender: String? = null,
    onClick: () -> Unit = {}
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.ScreenPadding, vertical = 6.dp)
            .scale(scale),
        shape = AppShapes.CardRadius,
        color = CardBackground,
        shadowElevation = AppElevation.Card
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.CardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 排名
            RankBadge(rank = rank)
            
            Spacer(modifier = Modifier.width(10.dp))
            
            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (isLive) {
                        StatusDot(isLive = true)
                    }
                }
                if (gender != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = gender,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier
                            .background(
                                color = Color(0xFFF3F4F6),
                                shape = AppShapes.ButtonRadiusSmall
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            // 数据
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (subValue.isNotEmpty()) {
                    Text(
                        text = subValue,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun RankBadge(rank: Int) {
    val (color, showIcon) = when (rank) {
        1 -> Gold to true
        2 -> Silver to true
        3 -> Bronze to true
        else -> TextSecondary to false
    }
    
    Box(
        modifier = Modifier.width(30.dp),
        contentAlignment = Alignment.Center
    ) {
        if (showIcon) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun StatusDot(isLive: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(
                color = if (isLive) Primary else Color(0xFFD1D5DB),
                shape = CircleShape
            )
            .then(
                if (isLive) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        spotColor = Primary
                    )
                } else Modifier
            )
    )
}
