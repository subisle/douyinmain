#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
调试总音浪导入问题
"""

import csv
import sys

def analyze_csv(csv_file):
    """分析CSV文件"""
    print("=" * 80)
    print("CSV文件分析")
    print("=" * 80)
    
    with open(csv_file, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        
        # 读取表头
        headers = next(reader)
        print(f"\n表头 ({len(headers)}列):")
        for i, header in enumerate(headers):
            print(f"  [{i}] {header}")
        
        # 查找关键列
        id_index = -1
        sound_index = -1
        name_index = -1
        
        for i, header in enumerate(headers):
            if 'ID' in header or '主播ID' in header:
                id_index = i
            if '音浪' in header:
                sound_index = i
            if '主播名' in header or '名' in header:
                name_index = i
        
        print(f"\n关键列索引:")
        print(f"  ID列: {id_index}")
        print(f"  音浪列: {sound_index}")
        print(f"  名称列: {name_index}")
        
        # 读取前5行数据
        print(f"\n前5行数据:")
        for i, row in enumerate(reader):
            if i >= 5:
                break
            
            if len(row) > max(id_index, sound_index, name_index):
                streamer_id = row[id_index] if id_index >= 0 else ""
                sound_str = row[sound_index] if sound_index >= 0 else ""
                name = row[name_index] if name_index >= 0 else ""
                
                # 解析音浪
                sound_value = sound_str.replace('音浪', '').replace(',', '').strip()
                
                # 提取ID前8位
                id_prefix = streamer_id[:8] if len(streamer_id) >= 8 else streamer_id
                
                print(f"\n  行{i+1}:")
                print(f"    完整ID: {streamer_id}")
                print(f"    ID前8位: {id_prefix}")
                print(f"    名称: {name}")
                print(f"    原始音浪: {sound_str}")
                print(f"    解析音浪: {sound_value}")
        
        # 统计总行数
        f.seek(0)
        next(f)  # 跳过表头
        total_rows = sum(1 for _ in f)
        print(f"\n总数据行数: {total_rows}")

def check_database_ids():
    """检查数据库中的主播ID"""
    print("\n" + "=" * 80)
    print("数据库主播ID检查")
    print("=" * 80)
    
    try:
        import mysql.connector
        
        conn = mysql.connector.connect(
            host='your_mysql_host',
            port=3306,
            user='pzz',
            password='your_password',
            database='pzz'
        )
        
        cursor = conn.cursor()
        
        # 查询所有主播ID（去重）
        cursor.execute("""
            SELECT DISTINCT streamer_id, streamer_name
            FROM sound_data
            ORDER BY streamer_id
        """)
        
        streamers = cursor.fetchall()
        print(f"\n数据库中共有 {len(streamers)} 个主播")
        
        # 显示前10个
        print(f"\n前10个主播:")
        for i, (sid, name) in enumerate(streamers[:10]):
            prefix = sid[:8] if len(sid) >= 8 else sid
            print(f"  {i+1}. ID={sid}, 前8位={prefix}, 名称={name}")
        
        # 检查特定主播（浩杰）
        cursor.execute("""
            SELECT streamer_id, streamer_name, record_date, sound_wave, total_sound_wave
            FROM sound_data
            WHERE streamer_name LIKE '%浩杰%'
            ORDER BY record_date DESC
            LIMIT 5
        """)
        
        haojie_data = cursor.fetchall()
        if haojie_data:
            print(f"\n浩杰的数据:")
            for sid, name, date, daily, total in haojie_data:
                prefix = sid[:8] if len(sid) >= 8 else sid
                print(f"  日期={date}, ID={sid}, 前8位={prefix}, 日音浪={daily}, 总音浪={total}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"\n数据库连接失败: {e}")
        print("请确保已安装 mysql-connector-python: pip install mysql-connector-python")

if __name__ == '__main__':
    csv_file = '截至19日总音浪.csv'
    
    # 分析CSV文件
    analyze_csv(csv_file)
    
    # 检查数据库
    check_database_ids()
