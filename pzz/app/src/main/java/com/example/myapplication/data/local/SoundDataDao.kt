package com.example.myapplication.data.local

import androidx.room.*
import com.example.myapplication.data.model.SoundData
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundDataDao {
    @Query("SELECT * FROM streamer_data WHERE record_date = :date ORDER BY overall_rank ASC")
    fun getDataByDate(date: String): Flow<List<SoundData>>
    
    @Query("SELECT * FROM streamer_data WHERE record_date = :date ORDER BY overall_rank ASC")
    suspend fun getDataByDateSync(date: String): List<SoundData>
    
    @Query("SELECT * FROM streamer_data WHERE id = :id")
    suspend fun getDataById(id: Long): SoundData?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(data: List<SoundData>): List<Long>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: SoundData): Long
    
    @Update
    suspend fun update(data: SoundData): Int
    
    /**
     * 根据streamer_id和record_date查询数据
     */
    @Query("SELECT * FROM streamer_data WHERE streamer_id = :streamerId AND record_date = :recordDate LIMIT 1")
    suspend fun getByStreamerIdAndDate(streamerId: String, recordDate: String): SoundData?
    
    /**
     * 查询指定主播在指定日期之前的最新数据
     * 用于导入时获取最新的总音浪
     */
    @Query("""
        SELECT * FROM streamer_data 
        WHERE streamer_id = :streamerId 
        AND record_date < :beforeDate 
        ORDER BY record_date DESC 
        LIMIT 1
    """)
    suspend fun getLatestDataBeforeDate(streamerId: String, beforeDate: String): SoundData?
    
    /**
     * 根据ID前8位和日期查询数据
     */
    @Query("SELECT * FROM streamer_data WHERE SUBSTR(streamer_id, 1, 8) = :idPrefix AND record_date = :recordDate LIMIT 1")
    suspend fun getByIdPrefixAndDate(idPrefix: String, recordDate: String): SoundData?
    
    /**
     * 导入或更新数据（如果streamer_id+record_date已存在则覆盖）
     */
    suspend fun insertOrUpdate(data: SoundData): Long {
        val existing = getByStreamerIdAndDate(data.streamerId, data.recordDate)
        return if (existing != null) {
            // 存在则更新，保留原有的id
            val updated = data.copy(id = existing.id)
            android.util.Log.d("SoundDataDao", "更新数据: ID=${data.streamerId}, 名字=${data.streamerName}, 日音浪=${data.soundWave}, 总音浪=${data.totalSoundWave}")
            update(updated)
            existing.id
        } else {
            // 不存在则插入
            android.util.Log.d("SoundDataDao", "插入数据: ID=${data.streamerId}, 名字=${data.streamerName}, 日音浪=${data.soundWave}, 总音浪=${data.totalSoundWave}")
            insert(data)
        }
    }
    
    /**
     * 批量导入或更新数据
     */
    suspend fun insertOrUpdateAll(dataList: List<SoundData>): List<Long> {
        return dataList.map { insertOrUpdate(it) }
    }
    
    @Delete
    suspend fun delete(data: SoundData): Int
    
    @Query("DELETE FROM streamer_data WHERE record_date = :date")
    suspend fun deleteByDate(date: String): Int
    
    @Query("DELETE FROM streamer_data")
    suspend fun deleteAll(): Int
    
    @Query("SELECT COUNT(*) FROM streamer_data WHERE record_date = :date")
    suspend fun getCountByDate(date: String): Int
    
    @Query("SELECT COUNT(*) FROM streamer_data WHERE record_date = :date AND sound_wave > 1")
    suspend fun getActiveCountByDate(date: String): Int
    
    @Query("SELECT COALESCE(SUM(sound_wave), 0) FROM streamer_data WHERE record_date = :date")
    suspend fun getTotalSoundWaveByDate(date: String): Long
    
    /**
     * 获取数据库中最新的日期
     */
    @Query("SELECT record_date FROM streamer_data ORDER BY record_date DESC LIMIT 1")
    suspend fun getLatestDate(): String?
    
    /**
     * 根据ID前缀更新总音浪
     * 用于从主播榜CSV更新总音浪数据
     */
    @Query("""
        UPDATE streamer_data 
        SET total_sound_wave = :totalSoundWave 
        WHERE SUBSTR(streamer_id, 1, 10) = :idPrefix 
        AND record_date = :recordDate
    """)
    suspend fun updateTotalSoundWaveByIdPrefix(
        idPrefix: String,
        totalSoundWave: Long,
        recordDate: String
    ): Int
    
    /**
     * 根据ID前缀更新日音浪
     * 用于数据校准，不影响总音浪
     */
    @Query("""
        UPDATE streamer_data 
        SET sound_wave = :soundWave 
        WHERE SUBSTR(streamer_id, 1, 10) = :idPrefix 
        AND record_date = :recordDate
    """)
    suspend fun updateDailySoundWaveByIdPrefix(
        idPrefix: String,
        soundWave: Long,
        recordDate: String
    ): Int
    
    /**
     * 清空指定日期的日音浪字段
     */
    @Query("""
        UPDATE streamer_data 
        SET sound_wave = 0 
        WHERE record_date = :date
    """)
    suspend fun clearDailySoundWaveByDate(date: String): Int
    
    /**
     * 获取所有主播列表（去重）
     * 返回每个主播ID对应的最新名字
     */
    @Query("""
        SELECT streamer_id, streamer_name
        FROM streamer_data
        WHERE (streamer_id, record_date) IN (
            SELECT streamer_id, MAX(record_date)
            FROM streamer_data
            GROUP BY streamer_id
        )
    """)
    suspend fun getAllStreamers(): List<StreamerInfo>
    
    /**
     * 根据ID前8位查询主播信息（完整ID和名称）
     * 从任意日期的记录中获取
     */
    @Query("""
        SELECT streamer_id, streamer_name
        FROM streamer_data
        WHERE SUBSTR(streamer_id, 1, 8) = :idPrefix
        ORDER BY record_date DESC
        LIMIT 1
    """)
    suspend fun getStreamerInfoByPrefix(idPrefix: String): StreamerInfo?
    
    /**
     * 主播信息（用于getAllStreamers返回）
     */
    data class StreamerInfo(
        @ColumnInfo(name = "streamer_id") val streamerId: String,
        @ColumnInfo(name = "streamer_name") val streamerName: String
    )
}
