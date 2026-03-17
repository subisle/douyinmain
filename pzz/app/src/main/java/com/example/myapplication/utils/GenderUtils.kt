package com.example.myapplication.utils

import android.content.Context
import com.example.myapplication.data.model.Gender
import com.example.myapplication.data.model.Streamer
import com.example.myapplication.data.remote.RemoteDatabaseManagerV2
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 性别判断工具
 * 支持从数据库同步主播名单到本地缓存
 */
class GenderUtils private constructor(private val context: Context) {
    
    private var maleIds: Set<String>
    private var femaleIds: Set<String>
    private var nicknameMap: Map<String, String>  // ID -> 昵称的映射
    private var lastSyncTime: Long = 0L
    
    private val cacheFile: File
        get() = File(context.filesDir, "user_cache.json")
    
    private val syncTimeFile: File
        get() = File(context.filesDir, "user_sync_time.txt")
    
    init {
        // 只从缓存加载（数据库同步的数据）
        val userData = loadFromCache()
        
        if (userData != null) {
            maleIds = userData.males.map { it.id }.toSet()
            femaleIds = userData.females.map { it.id }.toSet()
            
            // 构建ID到昵称的映射
            val nicknames = mutableMapOf<String, String>()
            userData.males.forEach { nicknames[it.id] = it.name }
            userData.females.forEach { nicknames[it.id] = it.name }
            nicknameMap = nicknames
            
            android.util.Log.d(TAG, "GenderUtils初始化完成: 男性${maleIds.size}人, 女性${femaleIds.size}人, 总计${nicknameMap.size}人")
        } else {
            // 如果缓存不存在，初始化为空
            maleIds = emptySet()
            femaleIds = emptySet()
            nicknameMap = emptyMap()
            android.util.Log.w(TAG, "GenderUtils初始化: 缓存不存在，等待首次同步")
        }
        
        // 读取上次同步时间
        lastSyncTime = readSyncTime()
        android.util.Log.d(TAG, "数据源: 数据库缓存, 上次同步: ${getLastSyncTimeString()}")
    }
    
