package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.model.SoundData
import com.example.myapplication.data.model.Statistics
import com.example.myapplication.data.local.PreferencesManager
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.dialogs.ImportPreviewDialog
import com.example.myapplication.ui.dialogs.RankingPosterExportDialog
import com.example.myapplication.ui.screens.SettingsScreen
import com.example.myapplication.ui.theme.*
import com.example.myapplication.utils.ExportUtils
import com.example.myapplication.utils.GenderUtils
import com.example.myapplication.utils.RankingPosterExportUtils
import com.example.myapplication.utils.TotalSoundUpdateUtils
import com.example.myapplication.utils.TotalSoundUpdateUtils.UpdateResult
import com.example.myapplication.viewmodel.SoundDataViewModel
import com.example.myapplication.viewmodel.UiState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * 屏幕导航枚举
 */
enum class Screen {
    SoundData,      // 日音浪数据
    Duration        // 直播时长数据
}

class MainActivity : ComponentActivity() {
    private val viewModel: SoundDataViewModel by viewModels()
    
    // 当前显示的屏幕
    private var currentScreen by mutableStateOf<Screen>(Screen.SoundData)
    
    // 导入预览状态
    private var showImportPreview by mutableStateOf(false)
    private var importPreviewData by mutableStateOf<List<SoundData>>(emptyList())
    private var importPreviewDate by mutableStateOf("")
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 静默处理权限结果，不显示Toast
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            // 只在权限被拒绝时提示
            Toast.makeText(this, "需要存储权限才能导出文件", Toast.LENGTH_LONG).show()
        }
    }
    
    // CSV文件选择器
    private var importDate: String = ""
    private val csvFilePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            handleCsvImport(it, importDate)
        }
    }
    
    // 总音浪更新文件选择器
    private var updateTotalSoundDate: String = ""
    private val totalSoundCsvPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            handleTotalSoundUpdate(it, updateTotalSoundDate)
        }
    }
    
    // 日音浪更新文件选择器
    private var updateDailySoundDate: String = ""
    private val dailySoundCsvPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            handleDailySoundUpdate(it, updateDailySoundDate)
        }
    }
    
    // 总音浪覆盖文件选择器
    private var overwriteTotalSoundDate: String = ""
    private val overwriteTotalSoundCsvPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            handleOverwriteTotalSound(it, overwriteTotalSoundDate)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 请求权限
        requestStoragePermissions()
        
        // 启动时自动同步主播名单
        autoSyncStreamersOnStartup()
        
        // 启动时自动同步最新音浪数据
        autoSyncLatestDataOnStartup()
        
        setContent {
            MyApplicationTheme {
                AppNavigation(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> currentScreen = screen },
                    viewModel = viewModel,
                    showImportPreview = showImportPreview,
                    importPreviewData = importPreviewData,
                    importPreviewDate = importPreviewDate,
                    onDismissPreview = { showImportPreview = false },
                    onConfirmImport = { data, date ->
                        showImportPreview = false
                        confirmImport(data, date)
                    },
                    onImportCsv = { date ->
                        importDate = date
                        csvFilePicker.launch("*/*")
                    },
                    onUpdateTotalSound = { date ->
                        updateTotalSoundDate = date
                        totalSoundCsvPicker.launch("*/*")
                    },
                    onUpdateDailySound = { date ->
                        updateDailySoundDate = date
                        dailySoundCsvPicker.launch("*/*")
                    },
                    onOverwriteTotalSound = { date ->
                        overwriteTotalSoundDate = date
                        overwriteTotalSoundCsvPicker.launch("*/*")
                    }
                )
            }
        }
    }
    
    private fun requestStoragePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        requestPermissionLauncher.launch(permissions)
    }
    
    /**
     * 启动时自动同步主播名单
     */
    private fun autoSyncStreamersOnStartup() {
        lifecycleScope.launch {
            try {
                val genderUtils = GenderUtils.getInstance(this@MainActivity)
                
                // 检查是否需要同步（如果缓存为空或超过24小时未同步）
                val lastSyncTime = genderUtils.getLastSyncTime()
                val currentTime = System.currentTimeMillis()
                val hoursSinceSync = (currentTime - lastSyncTime) / (1000 * 60 * 60)
                
                if (lastSyncTime == 0L || hoursSinceSync >= 24) {
                    android.util.Log.d("MainActivity", "开始自动同步主播名单")
                    
                    val result = withContext(Dispatchers.IO) {
                        genderUtils.syncFromDatabase()
                    }
                    
                    if (result.isSuccess) {
                        android.util.Log.d("MainActivity", "主播名单同步成功")
                    } else {
                        android.util.Log.w("MainActivity", "主播名单同步失败: ${result.exceptionOrNull()?.message}")
                    }
                } else {
                    android.util.Log.d("MainActivity", "主播名单缓存有效，跳过同步")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "自动同步主播名单失败", e)
            }
        }
    }
    
    /**
     * 启动时自动同步最新音浪数据
     * 注意：已禁用自动同步，避免覆盖本地数据
     * 用户可以手动点击同步按钮来同步数据
     */
    private fun autoSyncLatestDataOnStartup() {
        // 禁用自动同步功能
        // 原因：自动同步会从远程数据库获取最新数据并覆盖本地
        // 这可能导致用户刚导入的数据被覆盖
        // 用户可以通过底部导航栏的"同步"按钮手动同步
        
        android.util.Log.d("MainActivity", "自动同步已禁用，用户可手动同步")
        
        /* 如果需要启用自动同步，取消下面的注释
        lifecycleScope.launch {
            try {
                android.util.Log.d("MainActivity", "开始自动同步最新音浪数据")
                
                // 延迟1秒，等待UI初始化完成
                kotlinx.coroutines.delay(1000)
                
                viewModel.syncLatestData { result ->
                    if (result.isSuccess) {
                        val message = result.getOrNull() ?: "同步成功"
                        android.util.Log.d("MainActivity", "最新数据同步成功: $message")
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "未知错误"
                        android.util.Log.w("MainActivity", "最新数据同步失败: $error")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "自动同步最新数据失败", e)
            }
        }
        */
    }
    
    /**
     * 处理CSV导入
     */
    private fun handleCsvImport(uri: Uri, recordDate: String) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "正在读取CSV文件...", Toast.LENGTH_SHORT).show()
                
                val result = withContext(Dispatchers.IO) {
                    android.util.Log.d("MainActivity", "========== 开始导入流程 ==========")
                    android.util.Log.d("MainActivity", "导入日期: $recordDate")
                    
                    // 从本地数据库查询每个主播在导入日期之前的最新总音浪
                    val localLatestTotalSoundWave = try {
                        val database = com.example.myapplication.data.local.AppDatabase.getDatabase(this@MainActivity)
                        val dao = database.soundDataDao()
                        
                        // 获取所有主播信息
                        val allStreamers = dao.getAllStreamers()
                        android.util.Log.d("MainActivity", "主播总数: ${allStreamers.size}")
                        
                        // 为每个主播查询最近一次的数据（不限定日期）
                        val latestDataList = mutableListOf<SoundData>()
                        var foundCount = 0
                        var notFoundCount = 0
                        
                        allStreamers.forEach { streamerInfo ->
                            // 查询该主播在导入日期之前的最新数据
                            val latestData = dao.getLatestDataBeforeDate(streamerInfo.streamerId, recordDate)
                            
                            if (latestData != null) {
                                latestDataList.add(latestData)
                                foundCount++
                                
                                // 打印前5个找到的数据
                                if (foundCount <= 5) {
                                    android.util.Log.d("MainActivity", "✅ 找到: ID=${latestData.streamerId}, 名字=${latestData.streamerName}, 日期=${latestData.recordDate}, 总音浪=${latestData.totalSoundWave}")
                                }
                            } else {
                                notFoundCount++
                                
                                // 打印前5个未找到的
                                if (notFoundCount <= 5) {
                                    android.util.Log.d("MainActivity", "⚠️ 未找到: ID=${streamerInfo.streamerId}, 名字=${streamerInfo.streamerName}")
                                }
                            }
                        }
                        
                        android.util.Log.d("MainActivity", "查询结果: 找到${foundCount}个, 未找到${notFoundCount}个")
                        
                        latestDataList
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "获取本地最新总音浪失败: ${e.message}", e)
                        emptyList()
                    }
                    
                    android.util.Log.d("MainActivity", "本地最新总音浪数据: ${localLatestTotalSoundWave.size}条")
                    
                    // 导入CSV，传入本地最新总音浪数据
                    val importResult = com.example.myapplication.utils.ImportUtils.importFromCsv(
                        this@MainActivity,
                        uri,
                        recordDate,
                        localLatestTotalSoundWave
                    )
                    
                    android.util.Log.d("MainActivity", "========== 导入流程结束 ==========")
                    importResult
                }
                
                if (result.isSuccess) {
                    val dataList = result.getOrNull() ?: emptyList()
                    
                    // 显示预览对话框
                    showImportPreviewDialog(dataList, recordDate)
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
     * 显示导入预览对话框
     */
    private fun showImportPreviewDialog(dataList: List<SoundData>, recordDate: String) {
        showImportPreview = true
        importPreviewData = dataList
        importPreviewDate = recordDate
    }
    
    /**
     * 确认导入数据
     * 只导入到本地数据库
     */
    private fun confirmImport(dataList: List<SoundData>, recordDate: String) {
        lifecycleScope.launch {
            Toast.makeText(
                this@MainActivity,
                "正在导入到本地数据库...",
                Toast.LENGTH_SHORT
            ).show()
            
            viewModel.importDataToLocal(dataList) { localResult ->
                if (localResult.isSuccess) {
                    val localCount = localResult.getOrNull() ?: 0
                    android.util.Log.d("MainActivity", "本地导入成功: ${localCount}条")
                    
                    Toast.makeText(
                        this@MainActivity,
                        "导入完成！\n\n📊 数据量: ${localCount}条\n💾 本地保存: ✅ 成功",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // 切换到导入的日期
                    viewModel.selectDate(recordDate)
                } else {
                    val errorMessage = "本地导入失败：${localResult.exceptionOrNull()?.message}"
                    android.util.Log.e("MainActivity", errorMessage)
                    
                    Toast.makeText(
                        this@MainActivity,
                        "❌ ${errorMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    /**
     * 处理总音浪更新
     */
    private fun handleTotalSoundUpdate(uri: Uri, targetDate: String) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "正在读取CSV文件...", Toast.LENGTH_SHORT).show()
                
                viewModel.updateTotalSoundWave(this@MainActivity, uri, targetDate) { result ->
                    if (result.isSuccess) {
                        val updateResult = result.getOrNull()!!
                        Toast.makeText(
                            this@MainActivity,
                            "更新完成！\n" +
                            "CSV数据: ${updateResult.totalCount}条\n" +
                            "本地成功: ${updateResult.successCount}条\n" +
                            "远程更新: ${updateResult.remoteSyncSuccess}条\n" +
                            "远程插入: ${updateResult.remoteInserted}条\n" +
                            "失败: ${updateResult.failCount + updateResult.remoteSyncFail}条",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // 🔥 关键修复：刷新界面显示更新后的数据
                        viewModel.selectDate(targetDate)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "更新失败：${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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
     * 处理日音浪更新（数据校准）
     */
    private fun handleDailySoundUpdate(uri: Uri, targetDate: String) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "正在读取CSV文件...", Toast.LENGTH_SHORT).show()
                
                viewModel.updateDailySoundWave(this@MainActivity, uri, targetDate) { result ->
                    if (result.isSuccess) {
                        val updateResult = result.getOrNull()!!
                        
                        // 构建详细的结果信息
                        val message = buildString {
                            append("日音浪更新完成！\n")
                            append("CSV数据: ${updateResult.totalCount}条\n")
                            append("本地更新: ${updateResult.successCount}条\n")
                            append("本地插入: ${updateResult.localInserted}条\n")
                            append("远程更新: ${updateResult.remoteSyncSuccess}条\n")
                            append("远程插入: ${updateResult.remoteInserted}条\n")
                            
                            val totalFail = updateResult.failCount + updateResult.remoteSyncFail
                            if (totalFail > 0) {
                                append("失败: ${totalFail}条\n")
                                append("\n⚠️ 远程同步失败，请查看日志")
                            }
                        }
                        
                        Toast.makeText(
                            this@MainActivity,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // 🔥 关键修复：刷新界面显示更新后的数据
                        viewModel.selectDate(targetDate)
                        
                        // 如果有远程同步失败，显示详细对话框
                        if (updateResult.remoteSyncFail > 0) {
                            android.app.AlertDialog.Builder(this@MainActivity)
                                .setTitle("远程同步失败")
                                .setMessage(
                                    "有 ${updateResult.remoteSyncFail} 条记录未能同步到远程数据库。\n\n" +
                                    "可能原因：\n" +
                                    "1. 网络连接问题\n" +
                                    "2. 数据库连接失败\n" +
                                    "3. SQL执行失败\n\n" +
                                    "请查看Logcat日志（过滤DailySoundUpdateUtils）获取详细信息。"
                                )
                                .setPositiveButton("查看日志") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setNegativeButton("关闭") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .show()
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "更新失败：${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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
     * 处理总音浪覆盖（从主播榜CSV）
     */
    private fun handleOverwriteTotalSound(uri: Uri, targetDate: String) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "正在读取主播榜CSV文件...", Toast.LENGTH_SHORT).show()
                
                viewModel.overwriteTotalSoundWave(this@MainActivity, uri, targetDate) { result ->
                    if (result.isSuccess) {
                        val overwriteResult = result.getOrNull()!!
                        
                        val message = buildString {
                            append("总音浪覆盖完成！\n\n")
                            append("CSV数据: ${overwriteResult.totalCount}条\n")
                            append("匹配主播: ${overwriteResult.matchedCount}个\n")
                            append("本地更新: ${overwriteResult.localUpdatedCount}条\n")
                            append("远程更新: ${overwriteResult.remoteUpdatedCount}条\n")
                            
                            if (overwriteResult.notFoundCount > 0) {
                                append("未找到: ${overwriteResult.notFoundCount}条\n")
                            }
                            
                            if (overwriteResult.failedCount > 0) {
                                append("失败: ${overwriteResult.failedCount}条")
                            }
                        }
                        
                        Toast.makeText(
                            this@MainActivity,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // 🔥 关键修复：刷新界面显示更新后的数据
                        viewModel.selectDate(targetDate)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "覆盖失败：${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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
}

/**
 * 应用导航组件
 */
@Composable
fun AppNavigation(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    viewModel: SoundDataViewModel,
    showImportPreview: Boolean,
    importPreviewData: List<SoundData>,
    importPreviewDate: String,
    onDismissPreview: () -> Unit,
    onConfirmImport: (List<SoundData>, String) -> Unit,
    onImportCsv: (String) -> Unit,
    onUpdateTotalSound: (String) -> Unit,
    onUpdateDailySound: (String) -> Unit,
    onOverwriteTotalSound: (String) -> Unit
) {
    when (currentScreen) {
        Screen.SoundData -> MainScreen(
            viewModel = viewModel,
            showImportPreview = showImportPreview,
            importPreviewData = importPreviewData,
            importPreviewDate = importPreviewDate,
            onDismissPreview = onDismissPreview,
            onConfirmImport = onConfirmImport,
            onImportCsv = onImportCsv,
            onUpdateTotalSound = onUpdateTotalSound,
            onUpdateDailySound = onUpdateDailySound,
            onOverwriteTotalSound = onOverwriteTotalSound,
            onNavigateToDuration = { onNavigate(Screen.Duration) }
        )
        Screen.Duration -> {
            DurationScreenWrapper(
                onBack = { onNavigate(Screen.SoundData) }
            )
        }
    }
}

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
    onUpdateTotalSound: (String) -> Unit,
    onUpdateDailySound: (String) -> Unit,
    onOverwriteTotalSound: (String) -> Unit,
    onNavigateToDuration: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    
    // 新增状态 - 用于新UI组件
    var selectedMode by remember { mutableStateOf(DataMode.Sound) }
    var fabExpanded by remember { mutableStateOf(false) }
    var selectedBottomTab by remember { mutableStateOf(BottomNavTab.Dashboard) }
    var showDurationScreen by remember { mutableStateOf(false) }  // 新增：显示时长考勤页面
    
    // 初始化DurationDataViewModel
    val durationViewModel: com.example.myapplication.viewmodel.DurationDataViewModel = 
        androidx.lifecycle.viewmodel.compose.viewModel()
    
    // 原有状态
    var showExportDialog by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showImportSyncDialog by remember { mutableStateOf(false) }
    var showUpdateTotalSoundDialog by remember { mutableStateOf(false) }
    var showUpdateDailySoundDialog by remember { mutableStateOf(false) }
    var showOverwriteTotalSoundDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRankingPosterExportDialog by remember { mutableStateOf(false) }
    var showStreamerManagement by remember { mutableStateOf(false) }
    var showStreamerEditDialog by remember { mutableStateOf(false) }
    var editingStreamer by remember { mutableStateOf<com.example.myapplication.data.model.Streamer?>(null) }
    var editingData by remember { mutableStateOf<SoundData?>(null) }
    var deletingData by remember { mutableStateOf<SoundData?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // PreferencesManager
    val prefsManager = remember { PreferencesManager.getInstance(context) }
    var posterCompanyName by remember { mutableStateOf(prefsManager.getPosterCompanyName()) }
    var posterEventTitle by remember { mutableStateOf(prefsManager.getPosterEventTitle()) }
    
    // 导入预览对话框
    if (showImportPreview) {
        ImportPreviewDialog(
            dataList = importPreviewData,
            recordDate = importPreviewDate,
            onDismiss = onDismissPreview,
            onConfirm = { onConfirmImport(importPreviewData, importPreviewDate) }
        )
    }
    
    // 统一的Scaffold结构,确保底部导航栏始终显示
    Scaffold(
        bottomBar = {
            ModernBottomNav(
                selectedTab = selectedBottomTab,
                onTabSelected = { tab ->
                    selectedBottomTab = tab
                    when (tab) {
                        BottomNavTab.Dashboard -> {
                            showSettings = false
                            showStreamerManagement = false
                            showDurationScreen = false
                        }
                        BottomNavTab.Streamer -> {
                            showSettings = false
                            showStreamerManagement = true
                            showDurationScreen = false
                        }
                        BottomNavTab.Settings -> {
                            showSettings = true
                            showStreamerManagement = false
                            showDurationScreen = false
                        }
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
            when {
                showSettings -> {
                    val genderUtils = remember { GenderUtils.getInstance(context) }
                    val lastSyncTime = remember { genderUtils.getLastSyncTimeString() }
                    var currentSyncTime by remember { mutableStateOf(lastSyncTime) }
                    
                    SettingsScreen(
            settings = viewModel.getExportSettings(),
            onSettingsChange = { settings ->
                viewModel.saveExportSettings(settings)
            },
            onBack = { showSettings = false },
            onDeleteDateData = { date ->
                viewModel.deleteDateDataAndRecalculate(date) { result ->
                    if (result.isSuccess) {
                        Toast.makeText(
                            context,
                            result.getOrNull() ?: "删除成功",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "删除失败：${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            onDeleteDurationData = { startDate, endDate ->
                scope.launch {
                    Toast.makeText(context, "正在删除时长数据...", Toast.LENGTH_SHORT).show()
                    
                    durationViewModel.deleteDateRangeData(startDate, endDate) { result ->
                        if (result.isSuccess) {
                            Toast.makeText(
                                context,
                                result.getOrNull() ?: "删除成功",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "删除失败：${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            },
            onPushLocalToRemote = { date ->
                scope.launch {
                    try {
                        Toast.makeText(context, "正在推送数据到远程...", Toast.LENGTH_SHORT).show()
                        
                        viewModel.pushLocalDataToRemote(date) { result ->
                            if (result.isSuccess) {
                                val count = result.getOrNull() ?: 0
                                Toast.makeText(
                                    context,
                                    "推送成功！共推送 $count 条数据",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "推送失败：${result.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "推送失败：${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            onDeleteAllLocalData = {
                scope.launch {
                    try {
                        Toast.makeText(context, "正在清空本地数据...", Toast.LENGTH_SHORT).show()
                        
                        // 删除所有音浪数据
                        viewModel.deleteAllLocalData { result ->
                            if (result.isSuccess) {
                                // 删除主播缓存
                                val cacheResult = genderUtils.clearCache()
                                
                                val message = if (cacheResult.isSuccess) {
                                    "${result.getOrNull()}\n${cacheResult.getOrNull()}"
                                } else {
                                    "${result.getOrNull()}\n主播缓存清除失败"
                                }
                                
                                Toast.makeText(
                                    context,
                                    message,
                                    Toast.LENGTH_LONG
                                ).show()
                                
                                // 更新同步时间显示
                                currentSyncTime = genderUtils.getLastSyncTimeString()
                            } else {
                                Toast.makeText(
                                    context,
                                    "清空失败：${result.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "清空失败：${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            onSyncStreamers = {
                scope.launch {
                    try {
                        Toast.makeText(context, "正在同步主播名单...", Toast.LENGTH_SHORT).show()
                        
                        val result = withContext(Dispatchers.IO) {
                            genderUtils.syncFromDatabase()
                        }
                        
                        if (result.isSuccess) {
                            currentSyncTime = genderUtils.getLastSyncTimeString()
                            Toast.makeText(
                                context,
                                result.getOrNull() ?: "同步成功",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "同步失败：${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "同步失败：${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            lastSyncTime = currentSyncTime,
            posterCompanyName = posterCompanyName,
            posterEventTitle = posterEventTitle,
            onPosterSettingsChange = { companyName, eventTitle ->
                posterCompanyName = companyName
                posterEventTitle = eventTitle
                prefsManager.setPosterCompanyName(companyName)
                prefsManager.setPosterEventTitle(eventTitle)
            }
        )
                }
                showDurationScreen -> {
                    // 时长考勤页面
                    DurationScreenWrapper(
                        onBack = { 
                            showDurationScreen = false
                            selectedBottomTab = BottomNavTab.Dashboard
                        }
                    )
                }
                showStreamerManagement -> {
                    val genderUtils = remember(showStreamerManagement) { GenderUtils.getInstance(context) }
                    val streamers = remember(showStreamerManagement) { genderUtils.getAllStreamersWithGender() }
                    var streamerList by remember(showStreamerManagement) { mutableStateOf(streamers) }
                    val lastSyncTime = remember(showStreamerManagement) { genderUtils.getLastSyncTimeString() }
                    var currentSyncTime by remember(showStreamerManagement) { mutableStateOf(lastSyncTime) }
                    
                    com.example.myapplication.ui.screens.StreamerManagementScreen(
            streamers = streamerList,
            onBack = { showStreamerManagement = false },
            onAdd = {
                editingStreamer = null
                showStreamerEditDialog = true
            },
            onEdit = { streamer ->
                editingStreamer = streamer
                showStreamerEditDialog = true
            },
            onDelete = { streamer ->
                scope.launch {
                    try {
                        Toast.makeText(context, "正在删除主播...", Toast.LENGTH_SHORT).show()
                        
                        val result = withContext(Dispatchers.IO) {
                            genderUtils.deleteStreamer(streamer.streamerId)
                        }
                        
                        if (result.isSuccess) {
                            streamerList = genderUtils.getAllStreamersWithGender()
                            Toast.makeText(
                                context,
                                result.getOrNull() ?: "删除成功",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "删除失败：${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "删除失败：${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onSync = {
                scope.launch {
                    try {
                        Toast.makeText(context, "正在同步主播名单...", Toast.LENGTH_SHORT).show()
                        
                        val result = withContext(Dispatchers.IO) {
                            genderUtils.syncFromDatabase()
                        }
                        
                        if (result.isSuccess) {
                            streamerList = genderUtils.getAllStreamersWithGender()
                            currentSyncTime = genderUtils.getLastSyncTimeString()
                            Toast.makeText(
                                context,
                                result.getOrNull() ?: "同步成功",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "同步失败：${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "同步失败：${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            lastSyncTime = currentSyncTime
        )
                    
                    // 主播编辑对话框
                    if (showStreamerEditDialog) {
            com.example.myapplication.ui.dialogs.StreamerEditDialog(
                streamer = editingStreamer,
                onDismiss = { showStreamerEditDialog = false },
                onConfirm = { streamer ->
                    scope.launch {
                        try {
                            val isAdd = editingStreamer == null
                            Toast.makeText(
                                context,
                                if (isAdd) "正在添加主播..." else "正在更新主播...",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            val result = withContext(Dispatchers.IO) {
                                if (isAdd) {
                                    genderUtils.addStreamer(streamer)
                                } else {
                                    genderUtils.updateStreamer(editingStreamer!!.streamerId, streamer)
                                }
                            }
                            
                            if (result.isSuccess) {
                                streamerList = genderUtils.getAllStreamersWithGender()
                                showStreamerEditDialog = false
                                Toast.makeText(
                                    context,
                                    result.getOrNull() ?: if (isAdd) "添加成功" else "更新成功",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    result.exceptionOrNull()?.message ?: "操作失败",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "操作失败：${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // 1. 现代化头部
                            ModernHeader(
                        selectedDate = selectedDate,
                        totalSound = when (uiState) {
                            is UiState.Success -> {
                                val stats = (uiState as UiState.Success).statistics
                                formatSoundValue(stats.totalSoundWave)
                            }
                            else -> "0"
                        },
                        liveCount = when (uiState) {
                            is UiState.Success -> {
                                val stats = (uiState as UiState.Success).statistics
                                "${stats.activeCount} / ${stats.totalCount}"
                            }
                            else -> "0 / 0"
                        },
                        isDurationMode = selectedMode == DataMode.Duration,
                        onDateClick = { showDatePicker = true },
                        onDatePrevious = {
                            // 切换到前一天
                            val previousDate = getPreviousDate(selectedDate)
                            viewModel.selectDate(previousDate)
                        },
                        onDateNext = {
                            // 切换到后一天
                            val nextDate = getNextDate(selectedDate)
                            viewModel.selectDate(nextDate)
                        },
                        onPhoneClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:13354463148")
                            }
                            context.startActivity(intent)
                        },
                        onShareClick = { showExportOptionsDialog = true },
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
                                    CircularProgressIndicator(color = Primary)
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
                                            value = if (data.soundWave > 0) {
                                                formatNumber(data.soundWave)
                                            } else {
                                                "未开播"
                                            },
                                            subValue = "总浪 ${formatNumber(data.totalSoundWave)}",
                                            isLive = data.soundWave > 0,
                                            onClick = {
                                                editingData = data
                                                showEditDialog = true
                                            }
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
                                fabExpanded = false
                                showImportSyncDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                viewModel.selectDate(date)
                showDatePicker = false
            },
            currentDate = selectedDate
        )
    }
    
    // 导出选项对话框
    if (showExportOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showExportOptionsDialog = false },
            title = { Text("选择导出方式") },
            text = {
                Column {
                    Text("请选择导出图片的方式：")
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 保存到相册
                    TextButton(
                        onClick = {
                            showExportOptionsDialog = false
                            showExportDialog = true  // 触发保存到相册
                        }
                    ) {
                        Text("保存到相册")
                    }
                    
                    // 分享图片到微信
                    TextButton(
                        onClick = {
                            showExportOptionsDialog = false
                            // 导出并分享到微信
                            scope.launch {
                                try {
                                    val data = withContext(Dispatchers.IO) {
                                        viewModel.getCurrentData()
                                    }
                                    val settings = viewModel.getExportSettings()
                                    val result = withContext(Dispatchers.IO) {
                                        com.example.myapplication.utils.ExportUtils.exportToPngForShare(
                                            context, data, selectedDate, settings
                                        )
                                    }
                                    result.onSuccess { filePath ->
                                        // 分享到微信
                                        val shareSuccess = com.example.myapplication.utils.ShareUtils.shareImageToWeChat(
                                            context, filePath
                                        )
                                        if (!shareSuccess) {
                                            Toast.makeText(context, "分享失败，请检查微信是否已安装", Toast.LENGTH_SHORT).show()
                                        }
                                    }.onFailure { e ->
                                        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Text("分享图片")
                    }
                    
                    // 分享CSV到微信
                    TextButton(
                        onClick = {
                            showExportOptionsDialog = false
                            scope.launch {
                                try {
                                    Toast.makeText(context, "正在生成CSV文件...", Toast.LENGTH_SHORT).show()
                                    
                                    val data = withContext(Dispatchers.IO) {
                                        viewModel.getCurrentData()
                                    }
                                    
                                    android.util.Log.d("MainActivity", "获取到数据: ${data.size}条")
                                    
                                    val result = viewModel.generateCsvForShare(context, data, selectedDate)
                                    
                                    result.onSuccess { filePath ->
                                        android.util.Log.d("MainActivity", "CSV导出成功: $filePath")
                                        val shareSuccess = com.example.myapplication.utils.ShareUtils.shareFileToWeChat(
                                            context, filePath, "text/csv"
                                        )
                                        if (!shareSuccess) {
                                            Toast.makeText(context, "分享失败，请检查微信是否已安装", Toast.LENGTH_SHORT).show()
                                        }
                                    }.onFailure { e ->
                                        android.util.Log.e("MainActivity", "CSV导出失败", e)
                                        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "分享CSV异常", e)
                                    Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Text("分享表格")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportOptionsDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    if (showExportDialog) {
        // 直接导出PNG，不显示对话框
        LaunchedEffect(Unit) {
            try {
                val data = withContext(Dispatchers.IO) {
                    viewModel.getCurrentData()
                }
                val settings = viewModel.getExportSettings()
                val result = withContext(Dispatchers.IO) {
                    ExportUtils.exportToPng(context, data, selectedDate, settings)
                }
                result.onSuccess { message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }.onFailure { e ->
                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
            showExportDialog = false
        }
    }
    
    if (showRankingPosterExportDialog) {
        RankingPosterExportDialog(
            onDismiss = { showRankingPosterExportDialog = false },
            onConfirm = {
                scope.launch {
                    try {
                        // 获取当前数据
                        val data = withContext(Dispatchers.IO) {
                            viewModel.getCurrentData()
                        }
                        
                        val result = RankingPosterExportUtils.exportRankingPosters(
                            context = context,
                            data = data,
                            date = selectedDate
                        )
                        
                        if (result.isSuccess) {
                            val filePaths = result.getOrNull() ?: emptyList()
                            Toast.makeText(
                                context,
                                "导出成功！共生成${filePaths.size}张图片",
                                Toast.LENGTH_LONG
                            ).show()
                            
                            // 分享图片
                            if (filePaths.isNotEmpty()) {
                                val uris = filePaths.map { path ->
                                    androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        java.io.File(path)
                                    )
                                }
                                
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND_MULTIPLE
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                    type = "image/png"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "分享排行榜"))
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "导出失败：${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "导出失败：${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    } finally {
                        showRankingPosterExportDialog = false
                    }
                }
            }
        )
    }
    
    if (showImportSyncDialog) {
        com.example.myapplication.ui.dialogs.ImportSyncDialog(
            onDismiss = { showImportSyncDialog = false },
            onImportCsv = { date ->
                onImportCsv(date)
            },
            onSyncFromRemote = { startDate, endDate ->
                viewModel.syncDataByDateRange(startDate, endDate) { result ->
                    if (result.isSuccess) {
                        val count = result.getOrNull()?.size ?: 0
                        Toast.makeText(
                            context,
                            "同步成功！共同步${count}条数据",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "同步失败：${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
    
    // 更新总音浪日期选择对话框
    if (showUpdateTotalSoundDialog) {
        DatePickerDialog(
            onDismiss = { showUpdateTotalSoundDialog = false },
            onDateSelected = { date ->
                showUpdateTotalSoundDialog = false
                onUpdateTotalSound(date)
            },
            currentDate = selectedDate
        )
    }
    
    // 更新日音浪日期选择对话框
    if (showUpdateDailySoundDialog) {
        DatePickerDialog(
            onDismiss = { showUpdateDailySoundDialog = false },
            onDateSelected = { date ->
                showUpdateDailySoundDialog = false
                onUpdateDailySound(date)
            },
            currentDate = selectedDate
        )
    }
    
    // 覆盖总音浪日期选择对话框
    if (showOverwriteTotalSoundDialog) {
        DatePickerDialog(
            onDismiss = { showOverwriteTotalSoundDialog = false },
            onDateSelected = { date ->
                showOverwriteTotalSoundDialog = false
                onOverwriteTotalSound(date)
            },
            currentDate = selectedDate
        )
    }
    
    if (showEditDialog) {
        com.example.myapplication.ui.dialogs.DataEditDialog(
            data = editingData,
            recordDate = selectedDate,
            onDismiss = { 
                showEditDialog = false
                editingData = null
            },
            onSave = { data ->
                if (editingData == null) {
                    // 新增
                    viewModel.addData(data) { result ->
                        if (result.isSuccess) {
                            Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                "添加失败：${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    // 更新
                    viewModel.updateData(data) { result ->
                        if (result.isSuccess) {
                            Toast.makeText(context, "更新成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                "更新失败：${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                showEditDialog = false
                editingData = null
            }
        )
    }
    
    if (showDeleteDialog && deletingData != null) {
        val dataToDelete = deletingData  // 创建局部变量避免智能转换问题
        com.example.myapplication.ui.dialogs.DeleteConfirmDialog(
            streamerName = dataToDelete!!.streamerName,
            onDismiss = {
                showDeleteDialog = false
                deletingData = null
            },
            onConfirm = {
                viewModel.deleteData(dataToDelete) { result ->
                    if (result.isSuccess) {
                        Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            context,
                            "删除失败：${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                showDeleteDialog = false
                deletingData = null
            }
        )
    }
}

/**
 * 直播时长界面包装器
 * 负责管理ViewModel和状态
 */
@Composable
fun DurationScreenWrapper(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? ComponentActivity
    val viewModel: com.example.myapplication.viewmodel.DurationDataViewModel = 
        androidx.lifecycle.viewmodel.compose.viewModel()
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDateRange by viewModel.selectedDateRange.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    var showDateRangePicker by remember { mutableStateOf(false) }
    var showDateRangePickerForImport by remember { mutableStateOf(false) }
    var showImportPreview by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var importPreviewData by remember { mutableStateOf<List<com.example.myapplication.data.model.DurationData>>(emptyList()) }
    var importStartDate by remember { mutableStateOf("") }
    var importEndDate by remember { mutableStateOf("") }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    
    // CSV文件选择器 - 选择文件后弹出日期范围选择
    val csvFilePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingImportUri = it
            showDateRangePickerForImport = true
        }
    }
    
    // 导入用的日期范围选择器
    if (showDateRangePickerForImport) {
        com.example.myapplication.ui.dialogs.DateRangePickerDialog(
            initialStartDate = selectedDateRange.first,
            initialEndDate = selectedDateRange.second,
            onDismiss = { 
                showDateRangePickerForImport = false
                pendingImportUri = null
            },
            onConfirm = { startDate, endDate ->
                showDateRangePickerForImport = false
                pendingImportUri?.let { uri ->
                    scope.launch {
                        try {
                            Toast.makeText(context, "正在读取CSV文件...", Toast.LENGTH_SHORT).show()
                            
                            // 1. 读取CSV数据
                            val result = withContext(Dispatchers.IO) {
                                com.example.myapplication.utils.DurationImportUtils.importFromCsv(
                                    context, uri, startDate, endDate
                                )
                            }
                            
                            if (result.isSuccess) {
                                val newDataList = result.getOrNull() ?: emptyList()
                                
                                // 2. 查询数据库中已有的数据
                                val existingDataList = withContext(Dispatchers.IO) {
                                    val database = com.example.myapplication.data.local.AppDatabase.getDatabase(context)
                                    val dao = database.durationDataDao()
                                    dao.getDataByDateRangeSync(startDate, endDate)
                                }
                                
                                // 3. 如果有已存在的数据,计算增量并显示
                                val (processedDataList, hasIncrement) = if (existingDataList.isNotEmpty()) {
                                    // 创建已有数据的映射 (streamerId -> DurationData)
                                    val existingDataMap = existingDataList.associateBy { it.streamerId }
                                    
                                    // 合并新旧数据,计算增量用于显示
                                    val mergedList = mutableListOf<com.example.myapplication.data.model.DurationData>()
                                    var hasAnyIncrement = false
                                    
                                    newDataList.forEach { newData ->
                                        val existingData = existingDataMap[newData.streamerId]
                                        
                                        if (existingData != null) {
                                            // 主播已存在,计算增量
                                            val newSeconds = com.example.myapplication.utils.DurationImportUtils.parseDurationToSeconds(newData.duration)
                                            val existingSeconds = com.example.myapplication.utils.DurationImportUtils.parseDurationToSeconds(existingData.duration)
                                            val incrementSeconds = newSeconds - existingSeconds
                                            
                                            if (incrementSeconds > 0) {
                                                // 有增量,显示增量信息（只显示小时）
                                                val hours = incrementSeconds / 3600
                                                val incrementDuration = "${hours}小时"
                                                
                                                // 在预览中显示增量,但实际保存时会使用新的总时长
                                                mergedList.add(newData.copy(
                                                    duration = "$incrementDuration (增量)"
                                                ))
                                                hasAnyIncrement = true
                                            }
                                            // 如果没有增量(<=0),不添加到预览列表
                                        } else {
                                            // 新主播,直接添加
                                            mergedList.add(newData)
                                            hasAnyIncrement = true
                                        }
                                    }
                                    
                                    Pair(mergedList, hasAnyIncrement)
                                } else {
                                    // 没有已存在的数据,直接使用新数据
                                    Pair(newDataList, true)
                                }
                                
                                if (!hasIncrement || processedDataList.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "没有新增的时长数据",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    // 保存原始的新数据列表(用于实际导入)
                                    importPreviewData = newDataList  // 注意:这里保存的是完整的新数据,不是增量
                                    importStartDate = startDate
                                    importEndDate = endDate
                                    showImportPreview = true
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "读取失败：${result.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "处理失败：${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                pendingImportUri = null
            }
        )
    }
    
    // 导入预览对话框
    if (showImportPreview) {
        com.example.myapplication.ui.dialogs.DurationImportDialog(
            dataList = importPreviewData,
            startDate = importStartDate,
            endDate = importEndDate,
            onDismiss = { showImportPreview = false },
            onConfirm = { selectedGender ->
                showImportPreview = false
                
                // 根据选择的性别筛选数据
                val filteredData = when (selectedGender) {
                    "男" -> importPreviewData.filter { it.gender == "男" }
                    "女" -> importPreviewData.filter { it.gender == "女" }
                    "在人员表" -> importPreviewData.filter { it.gender.isNotEmpty() }
                    else -> importPreviewData
                }
                
                // 只导入到本地数据库（移除远程数据库推送）
                viewModel.importDataToLocal(filteredData) { localResult ->
                    if (localResult.isSuccess) {
                        Toast.makeText(
                            context,
                            "导入成功！",
                            Toast.LENGTH_SHORT
                        ).show()
                        // 更新选中的日期范围并刷新数据
                        viewModel.selectDateRange(importStartDate, importEndDate)
                    } else {
                        Toast.makeText(
                            context,
                            "导入失败：${localResult.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
    
    // 日期范围选择器
    if (showDateRangePicker) {
        com.example.myapplication.ui.dialogs.DateRangePickerDialog(
            initialStartDate = selectedDateRange.first,
            initialEndDate = selectedDateRange.second,
            onDismiss = { showDateRangePicker = false },
            onConfirm = { startDate, endDate ->
                showDateRangePicker = false
                viewModel.selectDateRange(startDate, endDate)
            }
        )
    }
    
    // 导出对话框
    if (showExportDialog) {
        com.example.myapplication.ui.dialogs.DurationExportDialog(
            currentDateRange = selectedDateRange,
            onDismiss = { showExportDialog = false },
            onExport = { selectedGender, startDate, endDate ->
                showExportDialog = false
                scope.launch {
                    try {
                        Toast.makeText(context, "正在导出...", Toast.LENGTH_SHORT).show()
                        
                        // 根据选择的时间范围查询数据
                        val data = withContext(Dispatchers.IO) {
                            val database = com.example.myapplication.data.local.AppDatabase.getDatabase(context)
                            val dao = database.durationDataDao()
                            val allData = dao.getDataByDateRangeSync(startDate, endDate)
                            
                            // 根据性别筛选
                            when (selectedGender) {
                                "男" -> allData.filter { it.gender == "男" }
                                "女" -> allData.filter { it.gender == "女" }
                                else -> allData
                            }
                        }
                        
                        val result = withContext(Dispatchers.IO) {
                            // 获取全局导出设置
                            val globalSettings = viewModel.getExportSettings()
                            
                            // 只覆盖性别筛选,其他字段使用全局设置
                            val settings = globalSettings.copy(
                                genderFilter = when (selectedGender) {
                                    "男" -> com.example.myapplication.data.model.GenderFilter.MALE
                                    "女" -> com.example.myapplication.data.model.GenderFilter.FEMALE
                                    else -> com.example.myapplication.data.model.GenderFilter.ALL
                                }
                            )
                            
                            com.example.myapplication.utils.DurationExportUtils.exportToPng(
                                context, data, Pair(startDate, endDate), settings
                            )
                        }
                        
                        result.onSuccess { message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }.onFailure { e ->
                            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
    
    // 根据UI状态显示不同内容
    when (val state = uiState) {
        is com.example.myapplication.viewmodel.DurationUiState.Loading -> {
            LoadingView()
        }
        is com.example.myapplication.viewmodel.DurationUiState.Success -> {
            com.example.myapplication.ui.screens.DurationDataScreen(
                data = state.data,
                statistics = state.statistics,
                selectedDateRange = selectedDateRange,
                onBack = onBack,
                onImport = { csvFilePicker.launch("*/*") },
                onSelectDateRange = { showDateRangePicker = true },
                onExport = { showExportDialog = true },
                onPhoneClick = {
                    val phoneIntent = Intent(Intent.ACTION_DIAL)
                    phoneIntent.data = Uri.parse("tel:13354463148")
                    context.startActivity(phoneIntent)
                },
                onSettingsClick = {
                    // 返回主界面并打开设置
                    onBack()
                    // 注意：这里需要在MainActivity中处理打开设置的逻辑
                }
            )
        }
        is com.example.myapplication.viewmodel.DurationUiState.Error -> {
            ErrorView(
                message = state.message,
                onRetry = { viewModel.refreshData() }
            )
        }
    }
}

@Composable
fun BottomBarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = Primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "加载中...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = com.example.myapplication.ui.theme.Danger,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("重试")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit,
    currentDate: String
) {
    val datePickerState = rememberDatePickerState()
    
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val date = sdf.format(Date(millis))
                        onDateSelected(date)
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

/**
 * 格式化音浪数值
 * 超过1万显示为"XX.X万"
 */
private fun formatSoundValue(sound: Long): String {
    return when {
        sound >= 10000 -> String.format("%.1f万", sound / 10000.0)
        else -> sound.toString()
    }
}

/**
 * 格式化数字为中文格式
 * 例如：42224 -> "4万2224"，150000 -> "15万"
 */
private fun formatNumber(number: Long): String {
    return when {
        number >= 10000 -> {
            val wan = number / 10000
            val remainder = number % 10000
            if (remainder == 0L) {
                "${wan}万"
            } else {
                "${wan}万${remainder}"
            }
        }
        else -> number.toString()
    }
}

/**
 * 计算前一天的日期
 */
private fun getPreviousDate(date: String): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val calendar = java.util.Calendar.getInstance()
    calendar.time = sdf.parse(date) ?: java.util.Date()
    calendar.add(java.util.Calendar.DAY_OF_MONTH, -1)
    return sdf.format(calendar.time)
}

/**
 * 计算后一天的日期
 */
private fun getNextDate(date: String): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val calendar = java.util.Calendar.getInstance()
    calendar.time = sdf.parse(date) ?: java.util.Date()
    calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
    return sdf.format(calendar.time)
}
