#!/usr/bin/env python3
"""
测试远程数据库连接
验证新的数据库配置是否正确
"""

import pymysql
import sys

# 数据库配置（与AppConfig.kt保持一致）
DB_CONFIG = {
    'host': 'your_mysql_host',
    'port': 3310,
    'user': 'your_username',
    'password': 'your_password',
    'database': 'your_database',
    'charset': 'utf8mb4'
}

def test_connection():
    """测试数据库连接"""
    print("=" * 60)
    print("测试远程数据库连接")
    print("=" * 60)
    print(f"\n数据库地址: {DB_CONFIG['host']}:{DB_CONFIG['port']}")
    print(f"数据库名称: {DB_CONFIG['database']}")
    print(f"用户名: {DB_CONFIG['user']}")
    print("\n正在连接...")
    
    try:
        # 尝试连接
        conn = pymysql.connect(**DB_CONFIG)
        print("✅ 连接成功！\n")
        
        cursor = conn.cursor(pymysql.cursors.DictCursor)
        
        # 1. 检查数据库版本
        cursor.execute("SELECT VERSION() as version")
        result = cursor.fetchone()
        print(f"MySQL版本: {result['version']}")
        
        # 2. 列出所有表
        cursor.execute("SHOW TABLES")
        tables = cursor.fetchall()
        print(f"\n数据库中的表（共{len(tables)}个）：")
        for table in tables:
            table_name = list(table.values())[0]
            print(f"  - {table_name}")
        
        # 3. 检查streamer_data表是否存在
        cursor.execute("SHOW TABLES LIKE 'streamer_data'")
        if cursor.fetchone():
            print("\n✅ 找到 streamer_data 表")
            
            # 查询表结构
            cursor.execute("DESCRIBE streamer_data")
            columns = cursor.fetchall()
            print("\n表结构：")
            print(f"{'字段名':<25} {'类型':<20} {'允许NULL':<10} {'键':<10}")
            print("-" * 70)
            for col in columns:
                print(f"{col['Field']:<25} {col['Type']:<20} {col['Null']:<10} {col['Key']:<10}")
            
            # 统计数据量
            cursor.execute("SELECT COUNT(*) as total FROM streamer_data")
            result = cursor.fetchone()
            print(f"\n总记录数: {result['total']}")
            
            # 查询日期范围
            cursor.execute("""
                SELECT 
                    MIN(record_date) as min_date,
                    MAX(record_date) as max_date,
                    COUNT(DISTINCT record_date) as date_count
                FROM streamer_data
            """)
            result = cursor.fetchone()
            if result['min_date']:
                print(f"日期范围: {result['min_date']} 到 {result['max_date']}")
                print(f"不同日期数: {result['date_count']}")
            else:
                print("⚠️  表中暂无数据")
                
        else:
            print("\n⚠️  未找到 streamer_data 表")
            print("提示：可能需要先创建表结构")
        
        # 4. 检查user表是否存在
        cursor.execute("SHOW TABLES LIKE 'user'")
        if cursor.fetchone():
            print("\n✅ 找到 user 表")
            
            # 统计主播数量
            cursor.execute("SELECT COUNT(*) as total FROM user")
            result = cursor.fetchone()
            print(f"主播总数: {result['total']}")
            
            # 显示前5个主播
            cursor.execute("SELECT streamer_id, nickname, gender FROM user LIMIT 5")
            users = cursor.fetchall()
            if users:
                print("\n前5个主播：")
                for user in users:
                    print(f"  - {user['nickname']} (ID: {user['streamer_id']}, 性别: {user['gender']})")
        else:
            print("\n⚠️  未找到 user 表")
            print("提示：可能需要先创建表结构")
        
        # 5. 检查duration_data表是否存在
        cursor.execute("SHOW TABLES LIKE 'duration_data'")
        if cursor.fetchone():
            print("\n✅ 找到 duration_data 表")
            
            cursor.execute("SELECT COUNT(*) as total FROM duration_data")
            result = cursor.fetchone()
            print(f"时长数据记录数: {result['total']}")
        else:
            print("\n⚠️  未找到 duration_data 表")
        
        cursor.close()
        conn.close()
        
        print("\n" + "=" * 60)
        print("✅ 数据库连接测试完成！")
        print("=" * 60)
        return True
        
    except pymysql.Error as e:
        print(f"\n❌ 连接失败！")
        print(f"错误代码: {e.args[0]}")
        print(f"错误信息: {e.args[1]}")
        print("\n可能的原因：")
        print("  1. 数据库地址或端口错误")
        print("  2. 用户名或密码错误")
        print("  3. 数据库不存在")
        print("  4. 网络连接问题")
        print("  5. 防火墙阻止连接")
        print("\n" + "=" * 60)
        return False
        
    except Exception as e:
        print(f"\n❌ 发生异常: {e}")
        return False

def create_tables():
    """创建必要的表结构（如果不存在）"""
    print("\n是否需要创建表结构？(y/n): ", end='')
    choice = input().strip().lower()
    
    if choice != 'y':
        return
    
    try:
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor()
        
        print("\n正在创建表结构...")
        
        # 创建streamer_data表
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS streamer_data (
                id INT AUTO_INCREMENT PRIMARY KEY,
                streamer_id VARCHAR(50) NOT NULL,
                streamer_name VARCHAR(100) NOT NULL,
                sound_wave BIGINT DEFAULT 0,
                total_sound_wave BIGINT DEFAULT 0,
                record_date DATE NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY unique_streamer_date (streamer_id, record_date),
                INDEX idx_record_date (record_date),
                INDEX idx_streamer_id (streamer_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """)
        print("✅ streamer_data 表创建成功")
        
        # 创建user表
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS user (
                id INT AUTO_INCREMENT PRIMARY KEY,
                serial_number INT NOT NULL,
                streamer_id VARCHAR(50) NOT NULL UNIQUE,
                nickname VARCHAR(100) NOT NULL,
                gender ENUM('male', 'female') NOT NULL DEFAULT 'male',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_streamer_id (streamer_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """)
        print("✅ user 表创建成功")
        
        # 创建duration_data表
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS duration_data (
                id INT AUTO_INCREMENT PRIMARY KEY,
                streamer_id VARCHAR(50) NOT NULL,
                streamer_name VARCHAR(100) NOT NULL,
                duration VARCHAR(20) NOT NULL,
                start_date DATE NOT NULL,
                end_date DATE NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY unique_streamer_daterange (streamer_id, start_date, end_date),
                INDEX idx_date_range (start_date, end_date)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """)
        print("✅ duration_data 表创建成功")
        
        conn.commit()
        cursor.close()
        conn.close()
        
        print("\n✅ 所有表创建完成！")
        
    except Exception as e:
        print(f"\n❌ 创建表失败: {e}")

if __name__ == '__main__':
    success = test_connection()
    
    if success:
        create_tables()
        sys.exit(0)
    else:
        sys.exit(1)
