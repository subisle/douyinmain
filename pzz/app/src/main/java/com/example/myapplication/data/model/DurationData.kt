package com.example.myapplication.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 直播有效时长数据模型
 */
@Entity(
    tableName = "duration_data",
    indices = [
        androidx.room.Index(value = ["streamer_id", "start_date", "end_date"], unique = true),
        androidx.room.Index(value = ["start_date", "end_date"])
    ]
)
data class DurationData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "streamer_id")
    val streamerId: String,
    
    @ColumnInfo(name = "streamer_name")
    val streamerName: String,
    
    @ColumnInfo(name = "duration")
    val duration: String,  // 开播有效时长，格式：XX小时XX分钟XX秒
    
    @ColumnInfo(name = "start_date")
    val startDate: String,  // 开始日期 yyyy-MM-dd
    
    @ColumnInfo(name = "end_date")
    val endDate: String,    // 结束日期 yyyy-MM-dd
    
    @ColumnInfo(name = "gender")
    val gender: String = "",  // 性别：男/女
    
    @ColumnInfo(name = "created_at")
    val createdAt: String = ""
)

/**
 * 直播时长统计
 */
data class DurationStatistics(
    val totalCount: Int = 0,
    val activeCount: Int = 0,  // 有开播时长的人数
    val inactiveCount: Int = 0  // 未开播人数
)
