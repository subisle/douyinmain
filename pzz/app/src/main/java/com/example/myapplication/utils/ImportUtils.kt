package com.example.myapplication.utils

import android.content.Context
import android.net.Uri
import com.example.myapplication.data.model.SoundData
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * CSV导入工具类
 */
object ImportUtils {
    
    /**
     * 从CSV文件导入数据
     * @param context 上下文
     * @param uri 文件URI
     * @param recordDate 记录日期
     * @param remoteLatestTotalSoundWave 从本地数据库查询的最新总音浪数据（用于计算新总音浪）
     * @return 导入的数据列表（只包含CSV中有数据且在主播名单中的主播）
     * 
     * 逻辑说明：
     * 1. 只为CSV中有数据的主播创建记录
     * 2. CSV中不在主播名单的数据不导入
     * 3. 新总音浪 = 前一天的总音浪 + 今天的日音浪
     * 4. 使用ID前8位进行匹配
     */
    fun importFromCsv(
        context: Context, 
        uri: Uri, 
        recordDate: String,
        remoteLatestTotalSoundWave: List<SoundData> = emptyList()
    ): Result<List<SoundData>> {
        return try {
            // 获取主播名单
            val genderUtils = com.example.myapplication.utils.GenderUtils.getInstance(context)
            
            // 检查是否已同步主播名单
            if (genderUtils.getLastSyncTime() == 0L) {
                android.util.Log.w("ImportUtils", "警告：尚未从数据库同步主播名单，使用本地缓存")
            }
            
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
                    android.util.Log.d("ImportUtils", "检测到UTF-8 BOM")
                    Charsets.UTF_8
                }
                bomRead >= 2 && bomBytes[0] == 0xFF.toByte() && bomBytes[1] == 0xFE.toByte() -> {
                    android.util.Log.d("ImportUtils", "检测到UTF-16LE BOM")
                    Charsets.UTF_16LE
                }
                bomRead >= 2 && bomBytes[0] == 0xFE.toByte() && bomBytes[1] == 0xFF.toByte() -> {
                    android.util.Log.d("ImportUtils", "检测到UTF-16BE BOM")
                    Charsets.UTF_16BE
                }
                else -> {
                    android.util.Log.d("ImportUtils", "未检测到BOM，使用UTF-8")
                    Charsets.UTF_8
                }
            }
            
            val reader = BufferedReader(InputStreamReader(bufferedInputStream, charset))
            val csvDataMap = mutableMapOf<String, Pair<String, Long>>()  // ID -> (名字, 音浪)
            var skippedRowsCount = 0  // 跳过的CSV行数量
            
            // 读取标题行
            val headerLine = reader.readLine()
            if (headerLine == null) {
                reader.close()
                bufferedInputStream.close()
                return Result.failure(Exception("文件为空"))
            }
            
            android.util.Log.d("ImportUtils", "原始标题行: $headerLine")
            android.util.Log.d("ImportUtils", "标题行长度: ${headerLine.length}")
            android.util.Log.d("ImportUtils", "标题行字节: ${headerLine.toByteArray().joinToString(" ") { "%02X".format(it) }}")
            
            // 解析标题行，找到各列的索引
            val headers = parseCsvLine(headerLine)
            android.util.Log.d("ImportUtils", "========== CSV表头解析 ==========")
            android.util.Log.d("ImportUtils", "解析后的列数: ${headers.size}")
            headers.forEachIndexed { index, header ->
                android.util.Log.d("ImportUtils", "列[$index]: '$header' (长度:${header.length})")
            }
            
            // 更精确地查找列索引，只需要主播ID和音浪两列
            var idIndex = -1
            var soundWaveIndex = -1
            
            headers.forEachIndexed { index, header ->
                val trimmedHeader = header.trim()
                
                // 查找主播ID列（精确匹配优先）
                if (idIndex == -1) {
                    when {
                        trimmedHeader.equals("主播ID", ignoreCase = true) -> idIndex = index
                        trimmedHeader.equals("抖音号", ignoreCase = true) -> idIndex = index
                        trimmedHeader.contains("主播ID", ignoreCase = true) -> idIndex = index
                    }
                }
                
                // 查找音浪列（精确匹配）
                if (soundWaveIndex == -1) {
                    when {
                        trimmedHeader.equals("音浪", ignoreCase = true) -> soundWaveIndex = index
                        trimmedHeader.equals("声浪", ignoreCase = true) -> soundWaveIndex = index
                    }
                }
            }
            
