package com.example.myapplication.data.remote

import com.example.myapplication.config.AppConfig
import com.example.myapplication.data.model.SoundData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.Properties

/**
 * 远程数据库管理器
 * 直接连接MySQL数据库进行数据同步
 */
object RemoteDatabaseManager {
    
    private const val TAG = "RemoteDatabaseManager"
    
    /**
     * 获取数据库连接
     * 使用MariaDB Connector（对Android兼容性更好）
     */
    private suspend fun getConnection(): Connection = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d(TAG, "开始连接数据库: ${AppConfig.DB_HOST}:${AppConfig.DB_PORT}")
            
            // 加载MariaDB驱动（兼容MySQL）
            Class.forName("org.mariadb.jdbc.Driver")
            android.util.Log.d(TAG, "MariaDB驱动加载成功")
            
            // 构建连接URL（MariaDB格式，兼容MySQL）
            val url = "jdbc:mariadb://${AppConfig.DB_HOST}:${AppConfig.DB_PORT}/${AppConfig.DB_NAME}" +
                    "?useSSL=false" +
                    "&allowPublicKeyRetrieval=true" +
                    "&serverTimezone=Asia/Shanghai" +
                    "&connectTimeout=${AppConfig.DB_TIMEOUT * 1000}" +
                    "&socketTimeout=${AppConfig.DB_TIMEOUT * 1000}"
            
            android.util.Log.d(TAG, "连接URL: $url")
            
            // 设置连接属性
            val props = Properties().apply {
                setProperty("user", AppConfig.DB_USER)
                setProperty("password", AppConfig.DB_PASSWORD)
                setProperty("useUnicode", "true")
                setProperty("characterEncoding", "UTF-8")
            }
            
