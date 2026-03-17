package com.example.myapplication.utils

import android.content.Context
import android.net.Uri
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.model.SoundData
import com.example.myapplication.config.AppConfig
import java.io.BufferedReader
import java.io.InputStreamReader

object TotalSoundUpdateUtils {
    
    private const val TAG = "TotalSoundUpdateUtils"
    
    suspend fun updateFromCsv(
        context: Context,
        uri: Uri,
        targetDate: String
    ): Result<UpdateResult> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val csvData = parseCsvFile(context, uri)
            if (csvData.isEmpty()) {
                return@withContext Result.success(UpdateResult.empty())
            }
            
            android.util.Log.d(TAG, "CSV解析: ${csvData.size}条")
            
            // 只更新本地数据库（移除远程数据库推送）
            val localResult = updateLocalDatabase(context, csvData, targetDate)
            android.util.Log.d(TAG, "本地数据库更新完成: 成功=${localResult.successCount}, 插入=${localResult.insertCount}, 失败=${localResult.failCount}")
            
            Result.success(UpdateResult(
                totalCount = csvData.size,
                successCount = localResult.successCount,
                failCount = localResult.failCount,
                remoteSyncSuccess = 0,  // 不再推送到远程
                remoteInserted = 0,     // 不再推送到远程
                remoteSyncFail = 0,     // 不再推送到远程
                localUpdateSuccess = localResult.successCount,
                localUpdateFail = localResult.failCount
            ))
        } catch (e: Exception) {
            android.util.Log.e(TAG, "更新失败", e)
            Result.failure(Exception("更新失败: ${e.message}"))
        }
    }

    private fun parseCsvFile(context: Context, uri: Uri): Map<String, Long> {
        android.util.Log.d(TAG, "========== 开始解析CSV文件 ==========")
        
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("无法打开文件")
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val result = mutableMapOf<String, Long>()
        
        val headerLine = reader.readLine() ?: throw Exception("文件为空")
        val headers = parseCsvLine(headerLine)
        
        android.util.Log.d(TAG, "CSV表头 (${headers.size}列): ${headers.joinToString(" | ")}")
        
        // 更精确地查找列索引
        var idIndex = -1
        var soundIndex = -1
        
        headers.forEachIndexed { index, header ->
            val trimmedHeader = header.trim()
            android.util.Log.d(TAG, "列[$index]: '$trimmedHeader'")
            
            // 查找主播ID列（第2列）
            if (trimmedHeader.equals("主播ID", ignoreCase = true) || 
                trimmedHeader.contains("主播ID", ignoreCase = true)) {
                idIndex = index
            }
            
            // 查找音浪列（第6列）
            if (trimmedHeader.equals("音浪", ignoreCase = true)) {
                soundIndex = index
            }
        }
        
        android.util.Log.d(TAG, "找到列索引 - 主播ID: $idIndex, 音浪: $soundIndex")
        
        if (idIndex == -1 || soundIndex == -1) {
            throw Exception("CSV格式错误: 找不到主播ID列或音浪列\n表头: ${headers.joinToString(", ")}")
        }
        
        var lineNumber = 1
        var successCount = 0
        var skipCount = 0
        var line: String?
        
        while (reader.readLine().also { line = it } != null) {
            lineNumber++
            try {
                val currentLine = line!!
                if (currentLine.isBlank()) {
                    skipCount++
                    continue
                }
                
                val values = parseCsvLine(currentLine)
                if (values.size <= maxOf(idIndex, soundIndex)) {
                    android.util.Log.w(TAG, "第${lineNumber}行: 列数不足(${values.size}列)，跳过")
                    skipCount++
                    continue
                }
                
                val id = values[idIndex].trim()
                val soundStr = values[soundIndex].trim()
                
                // 跳过无效ID（如"0"或"账号已注销"）
                if (id.isEmpty() || id == "0" || id.contains("注销", ignoreCase = true)) {
                    skipCount++
                    continue
                }
                
                // 解析音浪值
                val sound = soundStr
                    .replace("音浪", "")
                    .replace("万", "0000")
                    .replace(",", "")
                    .replace(".", "")
                    .trim()
                    .toLongOrNull() ?: 0L
                
                // 只保存有音浪的数据（音浪>0）
                if (sound > 0) {
                    // 使用ID前8位作为key
                    val prefix = if (id.length >= 8) id.substring(0, 8) else id
                    result[prefix] = sound
                    successCount++
                    
                    // 打印前5条数据用于调试
                    if (successCount <= 5) {
                        android.util.Log.d(TAG, "✅ 第${lineNumber}行: 完整ID=$id, ID前8位=$prefix, 原始音浪=$soundStr, 解析音浪=$sound")
                    }
                } else {
                    skipCount++
                    if (lineNumber <= 10) {
                        android.util.Log.d(TAG, "⏭️ 第${lineNumber}行: 音浪为0，跳过 (ID=$id, 原始值: $soundStr)")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ 第${lineNumber}行解析失败: ${e.message}")
                skipCount++
            }
        }
        
        reader.close()
        
        android.util.Log.d(TAG, "========== CSV解析完成 ==========")
        android.util.Log.d(TAG, "成功解析: $successCount 条, 跳过: $skipCount 条")
        android.util.Log.d(TAG, "有效数据（音浪>0）: ${result.size} 条")
        
        return result
    }

    private suspend fun updateRemoteDatabase(csvData: Map<String, Long>, targetDate: String): DbResult {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            var successCount = 0
            var insertCount = 0
            var failCount = 0
            
            val client = com.example.myapplication.data.remote.SimpleMySQLClient(
                host = AppConfig.DB_HOST,
                port = AppConfig.DB_PORT,
                user = AppConfig.DB_USER,
                password = AppConfig.DB_PASSWORD,
                database = AppConfig.DB_NAME
            )
            
            if (!client.connect()) {
                android.util.Log.e(TAG, "❌ 远程数据库连接失败")
                return@withContext DbResult(0, 0, csvData.size)
            }
            
            android.util.Log.d(TAG, "========== 开始更新远程数据库 ==========")
            android.util.Log.d(TAG, "目标日期: $targetDate, CSV数据: ${csvData.size}条")
            
            try {
                for ((idPrefix, totalSound) in csvData) {
                    try {
                        // 根据ID前8位查询主播信息
                        val streamerInfo = fetchStreamerInfo(client, idPrefix)
                        
                        if (streamerInfo == null) {
                            android.util.Log.w(TAG, "⚠️ 未找到主播: ID前8位=$idPrefix")
                            failCount++
                            continue
                        }
                        
                        val (streamerId, streamerName) = streamerInfo
                        
                        // 检查该日期是否已有记录
                        val exists = checkRecordExists(client, streamerId, targetDate)
                        
                        if (exists) {
                            // 更新总音浪
                            val updateSql = """
                                UPDATE ${AppConfig.DB_TABLE}
                                SET total_sound_wave = $totalSound
                                WHERE streamer_id = '$streamerId' AND record_date = '$targetDate'
                            """.trimIndent()
                            
                            val affected = client.execute(updateSql)
                            if (affected > 0) {
                                android.util.Log.d(TAG, "✅ 更新: ID=$streamerId, 名称=$streamerName, 总音浪=$totalSound")
                                successCount++
                            } else {
                                android.util.Log.w(TAG, "⚠️ 更新失败: ID=$streamerId")
                                failCount++
                            }
                        } else {
                            // 插入新记录
                            val insertSql = """
                                INSERT INTO ${AppConfig.DB_TABLE} 
                                (streamer_id, streamer_name, sound_wave, total_sound_wave, record_date)
                                VALUES ('$streamerId', '$streamerName', 0, $totalSound, '$targetDate')
                            """.trimIndent()
                            
                            val affected = client.execute(insertSql)
                            if (affected > 0) {
                                android.util.Log.d(TAG, "✅ 插入: ID=$streamerId, 名称=$streamerName, 总音浪=$totalSound")
                                insertCount++
                            } else {
                                android.util.Log.w(TAG, "⚠️ 插入失败: ID=$streamerId")
                                failCount++
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "❌ 处理失败: ID前8位=$idPrefix, 错误: ${e.message}")
                        failCount++
                    }
                }
            } finally {
                client.close()
            }
            
            android.util.Log.d(TAG, "========== 远程更新完成 ==========")
            android.util.Log.d(TAG, "更新: $successCount, 插入: $insertCount, 失败: $failCount")
            
            DbResult(successCount, insertCount, failCount)
        }
    }

    private suspend fun updateLocalDatabase(context: Context, csvData: Map<String, Long>, targetDate: String): DbResult {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            var successCount = 0
            var insertCount = 0
            var failCount = 0
            
            val db = AppDatabase.getDatabase(context)
            val dao = db.soundDataDao()
            
            android.util.Log.d(TAG, "========== 开始更新本地数据库 ==========")
            android.util.Log.d(TAG, "目标日期: $targetDate")
            android.util.Log.d(TAG, "CSV数据: ${csvData.size}条")
            
            // 打印前5条CSV数据用于调试
            csvData.entries.take(5).forEach { (prefix, total) ->
                android.util.Log.d(TAG, "CSV数据: ID前8位=$prefix, 总音浪=$total")
            }
            
            // 查询该日期所有现有数据，建立ID前8位映射
            val existingData = dao.getDataByDateSync(targetDate)
            val existingMap = mutableMapOf<String, SoundData>()
            existingData.forEach { data ->
                val prefix = if (data.streamerId.length >= 8) {
                    data.streamerId.substring(0, 8)
                } else {
                    data.streamerId
                }
                existingMap[prefix] = data
            }
            android.util.Log.d(TAG, "该日期现有数据: ${existingData.size}条")
            
            for ((idPrefix, totalSound) in csvData) {
                try {
                    // 使用ID前8位查找现有记录
                    val existing = existingMap[idPrefix]
                    
                    if (existing != null) {
                        // 更新现有记录的总音浪
                        android.util.Log.d(TAG, "✅ 更新: ID=${existing.streamerId}(前8位=$idPrefix), 名称=${existing.streamerName}, 总音浪 ${existing.totalSoundWave} -> $totalSound")
                        
                        val updated = existing.copy(totalSoundWave = totalSound)
                        dao.update(updated)
                        successCount++
                    } else {
                        // 未找到该日期的记录，查询主播信息
                        val streamerInfo = dao.getStreamerInfoByPrefix(idPrefix)
                        
                        if (streamerInfo != null) {
                            android.util.Log.d(TAG, "✅ 插入: ID=${streamerInfo.streamerId}(前8位=$idPrefix), 名称=${streamerInfo.streamerName}, 总音浪=$totalSound")
                            
                            val newData = SoundData(
                                id = 0,
                                streamerId = streamerInfo.streamerId,
                                streamerName = streamerInfo.streamerName,
                                soundWave = 0L,
                                totalSoundWave = totalSound,
                                recordDate = targetDate,
                                effectiveBroadcastDuration = null,
                                gradeRank = null,
                                gradeLetter = null,
                                overallRank = 0,
                                createdAt = null
                            )
                            dao.insert(newData)
                            insertCount++
                        } else {
                            android.util.Log.w(TAG, "⚠️ 跳过: 未找到主播信息 (ID前8位=$idPrefix)")
                            failCount++
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "❌ 处理失败: ID前8位=$idPrefix, 错误: ${e.message}")
                    failCount++
                }
            }
            
            android.util.Log.d(TAG, "========== 本地更新完成 ==========")
            android.util.Log.d(TAG, "更新: $successCount, 插入: $insertCount, 失败: $failCount")
            
            // 验证结果
            try {
                val verifyCount = dao.getCountByDate(targetDate)
                android.util.Log.d(TAG, "验证: 该日期共有 $verifyCount 条记录")
                
                // 查询前3条记录验证
                val sampleData = dao.getDataByDateSync(targetDate).take(3)
                sampleData.forEach { data ->
                    val prefix = if (data.streamerId.length >= 8) data.streamerId.substring(0, 8) else data.streamerId
                    android.util.Log.d(TAG, "验证: ID=${data.streamerId}(前8位=$prefix), 名称=${data.streamerName}, 总音浪=${data.totalSoundWave}")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "验证失败: ${e.message}")
            }
            
            DbResult(successCount, insertCount, failCount)
        }
    }

    private suspend fun fetchStreamerInfo(client: com.example.myapplication.data.remote.SimpleMySQLClient, idPrefix: String): Pair<String, String>? {
        android.util.Log.d(TAG, "查询主播信息: ID前8位=$idPrefix")
        
        val sql = """
            SELECT streamer_id, streamer_name
            FROM ${AppConfig.DB_TABLE}
            WHERE SUBSTRING(streamer_id, 1, 8) = '$idPrefix'
            ORDER BY record_date DESC
            LIMIT 1
        """.trimIndent()
        
        val results = client.query(sql)
        if (results.isEmpty()) {
            android.util.Log.w(TAG, "未找到主播: ID前8位=$idPrefix")
            return null
        }
        
        val row = results[0]
        val id = row["streamer_id"]?.toString() ?: return null
        val name = row["streamer_name"]?.toString() ?: ""
        
        android.util.Log.d(TAG, "找到主播: ID=$id, 名称=$name")
        return Pair(id, name)
    }
    
    private suspend fun checkRecordExists(client: com.example.myapplication.data.remote.SimpleMySQLClient, streamerId: String, date: String): Boolean {
        val sql = """
            SELECT COUNT(*) as cnt
            FROM ${AppConfig.DB_TABLE}
            WHERE streamer_id = '$streamerId' AND record_date = '$date'
        """.trimIndent()
        
        val results = client.query(sql)
        if (results.isEmpty()) return false
        
        val count = results[0]["cnt"]?.toString()?.toIntOrNull() ?: 0
        return count > 0
    }
    
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        
        return result
    }
    
    data class DbResult(
        val successCount: Int,
        val insertCount: Int,
        val failCount: Int
    )
    
    data class UpdateResult(
        val totalCount: Int,
        val successCount: Int,
        val failCount: Int,
        val remoteSyncSuccess: Int,
        val remoteInserted: Int,
        val remoteSyncFail: Int,
        val localUpdateSuccess: Int,
        val localUpdateFail: Int
    ) {
        companion object {
            fun empty() = UpdateResult(
                totalCount = 0,
                successCount = 0,
                failCount = 0,
                remoteSyncSuccess = 0,
                remoteInserted = 0,
                remoteSyncFail = 0,
                localUpdateSuccess = 0,
                localUpdateFail = 0
            )
        }
    }
}
