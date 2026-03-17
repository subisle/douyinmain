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
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.model.SoundData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportUtils {
    
    fun exportToExcel(context: Context, data: List<SoundData>, date: String, settings: com.example.myapplication.data.model.ExportSettings): Result<String> {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("音浪数据")
            
            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                borderBottom = BorderStyle.THIN
                borderTop = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
                alignment = HorizontalAlignment.CENTER
            }
            
            val headerFont = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 12
            }
            headerStyle.setFont(headerFont)
            
            val headers = mutableListOf<String>()
            if (settings.showRank) headers.add("排名")
            if (settings.showStreamerId) headers.add("主播ID")
            if (settings.showStreamerName) headers.add("主播名称")
            if (settings.showSoundWave) headers.add("当日音浪")
            if (settings.showTotalSoundWave) headers.add("总音浪")
            if (settings.showGradeRank) headers.add("等级")
            if (settings.showEffectiveBroadcastDuration) headers.add("开播时长")
            if (settings.showRecordDate) headers.add("日期")
            
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }
            
            data.forEachIndexed { index, item ->
                val row = sheet.createRow(index + 1)
                var colIndex = 0
                
                if (settings.showRank) row.createCell(colIndex++).setCellValue(item.overallRank.toDouble())
                if (settings.showStreamerId) row.createCell(colIndex++).setCellValue(item.streamerId)
                if (settings.showStreamerName) row.createCell(colIndex++).setCellValue(item.streamerName)
                if (settings.showSoundWave) row.createCell(colIndex++).setCellValue(item.soundWave.toDouble())
                if (settings.showTotalSoundWave) row.createCell(colIndex++).setCellValue(item.totalSoundWave.toDouble())
                if (settings.showGradeRank) row.createCell(colIndex++).setCellValue(item.gradeRank ?: "")
                if (settings.showEffectiveBroadcastDuration) row.createCell(colIndex++).setCellValue(item.effectiveBroadcastDuration ?: "")
                if (settings.showRecordDate) row.createCell(colIndex++).setCellValue(item.recordDate)
            }
            
            for (i in 0 until headers.size) {
                sheet.autoSizeColumn(i)
            }
            
            val fileName = "音浪数据_${date}.xlsx"
            val file = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(context, workbook, fileName)
            } else {
                saveToExternalStorage(workbook, fileName)
            }
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun exportToExcelForShare(context: Context, data: List<SoundData>, date: String, settings: com.example.myapplication.data.model.ExportSettings): Result<String> {
        return try {
            android.util.Log.d("ExportUtils", "开始导出CSV用于分享，数据量: ${data.size}")
            
            // 如果需要显示有效时长，先填充数据
            val dataToExport = if (settings.showEffectiveBroadcastDuration) {
                android.util.Log.d("ExportUtils", "填充时长数据")
                fillDurationData(context, data, date)
            } else {
                data
            }
            
            // 构建CSV内容
            val csvBuilder = StringBuilder()
            
            // 添加BOM以支持Excel正确识别UTF-8编码
            csvBuilder.append("\uFEFF")
            
            // 构建表头
            val headers = mutableListOf<String>()
            if (settings.showRank) headers.add("排名")
            if (settings.showStreamerId) headers.add("主播ID")
            if (settings.showStreamerName) headers.add("主播名称")
            if (settings.showSoundWave) headers.add("当日音浪")
            if (settings.showTotalSoundWave) headers.add("总音浪")
            if (settings.showGradeRank) headers.add("等级")
            if (settings.showEffectiveBroadcastDuration) headers.add("开播时长")
            if (settings.showRecordDate) headers.add("日期")
            
            csvBuilder.append(headers.joinToString(",")).append("\n")
            
            android.util.Log.d("ExportUtils", "表头列数: ${headers.size}")
            
            // 添加数据行（使用索引作为排名）
            dataToExport.forEachIndexed { index, item ->
                val row = mutableListOf<String>()
                
                if (settings.showRank) row.add((index + 1).toString())  // 使用索引+1作为排名
                if (settings.showStreamerId) row.add(escapeCsvField(item.streamerId))
                if (settings.showStreamerName) row.add(escapeCsvField(item.streamerName))
                if (settings.showSoundWave) row.add(item.soundWave.toString())
                if (settings.showTotalSoundWave) row.add(item.totalSoundWave.toString())
                if (settings.showGradeRank) row.add(escapeCsvField(item.gradeRank ?: ""))
                if (settings.showEffectiveBroadcastDuration) row.add(escapeCsvField(item.effectiveBroadcastDuration ?: ""))
                if (settings.showRecordDate) row.add(escapeCsvField(item.recordDate))
                
                csvBuilder.append(row.joinToString(",")).append("\n")
            }
            
            val genderText = when (settings.genderFilter) {
                com.example.myapplication.data.model.GenderFilter.MALE -> "男"
                com.example.myapplication.data.model.GenderFilter.FEMALE -> "女"
                com.example.myapplication.data.model.GenderFilter.ALL -> "全部"
            }
            val fileName = "${date}_${genderText}_${dataToExport.size}人.csv"
            
            val cacheDir = context.cacheDir
            val file = File(cacheDir, fileName)
            
            android.util.Log.d("ExportUtils", "保存文件到: ${file.absolutePath}")
            
            // 写入文件
            file.writeText(csvBuilder.toString(), Charsets.UTF_8)
            
            android.util.Log.d("ExportUtils", "CSV导出成功: ${file.absolutePath}, 文件大小: ${file.length()} bytes")
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            android.util.Log.e("ExportUtils", "CSV导出失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 转义CSV字段
     * 如果字段包含逗号、引号或换行符，需要用引号包裹并转义内部引号
     */
    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }
    
    fun exportToPngForShare(context: Context, data: List<SoundData>, date: String, settings: com.example.myapplication.data.model.ExportSettings): Result<String> {
        return try {
            val bitmap = createDataBitmap(context, data, date, settings)
            
            val genderText = when (settings.genderFilter) {
                com.example.myapplication.data.model.GenderFilter.MALE -> "男"
                com.example.myapplication.data.model.GenderFilter.FEMALE -> "女"
                com.example.myapplication.data.model.GenderFilter.ALL -> "全部"
            }
            val fileName = "${date}_${genderText}_${data.size}人.png"
            
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
    
    fun exportToPng(context: Context, data: List<SoundData>, date: String, settings: com.example.myapplication.data.model.ExportSettings): Result<String> {
        return try {
            val bitmap = createDataBitmap(context, data, date, settings)
            
            val genderText = when (settings.genderFilter) {
                com.example.myapplication.data.model.GenderFilter.MALE -> "男"
                com.example.myapplication.data.model.GenderFilter.FEMALE -> "女"
                com.example.myapplication.data.model.GenderFilter.ALL -> "全部"
            }
            val fileName = "${date}_${genderText}_${data.size}人.png"
            
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
        data: List<SoundData>,
        date: String,
        settings: com.example.myapplication.data.model.ExportSettings
    ): Bitmap {
        val genderUtils = GenderUtils.getInstance(context)
        
        // 如果需要显示有效时长,从duration_data表查询并填充数据
        val dataWithDuration = if (settings.showEffectiveBroadcastDuration) {
            fillDurationData(context, data, date)
        } else {
            data
        }
        
        // 根据设置进行排序
        val sortedData = when (settings.sortBy) {
            com.example.myapplication.data.model.SortField.GRADE_RANK -> {
                // 等级排序：S > A > B > C > D，同等级按数字升序
                val gradeOrder = mapOf("S" to 1, "A" to 2, "B" to 3, "C" to 4, "D" to 5)
                if (settings.sortOrder == com.example.myapplication.data.model.SortOrder.ASC) {
                    dataWithDuration.sortedWith(compareBy(
                        { gradeOrder[it.gradeRank?.firstOrNull()?.toString() ?: "D"] ?: 5 },
                        { it.gradeRank?.substring(1)?.toIntOrNull() ?: 99 }
                    ))
                } else {
                    dataWithDuration.sortedWith(compareByDescending<SoundData>(
                        { gradeOrder[it.gradeRank?.firstOrNull()?.toString() ?: "D"] ?: 5 }
                    ).thenBy { it.gradeRank?.substring(1)?.toIntOrNull() ?: 99 })
                }
            }
            com.example.myapplication.data.model.SortField.SOUND_WAVE -> {
                if (settings.sortOrder == com.example.myapplication.data.model.SortOrder.ASC) {
                    dataWithDuration.sortedBy { it.soundWave }
                } else {
                    dataWithDuration.sortedByDescending { it.soundWave }
                }
            }
            com.example.myapplication.data.model.SortField.TOTAL_SOUND_WAVE -> {
                if (settings.sortOrder == com.example.myapplication.data.model.SortOrder.ASC) {
                    dataWithDuration.sortedBy { it.totalSoundWave }
                } else {
                    dataWithDuration.sortedByDescending { it.totalSoundWave }
                }
            }
            com.example.myapplication.data.model.SortField.STREAMER_NAME -> {
                if (settings.sortOrder == com.example.myapplication.data.model.SortOrder.ASC) {
                    dataWithDuration.sortedBy { it.streamerName }
                } else {
                    dataWithDuration.sortedByDescending { it.streamerName }
                }
            }
            com.example.myapplication.data.model.SortField.BROADCAST_DURATION -> {
                // 按直播时长排序
                if (settings.sortOrder == com.example.myapplication.data.model.SortOrder.ASC) {
                    dataWithDuration.sortedBy { 
                        it.effectiveBroadcastDuration?.let { duration ->
                            parseDurationToMinutes(duration)
                        } ?: 0
                    }
                } else {
                    dataWithDuration.sortedByDescending { 
                        it.effectiveBroadcastDuration?.let { duration ->
                            parseDurationToMinutes(duration)
                        } ?: 0
                    }
                }
            }
            else -> dataWithDuration  // 默认保持原顺序
        }
        
        // 计算昨日排名变化
        val previousDayData = getPreviousDayData(context, date, settings)
        val rankChanges = calculateRankChanges(sortedData, previousDayData)
        
        val scale = 2f
        
        // 构建标题文字
        val genderText = when (settings.genderFilter) {
            com.example.myapplication.data.model.GenderFilter.MALE -> "男"
            com.example.myapplication.data.model.GenderFilter.FEMALE -> "女"
            else -> ""
        }
        val parts = date.split("-")
        val year = parts[0].toIntOrNull() ?: 2026
        val month = parts[1].toIntOrNull() ?: 1
        val day = parts[2].toIntOrNull() ?: 1
        val titleText = "鹏仔传媒${genderText}主播 ${year}年${month}月${day}日音浪表"
        
        // 计算标题所需的最小宽度（使用Paint测量）
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 22f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.15f
        }
        val titleWidth = titlePaint.measureText(titleText)
        
        // 画布宽度：标题宽度 + 左右边距，最小500，最大750
        val containerWidth = (titleWidth + 60f * scale).toInt().coerceIn((500 * scale).toInt(), (750 * scale).toInt())
        
        val headers = mutableListOf<String>()
        if (settings.showRank) headers.add("序号")
        if (settings.showStreamerName) headers.add("主播姓名")
        if (settings.showSoundWave) headers.add("${day}号音浪")  // 动态显示日期
        if (settings.showTotalSoundWave) headers.add("累计总音浪")
        if (settings.showEffectiveBroadcastDuration) headers.add("有效时长")
        if (settings.showGradeRank) headers.add("等级")  // 等级放到最后
        
        val headerHeight = 50 * scale
        val unstartedBannerHeight = 60 * scale
        val tableHeaderHeight = 28 * scale
        val rowHeight = 36 * scale  // 统一行高
        val footerSummaryHeight = 80 * scale
        
        val inactiveStreamers = sortedData.filter { it.soundWave <= 1 }
        val hasInactive = inactiveStreamers.isNotEmpty()
        val footerHeight = if (hasInactive) 100f * scale else 0f  // 合并后的底部区域
        
        // 计算今日音浪最大值(用于数据条)
        val maxSoundWave = sortedData.filter { it.soundWave > 1 }.maxOfOrNull { it.soundWave } ?: 1L
        
        // 计算总高度
        val totalHeight = (headerHeight + 
                          tableHeaderHeight + 
                          (sortedData.size * rowHeight) + 
                          footerHeight).toInt()
        
        val bitmap = Bitmap.createBitmap(containerWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 整体背景 - 浅灰蓝色
        canvas.drawColor(Color.parseColor("#F8FAFC"))
        
        val paint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        // 颜色定义 - 莫兰迪色系
        val headerDark = Color.parseColor("#1E293B")  // 墨蓝色标题栏
        val absentBg = Color.parseColor("#fff1f2")
        val absentRed = Color.parseColor("#e11d48")
        val textMain = Color.parseColor("#334155")
        val textGray = Color.parseColor("#64748b")  // 深灰色
        val headerBg = Color.parseColor("#f1f5f9")
        val headerText = Color.parseColor("#64748b")
        val dataBarColor = Color.parseColor("#93c5fd")  // 莫兰迪蓝色 - 加深饱和度
        
        // 根据实际显示的列数动态计算列宽
        val columnCount = headers.size
        
        // 智能分配列宽（根据列数和列类型）- 等级放到最后
        val colWidths = when (columnCount) {
            6 -> {
                // 6列：序号、姓名、今日音浪、总音浪、有效时长、等级
                floatArrayOf(
                    containerWidth * 0.08f,  // 序号 8%
                    containerWidth * 0.14f,  // 主播姓名 14% (从16%缩小)
                    containerWidth * 0.28f,  // 今日音浪 28% (从26%增加)
                    containerWidth * 0.28f,  // 累计总音浪 28% (从26%增加)
                    containerWidth * 0.10f,  // 有效时长 10% (从12%缩小)
                    containerWidth * 0.12f   // 等级 12%
                )
            }
            5 -> {
                // 5列情况：根据具体显示的列调整
                if (settings.showEffectiveBroadcastDuration) {
                    // 序号、姓名、今日音浪、总音浪、有效时长
                    floatArrayOf(
                        containerWidth * 0.08f,  // 序号 8%
                        containerWidth * 0.16f,  // 主播姓名 16% (从18%缩小)
                        containerWidth * 0.32f,  // 今日音浪 32% (从30%增加)
                        containerWidth * 0.32f,  // 累计总音浪 32% (从30%增加)
                        containerWidth * 0.12f   // 有效时长 12% (从14%缩小)
                    )
                } else {
                    // 序号、姓名、今日音浪、总音浪、等级
                    floatArrayOf(
                        containerWidth * 0.08f,  // 序号 8%
                        containerWidth * 0.16f,  // 主播姓名 16% (从18%缩小)
                        containerWidth * 0.32f,  // 今日音浪 32% (从30%增加)
                        containerWidth * 0.32f,  // 累计总音浪 32% (从30%增加)
                        containerWidth * 0.12f   // 等级 12% (从14%缩小)
                    )
                }
            }
            4 -> {
                // 4列情况：根据具体显示的列调整
                if (!settings.showRank) {
                    // 没有序号：姓名、今日音浪、总音浪、等级
                    floatArrayOf(
                        containerWidth * 0.18f,  // 主播姓名 18% (从20%缩小)
                        containerWidth * 0.35f,  // 今日音浪 35% (从33%增加)
                        containerWidth * 0.35f,  // 累计总音浪 35% (从33%增加)
                        containerWidth * 0.12f   // 等级 12% (从14%缩小)
                    )
                } else if (!settings.showGradeRank) {
                    // 没有等级：序号、姓名、今日音浪、总音浪
                    floatArrayOf(
                        containerWidth * 0.08f,  // 序号 8%
                        containerWidth * 0.18f,  // 主播姓名 18% (从20%缩小)
                        containerWidth * 0.37f,  // 今日音浪 37% (从36%增加)
                        containerWidth * 0.37f   // 累计总音浪 37% (从36%增加)
                    )
                } else if (!settings.showSoundWave) {
                    // 没有今日音浪：序号、姓名、总音浪、等级
                    floatArrayOf(
                        containerWidth * 0.08f,  // 序号 8%
                        containerWidth * 0.24f,  // 主播姓名 24% (从26%缩小)
                        containerWidth * 0.56f,  // 累计总音浪 56% (从52%增加)
                        containerWidth * 0.12f   // 等级 12% (从14%缩小)
                    )
                } else if (!settings.showStreamerName) {
                    // 没有姓名：序号、今日音浪、总音浪、等级
                    floatArrayOf(
                        containerWidth * 0.10f,  // 序号 10%
                        containerWidth * 0.39f,  // 今日音浪 39% (从38%增加)
                        containerWidth * 0.39f,  // 累计总音浪 39% (从38%增加)
                        containerWidth * 0.12f   // 等级 12% (从14%缩小)
                    )
                } else if (settings.showEffectiveBroadcastDuration) {
                    // 有时长的4列组合
                    floatArrayOf(
                        containerWidth * 0.18f,  // 主播姓名 18% (从20%缩小)
                        containerWidth * 0.35f,  // 今日音浪 35% (从34%增加)
                        containerWidth * 0.35f,  // 累计总音浪 35% (从32%增加)
                        containerWidth * 0.12f   // 有效时长 12% (从14%缩小)
                    )
                } else {
                    // 其他4列组合，平均分配
                    FloatArray(4) { containerWidth * 0.25f }
                }
            }
            3 -> {
                // 3列情况
                if (settings.showStreamerName && settings.showSoundWave && settings.showTotalSoundWave) {
                    // 姓名、今日音浪、总音浪
                    floatArrayOf(
                        containerWidth * 0.20f,  // 主播姓名 20% (从22%缩小)
                        containerWidth * 0.40f,  // 今日音浪 40% (从39%增加)
                        containerWidth * 0.40f   // 累计总音浪 40% (从39%增加)
                    )
                } else if (settings.showRank && settings.showStreamerName && settings.showSoundWave) {
                    // 序号、姓名、今日音浪
                    floatArrayOf(
                        containerWidth * 0.10f,  // 序号 10%
                        containerWidth * 0.24f,  // 主播姓名 24% (从26%缩小)
                        containerWidth * 0.66f   // 今日音浪 66% (从64%增加)
                    )
                } else if (settings.showStreamerName && settings.showSoundWave && settings.showGradeRank) {
                    // 姓名、今日音浪、等级
                    floatArrayOf(
                        containerWidth * 0.24f,  // 主播姓名 24% (从26%缩小)
                        containerWidth * 0.64f,  // 今日音浪 64% (从60%增加)
                        containerWidth * 0.12f   // 等级 12% (从14%缩小)
                    )
                } else {
                    FloatArray(3) { containerWidth * 0.33f }
                }
            }
            2 -> {
                if (settings.showStreamerName && settings.showSoundWave) {
                    // 姓名、音浪
                    floatArrayOf(
                        containerWidth * 0.26f,  // 主播姓名 26% (从28%缩小)
                        containerWidth * 0.74f   // 音浪 74% (从72%增加)
                    )
                } else if (settings.showStreamerName && settings.showTotalSoundWave) {
                    // 姓名、总音浪
                    floatArrayOf(
                        containerWidth * 0.26f,  // 主播姓名 26% (从28%缩小)
                        containerWidth * 0.74f   // 总音浪 74% (从72%增加)
                    )
                } else {
                    FloatArray(2) { containerWidth * 0.50f }
                }
            }
            1 -> floatArrayOf(containerWidth.toFloat())    // 1列：100%
            else -> FloatArray(columnCount) { containerWidth / columnCount.toFloat() }  // 其他情况平均分配
        }
        
        fun getColX(colIdx: Int): Float {
            return colWidths.take(colIdx).sum()
        }
        
        var currentY = 0f
        
        // 1. 标题
        paint.color = headerDark
        canvas.drawRect(0f, currentY, containerWidth.toFloat(), currentY + headerHeight, paint)
        
        paint.color = Color.parseColor("#f8fafc")
        paint.textSize = 22f * scale
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.letterSpacing = 0.15f
        canvas.drawText(titleText, containerWidth / 2f, currentY + headerHeight / 2f + 8f * scale, paint)
        paint.letterSpacing = 0f
        
        currentY += headerHeight
        
        // 2. 表头 - 去掉垂直分割线
        paint.color = headerBg
        canvas.drawRect(0f, currentY, containerWidth.toFloat(), currentY + tableHeaderHeight, paint)
        
        paint.color = headerText
        paint.textSize = 13f * scale
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        
        headers.forEachIndexed { index, header ->
            val colWidth = colWidths[index]
            val colStartX = getColX(index)
            val centerX = colStartX + colWidth / 2f
            
            // 根据列类型设置对齐方式
            when {
                header == "主播姓名" -> {
                    paint.textAlign = Paint.Align.LEFT
                    val textX = colStartX + colWidth * 0.1f
                    canvas.drawText(header.uppercase(), textX, currentY + tableHeaderHeight / 2f + 5f * scale, paint)
                }
                header.endsWith("号音浪") || header == "累计总音浪" -> {
                    paint.textAlign = Paint.Align.RIGHT
                    val textX = colStartX + colWidth - colWidth * 0.08f
                    canvas.drawText(header.uppercase(), textX, currentY + tableHeaderHeight / 2f + 5f * scale, paint)
                }
                else -> {
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(header.uppercase(), centerX, currentY + tableHeaderHeight / 2f + 5f * scale, paint)
                }
            }
        }
        
        currentY += tableHeaderHeight
        
        // 3. 数据行(所有主播,前三名特殊处理)
        sortedData.forEachIndexed { index, item ->
            val actualRank = index + 1
            val isTop3 = actualRank <= 3
            val currentRowHeight = rowHeight  // 统一使用相同行高
            
            // 背景色 - 前三名特殊背景，其他根据等级分层
            paint.color = when {
                // 前三名使用极浅的金/银/灰色底
                isTop3 && actualRank == 1 -> Color.parseColor("#fffbeb")  // 极浅金色
                isTop3 && actualRank == 2 -> Color.parseColor("#f8fafc")  // 极浅银色
                isTop3 && actualRank == 3 -> Color.parseColor("#fafafa")  // 极浅灰色
                // 其他根据等级分层背景
                else -> {
                    val gradePrefix = (item.gradeRank ?: "D1").firstOrNull()?.toString() ?: "D"
                    val gradeNum = (item.gradeRank ?: "D1").substring(1).toIntOrNull() ?: 1
                    when {
                        gradePrefix in listOf("S", "A", "B") -> Color.parseColor("#fffef5")  // 极浅暖黄
                        gradePrefix == "C" -> Color.parseColor("#f0fdf4")  // 极浅青绿
                        gradePrefix == "D" && gradeNum <= 10 -> Color.parseColor("#f0f9ff")  // 极浅蓝
                        else -> Color.WHITE  // 纯白
                    }
                }
            }
            canvas.drawRect(0f, currentY, containerWidth.toFloat(), currentY + currentRowHeight, paint)
            
            // 极细分割线 - 只保留水平线
            paint.color = Color.parseColor("#f0f0f0")
            paint.strokeWidth = 0.5f * scale
            canvas.drawLine(0f, currentY, containerWidth.toFloat(), currentY, paint)
            
            val rowData = mutableListOf<String>()
            if (settings.showRank) rowData.add(actualRank.toString().padStart(2, '0'))
            if (settings.showStreamerName) rowData.add(genderUtils.getDisplayName(item.streamerId, item.streamerName))
            if (settings.showSoundWave) {
                // 未开播显示"未开播"，否则显示音浪数字
                rowData.add(if (item.soundWave <= 1) "未开播" else formatSoundWave(item.soundWave))
            }
            if (settings.showTotalSoundWave) rowData.add(formatSoundWave(item.totalSoundWave))
            if (settings.showEffectiveBroadcastDuration) {
                // 格式化时长为"x时x分"
                val duration = item.effectiveBroadcastDuration ?: "0小时"
                val formattedDuration = formatDurationToHourMinute(duration)
                rowData.add(formattedDuration)
            }
            if (settings.showGradeRank) rowData.add(item.gradeRank ?: "D1")
            
            rowData.forEachIndexed { colIndex, text ->
                val colWidth = colWidths[colIndex]
                val colStartX = getColX(colIndex)
                val centerX = colStartX + colWidth / 2f
                val centerY = currentY + currentRowHeight / 2f + 5f * scale
                val headerName = headers[colIndex]
                
                when {
                    headerName == "序号" -> {
                        // 前三名显示奖牌图标 + "今日之星"标识
                        if (isTop3) {
                            val icon = when (actualRank) {
                                1 -> "🥇⭐"  // 第一名加星星
                                2 -> "🥈"
                                3 -> "🥉"
                                else -> actualRank.toString().padStart(2, '0')
                            }
                            paint.textSize = 20f * scale
                            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                            paint.color = textMain
                            paint.textAlign = Paint.Align.CENTER
                            canvas.drawText(icon, centerX, centerY, paint)
                        } else {
                            // 普通序号 + 排名变化箭头
                            paint.textSize = 15f * scale
                            paint.typeface = Typeface.create("serif", Typeface.BOLD_ITALIC)
                            paint.color = textGray
                            paint.textAlign = Paint.Align.CENTER
                            canvas.drawText(text, centerX - 10f * scale, centerY, paint)
                            
                            // 显示排名变化箭头
                            val rankChange = rankChanges[item.streamerId] ?: 0
                            if (rankChange != 0) {
                                val arrow = if (rankChange > 0) "↑$rankChange" else "↓${-rankChange}"
                                paint.textSize = 10f * scale
                                paint.color = if (rankChange > 0) Color.parseColor("#10b981") else Color.parseColor("#ef4444")
                                canvas.drawText(arrow, centerX + 15f * scale, centerY, paint)
                            }
                        }
                    }
                    headerName == "主播姓名" -> {
                        // 应用用户设置的字体缩放
                        val nameSize = 15f * scale * settings.nameTextScale
                        paint.textSize = nameSize
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        paint.color = textMain
                        paint.textAlign = Paint.Align.LEFT
                        val textX = colStartX + colWidth * 0.1f
                        canvas.drawText(text, textX, centerY, paint)
                    }
                    headerName.endsWith("号音浪") -> {
                        // 绘制数据条背景 - 莫兰迪蓝色，垂直居中显示（未开播不显示数据条）
                        if (item.soundWave > 1) {
                            val barWidthRatio = item.soundWave.toFloat() / maxSoundWave.toFloat()
                            val barMaxWidth = colWidth * 0.85f
                            val barWidth = barMaxWidth * barWidthRatio
                            val barHeight = 20f * scale  // 固定高度,确保居中
                            
                            val barRight = colStartX + colWidth - colWidth * 0.05f
                            val barLeft = barRight - barWidth
                            val barTop = currentY + (currentRowHeight - barHeight) / 2f  // 垂直居中
                            
                            paint.color = dataBarColor  // 莫兰迪蓝色
                            paint.style = Paint.Style.FILL
                            canvas.drawRoundRect(barLeft, barTop, barRight, barTop + barHeight,
                                4f * scale, 4f * scale, paint)
                        }
                        
                        // 绘制文字(右对齐,使用等宽字体)
                        // 未开播显示红色"未开播"
                        if (item.soundWave <= 1) {
                            paint.textSize = 14f * scale
                            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            paint.color = absentRed  // 红色
                        } else {
                            paint.textSize = 15f * scale
                            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                            paint.color = textMain
                        }
                        paint.style = Paint.Style.FILL
                        
                        paint.textAlign = Paint.Align.RIGHT
                        val textX = colStartX + colWidth - colWidth * 0.08f
                        canvas.drawText(text, textX, centerY, paint)
                    }
                    headerName == "累计总音浪" -> {
                        // 使用等宽字体和深灰色
                        paint.textSize = 13f * scale
                        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                        paint.color = textGray  // 深灰色
                        
                        paint.textAlign = Paint.Align.RIGHT
                        val textX = colStartX + colWidth - colWidth * 0.08f
                        canvas.drawText(text, textX, centerY, paint)
                    }
                    headerName == "等级" -> {
                        // 等级标签 - 暖调/中性/冷调配色
                        val gradePrefix = text.firstOrNull()?.toString() ?: "D"
                        val gradeNum = text.substring(1).toIntOrNull() ?: 1
                        
                        val (lvlBgColor, textColor) = when {
                            // S/A/B级 - 暖调（暖黄、橙红）
                            gradePrefix == "S" -> Pair(
                                Color.parseColor("#fef3c7"),  // 暖黄
                                Color.parseColor("#b45309")   // 深橙棕
                            )
                            gradePrefix == "A" -> Pair(
                                Color.parseColor("#fed7aa"),  // 浅橙
                                Color.parseColor("#c2410c")   // 橙红
                            )
                            gradePrefix == "B" -> Pair(
                                Color.parseColor("#fecaca"),  // 浅红
                                Color.parseColor("#b91c1c")   // 深红
                            )
                            // C级 - 中性色（青色、蓝绿色）
                            gradePrefix == "C" -> Pair(
                                Color.parseColor("#a7f3d0"),  // 青绿色
                                Color.parseColor("#047857")   // 深青色
                            )
                            // D级 - 冷调（浅灰、淡蓝）
                            gradePrefix == "D" && gradeNum <= 10 -> Pair(
                                Color.parseColor("#e0f2fe"),  // 淡蓝
                                Color.parseColor("#0369a1")   // 深蓝
                            )
                            else -> Pair(
                                Color.parseColor("#f1f5f9"),  // 浅灰
                                Color.parseColor("#64748b")   // 中灰
                            )
                        }
                        
                        val lvlWidth = 45f * scale
                        val lvlHeight = 18f * scale
                        val lvlLeft = centerX - lvlWidth / 2f
                        val lvlTop = centerY - lvlHeight / 2f
                        paint.color = lvlBgColor
                        paint.style = Paint.Style.FILL
                        canvas.drawRoundRect(lvlLeft, lvlTop, lvlLeft + lvlWidth, lvlTop + lvlHeight, 
                            6f * scale, 6f * scale, paint)
                        
                        paint.color = textColor
                        paint.textSize = 11f * scale
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        paint.textAlign = Paint.Align.CENTER
                        canvas.drawText(text, centerX, centerY + 4f * scale, paint)
                    }
                    headerName == "有效时长" -> {
                        // 有效时长显示 - 紫色系
                        paint.textSize = 13f * scale
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        paint.color = Color.parseColor("#7c3aed")  // 紫色
                        paint.textAlign = Paint.Align.CENTER
                        canvas.drawText(text, centerX, centerY, paint)
                    }
                    else -> {
                        paint.textSize = 15f * scale
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        paint.color = textMain
                        paint.textAlign = Paint.Align.CENTER
                        canvas.drawText(text, centerX, centerY, paint)
                    }
                }
            }
            
            currentY += currentRowHeight
        }
        
        // 4. 底部统计与未开播名单合并板块
        if (hasInactive) {
            paint.color = absentBg
            canvas.drawRect(0f, currentY, containerWidth.toFloat(), currentY + footerHeight, paint)
            
            // 极细分割线
            paint.color = Color.parseColor("#fecaca")
            paint.strokeWidth = 0.5f * scale
            canvas.drawLine(0f, currentY, containerWidth.toFloat(), currentY, paint)
            
            val totalStreamers = sortedData.size
            val inactiveCount = inactiveStreamers.size
            
            // 顶部统计数字 - 小号字体
            val statGap = 100f * scale
            val stat1X = containerWidth / 2f - statGap
            val stat2X = containerWidth / 2f + statGap
            val statY = currentY + 25f * scale
            
            // 统计1 - 男主播人数（根据性别过滤）
            val genderText = when (settings.genderFilter) {
                com.example.myapplication.data.model.GenderFilter.MALE -> "男"
                com.example.myapplication.data.model.GenderFilter.FEMALE -> "女"
                else -> ""
            }
            paint.color = absentRed
            paint.textSize = 24f * scale  // 小号数字
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("$totalStreamers", stat1X, statY, paint)
            
            paint.textSize = 10f * scale
            paint.color = absentRed
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("${genderText}主播人数", stat1X, statY + 16f * scale, paint)
            
            // 统计2 - 未开播人数
            paint.color = absentRed
            paint.textSize = 24f * scale  // 小号数字
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("$inactiveCount", stat2X, statY, paint)
            
            paint.textSize = 10f * scale
            paint.color = absentRed
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("未开播人数", stat2X, statY + 16f * scale, paint)
            
            // 未开播名单 - 去除标题，直接显示名单
            val namesY = currentY + 50f * scale
            val names = inactiveStreamers.map { genderUtils.getDisplayName(it.streamerId, it.streamerName) }.joinToString("、")
            paint.color = Color.WHITE
            val namesBgWidth = 300f * scale
            val namesBgHeight = 28f * scale
            val namesBgLeft = containerWidth / 2f - namesBgWidth / 2f
            val namesBgTop = namesY
            canvas.drawRoundRect(namesBgLeft, namesBgTop, namesBgLeft + namesBgWidth, namesBgTop + namesBgHeight, 
                30f * scale, 30f * scale, paint)
            
            paint.color = absentRed
            paint.textSize = 16f * scale
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(names, containerWidth / 2f, namesBgTop + namesBgHeight / 2f + 6f * scale, paint)
            
            currentY += footerHeight
        }
        
        return bitmap
    }
    
    private fun saveToMediaStore(context: Context, workbook: XSSFWorkbook, fileName: String): String {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                workbook.write(outputStream)
            }
        }
        
        return "文件已保存到下载文件夹: $fileName"
    }
    
    private fun saveToExternalStorage(workbook: XSSFWorkbook, fileName: String): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { outputStream ->
            workbook.write(outputStream)
        }
        return "文件已保存: ${file.absolutePath}"
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
    
    private fun formatSoundWave(value: Long): String {
        return when {
            value >= 10000 -> {
                val wan = value / 10000
                val remainder = value % 10000
                if (remainder == 0L) {
                    "${wan}万"
                } else {
                    "${wan}万${remainder}"
                }
            }
            value <= 1 -> "0"
            else -> value.toString()
        }
    }
    
    /**
     * 格式化时长为"x时"格式（只保留小时）
     * 输入格式: "2.5小时" 或 "2小时30分" 或 "150分钟"
     * 输出格式: "2时" 或 "0时"
     */
    private fun formatDurationToHourMinute(duration: String): String {
        return try {
            when {
                // 处理"2.5小时"格式
                duration.contains("小时") && duration.contains(".") -> {
                    val hours = duration.replace("小时", "").toDoubleOrNull() ?: 0.0
                    val totalMinutes = (hours * 60).toInt()
                    val h = totalMinutes / 60
                    "${h}时"
                }
                // 处理"2小时30分"格式
                duration.contains("小时") && duration.contains("分") -> {
                    val parts = duration.split("小时")
                    val h = parts[0].toIntOrNull() ?: 0
                    "${h}时"
                }
                // 处理"2小时"格式
                duration.contains("小时") -> {
                    val h = duration.replace("小时", "").toIntOrNull() ?: 0
                    "${h}时"
                }
                // 处理"150分钟"格式
                duration.contains("分钟") -> {
                    val totalMinutes = duration.replace("分钟", "").toIntOrNull() ?: 0
                    val h = totalMinutes / 60
                    "${h}时"
                }
                // 处理"30分"格式
                duration.contains("分") -> {
                    val m = duration.replace("分", "").toIntOrNull() ?: 0
                    val h = m / 60
                    "${h}时"
                }
                else -> "0时"
            }
        } catch (e: Exception) {
            "0时"
        }
    }
    
    /**
     * 从duration_data表查询时长数据并填充到SoundData中
     * 查询本月的时长数据
     */
    private fun fillDurationData(context: Context, data: List<SoundData>, date: String): List<SoundData> {
        return try {
            val db = AppDatabase.getDatabase(context)
            val durationDao = db.durationDataDao()
            
            // 计算本月的开始和结束日期
            val (monthStart, monthEnd) = getMonthRange(date)
            
            // 查询本月所有的时长数据
            val durationDataList = runBlocking {
                // 查询所有start_date在本月范围内的数据
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
                monthlyData
            }
            
            // 按主播ID分组,累加时长
            val durationMap = mutableMapOf<String, Double>()
            durationDataList.forEach { durationData ->
                val hours = parseDurationToHours(durationData.duration)
                durationMap[durationData.streamerId] = (durationMap[durationData.streamerId] ?: 0.0) + hours
            }
            
            // 填充时长数据
            data.map { soundData ->
                val totalHours = durationMap[soundData.streamerId]
                if (totalHours != null && totalHours > 0) {
                    soundData.copy(effectiveBroadcastDuration = "${totalHours}小时")
                } else {
                    soundData.copy(effectiveBroadcastDuration = "0小时")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ExportUtils", "填充时长数据失败", e)
            // 如果查询失败,返回原数据并设置默认值
            data.map { it.copy(effectiveBroadcastDuration = "0小时") }
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
     * 获取昨日数据
     */
    private fun getPreviousDayData(
        context: Context,
        currentDate: String,
        settings: com.example.myapplication.data.model.ExportSettings
    ): List<SoundData> {
        return try {
            val parts = currentDate.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            
            // 计算昨日日期
            val calendar = java.util.Calendar.getInstance()
            calendar.set(year, month - 1, day)
            calendar.add(java.util.Calendar.DAY_OF_MONTH, -1)
            
            val previousDate = String.format(
                "%04d-%02d-%02d",
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH) + 1,
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
            
            // 从数据库查询昨日数据
            val db = AppDatabase.getDatabase(context)
            val dao = db.soundDataDao()
            // DAO返回Flow，需要使用runBlocking收集数据
            val allData: List<SoundData> = runBlocking {
                dao.getDataByDate(previousDate).first()
            }
            
            // 根据性别过滤
            val genderUtils = GenderUtils.getInstance(context)
            val filteredData: List<SoundData> = when (settings.genderFilter) {
                com.example.myapplication.data.model.GenderFilter.MALE -> {
                    allData.filter { soundData: SoundData -> 
                        genderUtils.getGender(soundData.streamerId) == "男" 
                    }
                }
                com.example.myapplication.data.model.GenderFilter.FEMALE -> {
                    allData.filter { soundData: SoundData -> 
                        genderUtils.getGender(soundData.streamerId) == "女" 
                    }
                }
                else -> allData
            }
            
            // 按音浪降序排序
            filteredData.sortedByDescending { soundData: SoundData -> soundData.soundWave }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 计算排名变化
     * 返回 Map<streamerId, rankChange>
     * rankChange > 0 表示上升，< 0 表示下降，0 表示不变
     */
    private fun calculateRankChanges(
        currentData: List<SoundData>,
        previousData: List<SoundData>
    ): Map<String, Int> {
        val previousRankMap = previousData.mapIndexed { index, data ->
            data.streamerId to (index + 1)
        }.toMap()
        
        return currentData.mapIndexed { index, data ->
            val currentRank = index + 1
            val previousRank = previousRankMap[data.streamerId] ?: 0
            val change = if (previousRank > 0) previousRank - currentRank else 0
            data.streamerId to change
        }.toMap()
    }
}