            android.util.Log.d("ImportUtils", "找到列索引 - 主播ID:$idIndex, 音浪:$soundWaveIndex")
            
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
            
            // 读取CSV数据到Map（只需要ID和音浪）
            var line: String?
            var lineNumber = 1
            var errorCount = 0
            var successCount = 0
            
            android.util.Log.d("ImportUtils", "========== 开始解析CSV数据行 ==========")
            android.util.Log.d("ImportUtils", "列索引 - 主播ID:$idIndex, 音浪:$soundWaveIndex")
            
            while (reader.readLine().also { line = it } != null) {
                lineNumber++
                try {
                    val currentLine = line!!
                    if (currentLine.isBlank()) {
                        continue
                    }
                    
                    val values = parseCsvLine(currentLine)
                    if (values.isEmpty() || values.size <= maxOf(idIndex, soundWaveIndex)) {
                        android.util.Log.w("ImportUtils", "第${lineNumber}行: 列数不足(${values.size}列)，跳过")
                        errorCount++
                        continue
                    }
                    
                    val streamerId = values[idIndex].trim()
                    val soundWaveStr = values[soundWaveIndex].trim()
                        .replace("音浪", "")  // 去掉"音浪"文字
                        .replace(",", "")     // 去掉千位分隔符
                        .trim()
                    val soundWave = soundWaveStr.toLongOrNull() ?: 0L
                    
                    // 跳过无效ID（如"0"或"账号已注销"）
                    if (streamerId.isEmpty() || streamerId == "0" || streamerId.contains("注销", ignoreCase = true)) {
                        errorCount++
                        continue
                    }
                    
                    // 保存到Map（如果音浪<2，设置为0表示未开播）
                    // 名字设为空字符串，后续从数据库获取
                    val finalSoundWave = if (soundWave < 2) 0L else soundWave
                    csvDataMap[streamerId] = Pair("", finalSoundWave)
                    successCount++
                    
                    // 打印前5条数据用于调试
                    if (successCount <= 5) {
                        val prefix = if (streamerId.length >= 8) streamerId.substring(0, 8) else streamerId
                        android.util.Log.d("ImportUtils", "✅ 第${lineNumber}行: 完整ID=$streamerId, ID前8位=$prefix, 原始音浪=$soundWaveStr, 解析音浪=$finalSoundWave")
                    }
                    
                } catch (e: Exception) {
                    android.util.Log.e("ImportUtils", "❌ 第${lineNumber}行解析失败: ${e.message}")
                    errorCount++
                    continue
                }
            }
            
            android.util.Log.d("ImportUtils", "========== CSV数据行解析完成 ==========")
            android.util.Log.d("ImportUtils", "成功解析: $successCount 条, 错误: $errorCount 条")
            android.util.Log.d("ImportUtils", "CSV数据Map大小: ${csvDataMap.size}")
            
            reader.close()
            bufferedInputStream.close()
            
            android.util.Log.d("ImportUtils", "========== 开始匹配主播 ==========")
            
            // 使用已有的genderUtils获取所有主播列表
            val allStreamersFromGender = genderUtils.getAllStreamersWithGender()
            
            // 转换为Map（ID -> 昵称）
            val allStreamers = allStreamersFromGender.associate { it.streamerId to it.nickname }
            
            android.util.Log.d("ImportUtils", "主播名单总数: ${allStreamers.size}")
            android.util.Log.d("ImportUtils", "CSV解析数据: ${csvDataMap.size}条")
            
            // 打印前5个主播用于调试
            allStreamersFromGender.take(5).forEach { streamer ->
                val prefix = if (streamer.streamerId.length >= 8) streamer.streamerId.substring(0, 8) else streamer.streamerId
                android.util.Log.d("ImportUtils", "主播名单: ID=${streamer.streamerId}, 前8位=$prefix, 昵称=${streamer.nickname}")
            }
            
            // 如果主播名单为空，给出警告
            if (allStreamers.isEmpty()) {
                android.util.Log.e("ImportUtils", "❌ 主播名单为空！请先同步主播名单")
                return Result.failure(Exception("主播名单为空，无法匹配。请先从设置中同步主播名单。"))
            }
            
            // 构建最新总音浪映射（ID -> 总音浪）- 直接使用远程数据库查询的最新值
            val latestTotalSoundWaveMap = remoteLatestTotalSoundWave.associate { 
                it.streamerId to it.totalSoundWave 
            }
            android.util.Log.d("ImportUtils", "最新总音浪数据(远程): ${remoteLatestTotalSoundWave.size}条")
            
