#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查user表中的主播ID
"""

import mysql.connector

# 远程数据库配置
DB_CONFIG = {
    'host': 'your_mysql_host',
    'port': 3310,
    'user': 'your_username',
    'password': 'your_password',
    'database': 'your_username'
}

def check_haojie_in_user_table():
    """检查user表中浩杰的ID"""
    print("=" * 80)
    print("检查user表中浩杰的数据")
    print("=" * 80)
    
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor(dictionary=True)
        
        # 查询user表中浩杰的记录
        sql = "SELECT streamer_id, nickname FROM user WHERE nickname LIKE '%浩杰%'"
        cursor.execute(sql)
        results = cursor.fetchall()
        
        if results:
            print(f"\nuser表中找到 {len(results)} 条浩杰的记录:\n")
            for row in results:
                streamer_id = row['streamer_id']
                prefix = streamer_id[:10] if len(streamer_id) >= 10 else streamer_id
                print(f"  昵称: {row['nickname']}")
                print(f"  ID: {streamer_id}")
                print(f"  前10位: {prefix}")
                print()
        else:
            print("\n⚠️ user表中没有找到浩杰的记录!")
            print("这就是问题所在 - 主播名单中没有浩杰,所以无法匹配")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"❌ 查询失败: {e}")

def check_sound_data_vs_user():
    """对比sound_data和user表中的浩杰数据"""
    print("\n" + "=" * 80)
    print("对比sound_data和user表")
    print("=" * 80)
    
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor(dictionary=True)
        
        # 查询sound_data表中浩杰的ID
        print("\n1. sound_data表中浩杰的ID:")
        sql1 = """
            SELECT DISTINCT streamer_id, streamer_name
            FROM sound_data
            WHERE streamer_name LIKE '%浩杰%'
        """
        cursor.execute(sql1)
        sound_data_results = cursor.fetchall()
        
        for row in sound_data_results:
            streamer_id = row['streamer_id']
            prefix = streamer_id[:10] if len(streamer_id) >= 10 else streamer_id
            print(f"  ID: {streamer_id}, 前10位: {prefix}, 名字: {row['streamer_name']}")
        
        # 查询user表中浩杰的ID
        print("\n2. user表中浩杰的ID:")
        sql2 = "SELECT streamer_id, nickname FROM user WHERE nickname LIKE '%浩杰%'"
        cursor.execute(sql2)
        user_results = cursor.fetchall()
        
        if user_results:
            for row in user_results:
                streamer_id = row['streamer_id']
                prefix = streamer_id[:10] if len(streamer_id) >= 10 else streamer_id
                print(f"  ID: {streamer_id}, 前10位: {prefix}, 昵称: {row['nickname']}")
        else:
            print("  ⚠️ 没有找到")
        
        # 对比前10位
        print("\n3. 前10位匹配检查:")
        if sound_data_results and user_results:
            sound_prefix = sound_data_results[0]['streamer_id'][:10]
            user_prefix = user_results[0]['streamer_id'][:10]
            
            if sound_prefix == user_prefix:
                print(f"  ✅ 匹配成功! 前10位都是: {sound_prefix}")
            else:
                print(f"  ❌ 匹配失败!")
                print(f"    sound_data前10位: {sound_prefix}")
                print(f"    user前10位: {user_prefix}")
        elif not user_results:
            print("  ❌ user表中没有浩杰,无法匹配!")
            print("  解决方案: 需要在user表中添加浩杰的记录")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"❌ 对比失败: {e}")

def suggest_fix():
    """提供修复建议"""
    print("\n" + "=" * 80)
    print("修复建议")
    print("=" * 80)
    
    print("""
如果user表中没有浩杰的记录,需要添加:

INSERT INTO user (streamer_id, nickname) 
VALUES ('2263268204753467', '浩杰🎹');

或者从sound_data表中同步:

INSERT INTO user (streamer_id, nickname)
SELECT DISTINCT streamer_id, streamer_name
FROM sound_data
WHERE streamer_name LIKE '%浩杰%'
AND streamer_id NOT IN (SELECT streamer_id FROM user);
""")

if __name__ == '__main__':
    # 检查user表中浩杰的数据
    check_haojie_in_user_table()
    
    # 对比sound_data和user表
    check_sound_data_vs_user()
    
    # 提供修复建议
    suggest_fix()
