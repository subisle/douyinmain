package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.Streamer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamerManagementScreen(
    streamers: List<Streamer>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Streamer) -> Unit,
    onDelete: (Streamer) -> Unit,
    onSync: () -> Unit,
    lastSyncTime: String = "从未同步"
) {
    var showDeleteDialog by remember { mutableStateOf<Streamer?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("主播管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, "添加主播")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 统计信息和同步按钮
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "主播总数：${streamers.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "男：${streamers.count { it.gender == com.example.myapplication.data.model.Gender.MALE }} | 女：${streamers.count { it.gender == com.example.myapplication.data.model.Gender.FEMALE }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "上次同步：$lastSyncTime",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Button(onClick = onSync) {
                            Text("从数据库同步")
                        }
                    }
                }
            }
            
            // 主播列表 - 按性别分组显示
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 男主播分组
                val maleStreamers = streamers.filter { it.gender == com.example.myapplication.data.model.Gender.MALE }
                if (maleStreamers.isNotEmpty()) {
                    item {
                        Text(
                            text = "男主播（${maleStreamers.size}位）",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(maleStreamers) { streamer ->
                        StreamerItem(
                            streamer = streamer,
                            index = maleStreamers.indexOf(streamer) + 1,
                            onEdit = { onEdit(streamer) },
                            onDelete = { showDeleteDialog = streamer }
                        )
                    }
                }
                
                // 女主播分组
                val femaleStreamers = streamers.filter { it.gender == com.example.myapplication.data.model.Gender.FEMALE }
                if (femaleStreamers.isNotEmpty()) {
                    item {
                        Text(
                            text = "女主播（${femaleStreamers.size}位）",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(femaleStreamers) { streamer ->
                        StreamerItem(
                            streamer = streamer,
                            index = femaleStreamers.indexOf(streamer) + 1,
                            onEdit = { onEdit(streamer) },
                            onDelete = { showDeleteDialog = streamer }
                        )
                    }
                }
            }
        }
    }
    
    // 删除确认对话框
    showDeleteDialog?.let { streamer ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除主播 ${streamer.nickname}（${streamer.streamerId}）吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(streamer)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun StreamerItem(
    streamer: Streamer,
    index: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 序号
            Text(
                text = "$index",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = streamer.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID: ${streamer.streamerId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "性别: ${streamer.gender.toDisplayString()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (streamer.gender == com.example.myapplication.data.model.Gender.MALE) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )
            }
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
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
