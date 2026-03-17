-- 直播有效时长数据表
-- 用于存储主播在指定时间范围内的开播有效时长

CREATE TABLE IF NOT EXISTS `duration_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `streamer_id` varchar(50) NOT NULL COMMENT '主播ID',
  `streamer_name` varchar(100) NOT NULL COMMENT '主播名称',
  `duration` varchar(50) NOT NULL COMMENT '开播有效时长（格式：XX小时XX分钟XX秒）',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date NOT NULL COMMENT '结束日期',
  
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_streamer_date` (`streamer_id`, `start_date`, `end_date`) COMMENT '主播+日期范围唯一索引',
  KEY `idx_date_range` (`start_date`, `end_date`) COMMENT '日期范围索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='直播有效时长数据表';

-- 示例数据
-- INSERT INTO duration_data (streamer_id, streamer_name, duration, start_date, end_date) 
-- VALUES 
-- ('1110932522276664', '十二宝  王悠悠🎤才艺', '56小时40分钟41秒', '2026-01-01', '2026-01-18'),
-- ('719554418848731', '晓雅🎹🔥【键盘🎹薇姐】', '46小时27分钟35秒', '2026-01-01', '2026-01-18');
