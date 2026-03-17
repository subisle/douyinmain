package com.example.myapplication.utils

import android.content.Context
import android.net.Uri
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.config.AppConfig
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 日音浪更新工具类
 * 从CSV文件(主播榜格式)导入并更新主播的日音浪数据
 * 用于数据校准，不累计到总音浪
 */
object DailySoundUpdateUtils {
    
    /**
     * 从CSV文件更新日音浪
     * @param context 上下文
     * @param uri 文件URI
     * @param targetDate 目标日期
     * @return 更新结果
     */
    fun updateFromCsv(
        context: Context,
        uri: Uri,
        targetDate: String
    ): Result<UpdateResult> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return Result.failure(Exception("无法打开文件"))
            }
            
            // 使用BufferedInputStream来支持mark/reset
            val bufferedInputStream = if (inputStream.markSupported()) {
                inputStream
            } else {
                java.io.BufferedInputStream(inputStream)
            }
            
            // 尝试检测BOM并选择正确的编码
            val bomBytes = ByteArray(3)
            bufferedInputStream.mark(3)
            val bomRead = bufferedInputStream.read(bomBytes)
            bufferedInputStream.reset()
            
            val charset = when {
                bomRead >= 3 && bomBytes[0] == 0xEF.toByte() && bomBytes[1] == 0xBB.toByte() && bomBytes[2] == 0xBF.toByte() -> {
                    android.util.Log.d("DailySoundUpdateUtils", "检测到UTF-8 BOM")
                    Charsets.UTF_8
                }
                bomRead >= 2 && bomBytes[0] == 0xFF.toByte() && bomBytes[1] == 0xFE.toByte() -> {
                    android.util.Log.d("DailySoundUpdateUtils", "检测到UTF-16LE BOM")
                    Charsets.UTF_16LE
                }
                bomRead >= 2 && bomBytes[0] == 0xFE.toByte() && bomBytes[1] == 0xFF.toByte() -> {
                    android.util.Log.d("DailySoundUpdateUtils", "检测到UTF-16BE BOM")
                    Charsets.UTF_16BE
                }
                else -> {
                    android.util.Log.d("DailySoundUpdateUtils", "未检测到BOM，使用UTF-8")
                    Charsets.UTF_8
                }
            }
            
            val reader = BufferedReader(InputStreamReader(bufferedInputStream, charset))
            val updateMap = mutableMapOf<String, Long>()  // ID前10位 -> 日音浪
            
            // 读取标题行
            val headerLine = reader.readLine()
            if (headerLine == null) {
                reader.close()
                bufferedInputStream.close()
                return Result.failure(Exception("文件为空"))
            }
            
            android.util.Log.d("DailySoundUpdateUtils", "标题行: $headerLine")
            
            // 解析标题行，找到各列的索引
            val headers = parseCsvLine(headerLine)
            android.util.Log.d("DailySoundUpdateUtils", "解析后的列数: ${headers.size}")
            
            val idIndex = headers.indexOfFirst { 
                it.contains("主播ID", ignoreCase = true) || 
                it.contains("ID", ignoreCase = true) ||
                it.contains("抖音号", ignoreCase = true)
            }
            val soundWaveIndex = headers.indexOfFirst { 
                it.contains("音浪", ignoreCase = true) ||
                it.contains("声浪", ignoreCase = true)
            }
            
            android.util.Log.d("DailySoundUpdateUtils", "列索引 - ID:$idIndex, 音浪:$soundWaveIndex")
            
            if (idIndex == -1 || soundWaveIndex == -1) {
                reader.close()
                bufferedInputStream.close()
                val missingColumns = mutableListOf<String>()
                if (idIndex == -1) missingColumns.add("主播ID/抖音号")
                if (soundWaveIndex == -1) missingColumns.add("音浪/声浪")
                
                return Result.failure(Exception(
                    "CSV格式不正确，缺少必要的列：${missingColumns.joinToString("、")}\n" +
                    "找到的列: ${headers.joinToString(", ")}\n" +
                    "请确保CSV文件包含：主播ID、音浪这两列"
                ))
            }
            
            // 读取CSV数据
            var line: String?
            var lineNumber = 1
            var errorCount = 0
            
            while (reader.readLine().also { line = it } != null) {
                lineNumber++
                try {
                    val currentLine = line!!
                    if (currentLine.isBlank()) continue
                    
                    val values = parseCsvLine(currentLine)
                    if (values.isEmpty() || values.size <= maxOf(idIndex, soundWaveIndex)) {
                        errorCount++
                        continue
                    }
                    
                    val streamerId = values[idIndex].trim()
                    val soundWaveStr = values[soundWaveIndex].trim()
                        .replace("音浪", "")
                        .replace("万", "0000")
                        .replace(",", "")
                        .replace(".", "")
                        .trim()
                    
                    val soundWave = soundWaveStr.toLongOrNull() ?: 0L
                    
                    if (streamerId.isEmpty()) {
                        errorCount++
                        continue
                    }
                    
                    // 使用ID前8位作为key
                    val idPrefix = if (streamerId.length >= 8) {
                        streamerId.substring(0, 8)
                    } else {
                        streamerId
                    }
                    
                    updateMap[idPrefix] = soundWave
                    
                } catch (e: Exception) {
                    android.util.Log.w("DailySoundUpdateUtils", "解析第${lineNumber}行失败: ${e.message}")
                    errorCount++
                    continue
                }
            }
            
            reader.close()
            bufferedInputStream.close()
            
            android.util.Log.d("DailySoundUpdateUtils", "CSV解析完成，共${updateMap.size}条数据，错误${errorCount}条")
            
            // 只更新本地数据库（移除远程数据库推送）
            var localUpdateSuccess = 0
            var localUpdateFail = 0
            
            if (updateMap.isNotEmpty()) {
                android.util.Log.d("DailySoundUpdateUtils", "开始更新本地数据库，共${updateMap.size}条")
                
                kotlinx.coroutines.runBlocking {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val dao = db.soundDataDao()
                        
                        updateMap.forEach { (idPrefix, dailySoundWave) ->
                            try {
                                android.util.Log.d("DailySoundUpdateUtils", "开始本地更新: ID前缀=$idPrefix, 日音浪=$dailySoundWave")
                                
                                // 查询该日期该主播的记录
                                val existingData = dao.getDataByDateSync(targetDate)
                                    .find { data ->
                                        val prefix = if (data.streamerId.length >= 8) {
                                            data.streamerId.substring(0, 8)
                                        } else {
                                            data.streamerId
                                        }
                                        prefix == idPrefix
                                    }
                                
                                if (existingData != null) {
                                    // 更新日音浪，不重新计算总音浪
                                    val updated = existingData.copy(soundWave = dailySoundWave)
                                    dao.update(updated)
                                    localUpdateSuccess++
                                    android.util.Log.d("DailySoundUpdateUtils", "✅ 本地更新成功: ID=${existingData.streamerId}, 日音浪=$dailySoundWave")
                                } else {
                                    localUpdateFail++
                                    android.util.Log.w("DailySoundUpdateUtils", "⚠️ 未找到记录: ID前缀=$idPrefix, 日期=$targetDate")
                                }
                            } catch (e: Exception) {
                                localUpdateFail++
                                android.util.Log.e("DailySoundUpdateUtils", "本地更新异常: ID前缀=$idPrefix", e)
                            }
                        }
                        
                        android.util.Log.d("DailySoundUpdateUtils", "本地更新完成: 成功${localUpdateSuccess}条, 失败${localUpdateFail}条")
                    } catch (e: Exception) {
                        android.util.Log.e("DailySoundUpdateUtils", "本地更新过程出错", e)
                        return@runBlocking Result.failure<UpdateResult>(Exception("本地更新失败: ${e.message}"))
                    }
                }
            }
            
            android.util.Log.d("DailySoundUpdateUtils", "更新完成: 本地更新${localUpdateSuccess}条, 失败${localUpdateFail}条")
            
            Result.success(UpdateResult(
                totalCount = updateMap.size,
                successCount = localUpdateSuccess,
                localInserted = 0,
                failCount = localUpdateFail,
                remoteSyncSuccess = 0,  // 不再推送到远程
                remoteInserted = 0,     // 不再推送到远程
                remoteSyncFail = 0      // 不再推送到远程
            ))
            
        } catch (e: Exception) {
            android.util.Log.e("DailySoundUpdateUtils", "更新日音浪失败", e)
            Result.failure(Exception("更新失败: ${e.message}", e))
        }
    }
    
    /**
     * 解析CSV行，处理引号包裹的字段
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        
        for (i in line.indices) {
            val char = line[i]
            
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        currentField.append('"')
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    result.add(currentField.toString())
                    currentField.clear()
                }
                else -> {
                    currentField.append(char)
                }
            }
        }
        
        result.add(currentField.toString())
        return result
    }
    
    /**
     * 更新结果
     */
    data class UpdateResult(
        val totalCount: Int,
        val successCount: Int,
        val localInserted: Int = 0,
        val failCount: Int,
        val remoteSyncSuccess: Int = 0,
        val remoteInserted: Int = 0,
        val remoteSyncFail: Int = 0,
        val syncDetails: List<SyncDetail> = emptyList()  // 详细同步信息
    )
    
    /**
     * 同步详情
     */
    data class SyncDetail(
        val streamerId: String,
        val streamerName: String,
        val soundWave: Long,
        val localSuccess: Boolean,
        val remoteSuccess: Boolean,
        val errorMessage: String = ""
    )
}
