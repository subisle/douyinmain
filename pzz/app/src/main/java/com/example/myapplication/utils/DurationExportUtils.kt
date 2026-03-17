package com.example.myapplication.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.myapplication.data.model.DurationData
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 直播时长导出工具类
 */
object DurationExportUtils {
    
    /**
     * 导出为PNG图片(用于分享)
     */
    fun exportToPngForShare(
        context: Context,
        data: List<DurationData>,
        dateRange: Pair<String, String>,
        settings: com.example.myapplication.data.model.ExportSettings
    ): Result<String> {
        return try {
            val bitmap = createDataBitmap(context, data, dateRange, settings)
            
            // 生成文件名
            val genderText = when (settings.genderFilter) {
                com.example.myapplication.data.model.GenderFilter.MALE -> "男"
                com.example.myapplication.data.model.GenderFilter.FEMALE -> "女"
                com.example.myapplication.data.model.GenderFilter.ALL -> "全部"
            }
            val fileName = "${dateRange.first}_${dateRange.second}_${genderText}_${data.size}人.png"
            
            // 保存到应用缓存目录
            val cacheDir = context.cacheDir
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 导出为PNG图片
     */
    fun exportToPng(
        context: Context,
        data: List<DurationData>,
        dateRange: Pair<String, String>,
        settings: com.example.myapplication.data.model.ExportSettings
    ): Result<String> {
        return try {
            val bitmap = createDataBitmap(context, data, dateRange, settings)
            
            // 生成文件名
            val genderText = when (settings.genderFilter) {
                com.example.myapplication.data.model.GenderFilter.MALE -> "男"
                com.example.myapplication.data.model.GenderFilter.FEMALE -> "女"
                com.example.myapplication.data.model.GenderFilter.ALL -> "全部"
            }
            val fileName = "${dateRange.first}_${dateRange.second}_${genderText}_${data.size}人.png"
            
            val file = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveImageToMediaStore(context, bitmap, fileName)
            } else {
                saveImageToExternalStorage(bitmap, fileName)
            }
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun createDataBitmap(
        context: Context,
        data: List<DurationData>,
        dateRange: Pair<String, String>,
        settings: com.example.myapplication.data.model.ExportSettings
    ): Bitmap {
        // 获取GenderUtils实例用于获取正确的昵称
        val genderUtils = GenderUtils.getInstance(context)
        
        // 高清缩放因子（2倍像素）
        val scale = 2
        val margin = 4 * scale
        
        // 解析日期范围
        val startParts = dateRange.first.split("-")
        val endParts = dateRange.second.split("-")
        val year = startParts[0].toIntOrNull() ?: 2026
        val startMonth = startParts[1].toIntOrNull() ?: 1
        val endMonth = endParts[1].toIntOrNull() ?: 1
        
        // 根据settings构建表头和列宽
        val headers = mutableListOf<String>()
        val colWidths = mutableListOf<Int>()
        
        if (settings.showRank) {
            headers.add("序号")
            colWidths.add(80 * scale)
        }
        if (settings.showStreamerId) {
            headers.add("主播ID")
            colWidths.add(150 * scale)
        }
        if (settings.showStreamerName) {
            headers.add("姓名")
            colWidths.add(120 * scale)
        }
        if (settings.showEffectiveBroadcastDuration) {
            headers.add("有效时长")
            colWidths.add(160 * scale)
        }
        if (settings.showRecordDate) {
            headers.add("日期范围")
            colWidths.add(180 * scale)
        }
        
        val tableWidth = colWidths.sum()
        val width = tableWidth + margin * 2
        
        // 高度设置
        val rowHeight = 48 * scale
        val headerHeight = 60 * scale
        val titleHeight = 80 * scale
        val statsHeight = 40 * scale
        val footerHeight = 60 * scale
        
        // 计算零时长主播名单的高度
        val inactiveStreamers = data.filter { 
            DurationImportUtils.parseDurationToSeconds(it.duration) == 0L 
        }
        val inactiveHeight = if (inactiveStreamers.isNotEmpty()) {
            val inactiveCount = inactiveStreamers.size
            val estimatedLines = (inactiveCount / 10) + 1
            (estimatedLines * 32 * scale) + (16 * scale)
        } else {
            0
        }
        
        // 计算总高度
        val totalHeight = titleHeight + headerHeight + (data.size * rowHeight) + 
                         statsHeight + inactiveHeight + footerHeight + margin * 4
        
        // 创建高清图片 - 纯白背景
        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        // 颜色定义（与音浪导出保持一致）
        val titleBgColor = Color.parseColor("#2C3E50")
        val titleTextColor = Color.parseColor("#FFFFFF")
        val headerBgColor = Color.parseColor("#34495E")
        val headerTextColor = Color.parseColor("#FFFFFF")
        val dataTextColor = Color.parseColor("#2C3E50")
        val statsBgColor = Color.parseColor("#ECF0F1")
        val statsTextColor = Color.parseColor("#2C3E50")
        val inactiveBgColor = Color.parseColor("#FADBD8")
        val inactiveTextColor = Color.parseColor("#C0392B")
        val borderColor = Color.parseColor("#BDC3C7")
        val strongBorderColor = Color.parseColor("#7F8C8D")
        
        var currentY = margin.toFloat()
        val tableLeft = margin.toFloat()
        
        // ========== 1. 绘制标题栏 ==========
        paint.color = titleBgColor
        canvas.drawRect(tableLeft, currentY, width - margin.toFloat(), currentY + titleHeight, paint)
        
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * scale
        paint.color = strongBorderColor
        canvas.drawRect(tableLeft, currentY, width - margin.toFloat(), currentY + titleHeight, paint)
        paint.style = Paint.Style.FILL
        
        paint.color = titleTextColor
        paint.textSize = 32f * scale
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val monthText = if (startMonth == endMonth) "${startMonth}月" else "${startMonth}-${endMonth}月"
        val titleText = "鹏仔传媒主播 ${year}年${monthText}份时长表"
        canvas.drawText(titleText, width / 2f, currentY + titleHeight / 2f + 12f * scale, paint)
        
        currentY += titleHeight + 4f * scale
        
        // ========== 2. 绘制表头 ==========
        paint.color = headerBgColor
        canvas.drawRect(tableLeft, currentY, tableLeft + tableWidth, currentY + headerHeight, paint)
        
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * scale
        paint.color = strongBorderColor
        canvas.drawRect(tableLeft, currentY, tableLeft + tableWidth, currentY + headerHeight, paint)
        paint.style = Paint.Style.FILL
        
        paint.color = headerTextColor
        paint.textSize = 22f * scale
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        var colX = tableLeft
        headers.forEachIndexed { index, header ->
            val centerX = colX + colWidths[index] / 2f
            canvas.drawText(header, centerX, currentY + headerHeight / 2f + 8f * scale, paint)
            
            if (index < headers.size - 1) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.5f * scale
                paint.color = Color.parseColor("#FFFFFF")
                canvas.drawLine(colX + colWidths[index], currentY + 8f * scale, 
                              colX + colWidths[index], currentY + headerHeight - 8f * scale, paint)
                paint.style = Paint.Style.FILL
            }
            
            colX += colWidths[index]
        }
        
        currentY += headerHeight
        
        // ========== 3. 绘制数据行 ==========
        paint.textSize = 19f * scale
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        data.forEachIndexed { index, item ->
            // 使用5色循环背景（心理学配色）
            val rowBgColor = when (index % 5) {
                0 -> Color.parseColor("#f0f9ff")  // 淡蓝
                1 -> Color.parseColor("#f5f3ff")  // 淡紫
                2 -> Color.parseColor("#f0fdf4")  // 淡绿
                3 -> Color.parseColor("#fffaf5")  // 淡橙
                else -> Color.parseColor("#f8fafc")  // 淡灰
            }
            
            paint.color = rowBgColor
            canvas.drawRect(tableLeft, currentY, tableLeft + tableWidth, currentY + rowHeight, paint)
            
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.8f * scale
            paint.color = borderColor
            canvas.drawRect(tableLeft, currentY, tableLeft + tableWidth, currentY + rowHeight, paint)
            paint.style = Paint.Style.FILL
            
            // 构建行数据
            val rowData = mutableListOf<String>()
            if (settings.showRank) rowData.add((index + 1).toString())
            if (settings.showStreamerId) rowData.add(item.streamerId)
            if (settings.showStreamerName) rowData.add(genderUtils.getDisplayName(item.streamerId, item.streamerName))
            if (settings.showEffectiveBroadcastDuration) {
                val seconds = DurationImportUtils.parseDurationToSeconds(item.duration)
                if (seconds == 0L) {
                    rowData.add("未开播")
                } else {
                    // 只显示小时，不显示分钟
                    val hours = seconds / 3600
                    rowData.add("${hours}小时")
                }
            }
            if (settings.showRecordDate) rowData.add("${item.startDate} ~ ${item.endDate}")
            
            colX = tableLeft
            rowData.forEachIndexed { colIndex, text ->
                val centerX = colX + colWidths[colIndex] / 2f
                val centerY = currentY + rowHeight / 2f + 6f * scale
                
                // 判断列类型
                val isRankColumn = headers[colIndex] == "序号"
                val isDurationColumn = headers[colIndex] == "有效时长"
                
                // 设置颜色和字体
                when {
                    // 排名列：前3名特殊样式
                    isRankColumn -> {
                        val rank = index + 1
                        when (rank) {
                            1 -> {
                                paint.color = Color.parseColor("#eab308")  // 金色
                                paint.textSize = 24f * scale
                                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            }
                            2 -> {
                                paint.color = Color.parseColor("#94a3b8")  // 银色
                                paint.textSize = 22f * scale
                                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            }
                            3 -> {
                                paint.color = Color.parseColor("#9a3412")  // 铜色
                                paint.textSize = 22f * scale
                                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            }
                            else -> {
                                paint.color = dataTextColor
                                paint.textSize = 19f * scale
                                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                            }
                        }
                    }
                    // 未开播标红
                    isDurationColumn && text == "未开播" -> {
                        paint.color = Color.parseColor("#E74C3C")
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    else -> {
                        paint.color = dataTextColor
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    }
                }
                
                canvas.drawText(text, centerX, centerY, paint)
                
                // 重置字体样式和大小
                paint.textSize = 19f * scale
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                
                colX += colWidths[colIndex]
            }
            
            currentY += rowHeight
        }
        
        // 绘制列分隔线
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f * scale
        paint.color = borderColor
        colX = tableLeft
        for (i in 1 until colWidths.size) {
            colX += colWidths[i - 1]
            canvas.drawLine(colX, currentY - data.size * rowHeight - headerHeight, 
                          colX, currentY, paint)
        }
        
        // 绘制表格外边框
        paint.strokeWidth = 2f * scale
        paint.color = strongBorderColor
        canvas.drawRect(tableLeft, currentY - data.size * rowHeight - headerHeight, 
                       tableLeft + tableWidth, currentY, paint)
        paint.style = Paint.Style.FILL
        
        // ========== 4. 绘制统计行 ==========
        currentY += 8f * scale
        
        val totalStreamers = data.size
        val activeStreamers = data.count { 
            DurationImportUtils.parseDurationToSeconds(it.duration) > 0 
        }
        val zeroStreamers = totalStreamers - activeStreamers
        
        // 计算总时长（只显示小时）
        val totalSeconds = data.sumOf { DurationImportUtils.parseDurationToSeconds(it.duration) }
        val totalHours = totalSeconds / 3600
        val totalDurationText = "${totalHours}小时"
        
        val statsText = "统计: 共${totalStreamers}位主播, ${activeStreamers}位有时长, ${zeroStreamers}位零时长, 总计${totalDurationText}"
        
        paint.color = statsBgColor
        canvas.drawRect(tableLeft, currentY, tableLeft + tableWidth, currentY + statsHeight, paint)
        
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f * scale
        paint.color = strongBorderColor
        canvas.drawRect(tableLeft, currentY, tableLeft + tableWidth, currentY + statsHeight, paint)
        paint.style = Paint.Style.FILL
        
        paint.color = statsTextColor
        paint.textSize = 17f * scale
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(statsText, width / 2f, currentY + statsHeight / 2f + 5f * scale, paint)
        
        currentY += statsHeight + 8f * scale
        
        // ========== 5. 绘制未开播主播名单 ==========
        if (inactiveStreamers.isNotEmpty()) {
            val inactiveNames = inactiveStreamers
                .map { genderUtils.getDisplayName(it.streamerId, it.streamerName) }
            
            val inactiveText = "未开播主播名单 (共${inactiveNames.size}位)"
            val namesText = inactiveNames.joinToString("、")
            
            val maxCharsPerLine = 30
            val lines = mutableListOf<String>()
            lines.add(inactiveText)
            
            var currentLine = ""
            namesText.split("、").forEach { name ->
                val testLine = if (currentLine.isEmpty()) name else "$currentLine、$name"
                if (testLine.length > maxCharsPerLine && currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = name
                } else {
                    currentLine = testLine
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }
            
            val actualInactiveHeight = (lines.size * 28f * scale) + (16f * scale)
            
            paint.color = inactiveBgColor
            canvas.drawRect(tableLeft, currentY, tableLeft + tableWidth, currentY + actualInactiveHeight, paint)
            
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f * scale
            paint.color = strongBorderColor
            canvas.drawRect(tableLeft, currentY, tableLeft + tableWidth, currentY + actualInactiveHeight, paint)
            paint.style = Paint.Style.FILL
            
            paint.color = inactiveTextColor
            paint.textSize = 16f * scale
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            
            lines.forEachIndexed { index, line ->
                canvas.drawText(line, width / 2f, currentY + (index + 1) * 26f * scale, paint)
            }
            
            currentY += actualInactiveHeight + 8f * scale
        }
        
        // ========== 6. 绘制页脚 ==========
        paint.color = Color.parseColor("#7F8C8D")
        paint.textSize = 13f * scale
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val footerText = "生成时间: ${SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", 
            Locale.CHINA).format(Date())} | 鹏仔传媒数据管理系统"
        canvas.drawText(footerText, width / 2f, currentY + 20f * scale, paint)
        
        return bitmap
    }
    
    private fun saveImageToMediaStore(context: Context, bitmap: Bitmap, fileName: String): String {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }
        
        return "图片已保存到相册: $fileName"
    }
    
    private fun saveImageToExternalStorage(bitmap: Bitmap, fileName: String): String {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val file = File(picturesDir, fileName)
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        return "图片已保存: ${file.absolutePath}"
    }
}
