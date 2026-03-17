package com.example.myapplication.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 直播时长导出对话框
 * 支持选择性别和时间范围
 */
@Composable
fun DurationExportDialog(
    currentDateRange: Pair<String, String>,
    onDismiss: () -> Unit,
    onExport: (selectedGender: String, startDate: String, endDate: String) -> Unit
) {
    var selectedGender by remember { mutableStateOf("全部") }
    var startDate by remember { mutableStateOf(currentDateRange.first) }
    var endDate by remember { mutableStateOf(currentDateRange.second) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出设置") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 时间范围选择
                Text(
                    text = "时间范围",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(startDate)
                    }
                    Text("至", modifier = Modifier.padding(top = 8.dp))
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(endDate)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 性别筛选
                Text(
                    text = "性别筛选",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("全部", "男", "女").forEach { gender ->
                        FilterChip(
                            selected = selectedGender == gender,
                            onClick = { selectedGender = gender },
                            label = { Text(gender) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "提示：导出字段在设置中配置",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onExport(selectedGender, startDate, endDate) }
            ) {
                Text("导出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
    
    // 开始日期选择器
    if (showStartDatePicker) {
        DatePickerDialog(
            selectedDate = startDate,
            onDateSelected = { date ->
                startDate = date
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }
    
    // 结束日期选择器
    if (showEndDatePicker) {
        DatePickerDialog(
            selectedDate = endDate,
            onDateSelected = { date ->
                endDate = date
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}
