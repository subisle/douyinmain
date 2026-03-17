#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查浩杰的数据 - 对比远程数据库和本地数据库
"""

import mysql.connector
import sqlite3
import os

# 远程数据库配置
REMOTE_DB_CONFIG = {
    'host': '47.109.78.124',
    'port': 3306,
    'user': 'pzz',
    'password': 'pzz666888',
    'database': 'pzz'
}

# 本地数据库路径 (需要从Android设备导出)
# 使用 adb pull /data/data/com.example.myapplication/databases/sound_data.db ./
LOCAL_DB_PATH = './sound_data.db'

def check_remote_data():
    """检查远程数据库中浩杰的数据"""
    print("=" * 80)
    print("检查远程数据库")
    print("=" * 80)
    
    try:
        conn = mysql.connector.connect(**REMOTE_DB_CONFIG)
        cursor = conn.cursor(dictionary=True)
        
        # 查询浩杰的所有记录
        sql = """
            SELECT streamer_id, streamer_name, sound_wave, total_sound_wave, record_date
            FROM sound_data
            WHERE streamer_name LIKE '%浩杰%'
            ORDER BY record_date DESC
        """
        
        cursor.execute(sql)
        results = cursor.fetchall()
        
        if results:
            print(f"\n找到 {len(results)} 条浩杰的记录:\n")
            for row in results:
                print(f"日期: {row['record_date']}")
                print(f"  ID: {row['streamer_id']}")
                print(f"  名字: {row['streamer_name']}")
                print(f"  日音浪: {row['sound_wave']:,}")
                print(f"  总音浪: {row['total_sound_wave']:,}")
                print()
        else:
            print("\n⚠️ 远程数据库中没有找到浩杰的记录")
        
        # 查询19号所有主播的总音浪统计
        sql_stats = """
            SELECT 
                COUNT(*) as total_count,
                SUM(CASE WHEN total_sound_wave > 0 THEN 1 ELSE 0 END) as has_total_count,
                SUM(CASE WHEN total_sound_wave = 0 THEN 1 ELSE 0 END) as zero_total_count
            FROM sound_data
            WHERE record_date = '2026-01-19'
        """
        
        cursor.execute(sql_stats)
        stats = cursor.fetchone()
        
        print("\n19号数据统计:")
        print(f"  总记录数: {stats['total_count']}")
        print(f"  有总音浪: {stats['has_total_count']}")
        print(f"  总音浪为0: {stats['zero_total_count']}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"❌ 查询远程数据库失败: {e}")

def check_local_data():
    """检查本地数据库中浩杰的数据"""
    print("\n" + "=" * 80)
    print("检查本地数据库")
    print("=" * 80)
    
    if not os.path.exists(LOCAL_DB_PATH):
        print(f"\n⚠️ 本地数据库文件不存在: {LOCAL_DB_PATH}")
        print("\n请先从Android设备导出数据库:")
        print("  adb pull /data/data/com.example.myapplication/databases/sound_data.db ./")
        return
    
    try:
        conn = sqlite3.connect(LOCAL_DB_PATH)
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        # 查询浩杰的所有记录
        sql = """
            SELECT streamer_id, streamer_name, sound_wave, total_sound_wave, record_date
            FROM sound_data
            WHERE streamer_name LIKE '%浩杰%'
            ORDER BY record_date DESC
        """
        
        cursor.execute(sql)
        results = cursor.fetchall()
        
        if results:
            print(f"\n找到 {len(results)} 条浩杰的记录:\n")
            for row in results:
                print(f"日期: {row['record_date']}")
                print(f"  ID: {row['streamer_id']}")
                print(f"  名字: {row['streamer_name']}")
                print(f"  日音浪: {row['sound_wave']:,}")
                print(f"  总音浪: {row['total_sound_wave']:,}")
                print()
        else:
            print("\n⚠️ 本地数据库中没有找到浩杰的记录")
        
        # 查询19号所有主播的总音浪统计
        sql_stats = """
            SELECT 
                COUNT(*) as total_count,
                SUM(CASE WHEN total_sound_wave > 0 THEN 1 ELSE 0 END) as has_total_count,
                SUM(CASE WHEN total_sound_wave = 0 THEN 1 ELSE 0 END) as zero_total_count
            FROM sound_data
            WHERE record_date = '2026-01-19'
        """
        
        cursor.execute(sql_stats)
        stats = cursor.fetchone()
        
        if stats:
            print("\n19号数据统计:")
            print(f"  总记录数: {stats['total_count']}")
            print(f"  有总音浪: {stats['has_total_count']}")
            print(f"  总音浪为0: {stats['zero_total_count']}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"❌ 查询本地数据库失败: {e}")

def compare_data():
    """对比远程和本地的数据"""
    print("\n" + "=" * 80)
    print("数据对比")
    print("=" * 80)
    
    # 查询远程19号的前10条数据
    try:
        conn = mysql.connector.connect(**REMOTE_DB_CONFIG)
        cursor = conn.cursor(dictionary=True)
        
        sql = """
            SELECT streamer_id, streamer_name, total_sound_wave
            FROM sound_data
            WHERE record_date = '2026-01-19'
            ORDER BY total_sound_wave DESC
            LIMIT 10
        """
        
        cursor.execute(sql)
        remote_data = cursor.fetchall()
        
        print("\n远程数据库 - 19号总音浪前10名:")
        for i, row in enumerate(remote_data, 1):
            print(f"{i}. {row['streamer_name']}: {row['total_sound_wave']:,} (ID: {row['streamer_id']})")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"❌ 查询远程数据失败: {e}")

if __name__ == '__main__':
    print("\n浩杰数据检查工具")
    print("=" * 80)
    
    # 检查远程数据
    check_remote_data()
    
    # 检查本地数据
    check_local_data()
    
    # 对比数据
    compare_data()
    
    print("\n" + "=" * 80)
    print("检查完成")
    print("=" * 80)