            // 如果最新总音浪数据为空,记录警告
            if (remoteLatestTotalSoundWave.isEmpty()) {
                android.util.Log.w("ImportUtils", "⚠️ 警告: 最新总音浪数据为空,所有主播的总音浪将等于日音浪")
            }
            
            // 构建CSV数据的ID映射（用于快速查找）
            val csvIdMap = csvDataMap.mapKeys { entry ->
                // 提取前8位作为key
                val csvId = entry.key
                if (csvId.length >= 8) csvId.substring(0, 8) else csvId
            }
            
            android.util.Log.d("ImportUtils", "========== 开始为CSV中的主播构建数据 ==========")
            
            // 构建最终数据列表 - 只为CSV中有数据且在主播名单中的主播创建记录
            val dataList = mutableListOf<SoundData>()
            var importedCount = 0      // 成功导入的数量
            var notInListCount = 0     // 不在主播名单中的数量
            
            // 遍历CSV数据，只处理在主播名单中的主播
            csvDataMap.forEach { (csvId, csvData) ->
                val csvIdPrefix = if (csvId.length >= 8) csvId.substring(0, 8) else csvId
                
                // 查找该CSV ID对应的主播信息
                val streamerInfo = findStreamerByIdPrefix(csvId, "", allStreamers)
                
                if (streamerInfo != null) {
                    // 在主播名单中，导入数据
                    val (dbId, nickname) = streamerInfo
                    val soundWave = csvData.second
                    
                    // 查找该主播在导入日期之前的最新总音浪
                    val latestTotal = findLatestTotalByIdPrefix(dbId, latestTotalSoundWaveMap)
                    
                    // 新总音浪 = 前一天的总音浪 + 今天的日音浪
                    val newTotal = latestTotal + soundWave
                    
                    importedCount++
                    if (importedCount <= 5) {
                        android.util.Log.d("ImportUtils", "✅ 导入: CSV_ID=$csvId -> DB_ID=$dbId, 名称=$nickname, 日音浪=$soundWave, 前总音浪=$latestTotal, 新总音浪=$newTotal")
                    }
                    
                    dataList.add(SoundData(
                        id = 0,
                        streamerId = dbId,
                        streamerName = nickname,
                        soundWave = soundWave,
                        totalSoundWave = newTotal,  // 累加计算
                        effectiveBroadcastDuration = null,
                        recordDate = recordDate,
                        gradeRank = null,
                        gradeLetter = null,
                        overallRank = 0,
                        createdAt = null
                    ))
                } else {
                    // 不在主播名单中，跳过
                    notInListCount++
                    if (notInListCount <= 5) {
                        android.util.Log.d("ImportUtils", "⏭️ 跳过: CSV_ID=$csvId (不在主播名单中)")
                    }
                }
            }
            
            android.util.Log.d("ImportUtils", "========== 数据构建完成 ==========")
            android.util.Log.d("ImportUtils", "成功导入: $importedCount 个, 跳过(不在名单): $notInListCount 个")
            
            // 为未开播的主播创建记录（在主播名单中但CSV中没有的）
            val importedStreamerIds = dataList.map { it.streamerId }.toSet()
            var absentCount = 0
            
            allStreamers.forEach { (streamerId, streamerName) ->
                if (streamerId !in importedStreamerIds) {
                    // 该主播未开播，查找前一天的总音浪
                    val latestTotal = findLatestTotalByIdPrefix(streamerId, latestTotalSoundWaveMap)
                    
                    dataList.add(SoundData(
                        id = 0,
                        streamerId = streamerId,
                        streamerName = streamerName,
                        soundWave = 0,  // 未开播
                        totalSoundWave = latestTotal,  // 保持前一天的总音浪
                        effectiveBroadcastDuration = null,
                        recordDate = recordDate,
                        gradeRank = null,
                        gradeLetter = null,
                        overallRank = 0,
                        createdAt = null
                    ))
                    absentCount++
                }
            }
            
            android.util.Log.d("ImportUtils", "补充未开播主播: $absentCount 个")
            android.util.Log.d("ImportUtils", "最终导入数据: ${dataList.size}条")
            
