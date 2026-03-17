#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查同步数据的完整性
"""
import pymysql
import os
from dotenv import load_dotenv
from datetime import datetime, timedelta

# 加载环境变量
load_dotenv()

def check_sync_data(start_date, end_date):
    """检查指定日期范围的数据"""
    try:
        # 连接远程数据库
        conn = pymysql.connect(
            host=os.getenv('REMOTE_DB_HOST'),
            port=int(os.getenv('REMOTE_DB_PORT', 3306)),
            user=os.getenv('REMOTE_DB_USER'),
            password=os.getenv('REMOTE_DB_PASSWORD'),
            database=os.getenv('REMOTE_DB_NAME'),
            charset='utf8mb4'
        )
        cursor = conn.cursor()
        
        print("=" * 80)
        print(f"检查同步数据：{start_date} 到 {end_date}")
        print("=" * 80)
        
        # 检查每个日期的数据量
        cursor.execute("""
            SELECT record_date, COUNT(*) as count
            FROM sound_data
            WHERE record_date BETWEEN %s AND %s
            GROUP BY record_date
            ORDER BY record_date
        """, (start_date, end_date))
        
        print("\n每日数据量:")
        date_counts = {}
        for row in cursor.fetchall():
            date_counts[row[0]] = row[1]
            print(f"  {row[0]}: {row[1]} 条")
        
        # 检查是否有缺失的日期
        start = datetime.strptime(start_date, '%Y-%m-%d')
        end = datetime.strptime(end_date, '%Y-%m-%d')
        current = start
        missing_dates = []
        
        print("\n日期完整性检查:")
        while current <= end:
            date_str = current.strftime('%Y-%m-%d')
            if date_str not in date_counts:
                missing_dates.append(date_str)
                print(f"  ❌ {date_str}: 缺失")
            else:
                print(f"  ✅ {date_str}: {date_counts[date_str]} 条")
            current += timedelta(days=1)
        
        if missing_dates:
            print(f"\n⚠️ 缺失的日期: {', '.join(missing_dates)}")
        else:
            print("\n✅ 所有日期都有数据")
        
        # 检查总数据量
        cursor.execute("""
            SELECT COUNT(*) as total
            FROM sound_data
            WHERE record_date BETWEEN %s AND %s
        """, (start_date, end_date))
        
        total = cursor.fetchone()[0]
        print(f"\n总数据量: {total} 条")
        
        # 检查每个日期的数据样本
        print("\n数据样本（每个日期前3条）:")
        cursor.execute("""
            SELECT record_date, streamer_id, streamer_name, sound_wave, total_sound_wave
            FROM sound_data
            WHERE record_date BETWEEN %s AND %s
            ORDER BY record_date, total_sound_wave DESC
        """, (start_date, end_date))
        
        current_date = None
        count = 0
        for row in cursor.fetchall():
            if row[0] != current_date:
                current_date = row[0]
                count = 0
                print(f"\n  {current_date}:")
            
            if count < 3:
                print(f"    ID: {row[1][:15]}..., 名字: {row[2]}, 日音浪: {row[3]}, 总音浪: {row[4]}")
                count += 1
        
        # 检查是否有空数据
        cursor.execute("""
            SELECT record_date, COUNT(*) as count
            FROM sound_data
            WHERE record_date BETWEEN %s AND %s
            AND (streamer_id IS NULL OR streamer_id = '' OR streamer_name IS NULL OR streamer_name = '')
            GROUP BY record_date
        """, (start_date, end_date))
        
        empty_data = cursor.fetchall()
        if empty_data:
            print("\n⚠️ 发现空数据:")
            for row in empty_data:
                print(f"  {row[0]}: {row[1]} 条")
        else:
            print("\n✅ 没有空数据")
        
        cursor.close()
        conn.close()
        
        print("\n" + "=" * 80)
        print("检查完成")
        print("=" * 80)
        
    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == '__main__':
    # 检查最近7天的数据
    end_date = '2026-01-21'
    start_date = '2026-01-14'
    check_sync_data(start_date, end_date)
