# MainActivity 集成新组件指南

## 概述

本指南说明如何将新创建的UI组件集成到 MainActivity.kt 中，实现现代化的界面设计。

---

## 一、需要修改的部分

### 1. 导入语句

在文件顶部添加新组件的导入：

```kotlin
// 新增导入
import com.example.myapplication.ui.components.ModernHeader
import com.example.myapplication.ui.components.SegmentControl
import com.example.myapplication.ui.components.DataMode
import com.example.myapplication.ui.components.GenderFilter
import com.example.myapplication.ui.components.Gender
import com.example.myapplication.ui.components.StreamerCard
import com.example.myapplication.ui.components.FABMenu
import com.example.myapplication.ui.components.ModernBottomNav
import com.example.myapplication.ui.components.BottomNavTab
```

### 2. MainScreen 函数重构

**原有的 MainScreen 需要完全重写，使用新组件。**

#### 关键状态变量

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: SoundDataViewModel,
    showImportPreview: Boolean,
    importPreviewData: List<SoundData>,
    importPreviewDate: String,
    onDismissPreview: () -> Unit,
    onConfirmImport: (List<SoundData>, String) -> Unit,
    onImportCsv: (String) -> Unit,
    onNavigateToDuration: () -> Unit = {}
) {
    // 现有状态
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    
    // 新增状态
    var selectedMode by remember { mutableStateOf(DataMode.Sound) }
    var fabExpanded by remember { mutableStateOf(false) }
    var selectedBottomTab by remember { mutableStateOf(BottomNavTab.Dashboard) }
    
    // 其他现有状态...
    var showExportDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showStreamerManagement by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
```

#### 新的布局结构

```kotlin
    // 导入预览对话框（保持不变）
    if (showImportPreview) {
        ImportPreviewDialog(
            dataList = importPreviewData,
            recordDate = importPreviewDate,
            onDismiss = onDismissPreview,
            onConfirm = { onConfirmImport(importPreviewData, importPreviewDate) }
        )
    }
    
    // 设置界面（保持不变）
    if (showSettings) {
        // ... 现有的设置界面代码
    } else if (showStreamerManagement) {
        // ... 现有的主播管理界面代码
    } else {
        // 主界面 - 使用新组件
        Scaffold(
            bottomBar = {
                ModernBottomNav(
                    selectedTab = selectedBottomTab,
                    onTabSelected = { tab ->
                        selectedBottomTab = tab
                        when (tab) {
                            BottomNavTab.Dashboard -> { /* 已在主界面 */ }
                            BottomNavTab.Streamer -> { showStreamerManagement = true }
                            BottomNavTab.Sync -> {
                                // 触发同步
                                scope.launch {
                                    Toast.makeText(context, "正在同步数据...", Toast.LENGTH_SHORT).show()
                                    // 调用同步逻辑
                                }
                            }
                            BottomNavTab.Settings -> { showSettings = true }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. 现代化头部
                    ModernHeader(
                        selectedDate = selectedDate,
                        totalSound = when (uiState) {
                            is UiState.Success -> {
                                val stats = (uiState as UiState.Success).statistics
                                formatSoundValue(stats.totalSound)
                            }
                            else -> "0"
                        },
                        liveCount = when (uiState) {
                            is UiState.Success -> {
                                val stats = (uiState as UiState.Success).statistics
                                "${stats.liveCount} / ${stats.totalCount}"
                            }
                            else -> "0 / 0"
                        },
                        isDurationMode = selectedMode == DataMode.Duration,
                        onDateClick = { showDatePicker = true },
                        onPhoneClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:13354463148")
                            }
                            context.startActivity(intent)
                        },
                        onStreamerClick = { showStreamerManagement = true },
                        onSettingsClick = { showSettings = true }
                    )
                    
                    // 2. 内容区域
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = AppSpacing.ScreenPadding)
                    ) {
                        // 分段控制器
                        SegmentControl(
                            selectedMode = selectedMode,
                            onModeChange = { mode ->
                                selectedMode = mode
                                if (mode == DataMode.Duration) {
                                    onNavigateToDuration()
                                }
                            },
                            modifier = Modifier.padding(horizontal = AppSpacing.ScreenPadding)
                        )
                        
                        Spacer(modifier = Modifier.height(AppSpacing.ScreenPadding))
                        
                        // 列表头部
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.ScreenPadding + 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "排名 / 主播",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "音浪收入",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // 主播列表
                        when (uiState) {
                            is UiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is UiState.Success -> {
                                val dataList = (uiState as UiState.Success).data
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    itemsIndexed(dataList) { index, data ->
                                        StreamerCard(
                                            rank = index + 1,
                                            name = data.streamerName,
                                            value = if (data.dailySound > 0) {
                                                formatNumber(data.dailySound)
                                            } else {
                                                "未开播"
                                            },
                                            subValue = if (data.dailySound > 0) "音浪" else "",
                                            isLive = data.dailySound > 0,
                                            onClick = { /* 可选：点击事件 */ }
                                        )
                                    }
                                }
                            }
                            is UiState.Error -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (uiState as UiState.Error).message,
                                        color = Danger
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 3. FAB 菜单
                FABMenu(
                    isExpanded = fabExpanded,
                    onToggle = { fabExpanded = !fabExpanded },
                    onImport = {
                        onImportCsv(selectedDate)
                    },
                    onRefresh = {
                        viewModel.refreshData()
                        Toast.makeText(context, "刷新数据", Toast.LENGTH_SHORT).show()
                    },
                    onExport = {
                        showExportDialog = true
                    }
                )
            }
        }
    }
```

### 3. 辅助函数

添加格式化函数：

```kotlin
/**
 * 格式化音浪数值
 */
private fun formatSoundValue(sound: Long): String {
    return when {
        sound >= 10000 -> String.format("%.1f万", sound / 10000.0)
        else -> sound.toString()
    }
}

/**
 * 格式化数字（添加千位分隔符）
 */
private fun formatNumber(number: Long): String {
    return String.format("%,d", number)
}
```

---

## 二、DurationDataScreen 更新

### 更新时长界面使用新组件

在 `DurationDataScreen.kt` 中：

```kotlin
@Composable
fun DurationDataScreen(
    onBack: () -> Unit
) {
    val viewModel: DurationDataViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateRange by viewModel.dateRange.collectAsStateWithLifecycle()
    
    // 新增状态
    var selectedGender by remember { mutableStateOf(Gender.All) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    Scaffold(
        bottomBar = {
            ModernBottomNav(
                selectedTab = BottomNavTab.Dashboard,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.Dashboard -> onBack()
                        else -> { /* 处理其他导航 */ }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. 头部（紫色主题）
            ModernHeader(
                selectedDate = "${dateRange.first} ~ ${dateRange.second}",
                totalSound = when (uiState) {
                    is UiState.Success -> {
                        val data = (uiState as UiState.Success).data
                        "${data.size}"
                    }
                    else -> "0"
                },
                liveCount = when (uiState) {
                    is UiState.Success -> {
                        val data = (uiState as UiState.Success).data
                        val hasData = data.count { it.duration > 0 }
                        "$hasData / ${data.size}"
                    }
                    else -> "0 / 0"
                },
                isDurationMode = true,
                onDateClick = { showDateRangePicker = true },
                onPhoneClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:13354463148")
                    }
                    context.startActivity(intent)
                },
                onStreamerClick = { /* 打开主播管理 */ },
                onSettingsClick = { /* 打开设置 */ }
            )
            
            // 2. 内容区域
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = AppSpacing.ScreenPadding)
            ) {
                // 性别筛选器
                GenderFilter(
                    selectedGender = selectedGender,
                    onGenderChange = { selectedGender = it },
                    modifier = Modifier.padding(horizontal = AppSpacing.ScreenPadding)
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.Medium))
                
                // 列表头部
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.ScreenPadding + 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "排名 / 主播",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "有效时长",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // 时长列表
                when (uiState) {
                    is UiState.Success -> {
                        val dataList = (uiState as UiState.Success).data
                        // 根据性别筛选
                        val filteredList = when (selectedGender) {
                            Gender.All -> dataList
                            Gender.Male -> dataList.filter { it.gender == "男" }
                            Gender.Female -> dataList.filter { it.gender == "女" }
                        }
                        
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(filteredList) { index, data ->
                                StreamerCard(
                                    rank = index + 1,
                                    name = data.streamerName,
                                    value = if (data.duration > 0) {
                                        formatDuration(data.duration)
                                    } else {
                                        "缺勤"
                                    },
                                    subValue = "有效时长",
                                    gender = data.gender,
                                    onClick = { /* 可选：点击事件 */ }
                                )
                            }
                        }
                    }
                    // ... 其他状态处理
                }
            }
        }
    }
}

