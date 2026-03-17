package com.example.myapplication.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * 排行榜海报导出对话框
 */
@Composable
fun RankingPosterExportDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var isExporting by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "导出排行榜海报",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Text(
                    text = "将根据当前筛选的数据生成排行榜海报图片。\n\n" +
                          "• 按总音浪排序\n" +
                          "• 显示等级而非音浪数值\n" +
                          "• 自动分为两张图片，每张单列显示\n" +
                          "• 头像使用png目录下的图片\n" +
                          "• 可在设置中修改公司名称和活动标题",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (isExporting) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "正在生成海报...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isExporting
                    ) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            isExporting = true
                            onConfirm()
                        },
                        enabled = !isExporting
                    ) {
                        Text("导出")
                    }
                }
            }
        }
    }
}
