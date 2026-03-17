package com.example.myapplication.utils

import android.content.Context
import android.net.Uri
import com.example.myapplication.PzzApplication
import com.example.myapplication.config.AppConfig
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.model.SoundData
import com.example.myapplication.data.remote.RemoteDatabaseManagerV2
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 总音浪覆盖工具类
 * 用于从主播榜CSV文件覆盖指定日期的总音浪数据
 */
object TotalSoundOverwriteUtils {
    
    /**
     * 覆盖结果
     */
    data class OverwriteResult(
        val totalCount: Int,           // CSV中的总数据量
        val matchedCount: Int,         // 匹配到的主播数量
        val localUpdatedCount: Int,    // 本地更新成功的数量
        val remoteUpdatedCount: Int,   // 远程更新成功的数量
        val failedCount: Int,          // 失败的数量
        val notFoundCount: Int         // 在数据库中未找到的数量
    )
    
    /**
     * 从CSV文件覆盖总音浪
     * @param context 上下文
     * @param uri CSV文件URI
     * @param targetDate 目标日期（要覆盖哪一天的数据）
     * @return 覆盖结果
     */
    suspend fun overwriteTotalSoundWave(
        context: Context,
        uri: Uri,
        targetDate: String
    ): Result<OverwriteResult> {
        return try {
            android.util.Log.d("TotalSoundOverwrite", "========== 开始覆盖总音浪 ==========")
            android.util.Log.d("TotalSoundOverwrite", "目标日期: $targetDate")
            
            // 1. 解析CSV文件
            val csvData = parseCsvFile(context, uri)
            android.util.Log.d("TotalSoundOverwrite", "CSV数据: ${csvData.size}条")
            
            if (csvData.isEmpty()) {
                return Result.failure(Exception("CSV文件为空或格式不正确"))
            }
            
            // 2. 获取数据库实例
            val database = AppDatabase.getDatabase(context)
            val dao = database.soundDataDao()
            
            // 3. 匹配并更新本地数据库
            var localUpdatedCount = 0
            var notFoundCount = 0
            val remoteUpdateList = mutableListOf<Pair<String, Long>>() // (streamerId, totalSoundWave)
            
            csvData.forEach { (csvId, totalSoundWave) ->
                // 使用ID前10位匹配
                val idPrefix = if (csvId.length >= 10) csvId.substring(0, 10) else csvId
                
                // 更新本地数据库
                val updated = dao.updateTotalSoundWaveByIdPrefix(idPrefix, totalSoundWave, targetDate)
                
                if (updated > 0) {
                    localUpdatedCount += updated
                    
                    // 查询完整的streamerId用于远程更新
                    val streamerInfo = dao.getStreamerInfoByPrefix(idPrefix)
                    if (streamerInfo != null) {
                        remoteUpdateList.add(Pair(streamerInfo.streamerId, totalSoundWave))
                    }
                    
                    if (localUpdatedCount <= 5) {
                        android.util.Log.d("TotalSoundOverwrite", "✅ 本地更新: ID前缀=$idPrefix, 总音浪=$totalSoundWave, 影响行数=$updated")
                    }
                } else {
                    notFoundCount++
                    if (notFoundCount <= 5) {
                        android.util.Log.w("TotalSoundOverwrite", "⚠️ 未找到: ID前缀=$idPrefix")
                    }
                }
            }
            
            android.util.Log.d("TotalSoundOverwrite", "本地更新完成: 成功=$localUpdatedCount, 未找到=$notFoundCount")
            
            // 4. 同步到远程数据库
            var remoteUpdatedCount = 0
            if (AppConfig.ENABLE_REMOTE_DB && remoteUpdateList.isNotEmpty()) {
                android.util.Log.d("TotalSoundOverwrite", "开始同步到远程数据库: ${remoteUpdateList.size}条")
                
                remoteUpdateList.forEach { (streamerId, totalSoundWave) ->
                    try {
                        val result = RemoteDatabaseManagerV2.updateTotalSoundWave(
                            streamerId = streamerId,
                            totalSoundWave = totalSoundWave,
                            recordDate = targetDate
                        )
                        
                        if (result.isSuccess) {
                            remoteUpdatedCount++
                            if (remoteUpdatedCount <= 5) {
                                android.util.Log.d("TotalSoundOverwrite", "✅ 远程更新: ID=$streamerId, 总音浪=$totalSoundWave")
                            }
                        } else {
                            android.util.Log.w("TotalSoundOverwrite", "❌ 远程更新失败: ID=$streamerId, 错误=${result.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TotalSoundOverwrite", "远程更新异常: ID=$streamerId", e)
                    }
                }
                
                android.util.Log.d("TotalSoundOverwrite", "远程更新完成: 成功=$remoteUpdatedCount/${remoteUpdateList.size}")
            }
            
            val result = OverwriteResult(
                totalCount = csvData.size,
                matchedCount = localUpdatedCount + notFoundCount,
                localUpdatedCount = localUpdatedCount,
                remoteUpdatedCount = remoteUpdatedCount,
                failedCount = csvData.size - localUpdatedCount - notFoundCount,
                notFoundCount = notFoundCount
            )
            
            android.util.Log.d("TotalSoundOverwrite", "========== 覆盖完成 ==========")
            android.util.Log.d("TotalSoundOverwrite", "结果: $result")
            
            Result.success(result)
            
        } catch (e: Exception) {
            android.util.Log.e("TotalSoundOverwrite", "覆盖失败", e)
            Result.failure(Exception("覆盖总音浪失败: ${e.message}", e))
        }
    }
    
    /**
     * 解析CSV文件
     * @return Map<主播ID, 总音浪>
     */
    private fun parseCsvFile(context: Context, uri: Uri): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return emptyMap()
            
            val bufferedInputStream = if (inputStream.markSupported()) {
                inputStream
            } else {
                java.io.BufferedInputStream(inputStream)
            }
            
            // 检测BOM
            val bomBytes = ByteArray(3)
            bufferedInputStream.mark(3)
            val bomRead = bufferedInputStream.read(bomBytes)
            bufferedInputStream.reset()
            
            val charset = when {
                bomRead >= 3 && bomBytes[0] == 0xEF.toByte() && 
                    bomBytes[1] == 0xBB.toByte() && bomBytes[2] == 0xBF.toByte() -> {
                    Charsets.UTF_8
                }
                else -> Charsets.UTF_8
            }
            
            val reader = BufferedReader(InputStreamReader(bufferedInputStream, charset))
            
            // 读取标题行
            val headerLine = reader.readLine() ?: return emptyMap()
            val headers = parseCsvLine(headerLine)
            
            // 查找列索引
            var idIndex = -1
            var soundWaveIndex = -1
            
            headers.forEachIndexed { index, header ->
                val trimmedHeader = header.trim()
                when {
                    idIndex == -1 && (trimmedHeader.equals("主播ID", ignoreCase = true) || 
                        trimmedHeader.equals("抖音号", ignoreCase = true)) -> {
                        idIndex = index
                    }
                    soundWaveIndex == -1 && (trimmedHeader.equals("音浪", ignoreCase = true) || 
                        trimmedHeader.equals("声浪", ignoreCase = true)) -> {
                        soundWaveIndex = index
                    }
                }
            }
            
            if (idIndex == -1 || soundWaveIndex == -1) {
                android.util.Log.e("TotalSoundOverwrite", "CSV格式不正确，缺少必要的列")
                return emptyMap()
            }
            
            // 读取数据行
            var line: String?
            var lineNumber = 1
            
            while (reader.readLine().also { line = it } != null) {
                lineNumber++
                try {
                    val currentLine = line!!
                    if (currentLine.isBlank()) continue
                    
                    val values = parseCsvLine(currentLine)
                    if (values.size <= maxOf(idIndex, soundWaveIndex)) continue
                    
                    val streamerId = values[idIndex].trim()
                    val soundWaveStr = values[soundWaveIndex].trim()
                        .replace("音浪", "")
                        .replace(",", "")
                        .trim()
                    
                    // 跳过无效ID
                    if (streamerId.isEmpty() || streamerId == "0" || 
                        streamerId.contains("注销", ignoreCase = true)) {
                        continue
                    }
                    
                    val soundWave = soundWaveStr.toLongOrNull() ?: 0L
                    result[streamerId] = soundWave
                    
                } catch (e: Exception) {
                    android.util.Log.w("TotalSoundOverwrite", "第${lineNumber}行解析失败: ${e.message}")
                }
            }
            
            reader.close()
            bufferedInputStream.close()
            
        } catch (e: Exception) {
            android.util.Log.e("TotalSoundOverwrite", "解析CSV文件失败", e)
        }
        
        return result
    }
    
    /**
     * 解析CSV行
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
}
