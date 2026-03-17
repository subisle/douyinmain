#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查导入问题
"""
import pymysql
import os
from dotenv import load_dotenv

# 加载环境变量
load_dotenv()

def check_import_issue():
    """检查导入相关的数据"""
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
        
        print("=" * 60)
        print("检查远程数据库中的主播列表")
        print("=" * 60)
        
        # 检查user表中的主播数量
        cursor.execute("SELECT COUNT(*) FROM user")
        user_count = cursor.fetchone()[0]
        print(f"\n主播总数: {user_count}")
        
        # 检查主播ID长度分布
        cursor.execute("""
            SELECT 
                LENGTH(streamer_id) as id_length,
                COUNT(*) as count
            FROM user
            GROUP BY LENGTH(streamer_id)
            ORDER BY id_length
        """)
        print("\n主播ID长度分布:")
        for row in cursor.fetchall():
            print(f"  长度 {row[0]}: {row[1]} 个主播")
        
        # 显示前10个主播的ID和昵称
        cursor.execute("""
            SELECT streamer_id, nickname
            FROM user
            ORDER BY serial_number
            LIMIT 10
        """)
        print("\n前10个主播:")
        for row in cursor.fetchall():
            print(f"  ID: {row[0]} (长度:{len(row[0])}), 昵称: {row[1]}")
        
        # 检查sound_data表中的最新数据
        cursor.execute("""
            SELECT record_date, COUNT(*) as count
            FROM sound_data
            GROUP BY record_date
            ORDER BY record_date DESC
            LIMIT 5
        """)
        print("\n最近的音浪数据:")
        for row in cursor.fetchall():
            print(f"  日期: {row[0]}, 记录数: {row[1]}")
        
        # 检查2026-01-19的数据
        cursor.execute("""
            SELECT 
                streamer_id,
                sound_wave,
                total_sound_wave
            FROM sound_data
            WHERE record_date = '2026-01-19'
            LIMIT 5
        """)
        print("\n2026-01-19的前5条数据:")
        for row in cursor.fetchall():
            print(f"  ID: {row[0]}, 日音浪: {row[1]}, 总音浪: {row[2]}")
        
        cursor.close()
        conn.close()
        
        print("\n" + "=" * 60)
        print("检查完成")
        print("=" * 60)
        
    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == '__main__':
    check_import_issue()
