package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.DurationData
import com.example.myapplication.data.model.DurationStatistics
import com.example.myapplication.data.model.ExportSettings
import com.example.myapplication.data.remote.RemoteDatabaseManagerV2
import com.example.myapplication.data.repository.DurationDataRepository
import com.example.myapplication.data.local.PreferencesManager
import com.example.myapplication.utils.DurationImportUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * 直播有效时长ViewModel
 */
class DurationDataViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DurationDataRepository(application)
    private val prefsManager = PreferencesManager(application)
    
    // 选中的日期范围
    private val _selectedDateRange = MutableStateFlow(getDefaultDateRange())
    val selectedDateRange: StateFlow<Pair<String, String>> = _selectedDateRange.asStateFlow()
    
    // UI状态
    private val _uiState = MutableStateFlow<DurationUiState>(DurationUiState.Loading)
    val uiState: StateFlow<DurationUiState> = _uiState.asStateFlow()
    
    init {
        // 先尝试加载数据库中最新的日期范围
        viewModelScope.launch {
            try {
                val dateRanges = repository.getAllDateRanges()
                if (dateRanges.isNotEmpty()) {
                    // 解析第一个日期范围(最新的)
                    val latestRange = dateRanges.first()
                    val parts = latestRange.split(" ~ ")
                    if (parts.size == 2) {
                        _selectedDateRange.value = Pair(parts[0], parts[1])
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DurationDataViewModel", "获取日期范围失败", e)
            } finally {
                loadData()
            }
        }
    }
    
    /**
     * 获取默认日期范围（本月1号到今天）
     */
    private fun getDefaultDateRange(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val endDate = sdf.format(calendar.time)
        
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = sdf.format(calendar.time)
        
        return Pair(startDate, endDate)
    }
    
    /**
     * 选择日期范围
     */
    fun selectDateRange(startDate: String, endDate: String) {
        _selectedDateRange.value = Pair(startDate, endDate)
        loadData()
    }
    
    /**
     * 加载数据
     */
    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = DurationUiState.Loading
            
            try {
                val (startDate, endDate) = _selectedDateRange.value
                repository.getDataByDateRange(startDate, endDate)
                    .catch { e ->
                        _uiState.value = DurationUiState.Error(e.message ?: "加载失败")
                    }
                    .collect { dataList ->
                        // 按时长排序（从大到小）
                        val sortedList = dataList.sortedByDescending { 
                            DurationImportUtils.parseDurationToSeconds(it.duration)
                        }
                        
                        val statistics = calculateStatistics(sortedList)
                        _uiState.value = DurationUiState.Success(sortedList, statistics)
                    }
            } catch (e: Exception) {
                _uiState.value = DurationUiState.Error(e.message ?: "加载失败")
            }
        }
    }
    
    /**
     * 计算统计数据
     */
    private fun calculateStatistics(dataList: List<DurationData>): DurationStatistics {
        val totalCount = dataList.size
        val activeCount = dataList.count { 
            DurationImportUtils.parseDurationToSeconds(it.duration) > 0
        }
        val inactiveCount = totalCount - activeCount
        
        return DurationStatistics(
            totalCount = totalCount,
            activeCount = activeCount,
            inactiveCount = inactiveCount
        )
    }
    
    /**
     * 刷新数据
     */
    fun refreshData() {
        loadData()
    }
    
    /**
     * 导入数据到本地
     */
    fun importDataToLocal(dataList: List<DurationData>, callback: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            try {
                val (startDate, endDate) = _selectedDateRange.value
                
                // 先删除该日期范围的旧数据
                repository.deleteByDateRange(startDate, endDate)
                
                // 插入新数据
                val result = repository.insertAll(dataList)
                
                withContext(Dispatchers.Main) {
                    callback(Result.success(result.size))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(Result.failure(e))
                }
            }
        }
    }
    
    /**
     * 导入数据到远程数据库
     */
    fun importDataToRemote(dataList: List<DurationData>, callback: (Result<String>) -> Unit) {
        viewModelScope.launch {
            try {
                val (startDate, endDate) = _selectedDateRange.value
                
                // 先删除远程数据库中该日期范围的数据
                RemoteDatabaseManagerV2.deleteDurationDataByDateRange(startDate, endDate)
                
                // 批量插入到远程数据库
                val result = RemoteDatabaseManagerV2.importDurationData(dataList)
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        callback(Result.success("成功上传${dataList.size}条数据"))
                    } else {
                        callback(Result.failure(result.exceptionOrNull() ?: Exception("上传失败")))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(Result.failure(e))
                }
            }
        }
    }
    
    /**
     * 删除指定日期范围的数据
     */
    fun deleteDateRangeData(startDate: String, endDate: String, callback: (Result<String>) -> Unit) {
        viewModelScope.launch {
            try {
                // 删除本地数据
                repository.deleteByDateRange(startDate, endDate)
                
                // 删除远程数据
                RemoteDatabaseManagerV2.deleteDurationDataByDateRange(startDate, endDate)
                
                withContext(Dispatchers.Main) {
                    callback(Result.success("删除成功"))
                    // 如果删除的是当前选中的日期范围，重新加载数据
                    if (_selectedDateRange.value == Pair(startDate, endDate)) {
                        loadData()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(Result.failure(e))
                }
            }
        }
    }
    
    /**
     * 获取所有日期范围
     */
    suspend fun getAllDateRanges(): List<String> {
        return withContext(Dispatchers.IO) {
            repository.getAllDateRanges()
        }
    }
    
    /**
     * 获取导出设置
     */
    fun getExportSettings(): ExportSettings {
        return prefsManager.getExportSettings()
    }
}

/**
 * UI状态
 */
sealed class DurationUiState {
    object Loading : DurationUiState()
    data class Success(
        val data: List<DurationData>,
        val statistics: DurationStatistics
    ) : DurationUiState()
    data class Error(val message: String) : DurationUiState()
}
