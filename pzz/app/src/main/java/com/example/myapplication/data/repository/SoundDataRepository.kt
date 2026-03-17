package com.example.myapplication.data.repository

import com.example.myapplication.PzzApplication
import com.example.myapplication.config.AppConfig
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.PreferencesManager
import com.example.myapplication.data.model.ExportSettings
import com.example.myapplication.data.model.SoundData
import com.example.myapplication.data.model.SortField
import com.example.myapplication.data.model.SortOrder
import com.example.myapplication.data.model.Statistics
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.data.remote.RemoteDatabaseManager
import com.example.myapplication.data.remote.RemoteDatabaseManagerV2
import com.example.myapplication.utils.GenderUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SoundDataRepository {
    private val context = PzzApplication.instance
    private val database = AppDatabase.getDatabase(context)
    private val soundDataDao = database.soundDataDao()
    private val durationDataDao = database.durationDataDao()
    private val apiService = RetrofitClient.apiService
    private val prefsManager = PreferencesManager.getInstance(context)
    private val genderUtils = GenderUtils.getInstance(context)
    
    /**
     * 获取数据（带缓存）- 包含所有主播
     * 如果主播没有音浪数据，则创建一个0音浪的记录
     */
    fun getDataByDate(date: String): Flow<List<SoundData>> {
        return soundDataDao.getDataByDate(date).map { data ->
            // 合并主播名单和音浪数据
            val mergedData = mergeStreamerListWithData(data, date)
            
            // 填充时长数据
            val dataWithDuration = fillDurationDataSync(mergedData, date)
            
            // 缓存数据
            prefsManager.cacheData("data_$date", dataWithDuration)
            
            // 获取设置
            val settings = prefsManager.getExportSettings()
            
            // 先应用性别和范围筛选
            val filteredData = applyFilters(dataWithDuration, settings)
            
            // 对筛选后的数据应用等级计算
            val dataWithGrades = com.example.myapplication.utils.GradeManager.assignGradeRanks(filteredData, settings)
            
            // 应用排序
            applySortingOnly(dataWithGrades, settings)
        }
    }
    
    /**
     * 同步获取数据（带缓存）- 包含所有主播
     */
    suspend fun getDataByDateSync(date: String): List<SoundData> {
        val settings = prefsManager.getExportSettings()
        
        // 先尝试从缓存获取
        val cachedData = prefsManager.getCachedData("data_$date", Array<SoundData>::class.java)?.toList()
        if (cachedData != null && cachedData.isNotEmpty()) {
            // 先应用性别和范围筛选
            val filteredData = applyFilters(cachedData, settings)
            // 对筛选后的数据应用等级计算
            val dataWithGrades = com.example.myapplication.utils.GradeManager.assignGradeRanks(filteredData, settings)
            return applySortingOnly(dataWithGrades, settings)
        }
        
        // 缓存未命中，从数据库获取
        val data = soundDataDao.getDataByDateSync(date)
        
        // 合并主播名单和音浪数据
        val mergedData = mergeStreamerListWithData(data, date)
        
        // 填充时长数据
        val dataWithDuration = fillDurationDataSync(mergedData, date)
        
        // 更新缓存
        if (dataWithDuration.isNotEmpty()) {
            prefsManager.cacheData("data_$date", dataWithDuration)
        }
        
        // 先应用性别和范围筛选
        val filteredData = applyFilters(dataWithDuration, settings)
        // 对筛选后的数据应用等级计算
        val dataWithGrades = com.example.myapplication.utils.GradeManager.assignGradeRanks(filteredData, settings)
        return applySortingOnly(dataWithGrades, settings)
    }
    
    /**
     * 合并主播名单和音浪数据
     * 确保所有主播都显示，没有音浪数据的当日音浪为0，但总音浪保持前一天的值
     * 使用ID前8位进行匹配
     */
    private suspend fun mergeStreamerListWithData(data: List<SoundData>, date: String): List<SoundData> {
        android.util.Log.d("SoundDataRepository", "========== mergeStreamerListWithData 开始 ==========")
        android.util.Log.d("SoundDataRepository", "日期: $date, 数据库数据: ${data.size}条")
        
        // 获取所有主播
        val allStreamers = genderUtils.getAllStreamersWithGender()
        
        // 如果没有主播名单，直接返回原数据
        if (allStreamers.isEmpty()) {
            android.util.Log.w("SoundDataRepository", "⚠️ 主播名单为空,直接返回数据库数据")
            return data
        }
        
        android.util.Log.d("SoundDataRepository", "主播名单: ${allStreamers.size}个")
        
        // 创建音浪数据的映射（ID前8位 -> SoundData）
        val dataMapByPrefix = mutableMapOf<String, SoundData>()
        data.forEach { soundData ->
            val prefix = if (soundData.streamerId.length >= 8) {
                soundData.streamerId.substring(0, 8)
            } else {
                soundData.streamerId
            }
            dataMapByPrefix[prefix] = soundData
            
            // 打印浩杰的数据
            if (soundData.streamerName.contains("浩杰")) {
                android.util.Log.d("SoundDataRepository", "📊 数据库中的浩杰: ID=${soundData.streamerId}, 前8位=$prefix, 日音浪=${soundData.soundWave}, 总音浪=${soundData.totalSoundWave}")
            }
        }
        
        android.util.Log.d("SoundDataRepository", "数据映射(按前8位): ${dataMapByPrefix.size}条")
        
        // 计算前一天的日期
        val previousDate = getPreviousDate(date)
        
        // 查询前一天所有主播的总音浪
        val previousDayData = try {
            soundDataDao.getDataByDateSync(previousDate)
        } catch (e: Exception) {
            android.util.Log.w("SoundDataRepository", "查询前一天数据失败: ${e.message}")
            emptyList()
        }
        
        // 创建前一天的总音浪映射（ID前8位 -> 总音浪）
        val previousTotalMapByPrefix = mutableMapOf<String, Long>()
        previousDayData.forEach { prevData ->
            val prefix = if (prevData.streamerId.length >= 8) {
                prevData.streamerId.substring(0, 8)
            } else {
                prevData.streamerId
            }
            previousTotalMapByPrefix[prefix] = prevData.totalSoundWave
        }
        
        android.util.Log.d("SoundDataRepository", "前一天数据: ${previousDayData.size}条")
        
        // 为每个主播创建或获取数据
        val mergedList = allStreamers.map { streamer ->
            // 使用ID前8位匹配
            val streamerPrefix = if (streamer.streamerId.length >= 8) {
                streamer.streamerId.substring(0, 8)
            } else {
                streamer.streamerId
            }
            
            val existingData = dataMapByPrefix[streamerPrefix]
            
            // 打印浩杰的匹配过程
            if (streamer.nickname.contains("浩杰")) {
                android.util.Log.d("SoundDataRepository", "🔍 主播名单中的浩杰: ID=${streamer.streamerId}, 前8位=$streamerPrefix, 昵称=${streamer.nickname}")
                if (existingData != null) {
                    android.util.Log.d("SoundDataRepository", "✅ 匹配成功! 总音浪=${existingData.totalSoundWave}")
                } else {
                    android.util.Log.w("SoundDataRepository", "❌ 匹配失败! 在数据映射中找不到前8位=$streamerPrefix")
                    android.util.Log.d("SoundDataRepository", "数据映射的所有key: ${dataMapByPrefix.keys.joinToString(", ")}")
                }
            }
            
            if (existingData != null) {
                // 已有数据，使用现有数据（但使用主播名单中的昵称）
                existingData.copy(
                    streamerName = streamer.nickname  // 使用主播名单中的昵称
                )
            } else {
                // 没有数据，创建一个0当日音浪的记录
                // 总音浪使用前一天的总音浪（如果有的话）
                val previousTotal = previousTotalMapByPrefix[streamerPrefix] ?: 0L
                
                SoundData(
                    id = 0,  // 新记录，id为0
                    streamerId = streamer.streamerId,
                    streamerName = streamer.nickname,
                    soundWave = 0,  // 当日音浪为0
                    totalSoundWave = previousTotal,  // 总音浪保持前一天的值
                    effectiveBroadcastDuration = null,
                    recordDate = date,
                    gradeRank = null,
                    gradeLetter = null,
                    overallRank = 0,
                    createdAt = null
                )
            }
        }
        
        android.util.Log.d("SoundDataRepository", "合并后数据: ${mergedList.size}条")
        android.util.Log.d("SoundDataRepository", "========== mergeStreamerListWithData 结束 ==========")
        
        return mergedList
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
     * 从duration_data表查询时长数据并填充到SoundData中
     * 查询本月的时长数据
     */
    private suspend fun fillDurationDataSync(data: List<SoundData>, date: String): List<SoundData> {
        return try {
            val durationDao = database.durationDataDao()
            
            // 计算本月的开始和结束日期
            val (monthStart, monthEnd) = getMonthRange(date)
            
            // 查询本月所有的时长数据
            val allRanges = durationDao.getAllDateRanges()
            val monthlyData = mutableListOf<com.example.myapplication.data.model.DurationData>()
            
            allRanges.forEach { dateRange ->
                val parts = dateRange.split(" ~ ")
                if (parts.size == 2) {
                    val startDate = parts[0]
                    val endDate = parts[1]
                    // 如果时间段在本月内,查询该时间段的数据
                    if (isInMonth(startDate, monthStart, monthEnd) || isInMonth(endDate, monthStart, monthEnd)) {
                        val rangeData = durationDao.getDataByDateRangeSync(startDate, endDate)
                        monthlyData.addAll(rangeData)
                    }
                }
            }
            
            // 按主播ID分组,累加时长
            val durationMap = mutableMapOf<String, Double>()
            monthlyData.forEach { durationData ->
                val hours = parseDurationToHours(durationData.duration)
                durationMap[durationData.streamerId] = (durationMap[durationData.streamerId] ?: 0.0) + hours
            }
            
            // 填充时长数据
            data.map { soundData ->
                val totalHours = durationMap[soundData.streamerId]
                if (totalHours != null && totalHours > 0) {
                    soundData.copy(effectiveBroadcastDuration = "${totalHours}小时")
                } else {
                    soundData
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SoundDataRepository", "填充时长数据失败", e)
            // 如果查询失败,返回原数据
            data
        }
    }
    
    /**
     * 获取指定日期所在月份的开始和结束日期
     */
    private fun getMonthRange(date: String): Pair<String, String> {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()
        calendar.time = sdf.parse(date) ?: java.util.Date()
        
        // 月初
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        val monthStart = sdf.format(calendar.time)
        
        // 月末
        calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        val monthEnd = sdf.format(calendar.time)
        
        return Pair(monthStart, monthEnd)
    }
    
    /**
     * 判断日期是否在指定月份范围内
     */
    private fun isInMonth(date: String, monthStart: String, monthEnd: String): Boolean {
        return date >= monthStart && date <= monthEnd
    }
    
    /**
     * 解析时长字符串为小时数
     * 支持格式: "2.5小时", "2小时30分钟", "150分钟"等
     */
    private fun parseDurationToHours(duration: String): Double {
        return try {
            when {
                duration.contains("小时") && duration.contains("分钟") -> {
                    val parts = duration.split("小时")
                    val hours = parts[0].toDoubleOrNull() ?: 0.0
                    val minutes = parts.getOrNull(1)?.replace("分钟", "")?.replace("秒", "")?.toDoubleOrNull() ?: 0.0
                    hours + (minutes / 60.0)
                }
                duration.contains("小时") -> {
                    duration.replace("小时", "").toDoubleOrNull() ?: 0.0
                }
                duration.contains("分钟") -> {
                    val minutes = duration.replace("分钟", "").replace("秒", "").toDoubleOrNull() ?: 0.0
                    minutes / 60.0
                }
                else -> 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }
    
    /**
     * 解析时长字符串为分钟数（用于排序）
     * 支持格式: "2.5小时", "2小时30分钟", "150分钟"等
     */
    private fun parseDurationToMinutes(duration: String): Int {
        return (parseDurationToHours(duration) * 60).toInt()
    }
    
    /**
     * 获取统计信息（带缓存）
     */
    suspend fun getStatistics(date: String): Statistics {
        // 先尝试从缓存获取
        val cachedStats = prefsManager.getCachedData("stats_$date", Statistics::class.java)
        if (cachedStats != null) {
            return cachedStats
        }
        
        // 缓存未命中，计算统计
        val totalCount = soundDataDao.getCountByDate(date)
        val activeCount = soundDataDao.getActiveCountByDate(date)
        val totalSoundWave = soundDataDao.getTotalSoundWaveByDate(date)
        val inactiveCount = totalCount - activeCount  // 计算未开播人数
        val stats = Statistics(totalCount, activeCount, totalSoundWave, inactiveCount)
        
        // 更新缓存
        prefsManager.cacheData("stats_$date", stats)
        
        return stats
    }
    
    /**
     * 从服务器获取数据并覆盖本地数据
     */
    suspend fun fetchDataFromServer(date: String): Result<List<SoundData>> {
        // 优先使用远程数据库
        if (AppConfig.ENABLE_REMOTE_DB) {
            return fetchDataFromRemoteDatabase(date)
        }
        
        // 备用方案：使用API
        if (AppConfig.ENABLE_NETWORK_SYNC) {
            return fetchDataFromApi(date)
        }
        
        return Result.failure(Exception("未启用远程数据同步"))
    }
    
    /**
     * 从远程数据库获取数据并覆盖本地数据
     * 所有数据库操作都在子线程中执行
     */
    private suspend fun fetchDataFromRemoteDatabase(date: String): Result<List<SoundData>> {
        return try {
            android.util.Log.d("SoundDataRepository", "开始从远程数据库同步数据: $date")
            
            // 选择使用哪个版本的远程数据库管理器
            val result = if (AppConfig.USE_REMOTE_DB_V2) {
                android.util.Log.d("SoundDataRepository", "使用RemoteDatabaseManagerV2（自定义MySQL协议）")
                RemoteDatabaseManagerV2.fetchDataByDate(date)
            } else {
                android.util.Log.d("SoundDataRepository", "使用RemoteDatabaseManager（JDBC）")
                RemoteDatabaseManager.fetchDataByDate(date)
            }
            
            if (result.isSuccess) {
                val data = result.getOrNull() ?: emptyList()
                android.util.Log.d("SoundDataRepository", "远程数据库返回 ${data.size} 条数据")
                
                // 在IO线程中删除本地旧数据并插入新数据
                if (data.isNotEmpty()) {
                    // 先删除该日期的所有本地数据
                    soundDataDao.deleteByDate(date)
                    android.util.Log.d("SoundDataRepository", "已删除本地旧数据: $date")
                    
                    // 插入远程数据到本地
                    soundDataDao.insertAll(data)
                    android.util.Log.d("SoundDataRepository", "已插入 ${data.size} 条新数据到本地")
                }
                
                // 清除缓存，强制刷新
                prefsManager.clearCache("data_$date")
                prefsManager.clearCache("stats_$date")
                
                Result.success(data)
            } else {
                android.util.Log.e("SoundDataRepository", "远程数据库查询失败: ${result.exceptionOrNull()?.message}")
                result
            }
        } catch (e: Exception) {
            android.util.Log.e("SoundDataRepository", "远程数据库同步异常", e)
            Result.failure(Exception("远程数据库同步失败: ${e.message}", e))
        }
    }
    
    /**
     * 从API获取数据（备用方案）
     */
    private suspend fun fetchDataFromApi(date: String): Result<List<SoundData>> {
        return try {
            val response = apiService.getSoundData(date)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data
                // 保存到本地数据库
                soundDataDao.insertAll(data)
                // 清除缓存，强制刷新
                prefsManager.clearCache("data_$date")
                prefsManager.clearCache("stats_$date")
                Result.success(data)
            } else {
                Result.failure(Exception("API请求失败: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 测试远程数据库连接
     */
    suspend fun testRemoteDatabaseConnection(): Result<Boolean> {
        return if (AppConfig.USE_REMOTE_DB_V2) {
            RemoteDatabaseManagerV2.testConnection()
        } else {
            RemoteDatabaseManager.testConnection()
        }
    }
    
    /**
     * 导入数据到远程数据库
     */
    suspend fun importDataToRemote(dataList: List<SoundData>): Result<Int> {
        return if (AppConfig.USE_REMOTE_DB_V2) {
            RemoteDatabaseManagerV2.importData(dataList)
        } else {
            RemoteDatabaseManager.importData(dataList)
        }
    }
    
    /**
     * 同步指定日期范围的数据并覆盖本地数据
     * 所有数据库操作都在子线程中执行
     */
    suspend fun syncDataByDateRange(startDate: String, endDate: String): Result<List<SoundData>> {
        return try {
            android.util.Log.d("SoundDataRepository", "========== 开始同步流程 ==========")
            android.util.Log.d("SoundDataRepository", "同步日期范围: $startDate 到 $endDate")
            
            // 选择使用哪个版本的远程数据库管理器
            val result = if (AppConfig.USE_REMOTE_DB_V2) {
                android.util.Log.d("SoundDataRepository", "使用RemoteDatabaseManagerV2（自定义MySQL协议）")
                RemoteDatabaseManagerV2.fetchDataByDateRange(startDate, endDate)
            } else {
                android.util.Log.d("SoundDataRepository", "使用RemoteDatabaseManager（JDBC）")
                RemoteDatabaseManager.fetchDataByDateRange(startDate, endDate)
            }
            
            if (result.isSuccess) {
                val data = result.getOrNull() ?: emptyList()
                android.util.Log.d("SoundDataRepository", "✅ 远程数据库返回 ${data.size} 条数据")
                
                // 在IO线程中删除本地旧数据并插入新数据
                if (data.isNotEmpty()) {
                    // 获取所有涉及的日期
                    val dates = data.map { it.recordDate }.distinct().sorted()
                    android.util.Log.d("SoundDataRepository", "涉及 ${dates.size} 个日期: ${dates.joinToString(", ")}")
                    
                    // 删除这些日期的所有本地数据
                    var totalDeleted = 0
                    dates.forEach { date ->
                        val deleted = soundDataDao.deleteByDate(date)
                        totalDeleted += deleted
                        android.util.Log.d("SoundDataRepository", "  删除 $date 的旧数据: $deleted 条")
                    }
                    android.util.Log.d("SoundDataRepository", "✅ 共删除本地旧数据: $totalDeleted 条")
                    
                    // 插入远程数据到本地
                    val insertedIds = soundDataDao.insertAll(data)
                    android.util.Log.d("SoundDataRepository", "✅ 已插入 ${insertedIds.size} 条新数据到本地")
                    
                    // 验证插入结果
                    dates.forEach { date ->
                        val count = soundDataDao.getCountByDate(date)
                        android.util.Log.d("SoundDataRepository", "  验证 $date: 本地现有 $count 条数据")
                    }
                    
                    // 清除相关日期的缓存
                    dates.forEach { date ->
                        prefsManager.clearCache("data_$date")
                        prefsManager.clearCache("stats_$date")
                    }
                    android.util.Log.d("SoundDataRepository", "✅ 已清除缓存")
                }
                
                android.util.Log.d("SoundDataRepository", "========== 同步流程完成 ==========")
                Result.success(data)
            } else {
                android.util.Log.e("SoundDataRepository", "❌ 远程数据库查询失败: ${result.exceptionOrNull()?.message}")
                result
            }
        } catch (e: Exception) {
            android.util.Log.e("SoundDataRepository", "❌ 同步数据异常", e)
            Result.failure(Exception("同步数据失败: ${e.message}", e))
        }
    }
    
    /**
     * 插入数据（如果已存在则覆盖）
     */
    suspend fun insertData(data: List<SoundData>) {
        soundDataDao.insertOrUpdateAll(data)
        // 清除相关缓存
        data.map { it.recordDate }.distinct().forEach { date ->
            prefsManager.clearCache("data_$date")
            prefsManager.clearCache("stats_$date")
        }
    }
    
    /**
     * 删除指定日期的数据
     */
    suspend fun deleteByDate(date: String) {
        soundDataDao.deleteByDate(date)
        // 清除缓存
        prefsManager.clearCache("data_$date")
        prefsManager.clearCache("stats_$date")
    }
    
    /**
     * 删除所有本地数据（音浪数据）
     */
    suspend fun deleteAllData(): Result<Int> {
        return try {
            val count = soundDataDao.deleteAll()
            // 清除所有缓存
            prefsManager.clearAllCache()
            android.util.Log.d("SoundDataRepository", "已删除所有本地音浪数据，共 $count 条")
            Result.success(count)
        } catch (e: Exception) {
            android.util.Log.e("SoundDataRepository", "删除所有数据失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 检查授权
     */
    suspend fun checkAuth(): Result<Boolean> {
        return try {
            val response = apiService.checkAuth()
            if (response.isSuccessful) {
                Result.success(response.body()?.authorized ?: false)
            } else {
                Result.failure(Exception("Auth check failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 应用排序和筛选
     */
    /**
     * 应用性别和音浪范围筛选
     */
    private fun applyFilters(data: List<SoundData>, settings: ExportSettings): List<SoundData> {
        // 先应用性别筛选
        val genderFiltered = when (settings.genderFilter) {
            com.example.myapplication.data.model.GenderFilter.ALL -> data
            com.example.myapplication.data.model.GenderFilter.MALE -> {
                data.filter { 
                    val gender = genderUtils.inferGender(it.streamerId, it.streamerName)
                    gender == true
                }
            }
            com.example.myapplication.data.model.GenderFilter.FEMALE -> {
                data.filter { 
                    val gender = genderUtils.inferGender(it.streamerId, it.streamerName)
                    gender == false
                }
            }
        }
        
        // 再应用音浪范围筛选
        return genderFiltered.filter { soundData ->
            val totalSoundWave = soundData.totalSoundWave
            totalSoundWave >= settings.customMinSoundWave && 
            (settings.customMaxSoundWave == Long.MAX_VALUE || totalSoundWave <= settings.customMaxSoundWave)
        }
    }
    
    /**
     * 只应用排序（不做筛选）
     */
    private fun applySortingOnly(data: List<SoundData>, settings: ExportSettings): List<SoundData> {
        return when (settings.sortBy) {
            SortField.OVERALL_RANK -> {
                // 按总音浪排序（因为overallRank应该基于总音浪）
                if (settings.sortOrder == SortOrder.ASC) {
                    data.sortedBy { it.totalSoundWave }
                } else {
                    data.sortedByDescending { it.totalSoundWave }
                }
            }
            SortField.SOUND_WAVE -> {
                if (settings.sortOrder == SortOrder.ASC) {
                    data.sortedBy { it.soundWave }
                } else {
                    data.sortedByDescending { it.soundWave }
                }
            }
            SortField.TOTAL_SOUND_WAVE -> {
                if (settings.sortOrder == SortOrder.ASC) {
                    data.sortedBy { it.totalSoundWave }
                } else {
                    data.sortedByDescending { it.totalSoundWave }
                }
            }
            SortField.STREAMER_NAME -> {
                if (settings.sortOrder == SortOrder.ASC) {
                    data.sortedBy { it.streamerName }
                } else {
                    data.sortedByDescending { it.streamerName }
                }
            }
            SortField.GRADE_RANK -> {
                // 等级排序：S > A > B > C > D，同等级按数字升序
                val gradeOrder = mapOf("S" to 1, "A" to 2, "B" to 3, "C" to 4, "D" to 5)
                if (settings.sortOrder == SortOrder.ASC) {
                    data.sortedWith(compareBy(
                        { gradeOrder[it.gradeRank?.firstOrNull()?.toString() ?: "D"] ?: 5 },
                        { it.gradeRank?.substring(1)?.toIntOrNull() ?: 99 }
                    ))
                } else {
                    data.sortedWith(compareByDescending<SoundData>(
                        { gradeOrder[it.gradeRank?.firstOrNull()?.toString() ?: "D"] ?: 5 }
                    ).thenBy { it.gradeRank?.substring(1)?.toIntOrNull() ?: 99 })
                }
            }
            SortField.BROADCAST_DURATION -> {
                // 按直播时长排序
                if (settings.sortOrder == SortOrder.ASC) {
                    data.sortedBy { 
                        it.effectiveBroadcastDuration?.let { duration ->
                            parseDurationToMinutes(duration)
                        } ?: 0
                    }
                } else {
                    data.sortedByDescending { 
                        it.effectiveBroadcastDuration?.let { duration ->
                            parseDurationToMinutes(duration)
                        } ?: 0
                    }
                }
            }
        }
    }
    
    private fun applySorting(data: List<SoundData>, settings: ExportSettings): List<SoundData> {
        // 先应用性别筛选
        val genderFiltered = when (settings.genderFilter) {
            com.example.myapplication.data.model.GenderFilter.ALL -> data
            com.example.myapplication.data.model.GenderFilter.MALE -> {
                data.filter { 
                    val gender = genderUtils.inferGender(it.streamerId, it.streamerName)
                    gender == true
                }
            }
            com.example.myapplication.data.model.GenderFilter.FEMALE -> {
                data.filter { 
                    val gender = genderUtils.inferGender(it.streamerId, it.streamerName)
                    gender == false
                }
            }
        }
        
        // 再应用音浪范围筛选
        val rangeFiltered = genderFiltered.filter { soundData ->
            val totalSoundWave = soundData.totalSoundWave
            totalSoundWave >= settings.customMinSoundWave && 
            (settings.customMaxSoundWave == Long.MAX_VALUE || totalSoundWave <= settings.customMaxSoundWave)
        }
        
        // 最后应用排序
        val sorted = when (settings.sortBy) {
            SortField.OVERALL_RANK -> {
                // 按总音浪排序（因为overallRank应该基于总音浪）
                if (settings.sortOrder == SortOrder.ASC) {
                    rangeFiltered.sortedBy { it.totalSoundWave }
                } else {
                    rangeFiltered.sortedByDescending { it.totalSoundWave }
                }
            }
            SortField.SOUND_WAVE -> {
                if (settings.sortOrder == SortOrder.ASC) {
                    rangeFiltered.sortedBy { it.soundWave }
                } else {
                    rangeFiltered.sortedByDescending { it.soundWave }
                }
            }
            SortField.TOTAL_SOUND_WAVE -> {
                if (settings.sortOrder == SortOrder.ASC) {
                    rangeFiltered.sortedBy { it.totalSoundWave }
                } else {
                    rangeFiltered.sortedByDescending { it.totalSoundWave }
                }
            }
            SortField.STREAMER_NAME -> {
                if (settings.sortOrder == SortOrder.ASC) {
                    rangeFiltered.sortedBy { it.streamerName }
                } else {
                    rangeFiltered.sortedByDescending { it.streamerName }
                }
            }
            SortField.GRADE_RANK -> {
                if (settings.sortOrder == SortOrder.ASC) {
                    rangeFiltered.sortedBy { it.gradeRank ?: "Z" }
                } else {
                    rangeFiltered.sortedByDescending { it.gradeRank ?: "Z" }
                }
            }
            SortField.BROADCAST_DURATION -> {
                // 按直播时长排序
                if (settings.sortOrder == SortOrder.ASC) {
                    rangeFiltered.sortedBy { 
                        it.effectiveBroadcastDuration?.let { duration ->
                            parseDurationToMinutes(duration)
                        } ?: 0
                    }
                } else {
                    rangeFiltered.sortedByDescending { 
                        it.effectiveBroadcastDuration?.let { duration ->
                            parseDurationToMinutes(duration)
                        } ?: 0
                    }
                }
            }
        }
        
        // 重新设置排名（基于排序后的顺序）
        return sorted.mapIndexed { index, soundData ->
            soundData.copy(overallRank = index + 1)
        }
    }
    
    /**
     * 保存导出设置
     */
    fun saveExportSettings(settings: ExportSettings) {
        prefsManager.saveExportSettings(settings)
    }
    
    /**
     * 获取导出设置
     */
    fun getExportSettings(): ExportSettings {
        return prefsManager.getExportSettings()
    }
    
    /**
     * 清除所有缓存
     */
    fun clearAllCache() {
        prefsManager.clearAllCache()
    }
    
    /**
     * 检查是否首次启动
     */
    fun isFirstLaunch(): Boolean {
        return prefsManager.isFirstLaunch()
    }
    
    /**
     * 标记首次启动已完成
     */
    fun setFirstLaunchComplete() {
        prefsManager.setFirstLaunchComplete()
    }
    
    /**
     * 添加新数据
     */
    suspend fun insertData(data: SoundData): Result<Long> {
        return try {
            val id = soundDataDao.insert(data)
            // 清除缓存
            prefsManager.clearCache("data_${data.recordDate}")
            prefsManager.clearCache("stats_${data.recordDate}")
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(Exception("添加数据失败: ${e.message}", e))
        }
    }
    
    /**
     * 更新数据
     */
    suspend fun updateData(data: SoundData): Result<Int> {
        return try {
            val count = soundDataDao.update(data)
            // 清除缓存
            prefsManager.clearCache("data_${data.recordDate}")
            prefsManager.clearCache("stats_${data.recordDate}")
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(Exception("更新数据失败: ${e.message}", e))
        }
    }
    
    /**
     * 删除数据
     */
    suspend fun deleteData(data: SoundData): Result<Int> {
        return try {
            val count = soundDataDao.delete(data)
            // 清除缓存
            prefsManager.clearCache("data_${data.recordDate}")
            prefsManager.clearCache("stats_${data.recordDate}")
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(Exception("删除数据失败: ${e.message}", e))
        }
    }
    
    /**
     * 根据ID查询数据
     */
    suspend fun getDataById(id: Long): SoundData? {
        return try {
            soundDataDao.getDataById(id)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 清空指定日期的日音浪数据(本地和远程)
     */
    suspend fun deleteDateDataAndRecalculate(date: String): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d("SoundDataRepository", "开始清空日期的日音浪数据: $date")
            
            // 1. 清空远程数据库中的日音浪字段
            android.util.Log.d("SoundDataRepository", "步骤1: 清空远程数据库的日音浪字段")
            val clearResult = if (AppConfig.USE_REMOTE_DB_V2) {
                RemoteDatabaseManagerV2.clearDailySoundWave(date)
            } else {
                RemoteDatabaseManager.clearDailySoundWave(date)
            }
            
            if (clearResult.isFailure) {
                android.util.Log.e("SoundDataRepository", "清空远程日音浪失败: ${clearResult.exceptionOrNull()?.message}")
                return@withContext clearResult
            }
            android.util.Log.d("SoundDataRepository", "清空远程日音浪成功")
            
            // 2. 清空本地数据库中的日音浪字段
            android.util.Log.d("SoundDataRepository", "步骤2: 清空本地数据库的日音浪字段")
            soundDataDao.clearDailySoundWaveByDate(date)
            android.util.Log.d("SoundDataRepository", "清空本地日音浪成功")
            
            android.util.Log.d("SoundDataRepository", "清空操作完成")
            Result.success("清空成功，已将该日期的日音浪设置为0")
        } catch (e: Exception) {
            android.util.Log.e("SoundDataRepository", "清空操作异常", e)
            Result.failure(Exception("清空失败: ${e.message}", e))
        }
    }
    
    /**
     * 获取下一天的日期
     */
    private fun getNextDate(dateStr: String): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return dateStr
            val calendar = java.util.Calendar.getInstance()
            calendar.time = date
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            sdf.format(calendar.time)
        } catch (e: Exception) {
            dateStr
        }
    }
    
    /**
     * 获取本地数据库中最新的日期
     */
    suspend fun getLatestLocalDate(): String? {
        return try {
            soundDataDao.getLatestDate()
        } catch (e: Exception) {
            android.util.Log.e("SoundDataRepository", "获取最新日期失败", e)
            null
        }
    }
    
    /**
     * 同步最新数据 - 从远程数据库获取所有已有日期的音浪和时长数据
     */
    suspend fun syncLatestDataFromRemote(): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d("SoundDataRepository", "开始同步所有已有日期的数据")
            
            // 1. 获取远程所有日期列表
            val allDates = getAllDatesFromRemote()
            if (allDates.isEmpty()) {
                return@withContext Result.failure(Exception("远程数据库中没有数据"))
            }
            
            android.util.Log.d("SoundDataRepository", "远程数据库中的日期: ${allDates.joinToString(", ")}")
            
            var totalSoundCount = 0
            var totalDurationCount = 0
            
            // 2. 同步每个日期的音浪数据
            allDates.forEach { date ->
                try {
                    val soundResult = fetchDataFromServer(date)
                    if (soundResult.isSuccess) {
                        val count = soundResult.getOrNull()?.size ?: 0
                        totalSoundCount += count
                        android.util.Log.d("SoundDataRepository", "同步${date}音浪数据: ${count}条")
                    } else {
                        android.util.Log.w("SoundDataRepository", "同步${date}音浪数据失败: ${soundResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SoundDataRepository", "同步${date}音浪数据异常", e)
                }
            }
            
            // 3. 同步时长数据
            try {
                val durationResult = syncAllDurationDataFromRemote()
                if (durationResult.isSuccess) {
                    totalDurationCount = durationResult.getOrNull() ?: 0
                    android.util.Log.d("SoundDataRepository", "同步时长数据成功: ${totalDurationCount}条")
                } else {
                    android.util.Log.w("SoundDataRepository", "同步时长数据失败: ${durationResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.w("SoundDataRepository", "同步时长数据异常", e)
            }
            
            // 返回消息中包含最新日期,方便ViewModel提取并切换
            val latestDate = allDates.firstOrNull() ?: ""
            Result.success("同步成功\n音浪数据: ${totalSoundCount}条\n时长数据: ${totalDurationCount}条\n日期数: ${allDates.size}个\n最新日期: $latestDate")
        } catch (e: Exception) {
            android.util.Log.e("SoundDataRepository", "同步数据失败", e)
            Result.failure(Exception("同步失败: ${e.message}", e))
        }
    }
    
    /**
     * 获取远程数据库中所有的日期列表
     */
    private suspend fun getAllDatesFromRemote(): List<String> {
        return try {
            val result = if (AppConfig.USE_REMOTE_DB_V2) {
                RemoteDatabaseManagerV2.getAllDates()
            } else {
                RemoteDatabaseManager.getAllDates()
            }
            result.getOrNull() ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("SoundDataRepository", "获取所有日期失败", e)
            emptyList()
        }
    }
    
    /**
     * 同步所有时长数据
     */
    private suspend fun syncAllDurationDataFromRemote(): Result<Int> {
        return try {
            // 获取所有时长数据日期范围
            val dateRangesResult = if (AppConfig.USE_REMOTE_DB_V2) {
                RemoteDatabaseManagerV2.getAllDurationDateRanges()
            } else {
                Result.success(emptyList())
            }
            
            val dateRanges = dateRangesResult.getOrNull() ?: emptyList()
            if (dateRanges.isEmpty()) {
                android.util.Log.d("SoundDataRepository", "没有时长数据需要同步")
                return Result.success(0)
            }
            
            var totalCount = 0
            
            // 同步每个日期范围的时长数据
            dateRanges.forEach { (startDate, endDate) ->
                try {
                    android.util.Log.d("SoundDataRepository", "同步时长数据: $startDate ~ $endDate")
                    
                    val durationResult = if (AppConfig.USE_REMOTE_DB_V2) {
                        RemoteDatabaseManagerV2.fetchDurationDataByDateRange(startDate, endDate)
                    } else {
                        Result.success(emptyList())
                    }
                    
                    if (durationResult.isSuccess) {
                        val durationList = durationResult.getOrNull() ?: emptyList()
                        if (durationList.isNotEmpty()) {
                            durationDataDao.insertAll(durationList)
                            totalCount += durationList.size
                            android.util.Log.d("SoundDataRepository", "保存时长数据: ${durationList.size}条")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SoundDataRepository", "同步时长数据异常: $startDate ~ $endDate", e)
                }
            }
            
            Result.success(totalCount)
        } catch (e: Exception) {
            android.util.Log.e("SoundDataRepository", "同步时长数据失败", e)
            Result.failure(Exception("同步时长数据失败: ${e.message}", e))
        }
    }
    
    /**
     * 同步最新时长数据
     */
    private suspend fun syncLatestDurationDataFromRemote(): Result<Int> {
        return try {
            // 获取最新时长数据日期范围
            val dateRangeResult = if (AppConfig.USE_REMOTE_DB_V2) {
                RemoteDatabaseManagerV2.getLatestDurationDateRange()
            } else {
                Result.success(null) // 旧版本不支持
            }
            
            val dateRange = dateRangeResult.getOrNull()
            if (dateRange == null) {
                android.util.Log.d("SoundDataRepository", "没有时长数据需要同步")
                return Result.success(0)
            }
            
            val (startDate, endDate) = dateRange
            android.util.Log.d("SoundDataRepository", "同步时长数据: $startDate ~ $endDate")
            
            // 获取远程时长数据
            val durationResult = if (AppConfig.USE_REMOTE_DB_V2) {
                RemoteDatabaseManagerV2.fetchDurationDataByDateRange(startDate, endDate)
            } else {
                Result.success(emptyList())
            }
            
            if (durationResult.isFailure) {
                return Result.failure(Exception("获取时长数据失败: ${durationResult.exceptionOrNull()?.message}"))
            }
            
            val durationList = durationResult.getOrNull() ?: emptyList()
            if (durationList.isEmpty()) {
                return Result.success(0)
            }
            
            // 保存到本地数据库
            val durationDao = AppDatabase.getDatabase(context).durationDataDao()
            
            // 先删除本地该日期范围的数据
            durationDao.deleteByDateRange(startDate, endDate)
            
            // 插入新数据
            durationDao.insertAll(durationList)
            
            android.util.Log.d("SoundDataRepository", "时长数据同步完成: ${durationList.size}条")
            Result.success(durationList.size)
            
        } catch (e: Exception) {
            android.util.Log.e("SoundDataRepository", "同步时长数据失败", e)
            Result.failure(Exception("同步时长数据失败: ${e.message}", e))
        }
    }
    
    /**
     * 从远程数据库获取最新日期
     */
    private suspend fun getLatestDateFromRemote(): String? {
        return try {
            val result = if (AppConfig.USE_REMOTE_DB_V2) {
                RemoteDatabaseManagerV2.getLatestDate()
            } else {
                RemoteDatabaseManager.getLatestDate()
            }
            result.getOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 从远程数据库同步主播名单
     */
    suspend fun syncStreamersFromDatabase(): Result<String> {
        return genderUtils.syncFromDatabase()
    }
    
    /**
     * 检查并同步主播名单
     * 如果超过24小时未同步，则自动同步
     */
    suspend fun checkAndSyncStreamers(): Result<String> {
        return try {
            val lastSyncTime = genderUtils.getLastSyncTime()
            val currentTime = System.currentTimeMillis()
            val hoursSinceSync = (currentTime - lastSyncTime) / (1000 * 60 * 60)
            
            android.util.Log.d("SoundDataRepository", "主播名单上次同步: ${genderUtils.getLastSyncTimeString()}, 距今${hoursSinceSync}小时")
            
            // 如果超过24小时未同步，或从未同步过，则自动同步
            if (lastSyncTime == 0L || hoursSinceSync >= 24) {
                android.util.Log.d("SoundDataRepository", "自动同步主播名单")
                val syncResult = genderUtils.syncFromDatabase()
                
                if (syncResult.isSuccess) {
                    Result.success("主播名单同步成功: ${syncResult.getOrNull()}")
                } else {
                    Result.failure(syncResult.exceptionOrNull() ?: Exception("同步失败"))
                }
            } else {
                Result.success("主播名单无需同步（距上次同步${hoursSinceSync}小时）")
            }
        } catch (e: Exception) {
            android.util.Log.e("SoundDataRepository", "检查主播名单同步失败", e)
            Result.failure(e)
        }
    }
}
