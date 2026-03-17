#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
验证总音浪更新功能的测试脚本
"""

import sqlite3
import os
from datetime import datetime

def connect_db():
    """连接到本地数据库"""
    # 这里需要根据实际的数据库路径调整
    db_path = "../app/databases/sound_data.db"
    if not os.path.exists(db_path):
        print(f"❌ 数据库文件不存在: {db_path}")
        return None
    
    conn = sqlite3.connect(db_path)
    return conn

def test_id_prefix_query():
    """测试ID前8位查询功能"""
    print("\n=== 测试ID前8位查询 ===")
    
    conn = connect_db()
    if not conn:
        return
    
    cursor = conn.cursor()
    
    # 测试查询
    test_prefix = "12345678"
    test_date = "2024-01-20"
    
    query = """
        SELECT streamer_id, streamer_name, sound_wave, total_sound_wave
        FROM streamer_data
        WHERE SUBSTR(streamer_id, 1, 8) = ?
        AND record_date = ?
        LIMIT 1
    """
    
    cursor.execute(query, (test_prefix, test_date))
    result = cursor.fetchone()
    
    if result:
        print(f"✅ 找到记录:")
        print(f"   完整ID: {result[0]}")
        print(f"   名称: {result[1]}")
        print(f"   当日音浪: {result[2]}")
        print(f"   总音浪: {result[3]}")
    else:
        print(f"⚠️  未找到ID前缀为 {test_prefix} 的记录")
    
    conn.close()

def test_streamer_info_query():
    """测试查询主播信息功能"""
    print("\n=== 测试查询主播信息 ===")
    
    conn = connect_db()
    if not conn:
        return
    
    cursor = conn.cursor()
    
    # 测试查询
    test_prefix = "12345678"
    
    query = """
        SELECT streamer_id, streamer_name
        FROM streamer_data
        WHERE SUBSTR(streamer_id, 1, 8) = ?
        ORDER BY record_date DESC
        LIMIT 1
    """
    
    cursor.execute(query, (test_prefix,))
    result = cursor.fetchone()
    
    if result:
        print(f"✅ 找到主播信息:")
        print(f"   完整ID: {result[0]}")
        print(f"   名称: {result[1]}")
    else:
        print(f"⚠️  未找到ID前缀为 {test_prefix} 的主播")
    
    conn.close()

def verify_update_result(date, id_prefix, expected_total):
    """验证更新结果"""
    print(f"\n=== 验证更新结果 ===")
    print(f"日期: {date}")
    print(f"ID前缀: {id_prefix}")
    print(f"期望总音浪: {expected_total}")
    
    conn = connect_db()
    if not conn:
        return
    
    cursor = conn.cursor()
    
    query = """
        SELECT streamer_id, streamer_name, sound_wave, total_sound_wave
        FROM streamer_data
        WHERE SUBSTR(streamer_id, 1, 8) = ?
        AND record_date = ?
    """
    
    cursor.execute(query, (id_prefix, date))
    result = cursor.fetchone()
    
    if result:
        actual_total = result[3]
        if actual_total == expected_total:
            print(f"✅ 验证通过!")
            print(f"   完整ID: {result[0]}")
            print(f"   名称: {result[1]}")
            print(f"   当日音浪: {result[2]}")
            print(f"   总音浪: {actual_total}")
        else:
            print(f"❌ 验证失败!")
            print(f"   期望总音浪: {expected_total}")
            print(f"   实际总音浪: {actual_total}")
    else:
        print(f"❌ 未找到记录")
    
    conn.close()

def check_main_screen_data(date):
    """检查主界面显示的数据"""
    print(f"\n=== 检查主界面数据 ===")
    print(f"日期: {date}")
    
    conn = connect_db()
    if not conn:
        return
    
    cursor = conn.cursor()
    
    # 统计当日音浪总和（主界面显示的值）
    query = """
        SELECT 
            COUNT(*) as total_count,
            COUNT(CASE WHEN sound_wave > 1 THEN 1 END) as active_count,
            SUM(CASE WHEN sound_wave > 1 THEN sound_wave ELSE 0 END) as total_sound_wave
        FROM streamer_data
        WHERE record_date = ?
    """
    
    cursor.execute(query, (date,))
    result = cursor.fetchone()
    
    if result:
        total_count = result[0]
        active_count = result[1]
        total_sound_wave = result[2] or 0
        
        print(f"✅ 统计结果:")
        print(f"   总人数: {total_count}")
        print(f"   开播人数: {active_count}")
        print(f"   今日总音浪: {total_sound_wave:,} (主界面显示)")
        print(f"   开播率: {active_count}/{total_count} = {active_count/total_count*100:.1f}%")
    else:
        print(f"❌ 未找到数据")
    
    conn.close()

def main():
    """主函数"""
    print("=" * 60)
    print("总音浪更新功能验证脚本")
    print("=" * 60)
    
    # 测试ID前8位查询
    test_id_prefix_query()
    
    # 测试查询主播信息
    test_streamer_info_query()
    
    # 验证更新结果（需要根据实际情况调整参数）
    # verify_update_result("2024-01-20", "12345678", 1000000)
    
    # 检查主界面数据
    # check_main_screen_data("2024-01-20")
    
    print("\n" + "=" * 60)
    print("测试完成")
    print("=" * 60)

if __name__ == "__main__":
    main()
