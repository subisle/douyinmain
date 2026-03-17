-- 修复浩杰的错误数据
-- 问题: 数据库中有两条浩杰的记录,其中一条ID错误

-- 1. 查看当前浩杰的所有记录
SELECT streamer_id, streamer_name, sound_wave, total_sound_wave, record_date
FROM sound_data
WHERE streamer_name LIKE '%浩杰%'
ORDER BY record_date DESC;

-- 2. 删除ID错误的记录 (ID: 2203268204753467, 前10位: 2203268204)
-- 正确的ID应该是: 2263268204753467 (前10位: 2263268204)
DELETE FROM sound_data
WHERE streamer_id = '2203268204753467';

-- 3. 验证删除后的结果
SELECT streamer_id, streamer_name, sound_wave, total_sound_wave, record_date
FROM sound_data
WHERE streamer_name LIKE '%浩杰%'
ORDER BY record_date DESC;

-- 4. 检查是否还有其他ID错误的记录
-- 查找所有ID前10位不在user表中的记录
SELECT DISTINCT s.streamer_id, s.streamer_name, SUBSTRING(s.streamer_id, 1, 10) as id_prefix
FROM sound_data s
WHERE NOT EXISTS (
    SELECT 1 FROM user u 
    WHERE SUBSTRING(s.streamer_id, 1, 10) = SUBSTRING(u.streamer_id, 1, 10)
)
AND s.streamer_name NOT LIKE '%账号已注销%'
ORDER BY s.streamer_name;
