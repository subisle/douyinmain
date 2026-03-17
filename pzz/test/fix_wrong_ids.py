#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修复数据库中ID错误的记录
"""

import mysql.connector

# 远程数据库配置
DB_CONFIG = {
    'host': '47.109.78.124',
    'port': 3306,
    'user': 'pzz',
    'password': 'pzz666888',
    'database': 'pzz'
}

def fix_haojie_data():
    """修复浩杰的错误数据"""
    print("=" * 80)
    print("修复浩杰的错误数据")
    print("=" * 80)
    
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor(dictionary=True)
        
        # 1. 查看当前浩杰的所有记录
        print("\n1. 当前浩杰的所有记录:")
        sql = """
            SELECT streamer_id, streamer_name, sound_wave, total_sound_wave, record_date
            FROM sound_data
            WHERE streamer_name LIKE '%浩杰%'
            ORDER BY record_date DESC
        """
        cursor.execute(sql)
        results = cursor.fetchall()
        
        for row in results:
            print(f"  ID: {row['streamer_id']}, 名字: {row['streamer_name']}, "
                  f"日音浪: {row['sound_wave']}, 总音浪: {row['total_sound_wave']}, "
                  f"日期: {row['record_date']}")
        
        # 2. 删除ID错误的记录
        print("\n2. 删除ID错误的记录 (ID: 2203268204753467)...")
        delete_sql = "DELETE FROM sound_data WHERE streamer_id = '2203268204753467'"
        cursor.execute(delete_sql)
        conn.commit()
        deleted_count = cursor.rowcount
        print(f"  已删除 {deleted_count} 条记录")
        
        # 3. 验证删除后的结果
        print("\n3. 删除后浩杰的记录:")
        cursor.execute(sql)
        results = cursor.fetchall()
        
        for row in results:
            print(f"  ID: {row['streamer_id']}, 名字: {row['streamer_name']}, "
                  f"日音浪: {row['sound_wave']}, 总音浪: {row['total_sound_wave']}, "
                  f"日期: {row['record_date']}")
        
        cursor.close()
        conn.close()
        
        print("\n✅ 修复完成!")
        
    except Exception as e:
        print(f"❌ 修复失败: {e}")

def check_all_wrong_ids():
    """检查所有ID错误的记录"""
    print("\n" + "=" * 80)
    print("检查所有ID错误的记录")
    print("=" * 80)
    
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor(dictionary=True)
        
        # 查找所有ID前10位不在user表中的记录
        sql = """
            SELECT DISTINCT s.streamer_id, s.streamer_name, 
                   SUBSTRING(s.streamer_id, 1, 10) as id_prefix,
                   COUNT(*) as record_count
            FROM sound_data s
            WHERE NOT EXISTS (
                SELECT 1 FROM user u 
                WHERE SUBSTRING(s.streamer_id, 1, 10) = SUBSTRING(u.streamer_id, 1, 10)
            )
            AND s.streamer_name NOT LIKE '%账号已注销%'
            GROUP BY s.streamer_id, s.streamer_name
            ORDER BY s.streamer_name
        """
        
        cursor.execute(sql)
        results = cursor.fetchall()
        
        if results:
            print(f"\n找到 {len(results)} 个ID错误的主播:\n")
            for row in results:
                print(f"  名字: {row['streamer_name']}")
                print(f"    ID: {row['streamer_id']}")
                print(f"    前10位: {row['id_prefix']}")
                print(f"    记录数: {row['record_count']}")
                print()
        else:
            print("\n✅ 没有发现ID错误的记录")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"❌ 检查失败: {e}")

if __name__ == '__main__':
    # 修复浩杰的数据
    fix_haojie_data()
    
    # 检查是否还有其他ID错误的记录
    check_all_wrong_ids()
