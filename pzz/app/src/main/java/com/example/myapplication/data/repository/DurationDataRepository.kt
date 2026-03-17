package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.DurationDataDao
import com.example.myapplication.data.model.DurationData
import kotlinx.coroutines.flow.Flow

/**
 * 直播有效时长数据仓库
 */
class DurationDataRepository(context: Context) {
    private val dao: DurationDataDao = AppDatabase.getDatabase(context).durationDataDao()
    
    /**
     * 根据日期范围获取数据
     */
    fun getDataByDateRange(startDate: String, endDate: String): Flow<List<DurationData>> {
        return dao.getDataByDateRange(startDate, endDate)
    }
    
    /**
     * 同步查询
     */
    suspend fun getDataByDateRangeSync(startDate: String, endDate: String): List<DurationData> {
        return dao.getDataByDateRangeSync(startDate, endDate)
    }
    
    /**
     * 插入数据
     */
    suspend fun insert(data: DurationData): Long {
        return dao.insert(data)
    }
    
    /**
     * 批量插入
     */
    suspend fun insertAll(dataList: List<DurationData>): List<Long> {
        return dao.insertAll(dataList)
    }
    
    /**
     * 更新数据
     */
    suspend fun update(data: DurationData) {
        dao.update(data)
    }
    
    /**
     * 删除数据
     */
    suspend fun delete(data: DurationData) {
        dao.delete(data)
    }
    
    /**
     * 删除指定日期范围的数据
     */
    suspend fun deleteByDateRange(startDate: String, endDate: String) {
        dao.deleteByDateRange(startDate, endDate)
    }
    
    /**
     * 删除所有数据
     */
    suspend fun deleteAll() {
        dao.deleteAll()
    }
    
    /**
     * 获取所有日期范围
     */
    suspend fun getAllDateRanges(): List<String> {
        return dao.getAllDateRanges()
    }
}