            Result.success(dataList)
            
        } catch (e: Exception) {
            Result.failure(Exception("导入失败: ${e.message}", e))
        }
    }
    
    /**
     * 根据CSV的ID查找数据库中的主播（支持前8位匹配）
     * @param csvId CSV中的ID（可能较长）
     * @param csvName CSV中的名字（作为备用）
     * @param allStreamers 数据库中的所有主播
     * @return Pair(数据库ID, 昵称)，如果不在主播名单中则返回null
     */
    private fun findStreamerByIdPrefix(csvId: String, csvName: String, allStreamers: Map<String, String>): Pair<String, String>? {
        // 先尝试精确匹配
        val exactMatch = allStreamers[csvId]
        if (exactMatch != null) {
            return Pair(csvId, exactMatch)
        }
        
        // 如果精确匹配失败，使用前8位模糊匹配
        if (csvId.length >= 8) {
            val prefix = csvId.substring(0, 8)
            allStreamers.forEach { (dbId, nickname) ->
                if (dbId.startsWith(prefix) || csvId.startsWith(dbId.take(8))) {
                    android.util.Log.d("ImportUtils", "前8位匹配主播: CSV_ID=$csvId -> DB_ID=$dbId, 昵称=$nickname")
                    return Pair(dbId, nickname)
                }
            }
        }
        
        // 未找到，返回null（不在主播名单中）
        android.util.Log.w("ImportUtils", "未找到匹配的主播: CSV_ID=$csvId，跳过导入")
        return null
    }
    
    /**
     * 根据ID前缀查找音浪数据（支持前8位匹配）
     * @param dbId 数据库中的ID（较短）
     * @param csvDataMap CSV数据Map（ID可能较长）
     * @return 音浪值，未找到返回0
     */
    private fun findSoundWaveByIdPrefix(dbId: String, csvDataMap: Map<String, Pair<String, Long>>): Long {
        // 先尝试精确匹配
        val exactMatch = csvDataMap[dbId]?.second
        if (exactMatch != null) {
            return exactMatch
        }
        
        // 如果精确匹配失败，使用前8位模糊匹配
        if (dbId.length >= 8) {
            val prefix = dbId.substring(0, 8)
            csvDataMap.forEach { (csvId, pair) ->
                if (csvId.startsWith(prefix)) {
                    android.util.Log.d("ImportUtils", "前8位匹配成功: DB_ID=$dbId -> CSV_ID=$csvId, 音浪=${pair.second}")
                    return pair.second
                }
            }
        }
        
        // 未找到，返回0（表示未开播）
        return 0L
    }
    
    /**
     * 根据ID前缀查找最新的总音浪（支持前8位匹配）
     * @param dbId 数据库中的ID（较短）
     * @param latestTotalMap 最新的总音浪Map（ID可能较长）
     * @return 最新的总音浪，未找到返回0
     */
    private fun findLatestTotalByIdPrefix(dbId: String, latestTotalMap: Map<String, Long>): Long {
        // 先尝试精确匹配
        val exactMatch = latestTotalMap[dbId]
        if (exactMatch != null) {
            android.util.Log.d("ImportUtils", "精确匹配最新总音浪: DB_ID=$dbId, 总音浪=$exactMatch")
            return exactMatch
        }
        
        // 如果精确匹配失败，使用前8位模糊匹配
        if (dbId.length >= 8) {
            val prefix = dbId.substring(0, 8)
            latestTotalMap.forEach { (remoteId, total) ->
                val remotePrefix = if (remoteId.length >= 8) remoteId.substring(0, 8) else remoteId
                if (prefix == remotePrefix) {
                    android.util.Log.d("ImportUtils", "前8位匹配最新总音浪: DB_ID=$dbId(前8位=$prefix) -> MAP_ID=$remoteId(前8位=$remotePrefix), 总音浪=$total")
                    return total
                }
            }
        }
        
        // 未找到，记录详细警告
        android.util.Log.w("ImportUtils", "⚠️ 未找到最新总音浪: DB_ID=$dbId")
        android.util.Log.w("ImportUtils", "   可用的Map keys: ${latestTotalMap.keys.take(5).joinToString(", ")}...")
        return 0L
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
                    // 处理引号
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        // 双引号转义
                        currentField.append('"')
                        // 跳过下一个引号（在循环中会自动处理）
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    // 字段分隔符
                    result.add(currentField.toString())
                    currentField.clear()
                }
                else -> {
                    currentField.append(char)
                }
            }
        }
        
        // 添加最后一个字段
        result.add(currentField.toString())
        
        return result
    }
    
    // 保留旧方法名以兼容现有代码
    fun importFromExcel(
        context: Context, 
        uri: Uri, 
        recordDate: String,
        remoteLatestTotalSoundWave: List<SoundData> = emptyList()
    ): Result<List<SoundData>> {
        return importFromCsv(context, uri, recordDate, remoteLatestTotalSoundWave)
    }
}

