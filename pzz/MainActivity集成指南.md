# MainActivity 集成直播时长功能指南

## 需要修改的文件

### 1. MainActivity.kt

在MainActivity中添加以下代码：

#### 1.1 添加ViewModel
```kotlin
private val durationViewModel: DurationDataViewModel by viewModels()
```

#### 1.2 添加CSV文件选择器（用于直播时长）
```kotlin
// 直播时长CSV文件选择器
private var durationStartDate: String = ""
private var durationEndDate: String = ""
private val durationCsvFilePicker = registerForActivityResult(
    ActivityResultContracts.GetContent()
) { uri ->
    uri?.let {
        handleDurationCsvImport(it, durationStartDate, durationEndDate)
    }
}
```

#### 1.3 添加处理直播时长CSV导入的方法
```kotlin
/**
 * 处理直播时长CSV导入
 */
private fun handleDurationCsvImport(uri: Uri, startDate: String, endDate: String) {
    lifecycleScope.launch {
        try {
            Toast.makeText(this@MainActivity, "正在读取CSV文件...", Toast.LENGTH_SHORT).show()
            
            val result = withContext(Dispatchers.IO) {
                DurationImportUtils.importFromCsv(
                    this@MainActivity,
                    uri,
                    startDate,
                    endDate
                )
            }
            
            if (result.isSuccess) {
                val dataList = result.getOrNull() ?: emptyList()
                showDurationImportPreviewDialog(dataList, startDate, endDate)
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "读取失败：${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                this@MainActivity,
                "处理失败：${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

/**
 * 显示直播时长导入预览对话框
 */
private fun showDurationImportPreviewDialog(
    dataList: List<DurationData>,
    startDate: String,
    endDate: String
) {
    showDurationImportPreview = true
    durationImportPreviewData = dataList
    durationImportStartDate = startDate
    durationImportEndDate = endDate
}

/**
 * 确认导入直播时长数据
 */
private fun confirmDurationImport(
    dataList: List<DurationData>,
    startDate: String,
    endDate: String
) {
    lifecycleScope.launch {
        Toast.makeText(
            this@MainActivity,
            "正在导入到本地数据库...",
            Toast.LENGTH_SHORT
        ).show()
        
        // 1. 先导入到本地数据库
        durationViewModel.importDataToLocal(dataList) { localResult ->
            if (localResult.isSuccess) {
                val count = localResult.getOrNull() ?: 0
                Toast.makeText(
                    this@MainActivity,
                    "本地导入成功！共${count}条数据，正在上传到远程...",
                    Toast.LENGTH_SHORT
                ).show()
                
                // 2. 自动上传到远程数据库
                durationViewModel.importDataToRemote(dataList) { remoteResult ->
                    if (remoteResult.isSuccess) {
                        Toast.makeText(
                            this@MainActivity,
                            "远程上传成功！",
                            Toast.LENGTH_SHORT
                        ).show()
                        // 切换到导入的日期范围
                        durationViewModel.selectDateRange(startDate, endDate)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "远程上传失败：${remoteResult.exceptionOrNull()?.message}\n本地数据已保存",
                            Toast.LENGTH_LONG
                        ).show()
                        // 即使远程上传失败，也切换到导入的日期范围
                        durationViewModel.selectDateRange(startDate, endDate)
                    }
                }
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "本地导入失败：${localResult.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
```

#### 1.4 添加状态变量
```kotlin
// 直播时长导入预览状态
private var showDurationImportPreview by mutableStateOf(false)
private var durationImportPreviewData by mutableStateOf<List<DurationData>>(emptyList())
private var durationImportStartDate by mutableStateOf("")
private var durationImportEndDate by mutableStateOf("")
```

