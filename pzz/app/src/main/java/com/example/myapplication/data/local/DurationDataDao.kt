package com.example.myapplication.data.local

import androidx.room.*
import com.example.myapplication.data.model.DurationData
import kotlinx.coroutines.flow.Flow

@Dao
interface DurationDataDao {
    /**
     * 根据日期范围查询数据
     */
    @Query("SELECT * FROM duration_data WHERE start_date = :startDate AND end_date = :endDate ORDER BY duration DESC")
    fun getDataByDateRange(startDate: String, endDate: String): Flow<List<DurationData>>
    
    /**
     * 同步查询（用于导入时查询）
     */
    @Query("SELECT * FROM duration_data WHERE start_date = :startDate AND end_date = :endDate")
    suspend fun getDataByDateRangeSync(startDate: String, endDate: String): List<DurationData>
    
    /**
     * 插入单条数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: DurationData): Long
    
    /**
     * 批量插入
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dataList: List<DurationData>): List<Long>
    
    /**
     * 更新数据
     */
    @Update
    suspend fun update(data: DurationData)
    
    /**
     * 删除数据
     */
    @Delete
    suspend fun delete(data: DurationData)
    
    /**
     * 删除指定日期范围的所有数据
     */
    @Query("DELETE FROM duration_data WHERE start_date = :startDate AND end_date = :endDate")
    suspend fun deleteByDateRange(startDate: String, endDate: String)
    
    /**
     * 删除所有数据
     */
    @Query("DELETE FROM duration_data")
    suspend fun deleteAll()
    
    /**
     * 获取所有不同的日期范围
     */
    @Query("SELECT DISTINCT start_date || ' ~ ' || end_date as date_range FROM duration_data ORDER BY start_date DESC")
    suspend fun getAllDateRanges(): List<String>
}
