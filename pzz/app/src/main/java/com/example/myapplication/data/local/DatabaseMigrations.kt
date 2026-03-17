package com.example.myapplication.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移策略
 * 用于在数据库版本升级时保留现有数据
 */
object DatabaseMigrations {
    
    /**
     * 从版本1迁移到版本2
     * 添加 duration_data 表，保留 streamer_data 表的数据
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 创建 duration_data 表
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `duration_data` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `streamer_id` TEXT NOT NULL,
                    `streamer_name` TEXT NOT NULL,
                    `duration` TEXT NOT NULL,
                    `start_date` TEXT NOT NULL,
                    `end_date` TEXT NOT NULL,
                    `created_at` TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())
            
            // 创建唯一索引（主播ID + 日期范围）
            database.execSQL("""
                CREATE UNIQUE INDEX IF NOT EXISTS `index_duration_data_streamer_id_start_date_end_date` 
                ON `duration_data` (`streamer_id`, `start_date`, `end_date`)
            """.trimIndent())
            
            // 创建日期范围索引
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS `index_duration_data_start_date_end_date` 
                ON `duration_data` (`start_date`, `end_date`)
            """.trimIndent())
            
            android.util.Log.d("DatabaseMigration", "成功从版本1迁移到版本2")
        }
    }
    
    /**
     * 从版本2迁移到版本3
     * 为 duration_data 表添加 gender 字段
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 添加 gender 字段
            database.execSQL("""
                ALTER TABLE `duration_data` ADD COLUMN `gender` TEXT NOT NULL DEFAULT ''
            """.trimIndent())
            
            android.util.Log.d("DatabaseMigration", "成功从版本2迁移到版本3：添加gender字段")
        }
    }
}
