#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修复浩杰19号的总音浪
"""

import mysql.connector

# 远程数据库配置
DB_CONFIG = {
    'host': 'your_mysql_host',
    'port': 3310,
    'user': 'your_username',
    'password': 'your_password',
    'database': 'your_database'
}

def fix_haojie_19():
    """修复浩杰19号的总音浪"""
    print("=" * 80)
    print("修复浩杰19号的总音浪")
    print("=" * 80)
    
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor(dictionary=True)
        
        # 1. 查看当前19号的数据
        print("\n1. 当前19号浩杰的数据:")
        sql = """
            SELECT streamer_id, streamer_name, sound_wave, total_sound_wave, record_date
            FROM streamer_data
            WHERE streamer_id = '2263268204753467' AND record_date = '2026-01-19'
        """
        cursor.execute(sql)
        result = cursor.fetchone()
        
        if result:
            print(f"  ID: {result['streamer_id']}")
            print(f"  名字: {result['streamer_name']}")
            print(f"  日音浪: {result['sound_wave']}")
            print(f"  总音浪: {result['total_sound_wave']} ❌ (应该是42231)")
            print(f"  日期: {result['record_date']}")
        else:
            print("  ⚠️ 没有找到19号的记录")
            return
        
        # 2. 更新总音浪为42231
        print("\n2. 更新总音浪为42231...")
        update_sql = """
            UPDATE streamer_data
            SET total_sound_wave = 42231
            WHERE streamer_id = '2263268204753467' AND record_date = '2026-01-19'
        """
        cursor.execute(update_sql)
        conn.commit()
        print(f"  ✅ 已更新 {cursor.rowcount} 条记录")
        
        # 3. 验证更新后的数据
        print("\n3. 更新后的数据:")
        cursor.execute(sql)
        result = cursor.fetchone()
        
        if result:
            print(f"  ID: {result['streamer_id']}")
            print(f"  名字: {result['streamer_name']}")
            print(f"  日音浪: {result['sound_wave']}")
            print(f"  总音浪: {result['total_sound_wave']} ✅")
            print(f"  日期: {result['record_date']}")
        
        # 4. 同时更新20号的总音浪 (19号总音浪42231 + 20号日音浪1321 = 43552)
        print("\n4. 更新20号的总音浪...")
        update_sql_20 = """
            UPDATE streamer_data
            SET total_sound_wave = 43552
            WHERE streamer_id = '2263268204753467' AND record_date = '2026-01-20'
        """
        cursor.execute(update_sql_20)
        conn.commit()
        print(f"  ✅ 已更新 {cursor.rowcount} 条记录 (42231 + 1321 = 43552)")
        
        # 5. 查看所有浩杰的记录
        print("\n5. 浩杰的所有记录:")
        sql_all = """
            SELECT streamer_id, streamer_name, sound_wave, total_sound_wave, record_date
            FROM streamer_data
            WHERE streamer_id = '2263268204753467'
            ORDER BY record_date DESC
        """
        cursor.execute(sql_all)
        results = cursor.fetchall()
        
        for row in results:
            print(f"  {row['record_date']}: 日音浪={row['sound_wave']:,}, 总音浪={row['total_sound_wave']:,}")
        
        cursor.close()
        conn.close()
        
        print("\n✅ 修复完成! 请在app中点击'同步'按钮")
        
    except Exception as e:
        print(f"❌ 修复失败: {e}")

if __name__ == '__main__':
    fix_haojie_19()
