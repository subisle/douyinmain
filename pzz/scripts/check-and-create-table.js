/**
 * 检查并创建直播有效时长数据表
 * 使用方法：node check-and-create-table.js
 */

const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');

// 数据库配置。真实连接信息通过本地环境变量注入，不写入公开仓库。
const dbConfig = {
  host: process.env.DB_HOST || 'your_mysql_host',
  port: parseInt(process.env.DB_PORT || '3306', 10),
  user: process.env.DB_USER || 'your_username',
  password: process.env.DB_PASSWORD || 'your_password',
  database: process.env.DB_NAME || 'your_database',
  charset: 'utf8mb4'
};

// SQL文件路径
const sqlFilePath = path.join(__dirname, '..', 'duration_data_table.sql');

async function checkAndCreateTable() {
  let connection;
  
  try {
    console.log('========================================');
    console.log('检查直播有效时长数据表');
    console.log('========================================\n');
    
    console.log('正在连接远程数据库...');
    console.log(`数据库: ${dbConfig.host}:${dbConfig.port}/${dbConfig.database}`);
    
    // 创建数据库连接
    connection = await mysql.createConnection(dbConfig);
    console.log('✓ 数据库连接成功\n');
    
    // 检查表是否存在
    console.log('正在检查表 duration_data 是否存在...');
    const [tables] = await connection.execute(
      "SHOW TABLES LIKE 'duration_data'"
    );
    
    if (tables.length > 0) {
      console.log('✓ 表 duration_data 已存在\n');
      
      // 显示表结构
      console.log('表结构信息:');
      const [columns] = await connection.execute('DESCRIBE duration_data');
      
      console.log('┌─────────────────┬──────────────────┬──────┬─────┬─────────────────────┬────────────────┐');
      console.log('│ Field           │ Type             │ Null │ Key │ Default             │ Extra          │');
      console.log('├─────────────────┼──────────────────┼──────┼─────┼─────────────────────┼────────────────┤');
      columns.forEach(col => {
        const field = col.Field.padEnd(15);
        const type = col.Type.padEnd(16);
        const nullable = col.Null.padEnd(4);
        const key = col.Key.padEnd(3);
        const def = (col.Default || '').toString().padEnd(19);
        const extra = col.Extra.padEnd(14);
        console.log(`│ ${field} │ ${type} │ ${nullable} │ ${key} │ ${def} │ ${extra} │`);
      });
      console.log('└─────────────────┴──────────────────┴──────┴─────┴─────────────────────┴────────────────┘\n');
      
      // 显示索引
      console.log('索引信息:');
      const [indexes] = await connection.execute('SHOW INDEX FROM duration_data');
      
      const indexMap = {};
      indexes.forEach(idx => {
        if (!indexMap[idx.Key_name]) {
          indexMap[idx.Key_name] = {
            name: idx.Key_name,
            unique: idx.Non_unique === 0,
            columns: []
          };
        }
        indexMap[idx.Key_name].columns.push(idx.Column_name);
      });
      
      Object.values(indexMap).forEach(idx => {
        const type = idx.unique ? 'UNIQUE' : 'INDEX';
        const uniqueSymbol = idx.unique ? '🔒' : '📑';
        console.log(`  ${uniqueSymbol} ${type}: ${idx.name} (${idx.columns.join(', ')})`);
      });
      
      // 查询表中的数据量
      const [countResult] = await connection.execute(
        'SELECT COUNT(*) as count FROM duration_data'
      );
      console.log(`\n当前数据量: ${countResult[0].count} 条记录`);
      
      // 如果有数据，显示最近的几条
      if (countResult[0].count > 0) {
        console.log('\n最近的数据（最多5条）:');
        const [recentData] = await connection.execute(
          'SELECT streamer_id, streamer_name, duration, start_date, end_date FROM duration_data ORDER BY created_at DESC LIMIT 5'
        );
        
        console.log('┌──────────────────┬────────────────────┬──────────────────┬────────────┬────────────┐');
        console.log('│ 主播ID           │ 主播名称           │ 时长             │ 开始日期   │ 结束日期   │');
        console.log('├──────────────────┼────────────────────┼──────────────────┼────────────┼────────────┤');
        recentData.forEach(row => {
          const id = row.streamer_id.padEnd(16);
          const name = row.streamer_name.substring(0, 18).padEnd(18);
          const duration = row.duration.padEnd(16);
          const start = row.start_date.toISOString().split('T')[0].padEnd(10);
          const end = row.end_date.toISOString().split('T')[0].padEnd(10);
          console.log(`│ ${id} │ ${name} │ ${duration} │ ${start} │ ${end} │`);
        });
        console.log('└──────────────────┴────────────────────┴──────────────────┴────────────┴────────────┘');
      }
      
      console.log('\n✓ 表检查完成，无需创建');
      
    } else {
      console.log('✗ 表 duration_data 不存在\n');
      console.log('正在创建表...\n');
      
      // 读取SQL文件
      const sqlContent = fs.readFileSync(sqlFilePath, 'utf8');
      
      // 分割SQL语句
      const sqlStatements = sqlContent
        .split('\n')
        .filter(line => !line.trim().startsWith('--') && line.trim() !== '')
        .join('\n')
        .split(';')
        .map(stmt => stmt.trim())
        .filter(stmt => stmt.length > 0);
      
      // 执行SQL语句
      for (let i = 0; i < sqlStatements.length; i++) {
        const statement = sqlStatements[i];
        if (statement.trim()) {
          console.log(`执行SQL语句 ${i + 1}/${sqlStatements.length}...`);
          await connection.execute(statement);
          console.log('✓ 执行成功');
        }
      }
      
      // 验证表是否创建成功
      const [newTables] = await connection.execute(
        "SHOW TABLES LIKE 'duration_data'"
      );
      
      if (newTables.length > 0) {
        console.log('\n✓ 表 duration_data 创建成功！');
        
        // 显示表结构
        const [columns] = await connection.execute('DESCRIBE duration_data');
        console.log('\n表结构:');
        columns.forEach(col => {
          console.log(`  - ${col.Field}: ${col.Type} ${col.Null === 'NO' ? 'NOT NULL' : 'NULL'} ${col.Key ? `[${col.Key}]` : ''}`);
        });
      } else {
        console.error('\n✗ 表创建失败');
        process.exit(1);
      }
    }
    
    console.log('\n========================================');
    console.log('✓ 操作完成！');
    console.log('========================================');
    
  } catch (error) {
    console.error('\n========================================');
    console.error('✗ 错误发生');
    console.error('========================================');
    console.error('\n错误信息:', error.message);
    
    if (error.code === 'ENOTFOUND') {
      console.error('\n可能的原因:');
      console.error('  - 数据库主机名无法解析');
      console.error('  - 网络连接问题');
      console.error('  - 数据库服务器地址错误');
    } else if (error.code === 'ECONNREFUSED') {
      console.error('\n可能的原因:');
      console.error('  - 数据库服务未启动');
      console.error('  - 端口号错误');
      console.error('  - 防火墙阻止连接');
    } else if (error.code === 'ER_ACCESS_DENIED_ERROR') {
      console.error('\n可能的原因:');
      console.error('  - 用户名或密码错误');
      console.error('  - 用户没有访问权限');
    }
    
    console.error('\n详细错误:');
    console.error(error);
    process.exit(1);
  } finally {
    if (connection) {
      await connection.end();
      console.log('\n数据库连接已关闭');
    }
  }
}

// 执行检查和创建操作
checkAndCreateTable().catch(error => {
  console.error('执行失败:', error);
  process.exit(1);
});
