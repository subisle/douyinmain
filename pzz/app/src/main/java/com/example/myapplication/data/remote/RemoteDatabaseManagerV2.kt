package com.example.myapplication.data.remote

import com.example.myapplication.config.AppConfig
import com.example.myapplication.data.model.SoundData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 远程数据库管理器 V2
 * 使用SimpleMySQLClient直接实现MySQL协议，绕过JDBC
 */
object RemoteDatabaseManagerV2 {
    
    private const val TAG = "RemoteDatabaseManagerV2"
    
    /**
     * 创建数据库客户端
     */
    private fun createClient(): SimpleMySQLClient {
        return SimpleMySQLClient(
            host = AppConfig.DB_HOST,
            port = AppConfig.DB_PORT,
            user = AppConfig.DB_USER,
            password = AppConfig.DB_PASSWORD,
            database = AppConfig.DB_NAME
        )
    }
    
    /**
     * 从远程数据库获取指定日期的数据
     * 包括所有主播，即使当天没有数据也会显示（音浪为0）
     */
    suspend fun fetchDataByDate(date: String): Result<List<SoundData>> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "开始获取数据，日期: $date")
            
            // 连接数据库
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            // 1. 获取所有主播列表
            val allStreamersResult = fetchAllStreamers()
            if (allStreamersResult.isFailure) {
                return@withContext Result.failure(allStreamersResult.exceptionOrNull() ?: Exception("获取主播列表失败"))
            }
            val allStreamers = allStreamersResult.getOrNull() ?: emptyList()
            android.util.Log.d(TAG, "获取到 ${allStreamers.size} 个主播")
            
            // 2. 查询当天有数据的主播
            val sql = "SELECT streamer_id, streamer_name, sound_wave, total_sound_wave, record_date, created_at " +
                    "FROM ${AppConfig.DB_TABLE} " +
                    "WHERE record_date = '$date'"
            
            android.util.Log.d(TAG, "执行SQL: $sql")
            val results = client.query(sql)
            
            // 构建当天数据的Map（ID前8位 -> 数据）
            val todayDataMapByPrefix = mutableMapOf<String, Map<String, Any?>>()
            results.forEach { row ->
                val streamerId = row["streamer_id"]?.toString() ?: ""
                val prefix = if (streamerId.length >= 8) streamerId.substring(0, 8) else streamerId
                todayDataMapByPrefix[prefix] = row
            }
            android.util.Log.d(TAG, "当天有数据的主播: ${todayDataMapByPrefix.size} 个")
            
            // 3. 查询每个主播最新的total_sound_wave（用于没有当天数据的主播）
            // 使用ID前8位进行匹配
            val latestTotalsMapByPrefix = mutableMapOf<String, Long>()
            
            if (allStreamers.isNotEmpty()) {
                // 获取所有主播的ID前8位
                val idPrefixes = allStreamers.map { streamer ->
                    if (streamer.streamerId.length >= 8) {
                        streamer.streamerId.substring(0, 8)
                    } else {
                        streamer.streamerId
                    }
                }.distinct()
                
                // 为每个ID前缀查询最新的总音浪（查询指定日期之前的最新记录）
                idPrefixes.forEach { prefix ->
                    try {
                        val latestSql = """
                            SELECT streamer_id, total_sound_wave, record_date
                            FROM ${AppConfig.DB_TABLE}
                            WHERE SUBSTRING(streamer_id, 1, 8) = '$prefix' 
                            AND record_date < '$date'
                            ORDER BY record_date DESC
                            LIMIT 1
                        """.trimIndent()
                        
                        val latestResults = client.query(latestSql)
                        if (latestResults.isNotEmpty()) {
                            val totalSoundWave = latestResults[0]["total_sound_wave"]?.toString()?.toLongOrNull() ?: 0L
                            latestTotalsMapByPrefix[prefix] = totalSoundWave
                            android.util.Log.d(TAG, "ID前缀 $prefix 在 $date 之前的最新总音浪: $totalSoundWave (日期: ${latestResults[0]["record_date"]})")
                        }
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "查询ID前缀 $prefix 的历史总音浪失败: ${e.message}")
                    }
                }
                android.util.Log.d(TAG, "获取到 ${latestTotalsMapByPrefix.size} 个主播的历史总音浪")
            }
            
            // 4. 构建完整的数据列表（所有主播）- 使用ID前8位匹配
            val dataList = allStreamers.map { streamer ->
                val streamerPrefix = if (streamer.streamerId.length >= 8) {
                    streamer.streamerId.substring(0, 8)
                } else {
                    streamer.streamerId
                }
                
                val todayData = todayDataMapByPrefix[streamerPrefix]
                
                if (todayData != null) {
                    // 有当天数据 - 使用ID前8位匹配成功
                    var totalSoundWave = todayData["total_sound_wave"]?.toString()?.toLongOrNull() ?: 0L
                    val soundWave = todayData["sound_wave"]?.toString()?.toLongOrNull() ?: 0L
                    
                    // 如果当天的total_sound_wave为0，使用历史最新值
                    if (totalSoundWave == 0L) {
                        totalSoundWave = latestTotalsMapByPrefix[streamerPrefix] ?: 0L
                    }
                    
                    // 打印匹配信息（用于调试）
                    if (streamer.nickname.contains("浩杰") || streamer.nickname.contains("狼兴")) {
                        android.util.Log.d(TAG, "✅ 匹配成功: ${streamer.nickname}, ID前8位=$streamerPrefix, 日音浪=$soundWave, 总音浪=$totalSoundWave")
                    }
                    
                    SoundData(
                        id = 0L,
                        streamerId = streamer.streamerId,  // 使用主播名单中的ID（保持一致性）
                        streamerName = streamer.nickname,  // 使用主播名单中的昵称
                        soundWave = soundWave,
                        totalSoundWave = totalSoundWave,
                        effectiveBroadcastDuration = null,
                        recordDate = date,
                        gradeRank = null,
                        gradeLetter = null,
                        overallRank = 0,
                        createdAt = todayData["created_at"]?.toString() ?: ""
                    )
                } else {
                    // 没有当天数据，创建空记录
                    // 打印未匹配信息（用于调试）
                    if (streamer.nickname.contains("浩杰") || streamer.nickname.contains("狼兴")) {
                        android.util.Log.d(TAG, "⚠️ 未匹配: ${streamer.nickname}, ID前8位=$streamerPrefix, 使用历史总音浪=${latestTotalsMapByPrefix[streamerPrefix] ?: 0L}")
                    }
                    
                    SoundData(
                        id = 0L,
                        streamerId = streamer.streamerId,
                        streamerName = streamer.nickname,
                        soundWave = 0L,  // 当日音浪为0
                        totalSoundWave = latestTotalsMapByPrefix[streamerPrefix] ?: 0L,  // 使用历史总音浪
                        effectiveBroadcastDuration = null,
                        recordDate = date,
                        gradeRank = null,
                        gradeLetter = null,
                        overallRank = 0,
                        createdAt = ""
                    )
                }
            }
            
            // 按总音浪降序排序
            val sortedDataList = dataList.sortedByDescending { it.totalSoundWave }
            
            android.util.Log.d(TAG, "查询成功，共 ${sortedDataList.size} 个主播（当天有数据: ${todayDataMapByPrefix.size}，补充: ${sortedDataList.size - todayDataMapByPrefix.size}）")
            Result.success(sortedDataList)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "获取数据失败", e)
            Result.failure(Exception("获取数据失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 从远程数据库获取日期范围内的数据
     */
    suspend fun fetchDataByDateRange(startDate: String, endDate: String): Result<List<SoundData>> = 
        withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "========== 开始同步数据 ==========")
            android.util.Log.d(TAG, "日期范围: $startDate 到 $endDate")
            
            // 连接数据库
            if (!client.connect()) {
                android.util.Log.e(TAG, "❌ 数据库连接失败")
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            android.util.Log.d(TAG, "✅ 数据库连接成功")
            
            // 执行查询 - 按日期和总音浪排序
            val sql = "SELECT streamer_id, streamer_name, sound_wave, total_sound_wave, record_date, created_at " +
                    "FROM ${AppConfig.DB_TABLE} " +
                    "WHERE record_date BETWEEN '$startDate' AND '$endDate' " +
                    "ORDER BY record_date DESC, total_sound_wave DESC"
            
            android.util.Log.d(TAG, "执行SQL: $sql")
            val results = client.query(sql)
            android.util.Log.d(TAG, "✅ SQL执行完成，返回 ${results.size} 行原始数据")
            
            // 转换结果并按日期分组计算排名
            val dataList = mutableListOf<SoundData>()
            var currentDate = ""
            var rank = 1
            var processedCount = 0
            
            results.forEach { row ->
                val recordDate = row["record_date"]?.toString() ?: ""
                
                // 如果日期变化，重置排名
                if (recordDate != currentDate) {
                    if (currentDate.isNotEmpty()) {
                        android.util.Log.d(TAG, "  完成日期 $currentDate: 共 ${rank - 1} 条数据")
                    }
                    currentDate = recordDate
                    rank = 1
                    android.util.Log.d(TAG, "开始处理日期: $recordDate")
                }
                
                val soundData = SoundData(
                    id = 0L,
                    streamerId = row["streamer_id"]?.toString() ?: "",
                    streamerName = row["streamer_name"]?.toString() ?: "",
                    soundWave = row["sound_wave"]?.toString()?.toLongOrNull() ?: 0L,
                    totalSoundWave = row["total_sound_wave"]?.toString()?.toLongOrNull() ?: 0L,
                    effectiveBroadcastDuration = null,
                    recordDate = recordDate,
                    gradeRank = null,
                    gradeLetter = null,
                    overallRank = rank++,
                    createdAt = row["created_at"]?.toString() ?: ""
                )
                
                dataList.add(soundData)
                processedCount++
            }
            
            // 记录最后一个日期的统计
            if (currentDate.isNotEmpty()) {
                android.util.Log.d(TAG, "  完成日期 $currentDate: 共 ${rank - 1} 条数据")
            }
            
            android.util.Log.d(TAG, "✅ 数据转换完成，共处理 $processedCount 条数据")
            
            // 按日期统计数据量
            val dateGroups = dataList.groupBy { it.recordDate }
            android.util.Log.d(TAG, "========== 数据统计 ==========")
            dateGroups.forEach { (date, items) ->
                android.util.Log.d(TAG, "  $date: ${items.size} 条数据")
            }
            android.util.Log.d(TAG, "总计: ${dataList.size} 条数据，涉及 ${dateGroups.size} 个日期")
            android.util.Log.d(TAG, "========== 同步完成 ==========")
            
            Result.success(dataList)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ 获取数据失败", e)
            Result.failure(Exception("获取数据失败: ${e.message}", e))
        } finally {
            client.close()
            android.util.Log.d(TAG, "数据库连接已关闭")
        }
    }
    
    /**
     * 测试数据库连接
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "测试数据库连接")
            val connected = client.connect()
            
            if (connected) {
                android.util.Log.d(TAG, "连接测试成功")
                Result.success(true)
            } else {
                android.util.Log.e(TAG, "连接测试失败")
                Result.success(false)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "连接测试异常", e)
            Result.failure(Exception("连接测试失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 获取远程数据库中的最新日期
     */
    suspend fun getLatestDate(): Result<String?> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "获取最新日期")
            
            // 连接数据库
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            // 执行查询
            val sql = "SELECT MAX(record_date) as latest_date FROM ${AppConfig.DB_TABLE}"
            
            val results = client.query(sql)
            
            val latestDate = if (results.isNotEmpty()) {
                results[0]["latest_date"]?.toString()
            } else {
                null
            }
            
            android.util.Log.d(TAG, "最新日期: $latestDate")
            Result.success(latestDate)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "获取最新日期失败", e)
            Result.failure(Exception("获取最新日期失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 获取远程数据库中所有的日期列表
     */
    suspend fun getAllDates(): Result<List<String>> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "获取所有日期")
            
            // 连接数据库
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            // 执行查询 - 获取所有不同的日期，按日期降序排列
            val sql = "SELECT DISTINCT record_date FROM ${AppConfig.DB_TABLE} ORDER BY record_date DESC"
            
            val results = client.query(sql)
            
            val dates = results.mapNotNull { row ->
                row["record_date"]?.toString()
            }
            
            android.util.Log.d(TAG, "找到 ${dates.size} 个日期")
            Result.success(dates)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "获取所有日期失败", e)
            Result.failure(Exception("获取所有日期失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 获取所有时长数据的日期范围列表
     */
    suspend fun getAllDurationDateRanges(): Result<List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "获取所有时长数据日期范围")
            
            // 连接数据库
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            // 执行查询 - 获取所有不同的日期范围
            val sql = "SELECT DISTINCT start_date, end_date FROM duration_data ORDER BY start_date DESC"
            
            val results = client.query(sql)
            
            val dateRanges = results.mapNotNull { row ->
                val startDate = row["start_date"]?.toString()
                val endDate = row["end_date"]?.toString()
                if (startDate != null && endDate != null) {
                    Pair(startDate, endDate)
                } else {
                    null
                }
            }
            
            android.util.Log.d(TAG, "找到 ${dateRanges.size} 个时长数据日期范围")
            Result.success(dateRanges)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "获取时长数据日期范围失败", e)
            Result.failure(Exception("获取时长数据日期范围失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 导入数据到远程数据库
     * 策略：先删除对应日期的数据，然后重新插入
     */
    suspend fun importData(dataList: List<SoundData>): Result<Int> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "========== 开始导入数据到远程数据库 ==========")
            android.util.Log.d(TAG, "共 ${dataList.size} 条数据")
            
            // 连接数据库
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            // 获取导入日期（假设所有数据是同一天的）
            val recordDate = dataList.firstOrNull()?.recordDate ?: ""
            android.util.Log.d(TAG, "导入日期: $recordDate")
            
            // 步骤1：删除该日期的所有数据
            try {
                val deleteSql = "DELETE FROM ${AppConfig.DB_TABLE} WHERE record_date = '$recordDate'"
                android.util.Log.d(TAG, "删除旧数据: $deleteSql")
                val deletedRows = client.execute(deleteSql)
                android.util.Log.d(TAG, "✅ 已删除 $deletedRows 条旧数据")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "删除旧数据失败（可能该日期没有数据）: ${e.message}")
            }
            
            // 步骤2：计算前一天的日期
            val previousDate = getPreviousDate(recordDate)
            
            // 步骤3：查询前一天所有主播的总音浪
            val previousTotalMap = mutableMapOf<String, Long>()
            try {
                val sql = "SELECT streamer_id, total_sound_wave FROM ${AppConfig.DB_TABLE} WHERE record_date = '$previousDate'"
                val results = client.query(sql)
                results.forEach { row ->
                    val streamerId = row["streamer_id"]?.toString() ?: ""
                    val totalSoundWave = row["total_sound_wave"]?.toString()?.toLongOrNull() ?: 0L
                    previousTotalMap[streamerId] = totalSoundWave
                }
                android.util.Log.d(TAG, "查询到前一天($previousDate)的数据: ${previousTotalMap.size} 条")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "查询前一天数据失败，将使用当日音浪作为总音浪", e)
            }
            
            // 步骤4：查询user表获取所有主播的昵称
            val nicknameMap = mutableMapOf<String, String>()
            try {
                val userSql = "SELECT streamer_id, nickname FROM user"
                val userResults = client.query(userSql)
                userResults.forEach { row ->
                    val streamerId = row["streamer_id"]?.toString() ?: ""
                    val nickname = row["nickname"]?.toString() ?: ""
                    if (streamerId.isNotEmpty() && nickname.isNotEmpty()) {
                        nicknameMap[streamerId] = nickname
                    }
                }
                android.util.Log.d(TAG, "查询到user表中的昵称: ${nicknameMap.size} 个")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "查询user表失败，将使用CSV中的名字", e)
            }
            
            android.util.Log.d(TAG, "========== 开始逐条插入数据 ==========")
            
            var successCount = 0
            var failCount = 0
            val failedIds = mutableListOf<String>()
            val failedDetails = mutableListOf<String>()
            var consecutiveFailures = 0  // 连续失败次数
            
            // 步骤5：逐条插入数据（使用简单的INSERT，不用ON DUPLICATE KEY UPDATE）
            dataList.forEachIndexed { index, data ->
                var retryCount = 0
                val maxRetries = 3  // 每条最多重试3次（增加到3次）
                var inserted = false
                
                while (!inserted && retryCount <= maxRetries) {
                    try {
                        // 如果连续失败超过3次，可能是连接问题，尝试重新连接
                        if (consecutiveFailures >= 3) {
                            android.util.Log.w(TAG, "检测到连续${consecutiveFailures}次失败，尝试重新连接...")
                            try {
                                client.close()
                                kotlinx.coroutines.delay(1000)  // 等待1秒
                                if (client.connect()) {
                                    android.util.Log.d(TAG, "✅ 重新连接成功，继续插入")
                                    consecutiveFailures = 0
                                } else {
                                    android.util.Log.e(TAG, "❌ 重新连接失败，终止导入")
                                    throw Exception("数据库连接中断，已成功导入${successCount}条，失败${failCount}条")
                                }
                            } catch (reconnectError: Exception) {
                                android.util.Log.e(TAG, "❌ 重新连接异常", reconnectError)
                                throw Exception("数据库连接中断，已成功导入${successCount}条，失败${failCount}条")
                            }
                        }
                        
                        // 计算总音浪 = 前一天总音浪 + 当日音浪
                        val previousTotal = previousTotalMap[data.streamerId] ?: 0L
                        val newTotalSoundWave = previousTotal + data.soundWave
                        
                        // 使用user表中的昵称，如果没有则使用CSV中的名字
                        val streamerName = nicknameMap[data.streamerId] ?: data.streamerName
                        
                        // 转义特殊字符，防止SQL注入
                        val escapedName = streamerName.replace("'", "''")
                        
                        // 简单的INSERT语句（因为已经删除了旧数据）
                        val sql = "INSERT INTO ${AppConfig.DB_TABLE} " +
                                "(streamer_id, streamer_name, sound_wave, total_sound_wave, record_date) " +
                                "VALUES ('${data.streamerId}', '$escapedName', ${data.soundWave}, $newTotalSoundWave, '${data.recordDate}')"
                        
                        val affectedRows = client.execute(sql)
                        
                        // 注意：affected_rows可能为0（例如ON DUPLICATE KEY UPDATE时值未变化）
                        // 但只要execute没有抛出异常，就认为是成功的
                        if (affectedRows >= 0) {
                            successCount++
                            consecutiveFailures = 0  // 重置连续失败计数
                            inserted = true  // 标记为已插入
                            
                            // 每10条打印一次进度
                            if ((index + 1) % 10 == 0 || (index + 1) == dataList.size) {
                                android.util.Log.d(TAG, "✅ 进度: ${index + 1}/${dataList.size}, 成功: $successCount, 失败: $failCount")
                            }
                        } else {
                            // affected_rows = -1 表示执行失败（execute方法返回-1）
                            retryCount++
                            consecutiveFailures++
                            
                            if (retryCount > maxRetries) {
                                failCount++
                                failedIds.add(data.streamerId)
                                val detail = "ID=${data.streamerId}, 名称=$streamerName, affected_rows=$affectedRows, 重试${maxRetries}次后仍失败"
                                failedDetails.add(detail)
                                android.util.Log.e(TAG, "❌ 第${index + 1}条插入失败: $detail")
                            } else {
                                android.util.Log.w(TAG, "⚠️ 第${index + 1}条失败，准备重试 (${retryCount}/${maxRetries})")
                                kotlinx.coroutines.delay(500)  // 重试前等待0.5秒
                            }
                        }
                    } catch (e: Exception) {
                        retryCount++
                        consecutiveFailures++
                        
                        val errorMsg = e.message ?: "未知错误"
                        
                        if (retryCount > maxRetries) {
                            failCount++
                            failedIds.add(data.streamerId)
                            val detail = "ID=${data.streamerId}, 名称=${data.streamerName}, 错误=$errorMsg, 重试${maxRetries}次后仍失败"
                            failedDetails.add(detail)
                            android.util.Log.e(TAG, "❌ 第${index + 1}条插入异常: $detail", e)
                        } else {
                            android.util.Log.w(TAG, "⚠️ 第${index + 1}条异常，准备重试 (${retryCount}/${maxRetries}): $errorMsg")
                            kotlinx.coroutines.delay(500)  // 重试前等待0.5秒
                        }
                        
                        // 如果是严重错误（如连接断开），直接终止
                        if (retryCount > maxRetries && (
                            errorMsg.contains("connection", ignoreCase = true) ||
                            errorMsg.contains("socket", ignoreCase = true) ||
                            errorMsg.contains("closed", ignoreCase = true))) {
                            android.util.Log.e(TAG, "❌ 检测到连接错误，终止导入")
                            throw Exception("数据库连接中断，已成功导入${successCount}条，失败${failCount}条")
                        }
                    }
                }
            }
            
            android.util.Log.d(TAG, "========== 导入完成 ==========")
            android.util.Log.d(TAG, "成功: $successCount 条, 失败: $failCount 条")
            
            if (failCount > 0) {
                android.util.Log.e(TAG, "失败的ID列表: ${failedIds.joinToString(", ")}")
                android.util.Log.e(TAG, "失败详情:")
                failedDetails.take(10).forEach { detail ->  // 只打印前10条失败详情
                    android.util.Log.e(TAG, "  - $detail")
                }
                if (failedDetails.size > 10) {
                    android.util.Log.e(TAG, "  ... 还有 ${failedDetails.size - 10} 条失败记录")
                }
            }
            
            // 即使有失败，也返回成功的数量
            Result.success(successCount)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "导入数据失败", e)
            Result.failure(Exception("导入数据失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 计算前一天的日期
     */
    private fun getPreviousDate(dateStr: String): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return dateStr
            val calendar = java.util.Calendar.getInstance()
            calendar.time = date
            calendar.add(java.util.Calendar.DAY_OF_MONTH, -1)
            sdf.format(calendar.time)
        } catch (e: Exception) {
            dateStr
        }
    }
    
    /**
     * 清空指定日期的日音浪字段
     */
    suspend fun clearDailySoundWave(date: String): Result<String> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "开始清空日期的日音浪: $date")
            
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            val sql = "UPDATE ${AppConfig.DB_TABLE} SET sound_wave = 0 WHERE record_date = '$date'"
            client.execute(sql)
            
            android.util.Log.d(TAG, "清空完成: $date")
            Result.success("清空成功")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "清空日音浪失败", e)
            Result.failure(Exception("清空日音浪失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 删除指定日期的数据
     */
    suspend fun deleteDateData(date: String): Result<String> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "开始删除日期数据: $date")
            
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            val sql = "DELETE FROM ${AppConfig.DB_TABLE} WHERE record_date = '$date'"
            client.execute(sql)
            
            android.util.Log.d(TAG, "删除完成: $date")
            Result.success("删除成功")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "删除数据失败", e)
            Result.failure(Exception("删除数据失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 重新计算指定日期之后所有日期的总音浪
     */
    suspend fun recalculateTotalSoundWave(fromDate: String): Result<String> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "开始重新计算总音浪，从日期: $fromDate")
            
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            // 获取所有需要重新计算的日期（fromDate之后的所有日期）
            val sql = "SELECT DISTINCT record_date FROM ${AppConfig.DB_TABLE} WHERE record_date > '$fromDate' ORDER BY record_date"
            val dates = client.query(sql).mapNotNull { it["record_date"]?.toString() }
            
            android.util.Log.d(TAG, "需要重新计算的日期数: ${dates.size}")
            
            // 逐个日期重新计算
            dates.forEach { date ->
                val previousDate = getPreviousDate(date)
                
                // 查询前一天的总音浪
                val previousTotalMap = mutableMapOf<String, Long>()
                try {
                    val querySql = "SELECT streamer_id, total_sound_wave FROM ${AppConfig.DB_TABLE} WHERE record_date = '$previousDate'"
                    val results = client.query(querySql)
                    results.forEach { row ->
                        val streamerId = row["streamer_id"]?.toString() ?: ""
                        val totalSoundWave = row["total_sound_wave"]?.toString()?.toLongOrNull() ?: 0L
                        previousTotalMap[streamerId] = totalSoundWave
                    }
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "查询前一天数据失败: $previousDate", e)
                }
                
                // 查询当天的数据并更新总音浪
                val currentDataSql = "SELECT streamer_id, sound_wave FROM ${AppConfig.DB_TABLE} WHERE record_date = '$date'"
                val currentData = client.query(currentDataSql)
                
                currentData.forEach { row ->
                    val streamerId = row["streamer_id"]?.toString() ?: ""
                    val soundWave = row["sound_wave"]?.toString()?.toLongOrNull() ?: 0L
                    val previousTotal = previousTotalMap[streamerId] ?: 0L
                    val newTotalSoundWave = previousTotal + soundWave
                    
                    // 更新总音浪
                    val updateSql = "UPDATE ${AppConfig.DB_TABLE} SET total_sound_wave = $newTotalSoundWave WHERE streamer_id = '$streamerId' AND record_date = '$date'"
                    client.execute(updateSql)
                }
                
                android.util.Log.d(TAG, "重新计算完成: $date")
            }
            
            android.util.Log.d(TAG, "所有日期重新计算完成")
            Result.success("重新计算完成")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "重新计算失败", e)
            Result.failure(Exception("重新计算失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 获取所有主播列表
     */
    suspend fun fetchAllStreamers(): Result<List<com.example.myapplication.data.model.Streamer>> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "开始获取主播列表")
            
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            val sql = "SELECT id, serial_number, streamer_id, nickname, gender, created_at, updated_at " +
                    "FROM user ORDER BY gender, serial_number"
            
            android.util.Log.d(TAG, "执行SQL: $sql")
            val results = client.query(sql)
            
            val streamers = results.map { row ->
                com.example.myapplication.data.model.Streamer(
                    id = row["id"]?.toString()?.toIntOrNull() ?: 0,
                    serialNumber = row["serial_number"]?.toString()?.toIntOrNull() ?: 0,
                    streamerId = row["streamer_id"]?.toString() ?: "",
                    nickname = row["nickname"]?.toString() ?: "",
                    fullName = null,  // user表没有full_name字段
                    gender = com.example.myapplication.data.model.Gender.fromString(
                        row["gender"]?.toString() ?: "male"
                    ),
                    createdAt = row["created_at"]?.toString() ?: "",
                    updatedAt = row["updated_at"]?.toString() ?: ""
                )
            }
            
            android.util.Log.d(TAG, "获取主播列表成功，共 ${streamers.size} 个")
            Result.success(streamers)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "获取主播列表失败", e)
            Result.failure(Exception("获取主播列表失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 添加主播到远程数据库
     */
    suspend fun addStreamer(streamer: com.example.myapplication.data.model.Streamer): Result<String> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "开始添加主播: ${streamer.nickname}")
            
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            val sql = "INSERT INTO user (serial_number, streamer_id, nickname, gender) " +
                    "VALUES (${streamer.serialNumber}, '${streamer.streamerId}', '${streamer.nickname}', '${streamer.gender.toDbString()}')"
            
            android.util.Log.d(TAG, "执行SQL: $sql")
            val affectedRows = client.execute(sql)
            
            if (affectedRows > 0) {
                android.util.Log.d(TAG, "添加主播成功, 受影响行数=$affectedRows")
                Result.success("添加成功")
            } else {
                android.util.Log.e(TAG, "添加失败, 返回值=$affectedRows")
                Result.failure(Exception("添加失败"))
            }
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "添加主播失败", e)
            Result.failure(Exception("添加主播失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 更新主播信息（通过streamer_id查找）
     */
    suspend fun updateStreamer(streamer: com.example.myapplication.data.model.Streamer): Result<String> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "开始更新主播: ${streamer.nickname}")
            
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            // 使用streamer_id作为查找条件（而不是id）
            val sql = "UPDATE user SET " +
                    "serial_number = ${streamer.serialNumber}, " +
                    "nickname = '${streamer.nickname}', " +
                    "gender = '${streamer.gender.toDbString()}' " +
                    "WHERE streamer_id = '${streamer.streamerId}'"
            
            android.util.Log.d(TAG, "执行SQL: $sql")
            val affectedRows = client.execute(sql)
            
            if (affectedRows > 0) {
                android.util.Log.d(TAG, "更新主播成功, 受影响行数=$affectedRows")
                Result.success("更新成功")
            } else {
                android.util.Log.e(TAG, "更新失败, 返回值=$affectedRows")
                Result.failure(Exception("更新失败"))
            }
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "更新主播失败", e)
            Result.failure(Exception("更新主播失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 更新主播信息（支持修改streamer_id）
     */
    suspend fun updateStreamerWithIdChange(oldStreamerId: String, newStreamer: com.example.myapplication.data.model.Streamer): Result<String> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "开始更新主播（ID可能变更）: $oldStreamerId -> ${newStreamer.streamerId}")
            
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            // 使用旧的streamer_id作为查找条件，可以更新为新的streamer_id
            val sql = "UPDATE user SET " +
                    "serial_number = ${newStreamer.serialNumber}, " +
                    "streamer_id = '${newStreamer.streamerId}', " +
                    "nickname = '${newStreamer.nickname}', " +
                    "gender = '${newStreamer.gender.toDbString()}' " +
                    "WHERE streamer_id = '$oldStreamerId'"
            
            android.util.Log.d(TAG, "执行SQL: $sql")
            val affectedRows = client.execute(sql)
            
            if (affectedRows > 0) {
                android.util.Log.d(TAG, "更新主播成功, 受影响行数=$affectedRows")
                Result.success("更新成功")
            } else {
                android.util.Log.e(TAG, "更新失败, 返回值=$affectedRows")
                Result.failure(Exception("更新失败"))
            }
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "更新主播失败", e)
            Result.failure(Exception("更新主播失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 删除主播
     */
    suspend fun deleteStreamer(streamerId: String): Result<String> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "开始删除主播: $streamerId")
            
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            val sql = "DELETE FROM user WHERE streamer_id = '$streamerId'"
            
            android.util.Log.d(TAG, "执行SQL: $sql")
            val affectedRows = client.execute(sql)
            
            if (affectedRows > 0) {
                android.util.Log.d(TAG, "删除主播成功, 受影响行数=$affectedRows")
                Result.success("删除成功")
            } else {
                android.util.Log.e(TAG, "删除失败, 返回值=$affectedRows")
                Result.failure(Exception("删除失败"))
            }
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "删除主播失败", e)
            Result.failure(Exception("删除主播失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 导入直播有效时长数据到远程数据库
     */
    suspend fun importDurationData(dataList: List<com.example.myapplication.data.model.DurationData>): Result<Int> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "开始导入直播时长数据，共 ${dataList.size} 条")
            
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            var successCount = 0
            
            // 查询user表获取所有主播的昵称
            val nicknameMap = mutableMapOf<String, String>()
            try {
                val userSql = "SELECT streamer_id, nickname FROM user"
                val userResults = client.query(userSql)
                userResults.forEach { row ->
                    val streamerId = row["streamer_id"]?.toString() ?: ""
                    val nickname = row["nickname"]?.toString() ?: ""
                    if (streamerId.isNotEmpty() && nickname.isNotEmpty()) {
                        nicknameMap[streamerId] = nickname
                    }
                }
                android.util.Log.d(TAG, "查询到user表中的昵称: ${nicknameMap.size} 个")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "查询user表失败，将使用CSV中的名字", e)
            }
            
            // 逐条插入数据
            dataList.forEach { data ->
                try {
                    // 使用user表中的昵称，如果没有则使用CSV中的名字
                    val streamerName = nicknameMap[data.streamerId] ?: data.streamerName
                    
                    val sql = "INSERT INTO duration_data " +
                            "(streamer_id, streamer_name, duration, start_date, end_date, created_at) " +
                            "VALUES ('${data.streamerId}', '${streamerName}', '${data.duration}', " +
                            "'${data.startDate}', '${data.endDate}', '${data.createdAt}') " +
                            "ON DUPLICATE KEY UPDATE " +
                            "streamer_name = '${streamerName}', " +
                            "duration = '${data.duration}', " +
                            "created_at = '${data.createdAt}'"
                    
                    val affectedRows = client.execute(sql)
                    if (affectedRows > 0) {
                        successCount++
                    } else {
                        android.util.Log.e(TAG, "插入直播时长数据失败: ${data.streamerId}, 受影响行数=$affectedRows")
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "插入直播时长数据异常: ${data.streamerId}", e)
                }
            }
            
            android.util.Log.d(TAG, "导入直播时长完成，成功 $successCount 条")
            Result.success(successCount)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "导入直播时长数据失败", e)
            Result.failure(Exception("导入直播时长数据失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 删除指定日期范围的直播时长数据
     */
    suspend fun deleteDurationDataByDateRange(startDate: String, endDate: String): Result<String> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "开始删除直播时长数据: $startDate ~ $endDate")
            
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            val sql = "DELETE FROM duration_data WHERE start_date = '$startDate' AND end_date = '$endDate'"
            client.execute(sql)
            
            android.util.Log.d(TAG, "删除直播时长数据完成")
            Result.success("删除成功")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "删除直播时长数据失败", e)
            Result.failure(Exception("删除直播时长数据失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 从远程数据库获取指定日期范围的直播时长数据
     */
    suspend fun fetchDurationDataByDateRange(startDate: String, endDate: String): Result<List<com.example.myapplication.data.model.DurationData>> = 
        withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "开始获取直播时长数据，日期范围: $startDate ~ $endDate")
            
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            val sql = "SELECT streamer_id, streamer_name, duration, start_date, end_date, created_at " +
                    "FROM duration_data " +
                    "WHERE start_date = '$startDate' AND end_date = '$endDate' " +
                    "ORDER BY duration DESC"
            
            android.util.Log.d(TAG, "执行SQL: $sql")
            val results = client.query(sql)
            
            val dataList = results.map { row ->
                com.example.myapplication.data.model.DurationData(
                    id = 0L,
                    streamerId = row["streamer_id"]?.toString() ?: "",
                    streamerName = row["streamer_name"]?.toString() ?: "",
                    duration = row["duration"]?.toString() ?: "",
                    startDate = row["start_date"]?.toString() ?: "",
                    endDate = row["end_date"]?.toString() ?: "",
                    createdAt = row["created_at"]?.toString() ?: ""
                )
            }
            
            android.util.Log.d(TAG, "查询成功，获取到 ${dataList.size} 条直播时长数据")
            Result.success(dataList)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "获取直播时长数据失败", e)
            Result.failure(Exception("获取直播时长数据失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 获取最新的时长数据日期范围
     */
    suspend fun getLatestDurationDateRange(): Result<Pair<String, String>?> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            android.util.Log.d(TAG, "开始获取最新时长数据日期")
            
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            val sql = "SELECT start_date, end_date FROM duration_data " +
                    "ORDER BY start_date DESC LIMIT 1"
            
            val results = client.query(sql)
            
            if (results.isEmpty()) {
                android.util.Log.d(TAG, "没有时长数据")
                return@withContext Result.success(null)
            }
            
            val startDate = results[0]["start_date"]?.toString() ?: ""
            val endDate = results[0]["end_date"]?.toString() ?: ""
            
            android.util.Log.d(TAG, "最新时长数据日期: $startDate ~ $endDate")
            Result.success(Pair(startDate, endDate))
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "获取最新时长数据日期失败", e)
            Result.failure(Exception("获取最新时长数据日期失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    /**
     * 更新指定主播在指定日期的总音浪
     * @param streamerId 主播ID
     * @param totalSoundWave 新的总音浪值
     * @param recordDate 记录日期
     * @return 更新结果
     */
    suspend fun updateTotalSoundWave(
        streamerId: String,
        totalSoundWave: Long,
        recordDate: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        val client = createClient()
        try {
            if (!client.connect()) {
                return@withContext Result.failure(Exception("数据库连接失败"))
            }
            
            // 转义单引号，防止SQL注入
            val escapedStreamerId = streamerId.replace("'", "''")
            val escapedRecordDate = recordDate.replace("'", "''")
            
            val sql = """
                UPDATE ${AppConfig.DB_TABLE}
                SET total_sound_wave = $totalSoundWave
                WHERE streamer_id = '$escapedStreamerId'
                AND record_date = '$escapedRecordDate'
            """.trimIndent()
            
            val affectedRows = client.execute(sql)
            
            if (affectedRows > 0) {
                Result.success(affectedRows)
            } else {
                Result.failure(Exception("未找到匹配的记录"))
            }
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "更新总音浪失败: streamerId=$streamerId, date=$recordDate", e)
            Result.failure(Exception("更新总音浪失败: ${e.message}", e))
        } finally {
            client.close()
        }
    }
}
