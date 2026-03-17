# 总音浪更新功能流程测试文档

## 修改内容

### 1. 更新流程调整
- **原流程**: 先更新远程数据库 → 再更新本地数据库
- **新流程**: 先更新本地数据库 → 再更新远程数据库

### 2. ID匹配方式
- 使用主播ID前8位进行匹配
- CSV文件中的ID前8位与数据库中的完整ID进行匹配

### 3. 本地数据库更新逻辑
```kotlin
// 1. 使用ID前8位查询现有记录
val existing = dao.getByIdPrefixAndDate(idPrefix, targetDate)

if (existing != null) {
    // 2. 如果存在，更新总音浪
    val updated = existing.copy(totalSoundWave = totalSound)
    dao.update(updated)
} else {
    // 3. 如果不存在，查询主播的完整ID和名称
    val streamerInfo = dao.getStreamerInfoByPrefix(idPrefix)
    
    if (streamerInfo != null) {
        // 4. 使用完整ID创建新记录
        val newData = SoundData(
            streamerId = streamerInfo.streamerId,  // 完整ID
            streamerName = streamerInfo.streamerName,
            soundWave = 0L,  // 当日音浪为0
            totalSoundWave = totalSound,  // 设置总音浪
            recordDate = targetDate,
            ...
        )
        dao.insert(newData)
    }
}
```

### 4. 主界面显示
- 主界面的"今日总音浪"显示的是 `soundWave`（当日音浪）的总和
- 不是 `totalSoundWave`（总音浪）
- 这个设计是正确的，因为主界面应该显示当天的音浪情况

### 5. 数据库查询方法新增
在 `SoundDataDao.kt` 中新增：
```kotlin
@Query("""
    SELECT streamer_id, streamer_name
    FROM streamer_data
    WHERE SUBSTR(streamer_id, 1, 8) = :idPrefix
    ORDER BY record_date DESC
    LIMIT 1
""")
suspend fun getStreamerInfoByPrefix(idPrefix: String): StreamerInfo?
```

## 测试步骤

### 1. 准备测试数据
- 准备一个CSV文件，包含主播ID前8位和总音浪
- 格式示例：
  ```
  主播ID,总音浪
  12345678,1000000
  87654321,2000000
  ```

### 2. 测试场景A：更新现有记录
1. 确保数据库中已有某个日期的主播记录
2. 导入CSV文件，更新该日期的总音浪
3. 验证：
   - 本地数据库中的 `totalSoundWave` 字段已更新
   - 远程数据库中的 `total_sound_wave` 字段已更新
   - 主界面显示的"今日总音浪"不受影响（因为显示的是soundWave）

### 3. 测试场景B：创建新记录
1. 选择一个数据库中没有记录的日期
2. 导入CSV文件
3. 验证：
   - 本地数据库创建了新记录
   - 使用的是完整的主播ID（不是前8位）
   - `soundWave` 为 0
   - `totalSoundWave` 为CSV中的值
   - 远程数据库也创建了对应记录

### 4. 测试场景C：主界面显示
1. 更新总音浪后
2. 切换到更新的日期
3. 验证：
   - 主界面的"今日总音浪"显示的是当日音浪总和（soundWave）
   - 不是总音浪（totalSoundWave）
   - 列表中每个主播的总音浪字段正确显示

### 5. 测试场景D：不影响其他功能
1. 更新总音浪后
2. 测试以下功能是否正常：
   - 日音浪导入
   - 数据同步
   - 导出功能
   - 排序和筛选
   - 等级计算

## 预期结果

### 成功标准
1. ✅ 本地数据库先于远程数据库更新
2. ✅ 使用ID前8位正确匹配主播
3. ✅ 新记录使用完整ID而不是前8位
4. ✅ 主界面正确显示当日音浪总和
5. ✅ 总音浪字段正确更新
6. ✅ 其他功能不受影响

### 日志验证
查看日志输出，应该看到：
```
TotalSoundUpdateUtils: CSV解析: X条
TotalSoundUpdateUtils: 开始更新本地数据库，共X条数据
TotalSoundUpdateUtils: 更新本地记录: ID前缀=XXXXXXXX, 总音浪=XXXXXX
TotalSoundUpdateUtils: 本地数据库更新完成: 成功=X, 插入=X, 失败=X
TotalSoundUpdateUtils: 远程数据库更新完成: 成功=X, 插入=X, 失败=X
```

## 注意事项

1. **ID匹配**: CSV中的ID必须至少8位，否则匹配可能失败
2. **数据完整性**: 如果本地数据库中没有该主播的任何记录，将使用ID前8位创建记录
3. **主界面显示**: "今日总音浪"显示的是soundWave，不是totalSoundWave
4. **更新顺序**: 先本地后远程，确保即使远程失败，本地数据也已更新

## 回滚方案

如果发现问题，可以：
1. 从备份恢复数据库
2. 或者使用数据同步功能从远程数据库重新同步
