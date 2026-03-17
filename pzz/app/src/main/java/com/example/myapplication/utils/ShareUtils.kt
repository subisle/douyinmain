package com.example.myapplication.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * 分享工具类
 */
object ShareUtils {
    
    /**
     * 分享图片到微信
     * @param context 上下文
     * @param imagePath 图片路径
     * @return 是否成功启动分享
     */
    fun shareImageToWeChat(context: Context, imagePath: String): Boolean {
        return try {
            val file = File(imagePath)
            if (!file.exists()) {
                return false
            }
            
            // 使用FileProvider获取Uri
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            // 创建分享Intent
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                
                // 尝试直接打开微信
                setPackage("com.tencent.mm")
            }
            
            // 检查微信是否安装
            val packageManager = context.packageManager
            val resolveInfo = packageManager.resolveActivity(intent, 0)
            
            if (resolveInfo != null) {
                // 微信已安装，直接打开
                context.startActivity(intent)
                true
            } else {
                // 微信未安装，使用系统分享
                shareImageToSystem(context, imagePath)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 分享图片到系统（显示所有可用的分享应用）
     * @param context 上下文
     * @param imagePath 图片路径
     * @return 是否成功启动分享
     */
    fun shareImageToSystem(context: Context, imagePath: String): Boolean {
        return try {
            val file = File(imagePath)
            if (!file.exists()) {
                return false
            }
            
            // 使用FileProvider获取Uri
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            // 创建分享Intent
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // 使用选择器显示所有可用的分享应用
            val chooser = Intent.createChooser(intent, "分享图片到")
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 分享文件到微信
     * @param context 上下文
     * @param filePath 文件路径
     * @param mimeType 文件MIME类型
     * @return 是否成功启动分享
     */
    fun shareFileToWeChat(context: Context, filePath: String, mimeType: String = "*/*"): Boolean {
        return try {
            android.util.Log.d("ShareUtils", "准备分享文件: $filePath, MIME类型: $mimeType")
            
            val file = File(filePath)
            if (!file.exists()) {
                android.util.Log.e("ShareUtils", "文件不存在: $filePath")
                return false
            }
            
            android.util.Log.d("ShareUtils", "文件存在，大小: ${file.length()} bytes")
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            android.util.Log.d("ShareUtils", "获取到URI: $uri")
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.tencent.mm")
            }
            
            val packageManager = context.packageManager
            val resolveInfo = packageManager.resolveActivity(intent, 0)
            
            if (resolveInfo != null) {
                android.util.Log.d("ShareUtils", "微信已安装，启动分享")
                context.startActivity(intent)
                true
            } else {
                android.util.Log.d("ShareUtils", "微信未安装，使用系统分享")
                shareFileToSystem(context, filePath, mimeType)
            }
        } catch (e: Exception) {
            android.util.Log.e("ShareUtils", "分享文件失败", e)
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 分享文件到系统
     * @param context 上下文
     * @param filePath 文件路径
     * @param mimeType 文件MIME类型
     * @return 是否成功启动分享
     */
    fun shareFileToSystem(context: Context, filePath: String, mimeType: String = "*/*"): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return false
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, "分享文件到")
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 检查微信是否已安装
     * @param context 上下文
     * @return 是否已安装
     */
    fun isWeChatInstalled(context: Context): Boolean {
        return try {
            val packageManager = context.packageManager
            packageManager.getPackageInfo("com.tencent.mm", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
