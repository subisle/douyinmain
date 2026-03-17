package com.example.myapplication.data.model

/**
 * 主播信息数据类
 */
data class Streamer(
    val id: Int = 0,
    val serialNumber: Int,
    val streamerId: String,
    val nickname: String,
    val fullName: String? = null,
    val gender: Gender,
    val createdAt: String = "",
    val updatedAt: String = ""
)

/**
 * 性别枚举
 */
enum class Gender {
    MALE, FEMALE;
    
    fun toDisplayString(): String = when(this) {
        MALE -> "男"
        FEMALE -> "女"
    }
    
    fun toDbString(): String = when(this) {
        MALE -> "male"
        FEMALE -> "female"
    }
    
    companion object {
        fun fromString(value: String): Gender = when(value.lowercase()) {
            "male" -> MALE
            "female" -> FEMALE
            else -> MALE
        }
    }
}
