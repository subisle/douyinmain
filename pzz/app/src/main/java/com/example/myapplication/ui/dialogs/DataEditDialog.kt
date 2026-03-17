package com.example.myapplication.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.SoundData

/**
 * 数据编辑对话框
 * 用于添加或编辑主播数据
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataEditDialog(
    data: SoundData? = null,  // null表示新增，非null表示编辑
    recordDate: String,
    onDismiss: () -> Unit,
    onSave: (SoundData) -> Unit
) {
    var streamerId by remember { mutableStateOf(data?.streamerId ?: "") }
    var streamerName by remember { mutableStateOf(data?.streamerName ?: "") }
    var soundWave by remember { mutableStateOf(data?.soundWave?.toString() ?: "0") }
    var totalSoundWave by remember { mutableStateOf(data?.totalSoundWave?.toString() ?: "0") }
    var effectiveBroadcastDuration by remember { mutableStateOf(data?.effectiveBroadcastDuration ?: "") }
    
    var streamerIdError by remember { mutableStateOf(false) }
    var streamerNameError by remember { mutableStateOf(false) }
    
    val isEditMode = data != null
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (isEditMode) "编辑数据" else "添加数据") 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 主播ID
                OutlinedTextField(
                    value = streamerId,
                    onValueChange = { 
                        streamerId = it
                        streamerIdError = it.isBlank()
                    },
                    label = { Text("主播ID *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isEditMode,  // 编辑模式下不允许修改ID
                    isError = streamerIdError,
                    supportingText = if (streamerIdError) {
                        { Text("主播ID不能为空") }
                    } else null
                )
                
                // 主播名称
                OutlinedTextField(
                    value = streamerName,
                    onValueChange = { 
                        streamerName = it
                        streamerNameError = it.isBlank()
                    },
                    label = { Text("主播名称 *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = streamerNameError,
                    supportingText = if (streamerNameError) {
                        { Text("主播名称不能为空") }
                    } else null
                )
                
                // 当日声浪
                OutlinedTextField(
                    value = soundWave,
                    onValueChange = { soundWave = it.filter { char -> char.isDigit() } },
                    label = { Text("当日声浪") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                // 总声浪
                OutlinedTextField(
                    value = totalSoundWave,
                    onValueChange = { totalSoundWave = it.filter { char -> char.isDigit() } },
                    label = { Text("总声浪") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                // 有效直播时长
                OutlinedTextField(
                    value = effectiveBroadcastDuration,
                    onValueChange = { effectiveBroadcastDuration = it },
                    label = { Text("有效直播时长") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如: 2小时30分") }
                )
                
                // 日期显示
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("记录日期", style = MaterialTheme.typography.bodyMedium)
                        Text(recordDate, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                
                Text(
                    text = "* 必填项",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 验证
                    if (streamerId.isBlank()) {
                        streamerIdError = true
                        return@Button
                    }
                    if (streamerName.isBlank()) {
                        streamerNameError = true
                        return@Button
                    }
                    
                    // 创建数据对象
                    val soundData = SoundData(
                        id = data?.id ?: 0,
                        streamerId = streamerId,
                        streamerName = streamerName,
                        soundWave = soundWave.toLongOrNull() ?: 0L,
                        totalSoundWave = totalSoundWave.toLongOrNull() ?: 0L,
                        effectiveBroadcastDuration = effectiveBroadcastDuration.ifBlank { null },
                        recordDate = recordDate,
                        gradeRank = data?.gradeRank,
                        gradeLetter = data?.gradeLetter,
                        overallRank = data?.overallRank ?: 0,
                        createdAt = data?.createdAt
                    )
                    
                    onSave(soundData)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 删除确认对话框
 */
@Composable
fun DeleteConfirmDialog(
    streamerName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = { 
            Text("确定要删除主播 \"$streamerName\" 的数据吗？\n\n此操作不可恢复。") 
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
