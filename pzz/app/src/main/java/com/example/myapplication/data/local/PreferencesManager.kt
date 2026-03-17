package com.example.myapplication.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.data.model.ExportSettings
import com.example.myapplication.data.model.SortField
import com.example.myapplication.data.model.SortOrder
import com.google.gson.Gson

/**
 * SharedPreferences管理器
 * 用于存储应用设置和缓存
 */
class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "pzz_preferences"
        
        // 导出设置
        private const val KEY_EXPORT_SETTINGS = "export_settings"
        
        // 首次启动标记
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
        
        // 海报设置
        private const val KEY_POSTER_COMPANY_NAME = "poster_company_name"
        private const val KEY_POSTER_EVENT_TITLE = "poster_event_title"
        
        // 缓存
        private const val KEY_CACHE_PREFIX = "cache_"
        private const val KEY_CACHE_TIMESTAMP_PREFIX = "cache_timestamp_"
        const val CACHE_EXPIRY_MS = 5 * 60 * 1000L // 5分钟缓存 - 改为public
        
        @Volatile
        private var instance: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // ========== 导出设置 ==========
    
    fun saveExportSettings(settings: ExportSettings) {
        val json = gson.toJson(settings)
        prefs.edit().putString(KEY_EXPORT_SETTINGS, json).apply()
    }
    
    fun getExportSettings(): ExportSettings {
        val json = prefs.getString(KEY_EXPORT_SETTINGS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, ExportSettings::class.java)
            } catch (e: Exception) {
                ExportSettings()
            }
        } else {
            ExportSettings()
        }
    }
    
    // ========== 数据缓存 ==========
    
    fun <T> cacheData(key: String, data: T) {
        val json = gson.toJson(data)
        val cacheKey = KEY_CACHE_PREFIX + key
        val timestampKey = KEY_CACHE_TIMESTAMP_PREFIX + key
        
        prefs.edit()
            .putString(cacheKey, json)
            .putLong(timestampKey, System.currentTimeMillis())
            .apply()
    }
    
    // 改为普通函数，不使用inline和reified
    fun <T> getCachedData(key: String, clazz: Class<T>): T? {
        val cacheKey = KEY_CACHE_PREFIX + key
        val timestampKey = KEY_CACHE_TIMESTAMP_PREFIX + key
        
        val timestamp = prefs.getLong(timestampKey, 0)
        val currentTime = System.currentTimeMillis()
        
        // 检查缓存是否过期
        if (currentTime - timestamp > CACHE_EXPIRY_MS) {
            clearCache(key)
            return null
        }
        
        val json = prefs.getString(cacheKey, null) ?: return null
        
        return try {
            gson.fromJson(json, clazz)
        } catch (e: Exception) {
            clearCache(key)
            null
        }
    }
    
    fun clearCache(key: String) {
        val cacheKey = KEY_CACHE_PREFIX + key
        val timestampKey = KEY_CACHE_TIMESTAMP_PREFIX + key
        
        prefs.edit()
            .remove(cacheKey)
            .remove(timestampKey)
            .apply()
    }
    
    fun clearAllCache() {
        val editor = prefs.edit()
        prefs.all.keys.forEach { key ->
            if (key.startsWith(KEY_CACHE_PREFIX) || key.startsWith(KEY_CACHE_TIMESTAMP_PREFIX)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }
    
    // ========== 海报设置 ==========
    
    fun setPosterCompanyName(name: String) {
        prefs.edit().putString(KEY_POSTER_COMPANY_NAME, name).apply()
    }
    
    fun getPosterCompanyName(): String {
        return prefs.getString(KEY_POSTER_COMPANY_NAME, "星嗨艺创") ?: "星嗨艺创"
    }
    
    fun setPosterEventTitle(title: String) {
        prefs.edit().putString(KEY_POSTER_EVENT_TITLE, title).apply()
    }
    
    fun getPosterEventTitle(): String {
        return prefs.getString(KEY_POSTER_EVENT_TITLE, "月度PK挑战赛排名") ?: "月度PK挑战赛排名"
    }
    
    // ========== 首次启动 ==========
    
    /**
     * 检查是否首次启动
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    /**
     * 标记首次启动已完成
     */
    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }
    
    // ========== 通用设置 ==========
    
    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    
    fun getString(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }
    
    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
    
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }
    
    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }
    
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }
}
