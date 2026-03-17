#!/usr/bin/env python3
"""
诊断总音浪显示问题
检查某个主播在不同日期的数据
"""

import pymysql
import sys

# 数据库配置
DB_CONFIG = {
    'host': 'your_mysql_host',
    'port': 3310,
    'user': 'your_username',
    'password': 'your_password',
    'database': 'your_username',
    'charset': 'utf8mb4'
}

def diagnose_streamer(streamer_name_pattern=''):
    """诊断指定主播的数据"""
    try:
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor(pymysql.cursors.DictCursor)
        
        print("=" * 80)
        print("诊断总音浪问题")
        print("=" * 80)
        
        # 1. 查找主播
        if streamer_name_pattern:
            cursor.execute(f"""
                SELECT streamer_id, nickname 
                FROM user 
                WHERE nickname LIKE '%{streamer_name_pattern}%'
                LIMIT 10
            """)
        else:
            cursor.execute("""
                SELECT streamer_id, nickname 
                FROM user 
                ORDER BY serial_number
                LIMIT 10
            """)
        
        streamers = cursor.fetchall()
        
        if not streamers:
            print(f"未找到匹配的主播: {streamer_name_pattern}")
            return
        
        print(f"\n找到 {len(streamers)} 个主播：")
        for i, s in enumerate(streamers, 1):
            print(f"{i}. {s['nickname']} (ID: {s['streamer_id']})")
        
        # 选择主播
        if len(streamers) == 1:
            selected = streamers[0]
        else:
            choice = input(f"\n请选择主播编号 (1-{len(streamers)}): ").strip()
            try:
                idx = int(choice) - 1
                selected = streamers[idx]
            except:
                print("无效选择")
                return
        
        streamer_id = selected['streamer_id']
        streamer_name = selected['nickname']
        
        print(f"\n{'=' * 80}")
        print(f"主播: {streamer_name}")
        print(f"ID: {streamer_id}")
        print(f"ID前8位: {streamer_id[:8]}")
        print(f"{'=' * 80}")
        
        # 2. 查询该主播的所有历史数据
        cursor.execute(f"""
            SELECT 
                record_date,
                sound_wave,
                total_sound_wave,
                created_at
            FROM streamer_data
            WHERE streamer_id = '{streamer_id}'
            ORDER BY record_date DESC
            LIMIT 30
        """)
        
        records = cursor.fetchall()
        
        if not records:
            print(f"\n⚠️  数据库中没有该主播的任何记录")
            
            # 尝试用ID前8位查询
            prefix = streamer_id[:8]
            cursor.execute(f"""
                SELECT 
                    streamer_id,
                    record_date,
                    sound_wave,
                    total_sound_wave
                FROM streamer_data
                WHERE SUBSTRING(streamer_id, 1, 8) = '{prefix}'
                ORDER BY record_date DESC
                LIMIT 10
            """)
            
            prefix_records = cursor.fetchall()
            if prefix_records:
                print(f"\n但找到ID前8位匹配的记录：")
                for r in prefix_records:
                    print(f"  {r['record_date']}: ID={r['streamer_id']}, 日音浪={r['sound_wave']}, 总音浪={r['total_sound_wave']}")
            
            return
        
        print(f"\n找到 {len(records)} 条历史记录：\n")
        print(f"{'日期':<12} {'日音浪':<12} {'总音浪':<12} {'计算正确':<10} {'说明'}")
        print("-" * 80)
        
        prev_total = None
        issues = []
        
        for i, record in enumerate(records):
            date = record['record_date']
            sound = record['sound_wave']
            total = record['total_sound_wave']
            
            # 检查计算是否正确
            if prev_total is not None:
                expected_total = prev_total + sound
                is_correct = (total == expected_total)
                status = "✅" if is_correct else "❌"
                
                if not is_correct:
                    note = f"应为{expected_total}"
                    issues.append({
                        'date': date,
                        'sound': sound,
                        'total': total,
                        'expected': expected_total,
                        'prev_total': prev_total
                    })
                else:
                    note = ""
            else:
                status = "—"
                note = "最新记录"
            
            print(f"{date!s:<12} {sound:<12} {total:<12} {status:<10} {note}")
            
            # 更新prev_total（向前推）
            prev_total = total - sound
        
        # 3. 分析问题
        if issues:
            print(f"\n{'=' * 80}")
            print(f"发现 {len(issues)} 个问题：")
            print(f"{'=' * 80}")
            
            for issue in issues:
                print(f"\n日期: {issue['date']}")
                print(f"  日音浪: {issue['sound']}")
                print(f"  当前总音浪: {issue['total']}")
                print(f"  前一天总音浪: {issue['prev_total']}")
                print(f"  预期总音浪: {issue['expected']} (= {issue['prev_total']} + {issue['sound']})")
                print(f"  差异: {issue['total'] - issue['expected']}")
        else:
            print(f"\n✅ 所有记录的总音浪计算都正确！")
        
        # 4. 检查是否有日音浪为0但总音浪也为0的情况
        zero_issues = [r for r in records if r['sound_wave'] == 0 and r['total_sound_wave'] == 0]
        if zero_issues:
            print(f"\n{'=' * 80}")
            print(f"⚠️  发现 {len(zero_issues)} 个日期日音浪为0且总音浪也为0：")
            print(f"{'=' * 80}")
            for r in zero_issues:
                print(f"  {r['record_date']}: 日音浪=0, 总音浪=0 ← 这是错误的！")
            print("\n说明：当主播未开播时，日音浪应为0，但总音浪应保持前一天的值")
        
        # 5. 生成修复SQL
        if issues or zero_issues:
            print(f"\n{'=' * 80}")
            print("修复建议：")
            print(f"{'=' * 80}")
            
            # 按日期正序排列
            all_records = sorted(records, key=lambda x: x['record_date'])
            
            print("\n方法1：重新计算该主播的所有总音浪")
            print(f"运行以下Python脚本：")
            print(f"  python pzz/test/fix_streamer_total.py '{streamer_id}'")
            
            print("\n方法2：手动修复SQL（按日期顺序执行）")
            cumulative_total = 0
            for record in all_records:
                cumulative_total += record['sound_wave']
                if record['total_sound_wave'] != cumulative_total:
                    print(f"UPDATE streamer_data SET total_sound_wave = {cumulative_total} WHERE streamer_id = '{streamer_id}' AND record_date = '{record['record_date']}';")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"❌ 诊断失败: {e}")
        import traceback
        traceback.print_exc()

if __name__ == '__main__':
    if len(sys.argv) > 1:
        pattern = sys.argv[1]
    else:
        pattern = input("请输入主播昵称（部分匹配，留空显示前10个）: ").strip()
    
    diagnose_streamer(pattern)