#### 1.5 在MainScreen中添加导航
```kotlin
@Composable
fun MainScreen(
    viewModel: SoundDataViewModel,
    durationViewModel: DurationDataViewModel,  // 添加这个参数
    // ... 其他参数
) {
    // ... 现有代码
    
    var showDurationScreen by remember { mutableStateOf(false) }
    
    if (showDurationScreen) {
        // 显示直播时长界面
        val durationUiState by durationViewModel.uiState.collectAsStateWithLifecycle()
        val selectedDateRange by durationViewModel.selectedDateRange.collectAsStateWithLifecycle()
        
        when (val state = durationUiState) {
            is DurationUiState.Loading -> LoadingView()
            is DurationUiState.Success -> {
                DurationDataScreen(
                    data = state.data,
                    statistics = state.statistics,
                    selectedDateRange = selectedDateRange,
                    onBack = { showDurationScreen = false },
                    onImport = {
                        // 显示日期范围选择对话框
                        showDateRangePickerDialog = true
                    },
                    onSelectDateRange = {
                        // 显示日期范围选择对话框
                        showDateRangePickerDialog = true
                    }
                )
            }
            is DurationUiState.Error -> {
                ErrorView(message = state.message)
            }
        }
    } else {
        // 现有的音浪数据界面
        // ...
    }
}
```

#### 1.6 在底部导航栏添加按钮
```kotlin
bottomBar = {
    BottomAppBar(
        containerColor = Surface,
        contentColor = Primary
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomBarButton(
                icon = Icons.Filled.Add,
                label = "导入",
                onClick = { showImportSyncDialog = true }
            )
            
            BottomBarButton(
                icon = Icons.Filled.Share,
                label = "导出",
                onClick = { showExportOptionsDialog = true }
            )
            
            BottomBarButton(
                icon = Icons.Filled.AccessTime,  // 或其他合适的图标
                label = "时长",
                onClick = { showDurationScreen = true }
            )
            
            BottomBarButton(
                icon = Icons.Filled.Refresh,
                label = "同步",
                onClick = { viewModel.refreshData() }
            )
        }
    }
}
```

### 2. 需要导入的类

在MainActivity.kt顶部添加以下导入：

```kotlin
import com.example.myapplication.data.model.DurationData
import com.example.myapplication.ui.dialogs.DurationImportDialog
import com.example.myapplication.ui.dialogs.DateRangePickerDialog
import com.example.myapplication.ui.screens.DurationDataScreen
import com.example.myapplication.utils.DurationImportUtils
import com.example.myapplication.viewmodel.DurationDataViewModel
import com.example.myapplication.viewmodel.DurationUiState
import androidx.compose.material.icons.filled.AccessTime
```

### 3. 添加对话框

在MainScreen的Composable中添加：

```kotlin
// 直播时长导入预览对话框
if (showDurationImportPreview) {
    DurationImportDialog(
        dataList = durationImportPreviewData,
        startDate = durationImportStartDate,
        endDate = durationImportEndDate,
        onDismiss = { showDurationImportPreview = false },
        onConfirm = {
            showDurationImportPreview = false
            confirmDurationImport(
                durationImportPreviewData,
                durationImportStartDate,
                durationImportEndDate
            )
        }
    )
}

// 日期范围选择对话框
if (showDateRangePickerDialog) {
    val currentRange = durationViewModel.selectedDateRange.value
    DateRangePickerDialog(
        initialStartDate = currentRange.first,
        initialEndDate = currentRange.second,
        onDismiss = { showDateRangePickerDialog = false },
        onConfirm = { startDate, endDate ->
            showDateRangePickerDialog = false
            if (isSelectingForImport) {
                // 用于导入
                durationStartDate = startDate
                durationEndDate = endDate
                durationCsvFilePicker.launch("*/*")
            } else {
                // 用于查询
                durationViewModel.selectDateRange(startDate, endDate)
            }
        }
    )
}
```

## 完整的集成步骤

1. 确保所有新文件都已创建
2. 在MainActivity中添加上述代码
3. 在远程MySQL数据库中执行 `duration_data_table.sql` 创建表
4. 编译并运行应用
5. 测试导入功能

## 测试流程

1. 启动应用
2. 点击底部"时长"按钮
3. 点击右上角"+"按钮
4. 选择时间范围
5. 选择CSV文件
6. 预览数据
7. 确认导入
8. 查看导入结果

## 注意事项

1. 确保远程数据库已创建 `duration_data` 表
2. 确保CSV文件格式正确
3. 确保网络连接正常（用于远程同步）
4. 首次使用前需要同步主播名单
