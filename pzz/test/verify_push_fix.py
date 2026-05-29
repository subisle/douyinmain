#!/usr/bin/env python3
"""
验证推送功能修复的脚本
检查远程数据库中2026-01-18的数据是否完整
"""

import pymysql
import sys

# 数据库配置
DB_CONFIG = {
    'host': 'your_mysql_host',
    'port': 3310,
    'user': 'your_username',
    'password': 'your_password',
    'database': 'your_database',
    'charset': 'utf8mb4'
}

def check_push_result(date='2026-01-18'):
    """检查指定日期的推送结果"""
    try:
        # 连接数据库
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor(pymysql.cursors.DictCursor)
        
        print(f"========== 检查 {date} 的推送结果 ==========\n")
        
        # 1. 统计总记录数
        cursor.execute(f"""
            SELECT COUNT(*) as total 
            FROM sound_data 
            WHERE record_date = '{date}'
        """)
        result = cursor.fetchone()
        total_count = result['total']
        print(f"✓ 总记录数: {total_count}")
        
        # 2. 检查是否有56条记录（预期数量）
        expected_count = 56
        if total_count == expected_count:
            print(f"✅ 记录数正确！（预期{expected_count}条）\n")
        elif total_count > 0:
            print(f"⚠️  记录数不完整：实际{total_count}条，预期{expected_count}条\n")
        else:
            print(f"❌ 没有找到任何记录！\n")
            return False
        
        # 3. 显示前10条记录
        cursor.execute(f"""
            SELECT streamer_id, streamer_name, sound_wave, total_sound_wave
            FROM sound_data 
            WHERE record_date = '{date}'
            ORDER BY total_sound_wave DESC
            LIMIT 10
        """)
        records = cursor.fetchall()
        
        print("前10名主播数据：")
        print("-" * 80)
        print(f"{'排名':<6} {'主播ID':<20} {'昵称':<15} {'日音浪':<12} {'总音浪':<12}")
        print("-" * 80)
        
        for i, record in enumerate(records, 1):
            print(f"{i:<6} {record['streamer_id']:<20} {record['streamer_name']:<15} "
                  f"{record['sound_wave']:<12} {record['total_sound_wave']:<12}")
        
        print("-" * 80)
        
        # 4. 检查是否有音浪为0的异常数据
        cursor.execute(f"""
            SELECT COUNT(*) as zero_count
            FROM sound_data 
            WHERE record_date = '{date}' AND sound_wave = 0
        """)
        result = cursor.fetchone()
        zero_count = result['zero_count']
        
        if zero_count > 0:
            print(f"\n⚠️  发现 {zero_count} 条日音浪为0的记录")
            
            # 显示这些记录
            cursor.execute(f"""
                SELECT streamer_id, streamer_name, sound_wave, total_sound_wave
                FROM sound_data 
                WHERE record_date = '{date}' AND sound_wave = 0
                LIMIT 5
            """)
            zero_records = cursor.fetchall()
            
            print("\n日音浪为0的记录示例：")
            for record in zero_records:
                print(f"  - {record['streamer_name']} (ID: {record['streamer_id']})")
        else:
            print(f"\n✅ 所有记录的日音浪都大于0")
        
        # 5. 检查总音浪是否合理
        cursor.execute(f"""
            SELECT 
                MIN(total_sound_wave) as min_total,
                MAX(total_sound_wave) as max_total,
                AVG(total_sound_wave) as avg_total
            FROM sound_data 
            WHERE record_date = '{date}'
        """)
        result = cursor.fetchone()
        
        print(f"\n总音浪统计：")
        print(f"  最小值: {result['min_total']}")
        print(f"  最大值: {result['max_total']}")
        print(f"  平均值: {result['avg_total']:.2f}")
        
        cursor.close()
        conn.close()
        
        print(f"\n========== 检查完成 ==========")
        return total_count == expected_count
        
    except Exception as e:
        print(f"❌ 检查失败: {e}")
        return False

def compare_with_previous_day(date='2026-01-18'):
    """对比前一天的数据，检查总音浪计算是否正确"""
    try:
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor(pymysql.cursors.DictCursor)
        
        print(f"\n========== 对比前一天数据 ==========\n")
        
        # 计算前一天日期
        from datetime import datetime, timedelta
        current_date = datetime.strptime(date, '%Y-%m-%d')
        previous_date = (current_date - timedelta(days=1)).strftime('%Y-%m-%d')
        
        print(f"当前日期: {date}")
        print(f"前一天: {previous_date}\n")
        
        # 查询几个主播的数据进行对比
        cursor.execute(f"""
            SELECT 
                t1.streamer_name,
                t1.sound_wave as today_sound,
                t1.total_sound_wave as today_total,
                t2.total_sound_wave as prev_total
            FROM sound_data t1
            LEFT JOIN sound_data t2 
                ON t1.streamer_id = t2.streamer_id 
                AND t2.record_date = '{previous_date}'
            WHERE t1.record_date = '{date}'
            ORDER BY t1.total_sound_wave DESC
            LIMIT 5
        """)
        
        records = cursor.fetchall()
        
        print("总音浪计算验证（前5名）：")
        print("-" * 90)
        print(f"{'主播':<15} {'前一天总音浪':<15} {'当日音浪':<15} {'当日总音浪':<15} {'计算正确':<10}")
        print("-" * 90)
        
        all_correct = True
        for record in records:
            prev_total = record['prev_total'] or 0
            today_sound = record['today_sound']
            today_total = record['today_total']
            expected_total = prev_total + today_sound
            is_correct = (today_total == expected_total)
            
            status = "✅" if is_correct else "❌"
            print(f"{record['streamer_name']:<15} {prev_total:<15} {today_sound:<15} "
                  f"{today_total:<15} {status:<10}")
            
            if not is_correct:
                print(f"  预期: {expected_total}, 实际: {today_total}")
                all_correct = False
        
        print("-" * 90)
        
        if all_correct:
            print("\n✅ 总音浪计算全部正确！")
        else:
            print("\n⚠️  发现总音浪计算错误")
        
        cursor.close()
        conn.close()
        
        return all_correct
        
    except Exception as e:
        print(f"❌ 对比失败: {e}")
        return False

if __name__ == '__main__':
    date = sys.argv[1] if len(sys.argv) > 1 else '2026-01-18'
    
    # 检查推送结果
    push_ok = check_push_result(date)
    
    # 对比前一天数据
    calc_ok = compare_with_previous_day(date)
    
    # 总结
    print("\n" + "=" * 50)
    if push_ok and calc_ok:
        print("✅ 所有检查通过！推送功能正常工作。")
        sys.exit(0)
    else:
        print("⚠️  发现问题，请检查日志。")
        sys.exit(1)
