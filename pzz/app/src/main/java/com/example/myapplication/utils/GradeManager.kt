package com.example.myapplication.utils

import com.example.myapplication.data.model.ExportSettings
import com.example.myapplication.data.model.SoundData

/**
 * 等级管理器 - 根据总音浪划分主播等级
 * 支持自定义等级标准
 */
object GradeManager {
    
    /**
     * 根据总音浪和自定义标准获取等级字母
     */
    fun getGradeLetter(totalSoundWave: Long, settings: ExportSettings): String {
        val wanValue = totalSoundWave / 10000 // 转换为万
        return when {
            wanValue >= settings.gradeS -> "S"
            wanValue >= settings.gradeA -> "A"
            wanValue >= settings.gradeB -> "B"
            wanValue >= settings.gradeC -> "C"
            else -> "D"
        }
    }
    
    /**
     * 为数据列表分配等级排名
     * 返回更新后的数据列表，包含 gradeLetter 和 gradeRank
     * 
     * 所有主播（包括未开播的）都根据总音浪进行等级划分
     * 返回的数据按总音浪降序排列
     */
    fun assignGradeRanks(dataList: List<SoundData>, settings: ExportSettings): List<SoundData> {
        // 按总音浪降序排序（所有主播，包括未开播的）
        val sortedData = dataList.sortedByDescending { it.totalSoundWave }
        
        // 按等级分组并计算每个等级内的排名
        val gradeGroups = sortedData.groupBy { getGradeLetter(it.totalSoundWave, settings) }
        
        val result = mutableListOf<SoundData>()
        
        // 按等级顺序处理：S, A, B, C, D
        // 每个等级内的数据已经按总音浪降序排列
        listOf("S", "A", "B", "C", "D").forEach { grade ->
            val groupData = gradeGroups[grade] ?: emptyList()
            groupData.forEachIndexed { index, data ->
                val gradeRank = "$grade${index + 1}"
                result.add(
                    data.copy(
                        gradeLetter = grade,
                        gradeRank = gradeRank
                    )
                )
            }
        }
        
        return result
    }
    
    /**
     * 获取等级描述
     */
    fun getGradeDescription(grade: String, settings: ExportSettings): String {
        return when (grade) {
            "S" -> "S级（超级主播）- ${settings.gradeS}万以上"
            "A" -> "A级（顶级主播）- ${settings.gradeA}-${settings.gradeS}万"
            "B" -> "B级（优秀主播）- ${settings.gradeB}-${settings.gradeA}万"
            "C" -> "C级（普通主播）- ${settings.gradeC}-${settings.gradeB}万"
            "D" -> "D级（新人主播）- 0-${settings.gradeC}万"
            else -> "未知等级"
        }
    }
}

