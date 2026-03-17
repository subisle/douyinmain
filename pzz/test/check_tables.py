#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查数据库中的表
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

def check_tables():
    """检查数据库中的所有表"""
    print("=" * 80)
    print("检查数据库中的表")
    print("=" * 80)
    
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()
        
        # 查询所有表
        cursor.execute("SHOW TABLES")
        tables = cursor.fetchall()
        
        print(f"\n数据库 'your_username' 中的表 ({len(tables)}个):\n")
        for table in tables:
            print(f"  - {table[0]}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"❌ 查询失败: {e}")

def check_haojie_in_all_tables():
    """在所有表中查找浩杰的数据"""
    print("\n" + "=" * 80)
    print("在所有表中查找浩杰")
    print("=" * 80)
    
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()
        
        # 获取所有表名
        cursor.execute("SHOW TABLES")
        tables = [table[0] for table in cursor.fetchall()]
        
        for table_name in tables:
            try:
                # 尝试查询包含浩杰的记录
                sql = f"SELECT * FROM {table_name} WHERE "
                
                # 获取表的列名
                cursor.execute(f"SHOW COLUMNS FROM {table_name}")
                columns = [col[0] for col in cursor.fetchall()]
                
                # 构建WHERE子句 - 在所有文本列中搜索
                text_columns = []
                for col in columns:
                    text_columns.append(f"{col} LIKE '%浩杰%'")
                
                if text_columns:
                    sql += " OR ".join(text_columns)
                    sql += " LIMIT 5"
                    
                    cursor.execute(sql)
                    results = cursor.fetchall()
                    
                    if results:
                        print(f"\n表 '{table_name}' 中找到 {len(results)} 条记录:")
                        print(f"  列名: {', '.join(columns)}")
                        for row in results:
                            print(f"  数据: {row}")
                            
            except Exception as e:
                # 跳过查询失败的表
                pass
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"❌ 查询失败: {e}")

if __name__ == '__main__':
    check_tables()
    check_haojie_in_all_tables()
