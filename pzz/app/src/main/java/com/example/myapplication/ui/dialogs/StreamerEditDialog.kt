package com.example.myapplication.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.Streamer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamerEditDialog(
    streamer: Streamer?,
    onDismiss: () -> Unit,
    onConfirm: (Streamer) -> Unit
) {
    var streamerId by remember { mutableStateOf(streamer?.streamerId ?: "") }
    var nickname by remember { mutableStateOf(streamer?.nickname ?: "") }
    var gender by remember { mutableStateOf(streamer?.gender ?: com.example.myapplication.data.model.Gender.MALE) }
    var errorMessage by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (streamer == null) "添加主播" else "编辑主播") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 主播ID
                OutlinedTextField(
                    value = streamerId,
                    onValueChange = { 
                        streamerId = it
                        errorMessage = ""
                    },
                    label = { Text("主播ID") },
                    placeholder = { Text("请输入主播ID") },
                    singleLine = true,
                    enabled = streamer == null,  // 编辑时不允许修改ID
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 昵称
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { 
                        nickname = it
                        errorMessage = ""
                    },
                    label = { Text("昵称") },
                    placeholder = { Text("请输入昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 性别选择
                Text(
                    text = "性别",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = gender == com.example.myapplication.data.model.Gender.MALE,
                        onClick = { gender = com.example.myapplication.data.model.Gender.MALE },
                        label = { Text("男") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = gender == com.example.myapplication.data.model.Gender.FEMALE,
                        onClick = { gender = com.example.myapplication.data.model.Gender.FEMALE },
                        label = { Text("女") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // 错误提示
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        streamerId.isBlank() -> {
                            errorMessage = "请输入主播ID"
                        }
                        nickname.isBlank() -> {
                            errorMessage = "请输入昵称"
                        }
                        else -> {
                            onConfirm(
                                Streamer(
                                    serialNumber = streamer?.serialNumber ?: 0,
                                    streamerId = streamerId.trim(),
                                    nickname = nickname.trim(),
                                    gender = gender
                                )
                            )
                        }
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
}
