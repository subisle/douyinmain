package com.example.myapplication.utils

import com.example.myapplication.data.model.SoundData
import java.text.SimpleDateFormat
import java.util.*

object TestDataGenerator {
    private val names = listOf(
        "张三", "李四", "王五", "赵六", "孙七", "周八", "吴九", "郑十",
        "小红", "小明", "小芳", "小丽", "小华", "小强", "小刚", "小军",
        "阿强", "阿明", "阿华", "阿龙", "阿虎", "阿豹", "阿鹏", "阿飞",
        "晓东", "晓明", "晓红", "晓芳", "晓丽", "晓华", "晓强", "晓刚"
    )
    
    fun generateTestData(date: String, count: Int = 30): List<SoundData> {
        val random = Random()
        val data = mutableListOf<SoundData>()
        
        for (i in 1..count) {
            val soundWave = when {
                i <= 3 -> random.nextInt(50000) + 100000L // S级: 10-15万
                i <= 10 -> random.nextInt(40000) + 60000L // A级: 6-10万
                i <= 20 -> random.nextInt(30000) + 30000L // B级: 3-6万
                i <= 25 -> random.nextInt(20000) + 10000L // C级: 1-3万
                else -> random.nextInt(10000).toLong()     // D级: 0-1万
            }
            
            val gradeLetter = when {
                soundWave >= 100000 -> "S"
                soundWave >= 60000 -> "A"
                soundWave >= 30000 -> "B"
                soundWave >= 10000 -> "C"
                else -> "D"
            }
            
            val gradeNumber = when (gradeLetter) {
                "S" -> i
                "A" -> i - 3
                "B" -> i - 10
                "C" -> i - 20
                else -> i - 25
            }
            
            data.add(
                SoundData(
                    id = i.toLong(),
                    streamerId = "ID${1000 + i}",
                    streamerName = names[i % names.size],
                    soundWave = soundWave,
                    totalSoundWave = soundWave * (random.nextInt(5) + 5),
                    effectiveBroadcastDuration = "${random.nextInt(8) + 1}小时",
                    recordDate = date,
                    gradeRank = "$gradeLetter$gradeNumber",
                    gradeLetter = gradeLetter,
                    overallRank = i,
                    createdAt = getCurrentTimestamp()
                )
            )
        }
        
        return data
    }
    
    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