    /**
     * 从本地缓存加载（数据库同步的数据）
     */
    private fun loadFromCache(): UserData? {
        return try {
            if (cacheFile.exists()) {
                val json = cacheFile.readText()
                Gson().fromJson(json, UserData::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "从缓存加载失败", e)
            null
        }
    }
    
    /**
     * 从数据库同步主播名单
     */
    suspend fun syncFromDatabase(): Result<String> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d(TAG, "开始从数据库同步主播名单")
            
            // 从数据库获取主播列表
            val result = RemoteDatabaseManagerV2.fetchAllStreamers()
            
            if (result.isFailure) {
                return@withContext Result.failure(result.exceptionOrNull() ?: Exception("同步失败"))
            }
            
            val streamers = result.getOrNull() ?: emptyList()
            
            // 转换为UserData格式
            val males = streamers.filter { it.gender == Gender.MALE }.map {
                User(
                    serialNumber = it.serialNumber,
                    id = it.streamerId,
                    name = it.nickname
                )
            }
            
            val females = streamers.filter { it.gender == Gender.FEMALE }.map {
                User(
                    serialNumber = it.serialNumber,
                    id = it.streamerId,
                    name = it.nickname
                )
            }
            
            val userData = UserData(males = males, females = females)
            
            // 保存到缓存
            val json = Gson().toJson(userData)
            cacheFile.writeText(json)
            
            // 更新内存中的数据
            maleIds = males.map { it.id }.toSet()
            femaleIds = females.map { it.id }.toSet()
            
            val nicknames = mutableMapOf<String, String>()
            males.forEach { nicknames[it.id] = it.name }
            females.forEach { nicknames[it.id] = it.name }
            nicknameMap = nicknames
            
            // 保存同步时间
            lastSyncTime = System.currentTimeMillis()
            saveSyncTime(lastSyncTime)
            
            android.util.Log.d(TAG, "同步成功，共 ${streamers.size} 个主播")
            Result.success("同步成功，共 ${streamers.size} 个主播")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "同步失败", e)
            Result.failure(Exception("同步失败: ${e.message}", e))
        }
    }
    
    /**
     * 读取上次同步时间
     */
    private fun readSyncTime(): Long {
        return try {
            if (syncTimeFile.exists()) {
                syncTimeFile.readText().toLongOrNull() ?: 0L
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 保存同步时间
     */
    private fun saveSyncTime(time: Long) {
        try {
            syncTimeFile.writeText(time.toString())
        } catch (e: Exception) {
            android.util.Log.e(TAG, "保存同步时间失败", e)
        }
    }
    
    /**
     * 获取上次同步时间
     */
    fun getLastSyncTime(): Long = lastSyncTime
    
    /**
     * 获取上次同步时间的格式化字符串
     */
    fun getLastSyncTimeString(): String {
        return if (lastSyncTime == 0L) {
            "从未同步"
        } else {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            sdf.format(java.util.Date(lastSyncTime))
        }
    }
    
    /**
     * 根据主播ID判断性别（支持前10位模糊匹配）
     * @param streamerId 主播ID
     * @return true=男性, false=女性, null=无法判断
     */
    fun inferGenderById(streamerId: String): Boolean? {
        // 先尝试精确匹配
        when {
            maleIds.contains(streamerId) -> return true
            femaleIds.contains(streamerId) -> return false
        }
        
        // 如果精确匹配失败，尝试前10位模糊匹配
        if (streamerId.length >= 10) {
            val prefix = streamerId.substring(0, 10)
            
            // 检查男性ID
            maleIds.forEach { id ->
                if (id.startsWith(prefix)) {
                    return true
                }
            }
            
            // 检查女性ID
            femaleIds.forEach { id ->
                if (id.startsWith(prefix)) {
                    return false
                }
            }
        }
        
        return null
    }
    
    /**
     * 根据名字判断性别（备用方案）
     */
    fun inferGenderByName(name: String): Boolean? {
        if (name.isEmpty()) return null
        
        // 常见男性名字关键字
        val maleKeywords = setOf(
            "哥", "兄", "爷", "叔", "伯", "公", "郎", "男", "帅", "俊", "强", "刚", "勇", "威", "龙", "虎", "豹", "狼",
            "鹏", "鹤", "鹰", "雄", "杰", "峰", "磊", "涛", "浩", "宇", "轩", "昊", "博", "文", "武", "军", "斌"
        )
        
        // 常见女性名字关键字
        val femaleKeywords = setOf(
            "姐", "妹", "姨", "姑", "娘", "女", "妃", "嫣", "婷", "娜", "丽", "美", "艳", "芳", "花", "莲", "兰", "梅",
            "雪", "霜", "月", "星", "云", "凤", "燕", "莺", "蝶", "玉", "珠", "琳", "瑶", "萱", "薇", "蕊", "婉", "柔", "宝"
        )
        
        val hasMaleKeyword = maleKeywords.any { name.contains(it) }
        val hasFemaleKeyword = femaleKeywords.any { name.contains(it) }
        
        return when {
            hasMaleKeyword && !hasFemaleKeyword -> true
            hasFemaleKeyword && !hasMaleKeyword -> false
            else -> null
        }
    }
    
    /**
     * 综合判断性别（优先使用ID，其次使用名字）
     */
    fun inferGender(streamerId: String, streamerName: String): Boolean? {
        // 优先使用ID判断
        val genderById = inferGenderById(streamerId)
        if (genderById != null) {
            return genderById
        }
        
        // 备用：使用名字判断
        return inferGenderByName(streamerName)
    }
    
    /**
     * 获取所有主播列表
     * @return Map<主播ID, 昵称>
     */
    fun getAllStreamers(): Map<String, String> {
        return nicknameMap
    }
    
    /**
     * 获取所有主播列表（包含性别信息）
     * @return List<Streamer>
     */
    fun getAllStreamersWithGender(): List<Streamer> {
        val streamers = mutableListOf<Streamer>()
        
        // 添加男性主播
        maleIds.forEachIndexed { index, id ->
            val nickname = nicknameMap[id] ?: id
            streamers.add(
                Streamer(
                    serialNumber = index + 1,
                    streamerId = id,
                    nickname = nickname,
                    gender = Gender.MALE
                )
            )
        }
        
        // 添加女性主播
        femaleIds.forEachIndexed { index, id ->
            val nickname = nicknameMap[id] ?: id
            streamers.add(
                Streamer(
                    serialNumber = maleIds.size + index + 1,
                    streamerId = id,
                    nickname = nickname,
                    gender = Gender.FEMALE
                )
            )
        }
        
        return streamers.sortedBy { it.serialNumber }
    }
    
    /**
     * 添加主播（同步到本地缓存和远程数据库）
     */
    suspend fun addStreamer(streamer: Streamer): Result<String> {
        return try {
            // 检查是否已存在
            if (nicknameMap.containsKey(streamer.streamerId)) {
                return Result.failure(Exception("主播ID已存在"))
            }
            
            // 1. 先同步到远程数据库
            val remoteResult = RemoteDatabaseManagerV2.addStreamer(streamer)
            if (remoteResult.isFailure) {
                android.util.Log.w(TAG, "远程添加失败: ${remoteResult.exceptionOrNull()?.message}")
                // 继续添加到本地，但返回警告
            }
            
            // 2. 添加到本地缓存
            val newMaleIds = maleIds.toMutableSet()
            val newFemaleIds = femaleIds.toMutableSet()
            val newNicknameMap = nicknameMap.toMutableMap()
            
            when (streamer.gender) {
                Gender.MALE -> newMaleIds.add(streamer.streamerId)
                Gender.FEMALE -> newFemaleIds.add(streamer.streamerId)
            }
            newNicknameMap[streamer.streamerId] = streamer.nickname
            
            // 更新内存
            maleIds = newMaleIds
            femaleIds = newFemaleIds
            nicknameMap = newNicknameMap
            
            // 保存到缓存
            saveToCache()
            
            android.util.Log.d(TAG, "添加主播成功: ${streamer.nickname} (${streamer.streamerId})")
            
            if (remoteResult.isSuccess) {
                Result.success("添加成功（已同步到远程数据库）")
            } else {
                Result.success("添加成功（本地已保存，远程同步失败）")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "添加主播失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 更新主播（同步到本地缓存和远程数据库）
     */
    suspend fun updateStreamer(oldId: String, newStreamer: Streamer): Result<String> {
        return try {
            // 检查是否存在
            if (!nicknameMap.containsKey(oldId)) {
                return Result.failure(Exception("主播不存在"))
            }
            
            // 如果ID改变了，检查新ID是否已存在
            if (oldId != newStreamer.streamerId && nicknameMap.containsKey(newStreamer.streamerId)) {
                return Result.failure(Exception("新的主播ID已存在"))
            }
            
            // 1. 先同步到远程数据库
            val remoteResult = if (oldId == newStreamer.streamerId) {
                // ID没有变化，使用普通更新
                RemoteDatabaseManagerV2.updateStreamer(newStreamer)
            } else {
                // ID有变化，使用支持ID变更的更新方法
                RemoteDatabaseManagerV2.updateStreamerWithIdChange(oldId, newStreamer)
            }
            
            if (remoteResult.isFailure) {
                android.util.Log.w(TAG, "远程更新失败: ${remoteResult.exceptionOrNull()?.message}")
                // 继续更新本地，但返回警告
            }
            
            // 2. 更新本地缓存
            val newMaleIds = maleIds.toMutableSet()
            val newFemaleIds = femaleIds.toMutableSet()
            val newNicknameMap = nicknameMap.toMutableMap()
            
            // 删除旧的
            newMaleIds.remove(oldId)
            newFemaleIds.remove(oldId)
            newNicknameMap.remove(oldId)
            
            // 添加新的
            when (newStreamer.gender) {
                Gender.MALE -> newMaleIds.add(newStreamer.streamerId)
                Gender.FEMALE -> newFemaleIds.add(newStreamer.streamerId)
            }
            newNicknameMap[newStreamer.streamerId] = newStreamer.nickname
            
            // 更新内存
            maleIds = newMaleIds
            femaleIds = newFemaleIds
            nicknameMap = newNicknameMap
            
            // 保存到缓存
            saveToCache()
            
            android.util.Log.d(TAG, "更新主播成功: ${newStreamer.nickname} (${newStreamer.streamerId})")
            
            if (remoteResult.isSuccess) {
                Result.success("更新成功（已同步到远程数据库）")
            } else {
                Result.success("更新成功（本地已保存，远程同步失败）")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "更新主播失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 删除主播（同步到本地缓存和远程数据库）
     */
    suspend fun deleteStreamer(streamerId: String): Result<String> {
        return try {
            // 检查是否存在
            if (!nicknameMap.containsKey(streamerId)) {
                return Result.failure(Exception("主播不存在"))
            }
            
            val nickname = nicknameMap[streamerId]
            
            // 1. 先同步到远程数据库
            val remoteResult = RemoteDatabaseManagerV2.deleteStreamer(streamerId)
            if (remoteResult.isFailure) {
                android.util.Log.w(TAG, "远程删除失败: ${remoteResult.exceptionOrNull()?.message}")
                // 继续删除本地，但返回警告
            }
            
            // 2. 删除本地缓存
            val newMaleIds = maleIds.toMutableSet()
            val newFemaleIds = femaleIds.toMutableSet()
            val newNicknameMap = nicknameMap.toMutableMap()
            
            newMaleIds.remove(streamerId)
            newFemaleIds.remove(streamerId)
            newNicknameMap.remove(streamerId)
            
            // 更新内存
            maleIds = newMaleIds
            femaleIds = newFemaleIds
            nicknameMap = newNicknameMap
            
            // 保存到缓存
            saveToCache()
            
            android.util.Log.d(TAG, "删除主播成功: $nickname ($streamerId)")
            
            if (remoteResult.isSuccess) {
                Result.success("删除成功（已同步到远程数据库）")
            } else {
                Result.success("删除成功（本地已保存，远程同步失败）")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "删除主播失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 保存到缓存
     */
    private fun saveToCache() {
        try {
            val males = maleIds.mapIndexed { index, id ->
                User(
                    serialNumber = index + 1,
                    id = id,
                    name = nicknameMap[id] ?: id
                )
            }
            
            val females = femaleIds.mapIndexed { index, id ->
                User(
                    serialNumber = index + 1,
                    id = id,
                    name = nicknameMap[id] ?: id
                )
            }
            
            val userData = UserData(males, females)
            val json = Gson().toJson(userData)
            cacheFile.writeText(json)
            
            android.util.Log.d(TAG, "保存缓存成功")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "保存缓存失败", e)
        }
    }
    
    /**
     * 根据主播ID获取昵称（支持前10位模糊匹配）
     * @param streamerId 主播ID
     * @return 昵称，如果找不到则返回null
     */
    fun getNickname(streamerId: String): String? {
        // 先尝试精确匹配
        val exactMatch = nicknameMap[streamerId]
        if (exactMatch != null) {
            return exactMatch
        }
        
        // 如果精确匹配失败，尝试前10位模糊匹配
        if (streamerId.length >= 10) {
            val prefix = streamerId.substring(0, 10)
            nicknameMap.forEach { (id, nickname) ->
                if (id.startsWith(prefix)) {
                    android.util.Log.d(TAG, "模糊匹配成功: CSV_ID=$streamerId -> DB_ID=$id, 昵称=$nickname")
                    return nickname
                }
            }
        }
        
        // 记录未匹配的ID（但限制日志数量）
        if (streamerId.isNotEmpty() && Math.random() < 0.1) {  // 只记录10%的未匹配ID
            android.util.Log.d(TAG, "未找到昵称: ID=$streamerId, 名单大小=${nicknameMap.size}")
        }
        
        return null
    }
    
    /**
     * 获取显示名称（使用数据库同步的昵称）
     * @param streamerId 主播ID
     * @param dbName 数据库中的名字（备用）
     * @return 显示名称
     */
    fun getDisplayName(streamerId: String, dbName: String): String {
        // 使用数据库同步的昵称
        val nickname = getNickname(streamerId)
        if (nickname != null) {
            return nickname
        }
        
        // 如果缓存中没有，使用数据库中的名字作为备用
        return if (dbName.isNotEmpty()) dbName else streamerId
    }
    
    /**
     * 获取性别字符串
     * @param streamerId 主播ID
     * @return "男"/"女"/""
     */
    fun getGender(streamerId: String): String {
        return when {
            maleIds.contains(streamerId) -> "男"
            femaleIds.contains(streamerId) -> "女"
            else -> {
                // 尝试前10位模糊匹配
                if (streamerId.length >= 10) {
                    val prefix = streamerId.substring(0, 10)
                    
                    // 检查男性ID
                    maleIds.forEach { id ->
                        if (id.startsWith(prefix)) {
                            return "男"
                        }
                    }
                    
                    // 检查女性ID
                    femaleIds.forEach { id ->
                        if (id.startsWith(prefix)) {
                            return "女"
                        }
                    }
                }
                ""
            }
        }
    }
    
    /**
     * 清除本地主播缓存
     */
    fun clearCache(): Result<String> {
        return try {
            var deletedCount = 0
            
            // 删除主播缓存文件
            if (cacheFile.exists()) {
                cacheFile.delete()
                deletedCount++
                android.util.Log.d(TAG, "已删除主播缓存文件")
            }
            
            // 删除同步时间文件
            if (syncTimeFile.exists()) {
                syncTimeFile.delete()
                deletedCount++
                android.util.Log.d(TAG, "已删除同步时间文件")
            }
            
            // 清空内存数据
            maleIds = emptySet()
            femaleIds = emptySet()
            nicknameMap = emptyMap()
            lastSyncTime = 0L
            
            android.util.Log.d(TAG, "已清空内存中的主播数据")
            Result.success("已清除主播缓存（删除 $deletedCount 个文件）")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "清除缓存失败", e)
            Result.failure(e)
        }
    }
    
    companion object {
        private const val TAG = "GenderUtils"
        
        @Volatile
        private var instance: GenderUtils? = null
        
        fun getInstance(context: Context): GenderUtils {
            return instance ?: synchronized(this) {
                instance ?: GenderUtils(context.applicationContext).also { instance = it }
            }
        }
        
        /**
         * 获取性别显示文本
         */
        fun getGenderText(isMale: Boolean?): String {
            return when (isMale) {
                true -> "男"
                false -> "女"
                null -> "未知"
            }
        }
    }
    
    // 数据类
    data class UserData(
        val males: List<User>,
        val females: List<User>
    )
    
    data class User(
        @SerializedName("serial_number")
        val serialNumber: Int,
        val id: String,
        val name: String
    )
}
