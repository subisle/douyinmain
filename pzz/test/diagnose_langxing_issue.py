#!/usr/bin/env python3
"""
诊断狼兴(ID=4469972183296288)推送失败的问题
"""

import pymysql

# 数据库配置
DB_CONFIG = {
    'host': 'your_mysql_host',
    'port': 3310,
    'user': 'your_username',
    'password': 'your_password',
    'database': 'your_username',
    'charset': 'utf8mb4'
}

def diagnose_langxing():
    """诊断狼兴的数据问题"""
    try:
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor(pymysql.cursors.DictCursor)
        
        print("========== 诊断狼兴推送失败问题 ==========\n")
        
        # 1. 检查user表中的狼兴记录
        print("1. 检查user表中的狼兴记录：")
        cursor.execute("""
            SELECT * FROM user 
            WHERE nickname LIKE '%狼兴%' OR streamer_id LIKE '4469972183296288%'
        """)
        user_records = cursor.fetchall()
        
        if user_records:
            for record in user_records:
                print(f"  ID: {record['streamer_id']}")
                print(f"  昵称: {record['nickname']}")
                print(f"  性别: {record['gender']}")
                print(f"  序号: {record['serial_number']}")
        else:
            print("  ⚠️ user表中没有找到狼兴的记录")
        
        # 2. 检查streamer_data表中的狼兴记录
        print("\n2. 检查streamer_data表中的狼兴记录：")
        cursor.execute("""
            SELECT * FROM streamer_data 
            WHERE streamer_name LIKE '%狼兴%' OR streamer_id LIKE '4469972183296288%'
            ORDER BY record_date DESC
            LIMIT 5
        """)
        sound_records = cursor.fetchall()
        
        if sound_records:
            for record in sound_records:
                print(f"  日期: {record['record_date']}")
                print(f"  ID: {record['streamer_id']}")
                print(f"  昵称: {record['streamer_name']}")
                print(f"  日音浪: {record['sound_wave']}")
                print(f"  总音浪: {record['total_sound_wave']}")
                print()
        else:
            print("  ⚠️ streamer_data表中没有找到狼兴的记录")
        
        # 3. 检查ID前8位匹配的记录
        print("\n3. 检查ID前8位(44699721)匹配的记录：")
        cursor.execute("""
            SELECT * FROM streamer_data 
            WHERE SUBSTRING(streamer_id, 1, 8) = '44699721'
            ORDER BY record_date DESC
            LIMIT 5
        """)
        prefix_records = cursor.fetchall()
        
        if prefix_records:
            for record in prefix_records:
                print(f"  日期: {record['record_date']}")
                print(f"  ID: {record['streamer_id']}")
                print(f"  昵称: {record['streamer_name']}")
                print(f"  日音浪: {record['sound_wave']}")
                print(f"  总音浪: {record['total_sound_wave']}")
                print()
        else:
            print("  ⚠️ 没有找到ID前8位匹配的记录")
        
        # 4. 尝试手动插入测试
        print("\n4. 尝试手动插入测试（2026-01-22）：")
        test_date = '2026-01-22'
        
        # 先删除测试数据
        cursor.execute(f"""
            DELETE FROM streamer_data 
            WHERE streamer_id = '4469972183296288' AND record_date = '{test_date}'
        """)
        conn.commit()
        
        # 尝试插入
        try:
            cursor.execute(f"""
                INSERT INTO streamer_data 
                (streamer_id, streamer_name, sound_wave, total_sound_wave, record_date)
                VALUES ('4469972183296288', '狼兴', 100, 100, '{test_date}')
            """)
            conn.commit()
            print("  ✅ 手动插入成功！")
            
            # 验证插入
            cursor.execute(f"""
                SELECT * FROM streamer_data 
                WHERE streamer_id = '4469972183296288' AND record_date = '{test_date}'
            """)
            result = cursor.fetchone()
            if result:
                print(f"  验证: ID={result['streamer_id']}, 名称={result['streamer_name']}")
            
            # 清理测试数据
            cursor.execute(f"""
                DELETE FROM streamer_data 
                WHERE streamer_id = '4469972183296288' AND record_date = '{test_date}'
            """)
            conn.commit()
            
        except Exception as e:
            print(f"  ❌ 手动插入失败: {e}")
        
        cursor.close()
        conn.close()
        
        print("\n========== 诊断完成 ==========")
        
    except Exception as e:
        print(f"❌ 诊断失败: {e}")

if __name__ == '__main__':
    diagnose_langxing()
