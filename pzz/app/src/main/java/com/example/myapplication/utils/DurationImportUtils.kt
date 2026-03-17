package com.example.myapplication.utils

import android.content.Context
import android.net.Uri
import com.example.myapplication.data.model.DurationData
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

/**
 * 直播有效时长导入工具类
 */
object DurationImportUtils {
    
    /**
     * 从CSV文件导入直播有效时长数据（不过滤，读取所有数据）
     * @param context 上下文
     * @param uri CSV文件URI
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return Result<List<DurationData>>
     */
    fun importFromCsv(
        context: Context,
        uri: Uri,
        startDate: String,
        endDate: String
    ): Result<List<DurationData>> {
        return try {
            val dataList = mutableListOf<DurationData>()
            val genderUtils = GenderUtils.getInstance(context)
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())
            
            var totalLines = 0
            var matchedCount = 0
            var unmatchedCount = 0
            var durationColumnIndex = -1
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    // 读取标题行，找到"开播有效时长"列的索引
                    val headerLine = reader.readLine()
                    if (headerLine != null) {
                        val headers = headerLine.split(",")
                        durationColumnIndex = headers.indexOfFirst { header ->
                            val trimmed = header.trim()
                            // 精确匹配"开播有效时长"，不匹配包含其他字符的列名
                            trimmed == "开播有效时长" || trimmed.matches(Regex("^开播有效时长$"))
                        }
                        
                        if (durationColumnIndex == -1) {
                            // 如果精确匹配失败，尝试查找包含"开播有效时长"但不包含其他字的列
                            durationColumnIndex = headers.indexOfFirst { header ->
                                val trimmed = header.trim()
                                trimmed.contains("开播有效时长") && 
                                !trimmed.contains("抖音") && 
                                !trimmed.contains("火山") &&
                                trimmed.length < 10  // 列名长度不应该太长
                            }
                        }
                        
                        if (durationColumnIndex == -1) {
                            android.util.Log.w("DurationImportUtils", "未找到'开播有效时长'列，将尝试自动检测")
                            android.util.Log.d("DurationImportUtils", "可用列名: ${headers.joinToString(", ")}")
                        } else {
                            android.util.Log.d("DurationImportUtils", "找到'开播有效时长'列: [${headers[durationColumnIndex]}], 索引: $durationColumnIndex")
                        }
                    }
                    
                    var line: String?
                    var lineNumber = 1
                    while (reader.readLine().also { line = it } != null) {
                        line?.let { csvLine ->
                            totalLines++
                            lineNumber++
                            try {
                                val data = parseCsvLine(csvLine, startDate, endDate, currentTime, genderUtils, durationColumnIndex, lineNumber)
                                if (data != null) {
                                    dataList.add(data)
                                    if (data.gender.isNotEmpty()) {
                                        matchedCount++
                                    } else {
                                        unmatchedCount++
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("DurationImportUtils", "解析第${lineNumber}行失败: $csvLine", e)
                            }
                        }
                    }
                }
            }
            
            android.util.Log.d("DurationImportUtils", "读取完成: 总行数=$totalLines, 有效数据=${dataList.size}, 匹配=$matchedCount, 未匹配=$unmatchedCount")
            
            if (dataList.isEmpty()) {
                Result.failure(Exception("未找到有效数据"))
            } else {
                // 按时长从多到少排序
                val sortedList = dataList.sortedByDescending { parseDurationToSeconds(it.duration) }
                android.util.Log.d("DurationImportUtils", "排序完成，返回${sortedList.size}条数据")
                Result.success(sortedList)
            }
        } catch (e: Exception) {
            android.util.Log.e("DurationImportUtils", "导入失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 解析CSV行数据（不过滤，读取所有数据）
     */
    private fun parseCsvLine(
        line: String,
        startDate: String,
        endDate: String,
        currentTime: String,
        genderUtils: GenderUtils,
        durationColumnIndex: Int,
        lineNumber: Int
    ): DurationData? {
        val fields = line.split(",")
        
        android.util.Log.d("DurationImportUtils", "第${lineNumber}行: 字段数=${fields.size}, 时长列索引=$durationColumnIndex")
        
        if (fields.isEmpty()) {
            return null
        }
        
        // 清理主播ID（移除可能的行号前缀，如 "26:1110932522276664" -> "1110932522276664"）
        var streamerId = fields[0].trim()
        if (streamerId.contains(":")) {
            streamerId = streamerId.substringAfter(":")
        }
        
        if (streamerId.isEmpty() || streamerId == "0" || streamerId == "账号已注销" || streamerId == "主播ID") {
            return null
        }
        
        // 获取性别（可能为空，表示不在人员表中）
        val gender = genderUtils.getGender(streamerId)
        
        // 获取主播昵称（优先使用数据库中的名称）
        val csvName = if (fields.size > 1) fields[1].trim() else ""
        val streamerName = genderUtils.getNickname(streamerId) ?: csvName
        
        // 获取开播有效时长
        var rawDuration = ""
        if (durationColumnIndex >= 0 && fields.size > durationColumnIndex) {
            rawDuration = fields[durationColumnIndex].trim()
        } else {
            // 如果没有找到列索引，尝试在常见位置查找（索引17或19）
            for (index in listOf(17, 19)) {
                if (fields.size > index) {
                    val value = fields[index].trim()
                    if (value.contains("小时") && value.contains("分钟")) {
                        rawDuration = value
                        android.util.Log.d("DurationImportUtils", "在索引${index}找到时长数据: $rawDuration")
                        break
                    }
                }
            }
        }
        
        android.util.Log.d("DurationImportUtils", "第${lineNumber}行原始时长: [$rawDuration]")
        
        // 转换为简短格式（去掉秒）
        val duration = formatDurationShort(rawDuration)
        
        android.util.Log.d("DurationImportUtils", "第${lineNumber}行: ID=$streamerId, 名称=$streamerName, 格式化时长=[$duration], 性别=$gender")
        
        return DurationData(
            streamerId = streamerId,
            streamerName = streamerName,
            duration = duration,
            startDate = startDate,
            endDate = endDate,
            gender = gender,
            createdAt = currentTime
        )
    }
    
    /**
     * 解析时长字符串为秒数（用于排序和统计）
     * 支持格式：
     * 1. XX小时XX分钟XX秒
     * 2. XX小时XX分钟
     * 3. XXh XXm（短格式）
     */
    fun parseDurationToSeconds(duration: String): Long {
        return try {
            if (duration.isEmpty() || duration == "0" || duration == "-") {
                return 0L
            }
            
            var totalSeconds = 0L
            
            // 尝试解析短格式（XXh XXm）
            val shortHourPattern = "(\\d+)h".toRegex()
            val shortMinutePattern = "(\\d+)m".toRegex()
            
            var foundShortFormat = false
            shortHourPattern.find(duration)?.let {
                totalSeconds += it.groupValues[1].toLong() * 3600
                foundShortFormat = true
            }
            
            shortMinutePattern.find(duration)?.let {
                totalSeconds += it.groupValues[1].toLong() * 60
                foundShortFormat = true
            }
            
            if (foundShortFormat) {
                return totalSeconds
            }
            
            // 解析中文格式（XX小时XX分钟XX秒 或 XX小时XX分钟）
            val hourPattern = "(\\d+)小时".toRegex()
            hourPattern.find(duration)?.let {
                totalSeconds += it.groupValues[1].toLong() * 3600
            }
            
            val minutePattern = "(\\d+)分钟".toRegex()
            minutePattern.find(duration)?.let {
                totalSeconds += it.groupValues[1].toLong() * 60
            }
            
            val secondPattern = "(\\d+)秒".toRegex()
            secondPattern.find(duration)?.let {
                totalSeconds += it.groupValues[1].toLong()
            }
            
            totalSeconds
        } catch (e: Exception) {
            android.util.Log.e("DurationImportUtils", "解析时长失败: $duration", e)
            0L
        }
    }
    
    /**
     * 格式化时长为简洁格式（仅显示小时和分钟）
     * 输入格式：XX小时XX分钟XX秒
     * 输出格式：XX小时XX分钟
     */
    fun formatDurationShort(duration: String): String {
        return try {
            android.util.Log.d("DurationImportUtils", "开始格式化时长: [$duration]")
            
            if (duration.isEmpty() || duration == "0" || duration == "-") {
                android.util.Log.d("DurationImportUtils", "时长为空或0，返回默认值")
                return "0小时0分钟"
            }
            
            var hours = 0L
            var minutes = 0L
            
            // 提取小时
            val hourPattern = "(\\d+)小时".toRegex()
            hourPattern.find(duration)?.let {
                hours = it.groupValues[1].toLong()
                android.util.Log.d("DurationImportUtils", "提取到小时: $hours")
            }
            
            // 提取分钟
            val minutePattern = "(\\d+)分钟".toRegex()
            minutePattern.find(duration)?.let {
                minutes = it.groupValues[1].toLong()
                android.util.Log.d("DurationImportUtils", "提取到分钟: $minutes")
            }
            
            // 提取秒并四舍五入到分钟
            val secondPattern = "(\\d+)秒".toRegex()
            secondPattern.find(duration)?.let {
                val seconds = it.groupValues[1].toLong()
                android.util.Log.d("DurationImportUtils", "提取到秒: $seconds")
                if (seconds >= 30) {
                    minutes += 1
                    android.util.Log.d("DurationImportUtils", "秒数>=30，分钟+1: $minutes")
                }
            }
            
            // 处理分钟进位
            if (minutes >= 60) {
                hours += minutes / 60
                minutes %= 60
                android.util.Log.d("DurationImportUtils", "分钟进位后: ${hours}小时${minutes}分钟")
            }
            
            val result = "${hours}小时${minutes}分钟"
            android.util.Log.d("DurationImportUtils", "格式化结果: $result")
            result
        } catch (e: Exception) {
            android.util.Log.e("DurationImportUtils", "格式化时长失败: $duration", e)
            duration
        }
    }
}
