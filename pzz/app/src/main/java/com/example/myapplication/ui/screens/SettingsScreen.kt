package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.ExportSettings
import com.example.myapplication.data.model.GenderFilter
import com.example.myapplication.data.model.SortField
import com.example.myapplication.data.model.SortOrder
import com.example.myapplication.data.model.displayName
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: ExportSettings,
    onSettingsChange: (ExportSettings) -> Unit,
    onBack: () -> Unit,
    onDeleteDateData: ((String) -> Unit)? = null,
    onDeleteDurationData: ((String, String) -> Unit)? = null,  // 新增：删除时长数据
    onDeleteAllLocalData: (() -> Unit)? = null,
    onPushLocalToRemote: ((String) -> Unit)? = null,
    onSyncStreamers: (() -> Unit)? = null,
    lastSyncTime: String = "从未同步",
    posterCompanyName: String = "星嗨艺创",
    posterEventTitle: String = "月度PK挑战赛排名",
    onPosterSettingsChange: ((String, String) -> Unit)? = null
) {
    // 使用 remember(settings) 确保当 settings 改变时重新初始化
    var currentSettings by remember(settings) { mutableStateOf(settings) }
    var currentCompanyName by remember(posterCompanyName) { mutableStateOf(posterCompanyName) }
    var currentEventTitle by remember(posterEventTitle) { mutableStateOf(posterEventTitle) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showCustomRangeDialog by remember { mutableStateOf(false) }
    var showDeleteDateDialog by remember { mutableStateOf(false) }
    var showDeleteDurationDialog by remember { mutableStateOf(false) }  // 新增：删除时长数据对话框
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showPushToRemoteDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSettingsChange(currentSettings)
                            onPosterSettingsChange?.invoke(currentCompanyName, currentEventTitle)
                            onBack()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 导出字段设置
            Text(
                text = "导出字段",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            // 使用网格布局，每行2个开关
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // 第一行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompactSettingSwitch(
                        title = "排名",
                        checked = currentSettings.showRank,
                        onCheckedChange = { currentSettings = currentSettings.copy(showRank = it) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CompactSettingSwitch(
                        title = "主播ID",
                        checked = currentSettings.showStreamerId,
                        onCheckedChange = { currentSettings = currentSettings.copy(showStreamerId = it) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // 第二行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompactSettingSwitch(
                        title = "名称",
                        checked = currentSettings.showStreamerName,
                        onCheckedChange = { currentSettings = currentSettings.copy(showStreamerName = it) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CompactSettingSwitch(
                        title = "当日音浪",
                        checked = currentSettings.showSoundWave,
                        onCheckedChange = { currentSettings = currentSettings.copy(showSoundWave = it) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // 第三行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompactSettingSwitch(
                        title = "总音浪",
                        checked = currentSettings.showTotalSoundWave,
                        onCheckedChange = { currentSettings = currentSettings.copy(showTotalSoundWave = it) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CompactSettingSwitch(
                        title = "等级",
                        checked = currentSettings.showGradeRank,
                        onCheckedChange = { currentSettings = currentSettings.copy(showGradeRank = it) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // 第四行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompactSettingSwitch(
                        title = "有效时长",
                        checked = currentSettings.showEffectiveBroadcastDuration,
                        onCheckedChange = { currentSettings = currentSettings.copy(showEffectiveBroadcastDuration = it) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CompactSettingSwitch(
                        title = "记录日期",
                        checked = currentSettings.showRecordDate,
                        onCheckedChange = { currentSettings = currentSettings.copy(showRecordDate = it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 字体设置
            Text(
                text = "字体设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 姓名字体大小调整
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "姓名字体大小",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${(currentSettings.nameTextScale * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Slider(
                        value = currentSettings.nameTextScale,
                        onValueChange = { 
                            currentSettings = currentSettings.copy(nameTextScale = it)
                        },
                        valueRange = 0.8f..1.5f,
                        steps = 13,  // 0.05为一步
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        text = "调整主播姓名的字体大小（80%-150%）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 重置按钮
                OutlinedButton(
                    onClick = {
                        currentSettings = currentSettings.copy(nameTextScale = 1.0f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("恢复默认")
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 排序设置
            Text(
                text = "排序设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingItem(
                title = "排序字段",
                value = currentSettings.sortBy.displayName(),
                onClick = { showSortDialog = true }
            )
            
            SettingItem(
                title = "排序方式",
                value = if (currentSettings.sortOrder == SortOrder.ASC) "升序" else "降序",
                onClick = {
                    currentSettings = currentSettings.copy(
                        sortOrder = if (currentSettings.sortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
                    )
                }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 性别筛选
            Text(
                text = "性别筛选",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GenderFilter.values().forEach { filter ->
                    FilterChip(
                        selected = currentSettings.genderFilter == filter,
                        onClick = { currentSettings = currentSettings.copy(genderFilter = filter) },
                        label = { Text(filter.displayName()) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 等级定级标准
            Text(
                text = "等级定级标准（万）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GradeSettingItem(
                    grade = "S级",
                    value = currentSettings.gradeS,
                    onValueChange = { currentSettings = currentSettings.copy(gradeS = it) }
                )
                GradeSettingItem(
                    grade = "A级",
                    value = currentSettings.gradeA,
                    onValueChange = { currentSettings = currentSettings.copy(gradeA = it) }
                )
                GradeSettingItem(
                    grade = "B级",
                    value = currentSettings.gradeB,
                    onValueChange = { currentSettings = currentSettings.copy(gradeB = it) }
                )
                GradeSettingItem(
                    grade = "C级",
                    value = currentSettings.gradeC,
                    onValueChange = { currentSettings = currentSettings.copy(gradeC = it) }
                )
                Text(
                    text = "D级：0-${currentSettings.gradeC}万（自动）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 音浪范围筛选
            Text(
                text = "音浪范围筛选",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            // 自定义范围筛选
            Text(
                text = "设置音浪范围（万）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            
            // 显示当前范围
            SettingItem(
                title = "音浪范围",
                value = formatRangeDisplay(currentSettings.customMinSoundWave, currentSettings.customMaxSoundWave),
                onClick = { showCustomRangeDialog = true }
            )
            
            // 快速选择按钮
            Text(
                text = "快速选择",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            currentSettings = currentSettings.copy(
                                customMinSoundWave = 3000000L,
                                customMaxSoundWave = Long.MAX_VALUE
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("≥300万", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = { 
                            currentSettings = currentSettings.copy(
                                customMinSoundWave = 1500000L,
                                customMaxSoundWave = 2999999L
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("150-300万", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            currentSettings = currentSettings.copy(
                                customMinSoundWave = 300000L,
                                customMaxSoundWave = 1499999L
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("30-150万", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = { 
                            currentSettings = currentSettings.copy(
                                customMinSoundWave = 100000L,
                                customMaxSoundWave = 299999L
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("10-30万", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            currentSettings = currentSettings.copy(
                                customMinSoundWave = 0L,
                                customMaxSoundWave = 99999L
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("<10万", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = { 
                            currentSettings = currentSettings.copy(
                                customMinSoundWave = 0L,
                                customMaxSoundWave = Long.MAX_VALUE
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("全部", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            // 删除日期数据
            if (onDeleteDateData != null || onDeleteDurationData != null || onDeleteAllLocalData != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "数据管理",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                
                if (onDeleteDateData != null) {
                    OutlinedButton(
                        onClick = { showDeleteDateDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("删除某一天的音浪数据")
                    }
                    
                    Text(
                        text = "删除后会自动调整后续日期的总音浪",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (onDeleteDurationData != null) {
                    OutlinedButton(
                        onClick = { showDeleteDurationDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("删除时长考勤数据")
                    }
                    
                    Text(
                        text = "删除指定月份的时长考勤数据（本地和远程）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // 推送本地数据到远程
                if (onPushLocalToRemote != null) {
                    OutlinedButton(
                        onClick = { showPushToRemoteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("推送本地数据到远程")
                    }
                    
                    Text(
                        text = "将本地指定日期的数据推送到远程数据库，用于修复导入失败的情况",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (onDeleteAllLocalData != null) {
                    OutlinedButton(
                        onClick = { showDeleteAllDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("清空所有本地数据")
                    }
                    
                    Text(
                        text = "将删除所有本地音浪数据和主播缓存，不影响远程数据库",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            
            // 海报设置
            if (onPosterSettingsChange != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "海报设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 公司名称
                    OutlinedTextField(
                        value = currentCompanyName,
                        onValueChange = { currentCompanyName = it },
                        label = { Text("公司名称") },
                        placeholder = { Text("例如：星嗨艺创") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // 活动标题
                    OutlinedTextField(
                        value = currentEventTitle,
                        onValueChange = { currentEventTitle = it },
                        label = { Text("活动标题") },
                        placeholder = { Text("例如：月度PK挑战赛排名") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Text(
                        text = "这些设置将应用于排行榜海报导出",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 主播名单同步
            if (onSyncStreamers != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "主播名单",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "上次同步时间：$lastSyncTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedButton(
                        onClick = onSyncStreamers,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("从数据库同步主播名单")
                    }
                    
                    Text(
                        text = "同步后将更新主播昵称和性别信息",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    if (showSortDialog) {
        SortFieldDialog(
            currentField = currentSettings.sortBy,
            onDismiss = { showSortDialog = false },
            onSelect = { field ->
                currentSettings = currentSettings.copy(sortBy = field)
                showSortDialog = false
            }
        )
    }
    
    if (showCustomRangeDialog) {
        CustomRangeDialog(
            minValue = currentSettings.customMinSoundWave,
            maxValue = currentSettings.customMaxSoundWave,
            onDismiss = { showCustomRangeDialog = false },
            onConfirm = { min, max ->
                currentSettings = currentSettings.copy(
                    customMinSoundWave = min,
                    customMaxSoundWave = max
                )
                showCustomRangeDialog = false
            }
        )
    }
    
    if (showDeleteDateDialog && onDeleteDateData != null) {
        DeleteDateDialog(
            onDismiss = { showDeleteDateDialog = false },
            onConfirm = { date ->
                onDeleteDateData(date)
                showDeleteDateDialog = false
            }
        )
    }
    
    if (showDeleteDurationDialog && onDeleteDurationData != null) {
        DeleteDurationDataDialog(
            onDismiss = { showDeleteDurationDialog = false },
            onConfirm = { startDate, endDate ->
                onDeleteDurationData(startDate, endDate)
                showDeleteDurationDialog = false
            }
        )
    }
    
    if (showPushToRemoteDialog && onPushLocalToRemote != null) {
        PushToRemoteDialog(
            onDismiss = { showPushToRemoteDialog = false },
            onConfirm = { date ->
                onPushLocalToRemote(date)
                showPushToRemoteDialog = false
            }
        )
    }
    
    if (showDeleteAllDialog && onDeleteAllLocalData != null) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("确认清空所有本地数据") },
            text = { 
                Text(
                    "此操作将删除：\n" +
                    "• 所有本地音浪数据\n" +
                    "• 所有主播缓存\n\n" +
                    "远程数据库不受影响，可以重新同步。\n\n" +
                    "确定要继续吗？",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAllLocalData()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("确认清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun SettingSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun CompactSettingSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
fun SettingItem(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title)
            Text(
                text = value,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SortFieldDialog(
    currentField: SortField,
    onDismiss: () -> Unit,
    onSelect: (SortField) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择排序字段") },
        text = {
            Column {
                SortField.values().forEach { field ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = field == currentField,
                            onClick = { onSelect(field) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(field.displayName())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun CustomRangeDialog(
    minValue: Long,
    maxValue: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    var minText by remember { mutableStateOf((minValue / 10000).toString()) }
    var maxText by remember { mutableStateOf(if (maxValue == Long.MAX_VALUE) "" else (maxValue / 10000).toString()) }
    var errorMessage by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义音浪范围") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "请输入音浪范围（单位：万）",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = minText,
                    onValueChange = { 
                        minText = it.filter { char -> char.isDigit() }
                        errorMessage = ""
                    },
                    label = { Text("最小值（万）") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = maxText,
                    onValueChange = { 
                        maxText = it.filter { char -> char.isDigit() }
                        errorMessage = ""
                    },
                    label = { Text("最大值（万）") },
                    placeholder = { Text("不限") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Text(
                    text = "提示：留空最大值表示不限",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val min = minText.toLongOrNull()?.times(10000) ?: 0L
                    val max = if (maxText.isEmpty()) {
                        Long.MAX_VALUE
                    } else {
                        maxText.toLongOrNull()?.times(10000) ?: Long.MAX_VALUE
                    }
                    
                    if (max != Long.MAX_VALUE && min > max) {
                        errorMessage = "最小值不能大于最大值"
                    } else {
                        onConfirm(min, max)
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

fun formatRangeDisplay(min: Long, max: Long): String {
    val minWan = min / 10000
    val maxWan = max / 10000
    
    return when {
        min == 0L && max == Long.MAX_VALUE -> "全部"
        max == Long.MAX_VALUE -> "≥${minWan}万"
        min == 0L -> "≤${maxWan}万"
        else -> "${minWan}-${maxWan}万"
    }
}

@Composable
fun GradeSettingItem(
    grade: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showDialog = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = grade,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "≥${value}万",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    
    if (showDialog) {
        var inputValue by remember { mutableStateOf(value.toString()) }
        var errorMessage by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("设置${grade}标准") },
            text = {
                Column {
                    Text("请输入最低音浪（单位：万）")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { 
                            inputValue = it.filter { char -> char.isDigit() }
                            errorMessage = ""
                        },
                        label = { Text("音浪（万）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newValue = inputValue.toIntOrNull()
                        if (newValue != null && newValue > 0) {
                            onValueChange(newValue)
                            showDialog = false
                        } else {
                            errorMessage = "请输入有效的数值"
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}


/**
 * 删除日期数据对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteDateDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedDate by remember { mutableStateOf(getCurrentDate()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除日期数据") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "选择要删除的日期",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDate,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "选择日期"
                        )
                    }
                }
                
                Text(
                    text = "警告：删除后无法恢复！\n删除该日期的数据后，系统会自动调整后续日期的总音浪。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDate) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("确认删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
    
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseDateToMillis(selectedDate)
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = formatMillisToDate(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// 辅助函数
private fun getCurrentDate(): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date())
}

private fun parseDateToMillis(dateStr: String): Long {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

private fun formatMillisToDate(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushToRemoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedDate by remember { mutableStateOf(getCurrentDate()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("推送本地数据到远程") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "选择要推送的日期",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDate,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "选择日期"
                        )
                    }
                }
                
                Text(
                    text = "将本地该日期的所有数据推送到远程数据库。\n如果远程已有该日期的数据，将会被覆盖。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDate) }
            ) {
                Text("确认推送")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
    
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseDateToMillis(selectedDate)
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = formatMillisToDate(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}


/**
 * 删除时长数据对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteDurationDataDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    var showYearPicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    
    // 计算日期范围
    val startDate = String.format("%04d-%02d-01", selectedYear, selectedMonth)
    val calendar = Calendar.getInstance()
    calendar.set(selectedYear, selectedMonth - 1, 1)
    val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val endDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, lastDay)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除时长考勤数据") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "选择要删除的月份",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 年份选择
                    OutlinedCard(
                        modifier = Modifier.weight(1f),
                        onClick = { showYearPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedYear}年",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "选择年份"
                            )
                        }
                    }
                    
                    // 月份选择
                    OutlinedCard(
                        modifier = Modifier.weight(1f),
                        onClick = { showMonthPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedMonth}月",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "选择月份"
                            )
                        }
                    }
                }
                
                Text(
                    text = "日期范围：$startDate ~ $endDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "警告：删除后无法恢复！\n将同时删除本地和远程数据库中该月份的所有时长考勤数据。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(startDate, endDate) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("确认删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
    
    // 年份选择对话框
    if (showYearPicker) {
        AlertDialog(
            onDismissRequest = { showYearPicker = false },
            title = { Text("选择年份") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    (currentYear - 2..currentYear + 1).forEach { year ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = year == selectedYear,
                                onClick = { 
                                    selectedYear = year
                                    showYearPicker = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${year}年")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showYearPicker = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 月份选择对话框
    if (showMonthPicker) {
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            title = { Text("选择月份") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..12).forEach { month ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = month == selectedMonth,
                                onClick = { 
                                    selectedMonth = month
                                    showMonthPicker = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${month}月")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMonthPicker = false }) {
                    Text("取消")
                }
            }
        )
    }
}
