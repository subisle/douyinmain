package com.example.myapplication.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.PreferencesManager
import com.example.myapplication.data.model.SoundData
import com.example.myapplication.utils.GenderUtils
import com.example.myapplication.utils.GradeManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

/**
 * 排行榜海报导出工具
 * 基于HTML模板生成排行榜图片
 */
object RankingPosterExportUtils {
    
    /**
     * 导出排行榜海报（两张图片，每张单列）
     * @param context 上下文
     * @param data 主播数据列表
     * @param date 日期
     * @return 导出的图片文件路径列表
     */
    suspend fun exportRankingPosters(
        context: Context,
        data: List<SoundData>,
        date: String
    ): Result<List<String>> {
        return try {
            val prefsManager = PreferencesManager(context)
            
            // 获取设置
            val companyName = prefsManager.getPosterCompanyName()
            val eventTitle = prefsManager.getPosterEventTitle()
            
            // 按总音浪排序
            val sortedData = data.sortedByDescending { it.totalSoundWave }
            
            // 分成两组，每组单列显示
            val midPoint = (sortedData.size + 1) / 2
            val firstGroup = sortedData.take(midPoint)
            val secondGroup = sortedData.drop(midPoint)
            
            val filePaths = mutableListOf<String>()
            
            // 生成第一张图片
            val firstImagePath = generatePosterImage(
                context, firstGroup, date, companyName, eventTitle, 1
            )
            filePaths.add(firstImagePath)
            
            // 生成第二张图片（如果有第二组数据）
            if (secondGroup.isNotEmpty()) {
                val secondImagePath = generatePosterImage(
                    context, secondGroup, date, companyName, eventTitle, 2
                )
                filePaths.add(secondImagePath)
            }
            
            Result.success(filePaths)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 生成单张海报图片
     */
    private suspend fun generatePosterImage(
        context: Context,
        data: List<SoundData>,
        date: String,
        companyName: String,
        eventTitle: String,
        pageNumber: Int
    ): String {
        val html = generateHtml(context, data, date, companyName, eventTitle, pageNumber)
        val bitmap = renderHtmlToBitmap(context, html)
        
        // 保存图片
        val fileName = "排行榜_${date}_第${pageNumber}张.png"
        val cacheDir = context.cacheDir
        val file = File(cacheDir, fileName)
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        
        return file.absolutePath
    }
    
    /**
     * 生成HTML内容
     */
    private fun generateHtml(
        context: Context,
        data: List<SoundData>,
        date: String,
        companyName: String,
        eventTitle: String,
        pageNumber: Int
    ): String {
        val genderUtils = GenderUtils.getInstance(context)
        val prefsManager = PreferencesManager(context)
        val settings = prefsManager.getExportSettings()
        
        // 构建前三名HTML
        val podiumHtml = buildPodiumHtml(context, data.take(3), genderUtils, settings)
        
        // 构建列表HTML（从第4名开始）
        val listHtml = buildListHtml(context, data.drop(3), genderUtils, settings, 4)
        
        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$companyName - 月度榜单</title>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Ma+Shan+Zheng&family=Noto+Sans+SC:wght@400;700;900&display=swap');

        :root {
            --gold-high: #ffecb3;
            --gold-mid: #ffd700;
            --gold-dark: #b8860b;
            --bg-black: #050505;
        }

        body {
            margin: 0;
            padding: 20px;
            background-color: #000;
            display: flex;
            justify-content: center;
            font-family: 'Noto Sans SC', sans-serif;
        }

        .poster {
            width: 750px;
            min-height: 1400px;
            background: 
                radial-gradient(circle at 50% 10%, rgba(255, 215, 0, 0.2) 0%, transparent 60%),
                radial-gradient(circle at 50% 40%, rgba(0, 0, 0, 0.8) 0%, transparent 100%),
                linear-gradient(to bottom, #1a0b00 0%, #000 100%);
            position: relative;
            overflow: hidden;
            box-shadow: 0 0 50px rgba(184, 134, 11, 0.3);
            border: 1px solid #333;
        }

        .poster::before {
            content: "";
            position: absolute;
            top: 0; left: 0; width: 100%; height: 100%;
            background-image: 
                linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
                linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
            background-size: 20px 20px;
            z-index: 0;
            pointer-events: none;
        }

        .header {
            position: relative;
            z-index: 2;
            text-align: center;
            padding-top: 50px;
            margin-bottom: 20px;
        }

        .title-line-1 {
            font-family: 'Ma Shan Zheng', cursive;
            font-size: 60px;
            color: #fff;
            text-shadow: 0 0 10px rgba(255, 215, 0, 0.8);
            margin: 0;
            line-height: 1;
        }

        .title-line-2 {
            font-size: 68px;
            font-weight: 900;
            margin: 10px 0 0 0;
            letter-spacing: 2px;
            text-transform: uppercase;
            background: linear-gradient(to bottom, #fff 10%, #ffd700 40%, #b8860b 60%, #5e2c04 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            filter: drop-shadow(0 4px 0 #3e2706);
            position: relative;
        }

        .podium {
            position: relative;
            z-index: 2;
            display: flex;
            justify-content: center;
            align-items: flex-end;
            height: 320px;
            margin-bottom: 30px;
        }

        .rank-card {
            display: flex;
            flex-direction: column;
            align-items: center;
            position: relative;
            margin: 0 15px;
            text-align: center;
        }

        .avatar-box {
            position: relative;
            border-radius: 50%;
            padding: 4px;
            background: linear-gradient(180deg, #fff, #b8860b);
            box-shadow: 0 0 30px rgba(255, 215, 0, 0.3);
        }

        .avatar-img {
            width: 100%; height: 100%;
            border-radius: 50%;
            object-fit: cover;
            border: 2px solid #000;
            background: #333;
        }

        .decoration {
            position: absolute;
            top: 50%; left: 50%;
            transform: translate(-50%, -50%);
            width: 180%; height: 180%;
            background: radial-gradient(circle, transparent 40%, rgba(255, 215, 0, 0.2) 70%, transparent 80%);
            pointer-events: none;
            z-index: -1;
        }

        .rank-1 .avatar-box { width: 140px; height: 140px; border: 2px solid #fff; z-index: 10; }
        .rank-1 { padding-bottom: 20px; }
        .rank-1 .decoration { 
            border: 1px solid rgba(255,215,0,0.5); border-radius: 50%;
            box-shadow: 0 0 20px rgba(255,215,0,0.5);
        }
        
        .rank-2 .avatar-box, .rank-3 .avatar-box { width: 100px; height: 100px; opacity: 0.9; }

        .player-info { margin-top: 10px; color: #fff; text-shadow: 0 2px 4px #000; }
        .player-name { font-size: 20px; font-weight: bold; display: block; margin-bottom: 4px;}
        .player-grade { font-size: 14px; color: #ffd700; font-family: Arial; display: block; background: rgba(0,0,0,0.6); padding: 2px 8px; border-radius: 10px; border: 1px solid #b8860b;}

        .crown {
            position: absolute; top: -30px; left: 50%; transform: translateX(-50%);
            font-size: 30px; color: #ffd700; text-shadow: 0 0 10px #fff;
        }

        .list-section {
            position: relative;
            z-index: 2;
            padding: 0 30px;
        }

        .list-item {
            display: flex;
            align-items: center;
            height: 40px;
            background: linear-gradient(90deg, rgba(0,0,0,0.8) 0%, rgba(50,30,0,0.6) 50%, rgba(0,0,0,0) 100%);
            border-bottom: 1px solid #b8860b;
            border-left: 2px solid #b8860b;
            padding: 0 10px;
            position: relative;
            margin-bottom: 10px;
        }

        .rank-num {
            font-size: 24px;
            font-weight: 900;
            font-style: italic;
            width: 40px;
            color: #fff;
            text-shadow: 2px 2px 0 #b8860b;
        }

        .list-name {
            flex: 1;
            color: #fff;
            font-size: 16px;
            font-weight: bold;
            white-space: nowrap;
        }

        .list-grade {
            font-family: Arial, sans-serif;
            font-size: 14px;
            color: #ffd700;
            opacity: 0.8;
        }

        .footer-glow {
            position: absolute; bottom: 0; width: 100%; height: 200px;
            background: linear-gradient(to top, rgba(184, 134, 11, 0.3), transparent);
            pointer-events: none;
            z-index: 1;
        }

    </style>
</head>
<body>

<div class="poster">
    
    <div class="header">
        <h1 class="title-line-1">$companyName</h1>
        <h2 class="title-line-2">$eventTitle</h2>
    </div>

    $podiumHtml

    <div class="list-section">
        $listHtml
    </div>

    <div class="footer-glow"></div>
</div>

</body>
</html>
        """.trimIndent()
    }
    
    /**
     * 构建前三名的HTML
     */
    private fun buildPodiumHtml(
        context: Context,
        topThree: List<SoundData>,
        genderUtils: GenderUtils,
        settings: com.example.myapplication.data.model.ExportSettings
    ): String {
        if (topThree.isEmpty()) return ""
        
        val rank2Html = if (topThree.size >= 2) {
            val data = topThree[1]
            val avatarPath = getAvatarPath(context, data.streamerId)
            val grade = GradeManager.getGradeLetter(data.totalSoundWave, settings)
            """
            <div class="rank-card rank-2">
                <div class="avatar-box">
                    <img src="$avatarPath" class="avatar-img" alt="${data.streamerName}">
                </div>
                <div class="player-info">
                    <span class="player-name">${genderUtils.getDisplayName(data.streamerId, data.streamerName)}</span>
                    <span class="player-grade">$grade</span>
                </div>
            </div>
            """
        } else ""
        
        val rank1Html = if (topThree.isNotEmpty()) {
            val data = topThree[0]
            val avatarPath = getAvatarPath(context, data.streamerId)
            val grade = GradeManager.getGradeLetter(data.totalSoundWave, settings)
            """
            <div class="rank-card rank-1">
                <div class="crown">👑</div>
                <div class="decoration"></div>
                <div class="avatar-box">
                    <img src="$avatarPath" class="avatar-img" alt="${data.streamerName}">
                </div>
                <div class="player-info">
                    <span class="player-name" style="font-size: 26px; color: #ffd700;">${genderUtils.getDisplayName(data.streamerId, data.streamerName)}</span>
                    <span class="player-grade">$grade</span>
                </div>
            </div>
            """
        } else ""
        
        val rank3Html = if (topThree.size >= 3) {
            val data = topThree[2]
            val avatarPath = getAvatarPath(context, data.streamerId)
            val grade = GradeManager.getGradeLetter(data.totalSoundWave, settings)
            """
            <div class="rank-card rank-3">
                <div class="avatar-box">
                    <img src="$avatarPath" class="avatar-img" alt="${data.streamerName}">
                </div>
                <div class="player-info">
                    <span class="player-name">${genderUtils.getDisplayName(data.streamerId, data.streamerName)}</span>
                    <span class="player-grade">$grade</span>
                </div>
            </div>
            """
        } else ""
        
        return """
        <div class="podium">
            $rank2Html
            $rank1Html
            $rank3Html
        </div>
        """
    }
    
    /**
     * 构建列表HTML
     */
    private fun buildListHtml(
        context: Context,
        data: List<SoundData>,
        genderUtils: GenderUtils,
        settings: com.example.myapplication.data.model.ExportSettings,
        startRank: Int
    ): String {
        return data.mapIndexed { index, item ->
            val rank = startRank + index
            val grade = GradeManager.getGradeLetter(item.totalSoundWave, settings)
            """
            <div class="list-item">
                <span class="rank-num">$rank</span>
                <span class="list-name">${genderUtils.getDisplayName(item.streamerId, item.streamerName)}</span>
                <span class="list-grade">$grade</span>
            </div>
            """
        }.joinToString("\n")
    }
    
    /**
     * 获取头像路径
     */
    private fun getAvatarPath(context: Context, streamerId: String): String {
        // 检查多个可能的头像位置
        val possiblePaths = listOf(
            // 应用内部存储
            File(context.filesDir, "png/$streamerId.png"),
            // 外部存储
            File(context.getExternalFilesDir(null), "png/$streamerId.png"),
            // 项目根目录的png文件夹（开发环境）
            File("/storage/emulated/0/png/$streamerId.png")
        )
        
        for (file in possiblePaths) {
            if (file.exists()) {
                return "file://${file.absolutePath}"
            }
        }
        
        // 使用占位图
        return "https://via.placeholder.com/150/333/fff?text=${streamerId.take(3)}"
    }
    
    /**
     * 将HTML渲染为Bitmap
     */
    private suspend fun renderHtmlToBitmap(context: Context, html: String): Bitmap {
        return suspendCancellableCoroutine { continuation ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            
                            // 等待渲染完成后截图
                            view?.postDelayed({
                                try {
                                    val bitmap = Bitmap.createBitmap(
                                        view.width,
                                        view.contentHeight,
                                        Bitmap.Config.ARGB_8888
                                    )
                                    val canvas = android.graphics.Canvas(bitmap)
                                    view.draw(canvas)
                                    
                                    if (continuation.isActive) {
                                        continuation.resume(bitmap)
                                    }
                                } catch (e: Exception) {
                                    if (continuation.isActive) {
                                        continuation.cancel(e)
                                    }
                                }
                            }, 1000)
                        }
                    }
                    
                    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                }
                
                continuation.invokeOnCancellation {
                    webView.destroy()
                }
            }
        }
    }
}
