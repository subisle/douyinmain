package com.example.myapplication.data.model

/**
 * 导出设置
 */
data class ExportSettings(
    // 显示字段 - 默认：名称、当日音浪、总音浪、等级
    val showRank: Boolean = false,
    val showStreamerId: Boolean = false,
    val showStreamerName: Boolean = true,
    val showSoundWave: Boolean = true,
    val showSoundWaveChange: Boolean = true,  // 涨跌
    val showTotalSoundWave: Boolean = true,
    val showGradeRank: Boolean = true,
    val showEffectiveBroadcastDuration: Boolean = false,
    val showRecordDate: Boolean = false,
    
    // 姓名字体大小（scale倍数，默认1.0，范围0.8-1.5）
    val nameTextScale: Float = 1.0f,
    
    // 排序方式 - 默认：总音浪降序
    val sortBy: SortField = SortField.TOTAL_SOUND_WAVE,
    val sortOrder: SortOrder = SortOrder.DESC,
    
    // 性别筛选 - 默认：全部
    val genderFilter: GenderFilter = GenderFilter.ALL,
    
    // 音浪范围筛选（单位：万）
    val customMinSoundWave: Long = 0L,  // 最小音浪
    val customMaxSoundWave: Long = Long.MAX_VALUE,  // 最大音浪
    
    // 等级定级标准（单位：万）- D级<30万，C级30-80万，B级80-150万，A级≥150万
    val gradeS: Int = 300,   // S级：≥300万（保留）
    val gradeA: Int = 150,   // A级：≥150万
    val gradeB: Int = 80,    // B级：≥80万
    val gradeC: Int = 30,    // C级：≥30万
    // D级：<30万（自动）
)

enum class SortField {
    OVERALL_RANK,           // 按排名
    SOUND_WAVE,            // 按当日音浪
    TOTAL_SOUND_WAVE,      // 按总音浪
    STREAMER_NAME,         // 按姓名
    GRADE_RANK,            // 按等级
    BROADCAST_DURATION     // 按直播时长
}

enum class SortOrder {
    ASC,    // 升序
    DESC    // 降序
}

enum class GenderFilter {
    ALL,    // 全部
    MALE,   // 男性
    FEMALE  // 女性
}

/**
 * 字段显示名称
 */
fun SortField.displayName(): String {
    return when (this) {
        SortField.OVERALL_RANK -> "排名"
        SortField.SOUND_WAVE -> "当日音浪"
        SortField.TOTAL_SOUND_WAVE -> "总音浪"
        SortField.STREAMER_NAME -> "姓名"
        SortField.GRADE_RANK -> "等级"
        SortField.BROADCAST_DURATION -> "直播时长"
    }
}

/**
 * 性别筛选显示名称
 */
fun GenderFilter.displayName(): String {
    return when (this) {
        GenderFilter.ALL -> "全部"
        GenderFilter.MALE -> "男"
        GenderFilter.FEMALE -> "女"
    }
}
