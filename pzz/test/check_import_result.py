#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查导入结果
"""

import mysql.connector
import os
from datetime import datetime

def check_import_result(target_date='2026-01-18'):
    """检查指定日期的导入结果"""
    try:
        conn = mysql.connector.connect(
            host=os.getenv('DB_HOST', 'your_mysql_host'),
            port=int(os.getenv('DB_PORT', '3306')),
            user=os.getenv('DB_USER', 'your_username'),
            password=os.getenv('DB_PASSWORD', 'your_password'),
            database=os.getenv('DB_NAME', 'your_database')
        )
        
        cursor = conn.cursor()
        
        print("=" * 80)
        print(f"检查日期: {target_date}")
        print("=" * 80)
        
        # 1. 统计该日期的总记录数
        cursor.execute(f"""
            SELECT COUNT(*) as total
            FROM sound_data
            WHERE record_date = '{target_date}'
        """)
        total = cursor.fetchone()[0]
        print(f"\n✅ 该日期总记录数: {total}")
        
        # 2. 统计有音浪的记录数
        cursor.execute(f"""
            SELECT COUNT(*) as active
            FROM sound_data
            WHERE record_date = '{target_date}' AND sound_wave > 0
        """)
        active = cursor.fetchone()[0]
        print(f"✅ 有音浪的记录数: {active}")
        print(f"✅ 无音浪的记录数: {total - active}")
        
        # 3. 查看前10条记录
        cursor.execute(f"""
            SELECT streamer_id, streamer_name, sound_wave, total_sound_wave
            FROM sound_data
            WHERE record_date = '{target_date}'
            ORDER BY sound_wave DESC
            LIMIT 10
        """)
        
        print(f"\n前10条记录（按音浪排序）:")
        print("-" * 80)
        for row in cursor.fetchall():
            sid, name, daily, total = row
            prefix = sid[:8] if len(sid) >= 8 else sid
            print(f"ID={sid} (前8位={prefix}), 名称={name}, 日音浪={daily}, 总音浪={total}")
        
        # 4. 检查是否有重复记录
        cursor.execute(f"""
            SELECT streamer_id, COUNT(*) as cnt
            FROM sound_data
            WHERE record_date = '{target_date}'
            GROUP BY streamer_id
            HAVING cnt > 1
        """)
        
        duplicates = cursor.fetchall()
        if duplicates:
            print(f"\n⚠️ 发现重复记录:")
            for sid, cnt in duplicates:
                print(f"  ID={sid}, 重复次数={cnt}")
        else:
            print(f"\n✅ 没有重复记录")
        
        # 5. 检查CSV中的主播是否都导入了
        print(f"\n检查CSV中的主播是否都在数据库中:")
        csv_ids = [
            '44699721', '23006024', '23413141', '25359032', '27734455',
            '30285109', '36978005', '35342989', '39257189', '39949502'
        ]
        
        for csv_id in csv_ids:
            cursor.execute(f"""
                SELECT streamer_id, streamer_name, sound_wave
                FROM sound_data
                WHERE SUBSTRING(streamer_id, 1, 8) = '{csv_id}'
                AND record_date = '{target_date}'
            """)
            result = cursor.fetchone()
            if result:
                print(f"  ✅ {csv_id}: {result[1]}, 音浪={result[2]}")
            else:
                print(f"  ❌ {csv_id}: 未找到")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"❌ 错误: {e}")

if __name__ == '__main__':
    check_import_result('2026-01-18')
