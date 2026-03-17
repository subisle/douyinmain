package com.example.myapplication.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.DurationData
import com.example.myapplication.ui.theme.Primary
import com.example.myapplication.ui.theme.TextSecondary
import com.example.myapplication.utils.DurationImportUtils

/**
 * 直播时长导入预览对话框
 */
@Composable
fun DurationImportDialog(
    dataList: List<DurationData>,
    startDate: String,
    endDate: String,
    onDismiss: () -> Unit,
    onConfirm: (selectedGender: String) -> Unit
) {
    var selectedGender by remember { mutableStateOf("在人员表") }
    
    // 根据性别筛选数据
    val filteredData = remember(dataList, selectedGender) {
        when (selectedGender) {
            "男" -> dataList.filter { it.gender == "男" }
            "女" -> dataList.filter { it.gender == "女" }
            "在人员表" -> dataList.filter { it.gender.isNotEmpty() }
            else -> dataList
        }
    }
    
    // 统计信息
    val totalCount = dataList.size
    val maleCount = dataList.count { it.gender == "男" }
    val femaleCount = dataList.count { it.gender == "女" }
    val matchedCount = dataList.count { it.gender.isNotEmpty() }
    val unmatchedCount = totalCount - matchedCount
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("导入预览")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "时间范围: $startDate ~ $endDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                // 性别筛选按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("全部", "男", "女", "在人员表").forEach { gender ->
                        FilterChip(
                            selected = selectedGender == gender,
                            onClick = { selectedGender = gender },
                            label = { 
                                Text(
                                    text = when (gender) {
                                        "全部" -> "全部($totalCount)"
                                        "男" -> "男($maleCount)"
                                        "女" -> "女($femaleCount)"
                                        "在人员表" -> "在表($matchedCount)"
                                        else -> gender
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = androidx.compose.ui.graphics.Color.White
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 统计信息
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Primary.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = filteredData.size.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                                Text(
                                    text = "将导入",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val activeCount = filteredData.count {
                                    DurationImportUtils.parseDurationToSeconds(it.duration) > 0
                                }
                                Text(
                                    text = activeCount.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                                Text(
                                    text = "有时长",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        
                        if (unmatchedCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "提示：$unmatchedCount 个主播不在人员表中",
                                style = MaterialTheme.typography.bodySmall,
                                color = androidx.compose.ui.graphics.Color(0xFFFF9800),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 数据列表（显示前10条）
                Text(
                    text = "数据预览（前10条）",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredData.take(10)) { index, item ->
                        DurationPreviewItem(
                            rank = index + 1,
                            data = item
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedGender) },
                enabled = filteredData.isNotEmpty()
            ) {
                Text("导入 ${filteredData.size} 条")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun DurationPreviewItem(rank: Int, data: DurationData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$rank.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Primary,
                modifier = Modifier.width(30.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.streamerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = data.streamerId,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            
            Text(
                text = data.duration,
                style = MaterialTheme.typography.bodySmall,
                color = Primary
            )
        }
    }
}
