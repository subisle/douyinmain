#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查本地数据库中的总音浪数据
"""

import sqlite3
import os

def check_local_database():
    """检查本地数据库"""
    print("=" * 60)
    print("检查本地数据库中的总音浪数据")
    print("=" * 60)
    
    # Android应用的数据库路径（需要从设备中导出）
    # 使用 adb pull /data/data/com.example.myapplication/databases/sound_data.db
    db_path = "sound_data.db"
    
    if not os.path.exists(db_path):
        print(f"\n❌ 数据库文件不存在: {db_path}")
        print("请使用以下命令从设备导出数据库：")
        print("adb pull /data/data/com.example.myapplication/databases/sound_data.db")
        return
    
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # 检查指定日期的数据
    target_date = "2025-01-19"  # 根据实际情况修改
    
    print(f"\n检查日期: {target_date}")
    print("-" * 60)
    
    # 1. 统计总记录数
    cursor.execute("""
        SELECT COUNT(*) FROM streamer_data WHERE record_date = ?
    """, (target_date,))
    total_count = cursor.fetchone()[0]
    print(f"\n总记录数: {total_count}")
    
    # 2. 统计有总音浪的记录数
    cursor.execute("""
        SELECT COUNT(*) FROM streamer_data 
        WHERE record_date = ? AND total_sound_wave > 0
    """, (target_date,))
    has_total_count = cursor.fetchone()[0]
    print(f"有总音浪的记录数: {has_total_count}")
    
    # 3. 统计总音浪为0的记录数
    cursor.execute("""
        SELECT COUNT(*) FROM streamer_data 
        WHERE record_date = ? AND total_sound_wave = 0
    """, (target_date,))
    zero_total_count = cursor.fetchone()[0]
    print(f"总音浪为0的记录数: {zero_total_count}")
    
    # 4. 查看前10条记录
    print(f"\n前10条记录:")
    print("-" * 60)
    cursor.execute("""
        SELECT streamer_id, streamer_name, sound_wave, total_sound_wave
        FROM streamer_data
        WHERE record_date = ?
        ORDER BY total_sound_wave DESC
        LIMIT 10
    """, (target_date,))
    
    rows = cursor.fetchall()
    for i, row in enumerate(rows, 1):
        streamer_id, streamer_name, sound_wave, total_sound_wave = row
        id_prefix = streamer_id[:8] if len(streamer_id) >= 8 else streamer_id
        print(f"{i}. ID: {streamer_id} (前8位: {id_prefix})")
        print(f"   名称: {streamer_name}")
        print(f"   当日音浪: {sound_wave:,}")
        print(f"   总音浪: {total_sound_wave:,}")
        print()
    
    # 5. 检查是否有ID前8位重复的情况
    print("检查ID前8位重复情况:")
    print("-" * 60)
    cursor.execute("""
        SELECT SUBSTR(streamer_id, 1, 8) as id_prefix, COUNT(*) as cnt
        FROM streamer_data
        WHERE record_date = ?
        GROUP BY id_prefix
        HAVING cnt > 1
    """, (target_date,))
    
    duplicates = cursor.fetchall()
    if duplicates:
        print(f"⚠️ 发现 {len(duplicates)} 个ID前8位重复的情况:")
        for prefix, count in duplicates:
            print(f"  ID前缀: {prefix}, 重复次数: {count}")
            
            # 显示重复的记录
            cursor.execute("""
                SELECT streamer_id, streamer_name, total_sound_wave
                FROM streamer_data
                WHERE record_date = ? AND SUBSTR(streamer_id, 1, 8) = ?
            """, (target_date, prefix))
            
            dup_rows = cursor.fetchall()
            for dup_id, dup_name, dup_total in dup_rows:
                print(f"    - {dup_id} ({dup_name}): 总音浪={dup_total:,}")
    else:
        print("✅ 没有ID前8位重复的情况")
    
    # 6. 检查CSV中的几个主播
    print("\n检查CSV中的主播:")
    print("-" * 60)
    
    test_ids = [
        ("44699721", "狼兴"),  # CSV第1行，ID前8位
        ("23006024", "轩宝"),  # CSV第2行
        ("42907629", "狼俊"),  # CSV第3行
    ]
    
    for id_prefix, name_hint in test_ids:
        cursor.execute("""
            SELECT streamer_id, streamer_name, sound_wave, total_sound_wave
            FROM streamer_data
            WHERE record_date = ? AND SUBSTR(streamer_id, 1, 8) = ?
        """, (target_date, id_prefix))
        
        row = cursor.fetchone()
        if row:
            streamer_id, streamer_name, sound_wave, total_sound_wave = row
            print(f"✅ 找到: {name_hint}")
            print(f"   完整ID: {streamer_id}")
            print(f"   名称: {streamer_name}")
            print(f"   当日音浪: {sound_wave:,}")
            print(f"   总音浪: {total_sound_wave:,}")
        else:
            print(f"❌ 未找到: {name_hint} (ID前缀: {id_prefix})")
        print()
    
    conn.close()
    
    print("=" * 60)
    print("检查完成")
    print("=" * 60)

if __name__ == "__main__":
    check_local_database()