            // 建立连接
            android.util.Log.d(TAG, "正在建立连接...")
            val connection = DriverManager.getConnection(url, props)
            android.util.Log.d(TAG, "数据库连接成功")
            connection
        } catch (e: ClassNotFoundException) {
            android.util.Log.e(TAG, "MySQL驱动未找到", e)
            throw Exception("MySQL驱动未找到: ${e.message}", e)
        } catch (e: java.sql.SQLException) {
            android.util.Log.e(TAG, "数据库连接失败", e)
            throw Exception("数据库连接失败: ${e.message}", e)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "连接异常", e)
            throw Exception("数据库连接失败: ${e.message}", e)
        }
    }
    
    /**
     * 从远程数据库获取指定日期的数据
     */
    suspend fun fetchDataByDate(date: String): Result<List<SoundData>> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            android.util.Log.d(TAG, "开始获取数据，日期: $date")
            connection = getConnection()
            
            val sql = """
                SELECT 
                    streamer_id, streamer_name, sound_wave, 
                    total_sound_wave, record_date, created_at
                FROM ${AppConfig.DB_TABLE}
                WHERE record_date = ?
            """.trimIndent()
            
            android.util.Log.d(TAG, "执行SQL: $sql")
            val statement = connection.prepareStatement(sql)
            statement.setString(1, date)
            
            android.util.Log.d(TAG, "开始查询...")
            val resultSet = statement.executeQuery()
            val dataList = mutableListOf<SoundData>()
            
            while (resultSet.next()) {
                val soundData = resultSet.toSoundData()
                // 不设置排名，由App层面根据设置排序后再设置
                dataList.add(soundData.copy(overallRank = 0))
            }
            
            resultSet.close()
            statement.close()
            
            android.util.Log.d(TAG, "查询成功，获取到 ${dataList.size} 条数据")
            Result.success(dataList)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "获取数据失败", e)
            Result.failure(Exception("获取数据失败: ${e.message}", e))
        } finally {
            try {
                connection?.close()
                android.util.Log.d(TAG, "数据库连接已关闭")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "关闭连接失败", e)
            }
        }
    }
    
    /**
     * 从远程数据库获取日期范围内的数据
     */
    suspend fun fetchDataByDateRange(startDate: String, endDate: String): Result<List<SoundData>> = 
        withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            connection = getConnection()
            
            val sql = """
                SELECT 
                    streamer_id, streamer_name, sound_wave, 
                    total_sound_wave, record_date, created_at
                FROM ${AppConfig.DB_TABLE}
                WHERE record_date BETWEEN ? AND ?
                ORDER BY record_date DESC
            """.trimIndent()
            
            val statement = connection.prepareStatement(sql)
            statement.setString(1, startDate)
            statement.setString(2, endDate)
            
            val resultSet = statement.executeQuery()
            val dataList = mutableListOf<SoundData>()
            var currentDate = ""
            var rank = 1
            
            while (resultSet.next()) {
                val recordDate = resultSet.getString("record_date") ?: ""
                // 如果日期变化，重置排名
                if (recordDate != currentDate) {
                    currentDate = recordDate
                    rank = 1
                }
                
                val soundData = resultSet.toSoundData()
                dataList.add(soundData.copy(overallRank = rank++))
            }
            
            resultSet.close()
            statement.close()
            
            Result.success(dataList)
        } catch (e: Exception) {
            Result.failure(Exception("获取数据失败: ${e.message}", e))
        } finally {
            connection?.close()
        }
    }
    
    /**
     * 测试数据库连接
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            connection = getConnection()
            val isValid = connection.isValid(AppConfig.DB_TIMEOUT)
            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(Exception("连接测试失败: ${e.message}", e))
        } finally {
            connection?.close()
        }
    }
    
    /**
     * 获取远程数据库中的最新日期
     */
    suspend fun getLatestDate(): Result<String?> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            connection = getConnection()
            
            val sql = """
                SELECT MAX(record_date) as latest_date
                FROM ${AppConfig.DB_TABLE}
            """.trimIndent()
            
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(sql)
            
            val latestDate = if (resultSet.next()) {
                resultSet.getString("latest_date")
            } else {
                null
            }
            
            resultSet.close()
            statement.close()
            
            Result.success(latestDate)
        } catch (e: Exception) {
            Result.failure(Exception("获取最新日期失败: ${e.message}", e))
        } finally {
            connection?.close()
        }
    }
    
    /**
     * 获取远程数据库中所有的日期列表
     */
    suspend fun getAllDates(): Result<List<String>> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            connection = getConnection()
            
            val sql = """
                SELECT DISTINCT record_date
                FROM ${AppConfig.DB_TABLE}
                ORDER BY record_date DESC
            """.trimIndent()
            
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(sql)
            
            val dates = mutableListOf<String>()
            while (resultSet.next()) {
                val date = resultSet.getString("record_date")
                if (date != null) {
                    dates.add(date)
                }
            }
            
            resultSet.close()
            statement.close()
            
            Result.success(dates)
        } catch (e: Exception) {
            Result.failure(Exception("获取所有日期失败: ${e.message}", e))
        } finally {
            connection?.close()
        }
    }
    
    /**
     * 获取所有时长数据的日期范围列表
     */
    suspend fun getAllDurationDateRanges(): Result<List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            connection = getConnection()
            
            val sql = """
                SELECT DISTINCT start_date, end_date
                FROM duration_data
                ORDER BY start_date DESC
            """.trimIndent()
            
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(sql)
            
            val dateRanges = mutableListOf<Pair<String, String>>()
            while (resultSet.next()) {
                val startDate = resultSet.getString("start_date")
                val endDate = resultSet.getString("end_date")
                if (startDate != null && endDate != null) {
                    dateRanges.add(Pair(startDate, endDate))
                }
            }
            
            resultSet.close()
            statement.close()
            
            Result.success(dateRanges)
        } catch (e: Exception) {
            Result.failure(Exception("获取时长数据日期范围失败: ${e.message}", e))
        } finally {
            connection?.close()
        }
    }
    
    /**
     * 导入数据到远程数据库
     */
    suspend fun importData(dataList: List<SoundData>): Result<Int> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            connection = getConnection()
            connection.autoCommit = false
            
            // 获取导入日期（假设所有数据是同一天的）
            val recordDate = dataList.firstOrNull()?.recordDate ?: ""
            
            // 计算前一天的日期
            val previousDate = getPreviousDate(recordDate)
            
            // 查询前一天所有主播的总音浪
            val previousTotalMap = mutableMapOf<String, Long>()
            try {
                val querySql = "SELECT streamer_id, total_sound_wave FROM ${AppConfig.DB_TABLE} WHERE record_date = ?"
                val queryStmt = connection.prepareStatement(querySql)
                queryStmt.setString(1, previousDate)
                val rs = queryStmt.executeQuery()
                while (rs.next()) {
                    val streamerId = rs.getString("streamer_id") ?: ""
                    val totalSoundWave = rs.getLong("total_sound_wave") ?: 0L
                    previousTotalMap[streamerId] = totalSoundWave
                }
                rs.close()
                queryStmt.close()
            } catch (e: Exception) {
                // 查询失败，将使用当日音浪作为总音浪
            }
            
            // 查询user表获取所有主播的昵称
            val nicknameMap = mutableMapOf<String, String>()
            try {
                val userSql = "SELECT streamer_id, nickname FROM user"
                val userStmt = connection.createStatement()
                val userRs = userStmt.executeQuery(userSql)
                while (userRs.next()) {
                    val streamerId = userRs.getString("streamer_id") ?: ""
                    val nickname = userRs.getString("nickname") ?: ""
                    if (streamerId.isNotEmpty() && nickname.isNotEmpty()) {
                        nicknameMap[streamerId] = nickname
                    }
                }
                userRs.close()
                userStmt.close()
                android.util.Log.d(TAG, "查询到user表中的昵称: ${nicknameMap.size} 个")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "查询user表失败，将使用CSV中的名字", e)
            }
            
            val sql = """
                INSERT INTO ${AppConfig.DB_TABLE} 
                (streamer_id, streamer_name, sound_wave, total_sound_wave, record_date)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                streamer_name = VALUES(streamer_name),
                sound_wave = VALUES(sound_wave),
                total_sound_wave = VALUES(total_sound_wave)
            """.trimIndent()
            
            val statement = connection.prepareStatement(sql)
            var successCount = 0
            
            dataList.forEach { data ->
                try {
                    // 计算总音浪 = 前一天总音浪 + 当日音浪
                    val previousTotal = previousTotalMap[data.streamerId] ?: 0L
                    val newTotalSoundWave = previousTotal + data.soundWave
                    
                    // 使用user表中的昵称，如果没有则使用CSV中的名字
                    val streamerName = nicknameMap[data.streamerId] ?: data.streamerName
                    
                    statement.setString(1, data.streamerId)
                    statement.setString(2, streamerName)
                    statement.setLong(3, data.soundWave)
                    statement.setLong(4, newTotalSoundWave)
                    statement.setString(5, data.recordDate)
                    statement.addBatch()
                    successCount++
                } catch (e: Exception) {
                    // 记录错误但继续处理其他数据
                }
            }
            
            statement.executeBatch()
            connection.commit()
            statement.close()
            
            Result.success(successCount)
        } catch (e: Exception) {
            connection?.rollback()
            Result.failure(Exception("导入数据失败: ${e.message}", e))
        } finally {
            connection?.autoCommit = true
            connection?.close()
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
     * ResultSet转换为SoundData
     */
    private fun ResultSet.toSoundData(): SoundData {
        return SoundData(
            id = 0L,  // Python表没有自增ID，使用0
            streamerId = getString("streamer_id") ?: "",
            streamerName = getString("streamer_name") ?: "",
            soundWave = getLong("sound_wave") ?: 0L,
            totalSoundWave = getLong("total_sound_wave") ?: 0L,
            effectiveBroadcastDuration = null,  // Python表没有此字段
            recordDate = getString("record_date") ?: "",
            gradeRank = null,  // Python表没有此字段
            gradeLetter = null,  // Python表没有此字段
            overallRank = 0,  // Python表没有此字段，可以根据total_sound_wave排序
            createdAt = getString("created_at") ?: ""
        )
    }
    
    /**
     * 清空指定日期的日音浪字段
     */
    suspend fun clearDailySoundWave(date: String): Result<String> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            connection = getConnection()
            val sql = "UPDATE ${AppConfig.DB_TABLE} SET sound_wave = 0 WHERE record_date = ?"
            val statement = connection.prepareStatement(sql)
            statement.setString(1, date)
            statement.executeUpdate()
            statement.close()
            
            Result.success("清空成功")
        } catch (e: Exception) {
            Result.failure(Exception("清空日音浪失败: ${e.message}", e))
        } finally {
            connection?.close()
        }
    }
    
    /**
     * 删除指定日期的数据
     */
    suspend fun deleteDateData(date: String): Result<String> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            connection = getConnection()
            val sql = "DELETE FROM ${AppConfig.DB_TABLE} WHERE record_date = ?"
            val statement = connection.prepareStatement(sql)
            statement.setString(1, date)
            statement.executeUpdate()
            statement.close()
            
            Result.success("删除成功")
        } catch (e: Exception) {
            Result.failure(Exception("删除数据失败: ${e.message}", e))
        } finally {
            connection?.close()
        }
    }
    
    /**
     * 重新计算指定日期之后所有日期的总音浪
     */
    suspend fun recalculateTotalSoundWave(fromDate: String): Result<String> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            connection = getConnection()
            
            // 获取所有需要重新计算的日期
            val datesSql = "SELECT DISTINCT record_date FROM ${AppConfig.DB_TABLE} WHERE record_date > ? ORDER BY record_date"
            val datesStmt = connection.prepareStatement(datesSql)
            datesStmt.setString(1, fromDate)
            val datesRs = datesStmt.executeQuery()
            
            val dates = mutableListOf<String>()
            while (datesRs.next()) {
                dates.add(datesRs.getString("record_date"))
            }
            datesRs.close()
            datesStmt.close()
            
            // 逐个日期重新计算
            dates.forEach { date ->
                val previousDate = getPreviousDate(date)
                
                // 查询前一天的总音浪
                val previousTotalMap = mutableMapOf<String, Long>()
                try {
                    val querySql = "SELECT streamer_id, total_sound_wave FROM ${AppConfig.DB_TABLE} WHERE record_date = ?"
                    val queryStmt = connection.prepareStatement(querySql)
                    queryStmt.setString(1, previousDate)
                    val rs = queryStmt.executeQuery()
                    while (rs.next()) {
                        previousTotalMap[rs.getString("streamer_id")] = rs.getLong("total_sound_wave")
                    }
                    rs.close()
                    queryStmt.close()
                } catch (e: Exception) {
                    // 忽略错误
                }
                
                // 查询当天的数据并更新总音浪
                val currentDataSql = "SELECT streamer_id, sound_wave FROM ${AppConfig.DB_TABLE} WHERE record_date = ?"
                val currentDataStmt = connection.prepareStatement(currentDataSql)
                currentDataStmt.setString(1, date)
                val currentDataRs = currentDataStmt.executeQuery()
                
                val updateSql = "UPDATE ${AppConfig.DB_TABLE} SET total_sound_wave = ? WHERE streamer_id = ? AND record_date = ?"
                val updateStmt = connection.prepareStatement(updateSql)
                
                while (currentDataRs.next()) {
                    val streamerId = currentDataRs.getString("streamer_id")
                    val soundWave = currentDataRs.getLong("sound_wave")
                    val previousTotal = previousTotalMap[streamerId] ?: 0L
                    val newTotalSoundWave = previousTotal + soundWave
                    
                    updateStmt.setLong(1, newTotalSoundWave)
                    updateStmt.setString(2, streamerId)
                    updateStmt.setString(3, date)
                    updateStmt.addBatch()
                }
                
                updateStmt.executeBatch()
                updateStmt.close()
                currentDataRs.close()
                currentDataStmt.close()
            }
            
            Result.success("重新计算完成")
        } catch (e: Exception) {
            Result.failure(Exception("重新计算失败: ${e.message}", e))
        } finally {
            connection?.close()
        }
    }
    
    /**
     * 获取所有主播列表
     */
    suspend fun fetchAllStreamers(): Result<List<com.example.myapplication.data.model.Streamer>> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            android.util.Log.d(TAG, "开始获取主播列表")
            connection = getConnection()
            
            val sql = """
                SELECT id, serial_number, streamer_id, nickname, full_name, gender, 
                       created_at, updated_at
                FROM user
                ORDER BY gender, serial_number
            """.trimIndent()
            
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(sql)
            
            val streamers = mutableListOf<com.example.myapplication.data.model.Streamer>()
            while (resultSet.next()) {
                streamers.add(com.example.myapplication.data.model.Streamer(
                    id = resultSet.getInt("id"),
                    serialNumber = resultSet.getInt("serial_number"),
                    streamerId = resultSet.getString("streamer_id") ?: "",
                    nickname = resultSet.getString("nickname") ?: "",
                    fullName = resultSet.getString("full_name"),
                    gender = com.example.myapplication.data.model.Gender.fromString(
                        resultSet.getString("gender") ?: "male"
                    ),
                    createdAt = resultSet.getString("created_at") ?: "",
                    updatedAt = resultSet.getString("updated_at") ?: ""
                ))
            }
            
            resultSet.close()
            statement.close()
            
            android.util.Log.d(TAG, "获取主播列表成功，共 ${streamers.size} 个")
            Result.success(streamers)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "获取主播列表失败", e)
            Result.failure(Exception("获取主播列表失败: ${e.message}", e))
        } finally {
            connection?.close()
        }
    }
}
