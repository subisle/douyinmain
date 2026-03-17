package com.example.myapplication.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期范围选择对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    initialStartDate: String,
    initialEndDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间范围") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 开始日期
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("开始日期: $startDate")
                }
                
                // 结束日期
                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("结束日期: $endDate")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (startDate <= endDate) {
                        onConfirm(startDate, endDate)
                    }
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
    )
    
    // 开始日期选择器
    if (showStartDatePicker) {
        SimpleDatePickerDialog(
            initialDate = startDate,
            onDismiss = { showStartDatePicker = false },
            onConfirm = { date ->
                startDate = date
                showStartDatePicker = false
            }
        )
    }
    
    // 结束日期选择器
    if (showEndDatePicker) {
        SimpleDatePickerDialog(
            initialDate = endDate,
            onDismiss = { showEndDatePicker = false },
            onConfirm = { date ->
                endDate = date
                showEndDatePicker = false
            }
        )
    }
}

/**
 * 简单日期选择器对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePickerDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance()
    
    try {
        calendar.time = sdf.parse(initialDate) ?: Date()
    } catch (e: Exception) {
        calendar.time = Date()
    }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.timeInMillis
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Date(millis)
                        val dateStr = sdf.format(date)
                        onConfirm(dateStr)
                    }
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