/**
 * 格式化时长（秒 -> XX小时XX分钟）
 */
private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return "${hours}小时${minutes}分钟"
}
```

---

## 三、实施步骤

### 步骤1：备份原文件
```bash
Copy-Item "MainActivity.kt" "MainActivity.kt.backup"
```

### 步骤2：添加导入语句
在 MainActivity.kt 顶部添加新组件的导入

### 步骤3：更新 MainScreen 函数
- 添加新的状态变量
- 使用 Scaffold + ModernBottomNav
- 替换头部为 ModernHeader
- 添加 SegmentControl
- 更新列表使用 StreamerCard
- 添加 FABMenu

### 步骤4：更新 DurationDataScreen
- 使用 ModernHeader（紫色主题）
- 添加 GenderFilter
- 更新列表使用 StreamerCard

### 步骤5：测试
- 编译项目
- 测试所有功能
- 检查动画效果
- 验证数据显示

---

## 四、关键注意事项

### 1. 状态管理
- `selectedMode` - 音浪/时长模式
- `fabExpanded` - FAB菜单展开状态
- `selectedBottomTab` - 底部导航选中项
- `selectedGender` - 性别筛选（时长界面）

### 2. 颜色切换
- 音浪模式：Primary (翠绿色)
- 时长模式：Secondary (紫色)
- ModernHeader 会根据 `isDurationMode` 自动切换颜色

### 3. 导航逻辑
- 底部导航点击处理
- 分段控制器切换到时长界面
- 返回按钮处理

### 4. 数据格式化
- 音浪数值：超过1万显示为"XX.X万"
- 数字：添加千位分隔符
- 时长：显示为"XX小时XX分钟"

---

## 五、验证清单

完成集成后，请验证：

- [ ] 头部显示正确（Logo、标题、图标、日期、统计）
- [ ] 分段控制器工作正常
- [ ] 主播列表显示正确（排名、名称、数据）
- [ ] 前三名显示奖杯图标
- [ ] ID已移除
- [ ] FAB菜单展开/收起正常
- [ ] 底部导航切换正常
- [ ] 时长界面性别筛选工作正常
- [ ] 所有动画流畅
- [ ] 颜色切换正确

---

## 六、常见问题

### Q1: 编译错误 - 找不到组件
**A:** 确保所有新组件文件都已创建，并且导入语句正确

### Q2: 底部导航不显示
**A:** 检查 Scaffold 的 bottomBar 参数是否正确设置

### Q3: FAB菜单位置不对
**A:** 确保 FABMenu 在 Box 中，并且有正确的 padding

### Q4: 颜色没有切换
**A:** 检查 `isDurationMode` 参数是否正确传递

---

**准备好开始集成了吗？**

建议先在 Android Studio 中打开项目，然后按照步骤逐步实施。
