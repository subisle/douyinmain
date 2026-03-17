package com.example.myapplication.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.SoundData
import com.example.myapplication.ui.theme.*
import com.example.myapplication.utils.GenderUtils

@Composable
fun SoundDataList(
    data: List<SoundData>,
    modifier: Modifier = Modifier,
    onEdit: ((SoundData) -> Unit)? = null,
    onDelete: ((SoundData) -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(data) { item ->
            SoundDataItem(
                data = item,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
fun SoundDataItem(
    data: SoundData,
    onEdit: ((SoundData) -> Unit)? = null,
    onDelete: ((SoundData) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val genderUtils = GenderUtils.getInstance(context)
    val displayName = genderUtils.getDisplayName(data.streamerId, data.streamerName)
    
    // 使用主题感知的颜色
    val gradeColor = getGradeColor(data.gradeLetter)
    
    // 判断是否未开播
    val isNotBroadcasting = data.soundWave < 1L
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = gradeColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isNotBroadcasting) {
            // 未开播：红色边框，2dp宽度
            androidx.compose.foundation.BorderStroke(2.dp, Danger)
        } else {
            // 正常：无边框
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 第一行：排名、名字、等级
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getRankIcon(data.overallRank) + data.overallRank.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(60.dp)
                )
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = displayName,  // 使用昵称而不是全名
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    // 显示有效时长(如果有数据)
                    if (!data.effectiveBroadcastDuration.isNullOrEmpty()) {
                        Text(
                            text = "⏱ ${formatDuration(data.effectiveBroadcastDuration)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7c3aed)  // 紫色
                        )
                    }
                }
                
                Text(
                    text = data.gradeRank ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(50.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 第二行：当日音浪和总音浪
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "当日音浪",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 未开播时显示红色"未开播"，已开播显示当日音浪
                    if (isNotBroadcasting) {
                        Text(
                            text = "未开播",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Danger  // 红色
                        )
                    } else {
                        Text(
                            text = formatSoundWave(data.soundWave),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "总音浪",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatSoundWave(data.totalSoundWave),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // 操作按钮行
            if (onEdit != null || onDelete != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onEdit != null) {
                        IconButton(
                            onClick = { onEdit(data) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "编辑",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (onDelete != null) {
                        IconButton(
                            onClick = { onDelete(data) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getRankIcon(rank: Int): String {
    return when (rank) {
        1 -> "🥇 "
        2 -> "🥈 "
        3 -> "🥉 "
        else -> ""
    }
}

@Composable
fun getGradeColor(gradeLetter: String?): Color {
    val customColors = MaterialTheme.customColors
    return when (gradeLetter) {
        "S" -> customColors.gradeS
        "A" -> customColors.gradeA
        "B" -> customColors.gradeB
        "C" -> customColors.gradeC
        "D" -> customColors.gradeD
        else -> customColors.surface
    }
}

fun formatSoundWave(value: Long): String {
    return when {
        value >= 10000 -> {
            val wan = value / 10000  // 万位部分
            val remainder = value % 10000  // 余数部分
            if (remainder == 0L) {
                "${wan}万"
            } else {
                "${wan}万${remainder}"
            }
        }
        else -> value.toString()
    }
}

/**
 * 格式化时长为"x时x分"格式
 */
fun formatDuration(duration: String?): String {
    if (duration.isNullOrEmpty()) return "0时0分"
    
    return try {
        when {
            // 处理"2.5小时"格式
            duration.contains("小时") && duration.contains(".") -> {
                val hours = duration.replace("小时", "").toDoubleOrNull() ?: 0.0
                val totalMinutes = (hours * 60).toInt()
                val h = totalMinutes / 60
                val m = totalMinutes % 60
                "${h}时${m}分"
            }
            // 处理"2小时30分"格式
            duration.contains("小时") && duration.contains("分") -> {
                val parts = duration.split("小时")
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts.getOrNull(1)?.replace("分", "")?.toIntOrNull() ?: 0
                "${h}时${m}分"
            }
            // 处理"2小时"格式
            duration.contains("小时") -> {
                val h = duration.replace("小时", "").toIntOrNull() ?: 0
                "${h}时0分"
            }
            // 处理"150分钟"格式
            duration.contains("分钟") -> {
                val totalMinutes = duration.replace("分钟", "").toIntOrNull() ?: 0
                val h = totalMinutes / 60
                val m = totalMinutes % 60
                "${h}时${m}分"
            }
            // 处理"30分"格式
            duration.contains("分") -> {
                val m = duration.replace("分", "").toIntOrNull() ?: 0
                "0时${m}分"
            }
            else -> "0时0分"
        }
    } catch (e: Exception) {
        "0时0分"
    }
}
