package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.config.AppConfig
import com.example.myapplication.data.model.SoundData
import com.example.myapplication.data.model.Statistics
import com.example.myapplication.data.repository.SoundDataRepository
import com.example.myapplication.utils.TestDataGenerator
import com.example.myapplication.utils.TotalSoundUpdateUtils
import com.example.myapplication.utils.DailySoundUpdateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*

sealed class UiState {
    object Loading : UiState()
    data class Success(val data: List<SoundData>, val statistics: Statistics) : UiState()
    data class Error(val message: String) : UiState()
}

class SoundDataViewModel : ViewModel() {
    private val repository = SoundDataRepository()
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _selectedDate = MutableStateFlow(getCurrentDate())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()
    
    init {
        initializeData()
    }
    
    /**
     * 初始化数据
     * 每次启动都执行：
     * 1. 获取最新有数据的日期并设置为当前日期
     * 2. 同步主播名单（从user表）
     * 3. 加载并显示数据
     */
    private fun initializeData() {
        viewModelScope.launch {
            android.util.Log.d("SoundDataViewModel", "开始初始化数据")
            
            // 先获取最新有数据的日期
            val latestDate = repository.getLatestLocalDate()
            if (latestDate != null) {
                android.util.Log.d("SoundDataViewModel", "找到最新数据日期: $latestDate")
                _selectedDate.value = latestDate
            } else {
                android.util.Log.d("SoundDataViewModel", "本地无数据，使用今天日期")
            }
            
            // 只同步主播名单，不同步音浪数据
            if (AppConfig.ENABLE_REMOTE_DB) {
                android.util.Log.d("SoundDataViewModel", "同步主播名单")
                val syncStreamersResult = repository.syncStreamersFromDatabase()
                if (syncStreamersResult.isSuccess) {
                    android.util.Log.d("SoundDataViewModel", "✅ 主播名单同步成功")
                } else {
                    android.util.Log.w("SoundDataViewModel", "⚠️ 主播名单同步失败")
                }
            }
            
            // 加载本地数据
            loadData()
        }
    }
    

    
    private fun generateTestDataIfNeeded() {
        viewModelScope.launch {
            try {
                // 根据配置生成测试数据
                val dates = (0 until AppConfig.TEST_DATA_DAYS).map { getDateOffset(-it) }
                
                dates.forEach { date ->
                    val count = repository.getStatistics(date).totalCount
                    if (count == 0) {
                        val testData = TestDataGenerator.generateTestData(date, AppConfig.TEST_DATA_COUNT)
                        repository.insertData(testData)
                    }
                }
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }
    
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                android.util.Log.d("SoundDataViewModel", "开始加载数据，日期: ${_selectedDate.value}")
                repository.getDataByDate(_selectedDate.value).collect { data ->
                    android.util.Log.d("SoundDataViewModel", "收到数据: ${data.size}条")
                    // 基于筛选后的数据计算统计信息
                    val statistics = calculateStatistics(data)
                    android.util.Log.d("SoundDataViewModel", "统计信息: 总数=${statistics.totalCount}, 活跃=${statistics.activeCount}, 总音浪=${statistics.totalSoundWave}")
                    _uiState.value = UiState.Success(data, statistics)
                }
            } catch (e: Exception) {
                android.util.Log.e("SoundDataViewModel", "加载数据失败", e)
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * 生成CSV文件用于分享
     * 只在用户点击分享时调用
     */
    suspend fun generateCsvForShare(context: android.content.Context, data: List<SoundData>, date: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("SoundDataViewModel", "开始生成CSV文件用于分享")
                
                val settings = getExportSettings()
                com.example.myapplication.utils.ExportUtils.exportToExcelForShare(
                    context, data, date, settings
                )
            } catch (e: Exception) {
                android.util.Log.e("SoundDataViewModel", "CSV文件生成异常", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * 基于筛选后的数据计算统计信息
     * 只统计有音浪的主播（soundWave > 1）
     */
    private fun calculateStatistics(data: List<SoundData>): Statistics {
        android.util.Log.d("SoundDataViewModel", "计算统计信息，数据条数: ${data.size}")
        
        val totalCount = data.size  // 总人数（包括未开播）
        val activeCount = data.count { it.soundWave > 1 }  // 有音浪的人数（>1才算开播）
        val inactiveCount = totalCount - activeCount  // 未开播人数
        
        // 只统计有音浪的主播的当日音浪（使用soundWave而不是totalSoundWave）
        val totalSoundWave = data.filter { it.soundWave > 1 }.sumOf { it.soundWave }
        
        // 打印前3条数据用于调试
        data.take(3).forEachIndexed { index, item ->
            android.util.Log.d("SoundDataViewModel", "数据[$index]: 名字=${item.streamerName}, 当日音浪=${item.soundWave}, 总音浪=${item.totalSoundWave}")
        }
        
        return Statistics(
            totalCount = totalCount,
            activeCount = activeCount,
            totalSoundWave = totalSoundWave,
            inactiveCount = inactiveCount
        )
    }
    
    /**
     * 同步最新数据 - 已禁用
     */
    fun syncLatestData(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(Result.failure(Exception("数据同步功能已禁用，请使用导入功能")))
            }
        }
    }
    
    /**
     * 刷新数据 - 已禁用
     */
    fun refreshData() {
        viewModelScope.launch {
            android.util.Log.d("SoundDataViewModel", "数据同步功能已禁用")
            _uiState.value = UiState.Error("数据同步功能已禁用，请使用导入功能")
            kotlinx.coroutines.delay(2000)
            loadData()
        }
    }
    
    fun selectDate(date: String) {
        _selectedDate.value = date
        loadData()
    }
    
    fun selectToday() {
        selectDate(getCurrentDate())
    }
    
    fun selectYesterday() {
        selectDate(getDateOffset(-1))
    }
    
    fun selectDayBefore() {
        selectDate(getDateOffset(-2))
    }
    
    fun selectPreviousDay() {
        selectDate(getDateOffset(-1, _selectedDate.value))
    }
    
    fun selectNextDay() {
        selectDate(getDateOffset(1, _selectedDate.value))
    }
    
    suspend fun getCurrentData(): List<SoundData> {
        return repository.getDataByDateSync(_selectedDate.value)
    }
    
    fun getExportSettings() = repository.getExportSettings()
    
    fun saveExportSettings(settings: com.example.myapplication.data.model.ExportSettings) {
        repository.saveExportSettings(settings)
        // 刷新数据以应用新的排序
        loadData()
    }
    
    fun clearCache() {
        repository.clearAllCache()
    }
    
    /**
     * 导入数据到本地数据库
     */
    fun importDataToLocal(dataList: List<SoundData>, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            try {
                android.util.Log.d("SoundDataViewModel", "开始导入数据到本地，数据量: ${dataList.size}")
                // 打印前3条数据
                dataList.take(3).forEachIndexed { index, data ->
                    android.util.Log.d("SoundDataViewModel", "待导入[$index]: ID=${data.streamerId}, 名字=${data.streamerName}, 日音浪=${data.soundWave}, 总音浪=${data.totalSoundWave}")
                }
                
                repository.insertData(dataList)
                
                android.util.Log.d("SoundDataViewModel", "数据导入完成，开始验证...")
                // 验证数据是否正确保存
                val savedData = repository.getDataByDateSync(dataList.first().recordDate)
                android.util.Log.d("SoundDataViewModel", "验证结果: 数据库中有 ${savedData.size} 条数据")
                savedData.take(3).forEachIndexed { index, data ->
                    android.util.Log.d("SoundDataViewModel", "已保存[$index]: ID=${data.streamerId}, 名字=${data.streamerName}, 日音浪=${data.soundWave}, 总音浪=${data.totalSoundWave}")
                }
                
                onResult(Result.success(dataList.size))
            } catch (e: Exception) {
                android.util.Log.e("SoundDataViewModel", "导入数据失败", e)
                onResult(Result.failure(e))
            }
        }
    }
    

    
    /**
     * 同步指定日期范围的数据 - 已禁用
     */
    fun syncDataByDateRange(startDate: String, endDate: String, onResult: (Result<List<SoundData>>) -> Unit) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(Result.failure(Exception("数据同步功能已禁用，请使用导入功能")))
            }
        }
    }
    
    /**
     * 将本地指定日期的数据推送到远程数据库
     * 用于修复导入失败或数据不一致的情况
     */
    fun pushLocalDataToRemote(date: String, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                android.util.Log.d("SoundDataViewModel", "开始推送本地数据到远程: $date")
                
                // 1. 从本地数据库获取该日期的所有数据
                val localData = repository.getDataByDateSync(date)
                android.util.Log.d("SoundDataViewModel", "本地数据: ${localData.size}条")
                
                if (localData.isEmpty()) {
                    onResult(Result.failure(Exception("本地没有该日期的数据")))
                    _uiState.value = UiState.Error("本地没有该日期的数据")
                    kotlinx.coroutines.delay(2000)
                    loadData()
                    return@launch
                }
                
                // 2. 推送到远程数据库
                val result = repository.importDataToRemote(localData)
                android.util.Log.d("SoundDataViewModel", "推送结果: ${result.isSuccess}, 成功数量: ${result.getOrNull()}")
                
                onResult(result)
                
                if (result.isSuccess) {
                    // 推送成功后刷新数据
                    loadData()
                } else {
                    _uiState.value = UiState.Error(result.exceptionOrNull()?.message ?: "推送失败")
                    kotlinx.coroutines.delay(2000)
                    loadData()
                }
            } catch (e: Exception) {
                android.util.Log.e("SoundDataViewModel", "推送异常", e)
                onResult(Result.failure(e))
                _uiState.value = UiState.Error(e.message ?: "推送出错")
                kotlinx.coroutines.delay(2000)
                loadData()
            }
        }
    }
    
    /**
     * 添加新数据
     */
    fun addData(data: SoundData, onResult: (Result<Long>) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.insertData(data)
                onResult(result)
                if (result.isSuccess) {
                    // 添加成功后刷新数据
                    loadData()
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }
    
    /**
     * 更新数据
     */
    fun updateData(data: SoundData, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.updateData(data)
                onResult(result)
                if (result.isSuccess) {
                    // 更新成功后刷新数据
                    loadData()
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }
    
    /**
     * 删除数据
     */
    fun deleteData(data: SoundData, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.deleteData(data)
                onResult(result)
                if (result.isSuccess) {
                    // 删除成功后刷新数据
                    loadData()
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }
    
    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
    
    private fun getDateOffset(offset: Int, fromDate: String? = null): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        
        if (fromDate != null) {
            calendar.time = sdf.parse(fromDate) ?: Date()
        }
        
        calendar.add(Calendar.DAY_OF_MONTH, offset)
        return sdf.format(calendar.time)
    }
    
    /**
     * 删除指定日期的数据并重新计算后续日期的总音浪
     */
    fun deleteDateDataAndRecalculate(date: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val result = repository.deleteDateDataAndRecalculate(date)
                // 确保回调在主线程执行
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(result)
                }
                if (result.isSuccess) {
                    // 刷新当前显示的数据
                    loadData()
                } else {
                    _uiState.value = UiState.Error(result.exceptionOrNull()?.message ?: "删除失败")
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(Result.failure(Exception("删除失败: ${e.message}", e)))
                }
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * 删除所有本地数据（音浪数据）
     */
    fun deleteAllLocalData(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.deleteAllData()
                // 确保回调在主线程执行
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (result.isSuccess) {
                        val count = result.getOrNull() ?: 0
                        onResult(Result.success("已删除所有本地音浪数据，共 $count 条"))
                    } else {
                        onResult(Result.failure(result.exceptionOrNull() ?: Exception("删除失败")))
                    }
                }
                // 刷新当前显示的数据
                loadData()
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(Result.failure(Exception("删除失败: ${e.message}", e)))
                }
            }
        }
    }
    
    /**
     * 更新总音浪
     * @param uri CSV文件URI
     * @param targetDate 目标日期
     * @param onResult 回调函数
     */
    fun updateTotalSoundWave(
        context: android.content.Context,
        uri: android.net.Uri,
        targetDate: String,
        onResult: (Result<TotalSoundUpdateUtils.UpdateResult>) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // updateFromCsv已经是suspend函数,直接调用
                val result = TotalSoundUpdateUtils.updateFromCsv(
                    context,
                    uri,
                    targetDate
                )
                
                // 确保回调在主线程执行
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(result)
                }
                
                if (result.isSuccess) {
                    // 更新成功后刷新当前数据
                    loadData()
                } else {
                    _uiState.value = UiState.Error(result.exceptionOrNull()?.message ?: "更新失败")
                    kotlinx.coroutines.delay(2000)
                    loadData()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(Result.failure(Exception("更新失败: ${e.message}", e)))
                }
                _uiState.value = UiState.Error(e.message ?: "更新出错")
                kotlinx.coroutines.delay(2000)
                loadData()
            }
        }
    }
    
    /**
     * 更新日音浪（数据校准）
     * @param context 上下文
     * @param uri CSV文件URI
     * @param targetDate 目标日期
     * @param onResult 回调函数
     */
    fun updateDailySoundWave(
        context: android.content.Context,
        uri: android.net.Uri,
        targetDate: String,
        onResult: (Result<DailySoundUpdateUtils.UpdateResult>) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    DailySoundUpdateUtils.updateFromCsv(
                        context,
                        uri,
                        targetDate
                    )
                }
                
                // 确保回调在主线程执行
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(result)
                }
                
                if (result.isSuccess) {
                    // 更新成功后刷新当前数据
                    loadData()
                } else {
                    _uiState.value = UiState.Error(result.exceptionOrNull()?.message ?: "更新失败")
                    kotlinx.coroutines.delay(2000)
                    loadData()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(Result.failure(Exception("更新失败: ${e.message}", e)))
                }
                _uiState.value = UiState.Error(e.message ?: "更新出错")
                kotlinx.coroutines.delay(2000)
                loadData()
            }
        }
    }
    
    /**
     * 覆盖总音浪（从主播榜CSV）
     * @param context 上下文
     * @param uri CSV文件URI
     * @param targetDate 目标日期
     * @param onResult 回调函数
     */
    fun overwriteTotalSoundWave(
        context: android.content.Context,
        uri: android.net.Uri,
        targetDate: String,
        onResult: (Result<com.example.myapplication.utils.TotalSoundOverwriteUtils.OverwriteResult>) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val result = com.example.myapplication.utils.TotalSoundOverwriteUtils.overwriteTotalSoundWave(
                    context,
                    uri,
                    targetDate
                )
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(result)
                }
                
                if (result.isSuccess) {
                    // 清除缓存并刷新数据
                    repository.clearAllCache()
                    loadData()
                } else {
                    _uiState.value = UiState.Error(result.exceptionOrNull()?.message ?: "覆盖失败")
                    kotlinx.coroutines.delay(2000)
                    loadData()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(Result.failure(Exception("覆盖失败: ${e.message}", e)))
                }
                _uiState.value = UiState.Error(e.message ?: "覆盖出错")
                kotlinx.coroutines.delay(2000)
                loadData()
            }
        }
    }
}
