"""
测试ID前8位匹配逻辑
验证数据是否能正确显示
"""
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', 'pc', 'src'))

from database.connection import db as remote_db

def test_matching():
    """测试ID匹配"""
    
    print("=" * 80)
    print("测试ID前8位匹配逻辑")
    print("=" * 80)
    
    # 1. 查询user表中的主播
    user_sql = "SELECT id, nickname FROM user LIMIT 10"
    users = remote_db.execute_query(user_sql)
    
    print(f"\n主播名单（前10个）:")
    print(f"{'序号':<6} {'ID':<20} {'前8位':<12} {'昵称':<20}")
    print("-" * 80)
    
    for idx, user in enumerate(users, 1):
        user_id = user['id']
        nickname = user['nickname']
        prefix = user_id[:8] if len(user_id) >= 8 else user_id
        print(f"{idx:<6} {user_id:<20} {prefix:<12} {nickname:<20}")
    
    # 2. 查询2026-01-19的数据
    data_sql = """
    SELECT streamer_id, streamer_name, sound_wave, total_sound_wave
    FROM streamer_data
    WHERE record_date = '2026-01-19'
    ORDER BY total_sound_wave DESC
    LIMIT 10
    """
    
    data_list = remote_db.execute_query(data_sql)
    
    print(f"\n\n2026-01-19 数据（前10个）:")
    print(f"{'序号':<6} {'ID':<20} {'前8位':<12} {'主播名':<20} {'日音浪':<12} {'总音浪':<12}")
    print("-" * 100)
    
    for idx, data in enumerate(data_list, 1):
        data_id = data['streamer_id']
        name = data['streamer_name']
        sound_wave = data['sound_wave']
        total_sound_wave = data['total_sound_wave']
        prefix = data_id[:8] if len(data_id) >= 8 else data_id
        print(f"{idx:<6} {data_id:<20} {prefix:<12} {name:<20} {sound_wave:<12,} {total_sound_wave:<12,}")
    
    # 3. 测试匹配
    print(f"\n\n匹配测试:")
    print("-" * 80)
    
    for user in users[:5]:  # 测试前5个
        user_id = user['id']
        nickname = user['nickname']
        user_prefix = user_id[:8] if len(user_id) >= 8 else user_id
        
        # 查找匹配的数据
        matched = False
        for data in data_list:
            data_id = data['streamer_id']
            data_prefix = data_id[:8] if len(data_id) >= 8 else data_id
            
            if user_prefix == data_prefix:
                matched = True
                print(f"✅ {nickname:15s} | 前8位: {user_prefix} | 匹配到: {data['streamer_name']:15s} | 总音浪: {data['total_sound_wave']:,}")
                break
        
        if not matched:
            print(f"❌ {nickname:15s} | 前8位: {user_prefix} | 未匹配")

if __name__ == '__main__':
    test_matching()
