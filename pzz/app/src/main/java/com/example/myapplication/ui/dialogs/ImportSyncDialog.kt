package com.example.myapplication.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * 导入和同步对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSyncDialog(
    onDismiss: () -> Unit,
    onImportCsv: (String) -> Unit,
    onSyncFromRemote: (String, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("导入CSV", "同步数据")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("数据管理") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Tab选择
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                when (selectedTab) {
                    0 -> ImportCsvTab(
                        onImport = { date ->
                            onImportCsv(date)
                            onDismiss()
                        }
                    )
                    1 -> SyncDataTab(
                        onSync = { startDate, endDate ->
                            onSyncFromRemote(startDate, endDate)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 导入CSV标签页
 */
@Composable
fun ImportCsvTab(
    onImport: (String) -> Unit
) {
    var selectedDate by remember { mutableStateOf(getCurrentDate()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "从CSV文件导入数据到远程数据库",
            style = MaterialTheme.typography.bodyMedium
        )
        
        // 日期选择
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "导入日期",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = selectedDate,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, "选择日期")
                }
            }
        }
        
        Text(
            text = "说明：\n1. 选择要导入的日期\n2. 点击导入按钮选择CSV文件\n3. 数据将导入到远程数据库",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = { onImport(selectedDate) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("选择CSV文件并导入")
        }
    }
    
    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it },
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * 同步数据标签页
 */
@Composable
fun SyncDataTab(
    onSync: (String, String) -> Unit
) {
    var startDate by remember { mutableStateOf(getDateOffset(-7)) }
    var endDate by remember { mutableStateOf(getCurrentDate()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "从远程数据库同步数据到本地",
            style = MaterialTheme.typography.bodyMedium
        )
        
        // 开始日期
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "开始日期",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = startDate,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                IconButton(onClick = { showStartDatePicker = true }) {
                    Icon(Icons.Default.DateRange, "选择开始日期")
                }
            }
        }
        
        // 结束日期
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "结束日期",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = endDate,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                IconButton(onClick = { showEndDatePicker = true }) {
                    Icon(Icons.Default.DateRange, "选择结束日期")
                }
            }
        }
        
        // 快捷选择
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    startDate = getDateOffset(-7)
                    endDate = getCurrentDate()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("最近7天", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(
                onClick = {
                    startDate = getDateOffset(-30)
                    endDate = getCurrentDate()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("最近30天", style = MaterialTheme.typography.bodySmall)
            }
        }
        
        Text(
            text = "说明：\n1. 选择要同步的日期范围\n2. 点击同步按钮\n3. 数据将从远程数据库下载到本地",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = { onSync(startDate, endDate) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("开始同步")
        }
    }
    
    if (showStartDatePicker) {
        DatePickerDialog(
            selectedDate = startDate,
            onDateSelected = { startDate = it },
            onDismiss = { showStartDatePicker = false }
        )
    }
    
    if (showEndDatePicker) {
        DatePickerDialog(
            selectedDate = endDate,
            onDateSelected = { endDate = it },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

/**
 * 日期选择对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = parseDateToMillis(selectedDate)
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(formatMillisToDate(millis))
                    }
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

// 辅助函数
private fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}

private fun getDateOffset(offset: Int): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, offset)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(calendar.time)
}

private fun parseDateToMillis(dateStr: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

private fun formatMillisToDate(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(millis))
}
