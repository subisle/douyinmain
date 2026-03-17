#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
对比远程数据库和本地数据库的数据差异
"""
import pymysql
import sqlite3
import os
from dotenv import load_dotenv

# 加载环境变量
load_dotenv()

def compare_data(start_date, end_date):
    """对比远程和本地数据"""
    try:
        # 连接远程数据库
        remote_conn = pymysql.connect(
            host=os.getenv('REMOTE_DB_HOST'),
            port=int(os.getenv('REMOTE_DB_PORT', 3306)),
            user=os.getenv('REMOTE_DB_USER'),
            password=os.getenv('REMOTE_DB_PASSWORD'),
            database=os.getenv('REMOTE_DB_NAME'),
            charset='utf8mb4'
        )
        remote_cursor = remote_conn.cursor()
        
        print("=" * 80)
        print(f"对比数据：{start_date} 到 {end_date}")
        print("=" * 80)
        
        # 获取远程数据
        remote_cursor.execute("""
            SELECT record_date, streamer_id, streamer_name, sound_wave, total_sound_wave
            FROM sound_data
            WHERE record_date BETWEEN %s AND %s
            ORDER BY record_date, total_sound_wave DESC
        """, (start_date, end_date))
        
        remote_data = {}
        for row in remote_cursor.fetchall():
            date = row[0]
            if date not in remote_data:
                remote_data[date] = []
            remote_data[date].append({
                'id': row[1],
                'name': row[2],
                'sound_wave': row[3],
                'total_sound_wave': row[4]
            })
        
        print(f"\n远程数据库:")
        for date in sorted(remote_data.keys()):
            print(f"  {date}: {len(remote_data[date])} 条")
        
        # 尝试连接本地数据库（Android app的数据库）
        # 注意：这需要从Android设备导出数据库文件
        print(f"\n提示：")
        print(f"1. 从Android设备导出数据库: adb pull /data/data/com.example.myapplication/databases/sound_data.db")
        print(f"2. 将数据库文件放在当前目录")
        print(f"3. 重新运行此脚本")
        
        # 检查本地数据库文件是否存在
        local_db_path = "sound_data.db"
        if os.path.exists(local_db_path):
            print(f"\n找到本地数据库文件: {local_db_path}")
            local_conn = sqlite3.connect(local_db_path)
            local_cursor = local_conn.cursor()
            
            # 获取本地数据
            local_cursor.execute("""
                SELECT record_date, streamer_id, streamer_name, sound_wave, total_sound_wave
                FROM streamer_data
                WHERE record_date BETWEEN ? AND ?
                ORDER BY record_date, total_sound_wave DESC
            """, (start_date, end_date))
            
            local_data = {}
            for row in local_cursor.fetchall():
                date = row[0]
                if date not in local_data:
                    local_data[date] = []
                local_data[date].append({
                    'id': row[1],
                    'name': row[2],
                    'sound_wave': row[3],
                    'total_sound_wave': row[4]
                })
            
            print(f"\n本地数据库:")
            for date in sorted(local_data.keys()):
                print(f"  {date}: {len(local_data[date])} 条")
            
            # 对比差异
            print(f"\n数据差异:")
            all_dates = set(remote_data.keys()) | set(local_data.keys())
            for date in sorted(all_dates):
                remote_count = len(remote_data.get(date, []))
                local_count = len(local_data.get(date, []))
                
                if remote_count != local_count:
                    print(f"  ❌ {date}: 远程 {remote_count} 条, 本地 {local_count} 条, 差异 {remote_count - local_count} 条")
                else:
                    print(f"  ✅ {date}: {remote_count} 条")
            
            local_conn.close()
        else:
            print(f"\n未找到本地数据库文件: {local_db_path}")
        
        remote_conn.close()
        
        print("\n" + "=" * 80)
        print("对比完成")
        print("=" * 80)
        
    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == '__main__':
    # 对比最近7天的数据
    end_date = '2026-01-21'
    start_date = '2026-01-14'
    compare_data(start_date, end_date)
