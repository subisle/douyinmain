package com.example.myapplication.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.DurationData
import com.example.myapplication.data.model.DurationStatistics
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.*
import com.example.myapplication.utils.DurationImportUtils

/**
 * 直播有效时长界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationDataScreen(
    data: List<DurationData>,
    statistics: DurationStatistics,
    selectedDateRange: Pair<String, String>,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onSelectDateRange: () -> Unit,
    onExport: () -> Unit,
    onPhoneClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var selectedGender by remember { mutableStateOf(Gender.All) }
    var fabExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // 根据性别筛选数据
    val filteredData = remember(data, selectedGender) {
        when (selectedGender) {
            Gender.Male -> data.filter { it.gender == "男" }
            Gender.Female -> data.filter { it.gender == "女" }
            Gender.All -> data
        }
    }
    
    // 重新计算统计数据
    val filteredStatistics = remember(filteredData) {
        val totalCount = filteredData.size
        val activeCount = filteredData.count { 
            DurationImportUtils.parseDurationToSeconds(it.duration) > 0
        }
        val inactiveCount = totalCount - activeCount
        
        // 计算总时长（秒）
        val totalDurationSeconds = filteredData.sumOf { 
            DurationImportUtils.parseDurationToSeconds(it.duration)
        }
        
        DurationStatistics(totalCount, activeCount, inactiveCount)
    }
    
    // 计算总时长
    val totalDurationSeconds = remember(filteredData) {
        filteredData.sumOf { 
            DurationImportUtils.parseDurationToSeconds(it.duration)
        }
    }
    
    Scaffold(
        bottomBar = {
            ModernBottomNav(
                selectedTab = BottomNavTab.Dashboard,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.Dashboard -> onBack()
                        BottomNavTab.Streamer -> { /* 可以导航到主播管理 */ }
                        BottomNavTab.Settings -> onSettingsClick()
                    }
                }
            )
        },
        floatingActionButton = {
            FABMenu(
                isExpanded = fabExpanded,
                onToggle = { fabExpanded = !fabExpanded },
                onImport = onImport
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. 现代化头部（紫色主题）
            ModernHeader(
                selectedDate = "${selectedDateRange.first} ~ ${selectedDateRange.second}",
                totalSound = formatDurationForHeader(totalDurationSeconds),
                liveCount = "${filteredStatistics.activeCount} / ${filteredStatistics.totalCount}",
                isDurationMode = true,
                selectedGender = selectedGender,
                onGenderChange = { selectedGender = it },
                onDateClick = onSelectDateRange,
                onPhoneClick = onPhoneClick,
                onShareClick = onExport,
                onSettingsClick = onSettingsClick
            )
            
            // 2. 内容区域
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = AppSpacing.ScreenPadding)
            ) {
                // 分段控制器
                SegmentControl(
                    selectedMode = DataMode.Duration,
                    onModeChange = { mode ->
                        if (mode == DataMode.Sound) {
                            onBack()
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
                        text = "有效时长",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // 时长列表
                if (filteredData.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无数据",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(filteredData) { index, item ->
                            val durationSeconds = DurationImportUtils.parseDurationToSeconds(item.duration)
                            StreamerCard(
                                rank = index + 1,
                                name = item.streamerName,
                                value = if (durationSeconds > 0) {
                                    formatDuration(durationSeconds)
                                } else {
                                    "缺勤"
                                },
                                subValue = if (durationSeconds > 0) "有效时长" else "",
                                gender = item.gender,
                                onClick = { /* 可选：点击事件 */ }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 格式化时长（秒 -> XX小时）
 */
private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    return "${hours}小时"
}

/**
 * 格式化头部显示的总时长（简化版）
 */
private fun formatDurationForHeader(seconds: Long): String {
    val hours = seconds / 3600
    return "${hours}小时"
}
