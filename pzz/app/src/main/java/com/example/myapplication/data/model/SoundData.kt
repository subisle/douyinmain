package com.example.myapplication.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streamer_data")
data class SoundData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "streamer_id")
    val streamerId: String,
    
    @ColumnInfo(name = "streamer_name")
    val streamerName: String,
    
    @ColumnInfo(name = "sound_wave")
    val soundWave: Long = 0,
    
    @ColumnInfo(name = "total_sound_wave")
    val totalSoundWave: Long = 0,
    
    @ColumnInfo(name = "effective_broadcast_duration")
    val effectiveBroadcastDuration: String? = null,
    
    @ColumnInfo(name = "record_date")
    val recordDate: String,
    
    @ColumnInfo(name = "grade_rank")
    val gradeRank: String? = null,
    
    @ColumnInfo(name = "grade_letter")
    val gradeLetter: String? = null,
    
    @ColumnInfo(name = "overall_rank")
    val overallRank: Int = 0,
    
    @ColumnInfo(name = "created_at")
    val createdAt: String? = null
)

data class Statistics(
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val totalSoundWave: Long = 0,
    val inactiveCount: Int = 0  // 未开播人数
)
