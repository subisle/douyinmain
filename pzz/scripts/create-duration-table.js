/**
 * 创建直播有效时长数据表
 * 使用方法：node create-duration-table.js
 */

const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');

// 数据库配置（使用生产环境的远程数据库）
const dbConfig = {
  host: 'your_mysql_host',
  port: 3306,
  user: 'root',
  password: 'your_password',
  database: 'xs',
  charset: 'utf8mb4'
};

// SQL文件路径
const sqlFilePath = path.join(__dirname, '..', 'duration_data_table.sql');

async function createTable() {
  let connection;
  
  try {
    console.log('正在连接数据库...');
    console.log(`数据库: ${dbConfig.host}:${dbConfig.port}/${dbConfig.database}`);
    
    // 创建数据库连接
    connection = await mysql.createConnection(dbConfig);
    console.log('✓ 数据库连接成功');
    
    // 读取SQL文件
    console.log('\n正在读取SQL文件...');
    const sqlContent = fs.readFileSync(sqlFilePath, 'utf8');
    console.log('✓ SQL文件读取成功');
    
    // 分割SQL语句（按分号分割，但忽略注释中的分号）
    const sqlStatements = sqlContent
      .split('\n')
      .filter(line => !line.trim().startsWith('--') && line.trim() !== '')
      .join('\n')
      .split(';')
      .map(stmt => stmt.trim())
      .filter(stmt => stmt.length > 0);
    
    console.log(`\n找到 ${sqlStatements.length} 条SQL语句`);
    
    // 执行SQL语句
    for (let i = 0; i < sqlStatements.length; i++) {
      const statement = sqlStatements[i];
      if (statement.trim()) {
        console.log(`\n执行SQL语句 ${i + 1}/${sqlStatements.length}...`);
        console.log('SQL预览:', statement.substring(0, 100) + '...');
        
        await connection.execute(statement);
        console.log('✓ 执行成功');
      }
    }
    
    // 验证表是否创建成功
    console.log('\n正在验证表结构...');
    const [tables] = await connection.execute(
      "SHOW TABLES LIKE 'duration_data'"
    );
    
    if (tables.length > 0) {
      console.log('✓ 表 duration_data 创建成功');
      
      // 显示表结构
      const [columns] = await connection.execute(
        'DESCRIBE duration_data'
      );
      
      console.log('\n表结构:');
      console.log('┌─────────────────┬──────────────┬──────┬─────┬─────────┬────────┐');
      console.log('│ Field           │ Type         │ Null │ Key │ Default │ Extra  │');
      console.log('├─────────────────┼──────────────┼──────┼─────┼─────────┼────────┤');
      columns.forEach(col => {
        const field = col.Field.padEnd(15);
        const type = col.Type.padEnd(12);
        const nullable = col.Null.padEnd(4);
        const key = col.Key.padEnd(3);
        const def = (col.Default || '').toString().padEnd(7);
        const extra = col.Extra.padEnd(6);
        console.log(`│ ${field} │ ${type} │ ${nullable} │ ${key} │ ${def} │ ${extra} │`);
      });
      console.log('└─────────────────┴──────────────┴──────┴─────┴─────────┴────────┘');
      
      // 显示索引
      const [indexes] = await connection.execute(
        'SHOW INDEX FROM duration_data'
      );
      
      console.log('\n索引信息:');
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
        console.log(`  - ${type}: ${idx.name} (${idx.columns.join(', ')})`);
      });
      
      // 查询表中的数据量
      const [countResult] = await connection.execute(
        'SELECT COUNT(*) as count FROM duration_data'
      );
      console.log(`\n当前数据量: ${countResult[0].count} 条`);
      
    } else {
      console.error('✗ 表创建失败');
      process.exit(1);
    }
    
    console.log('\n========================================');
    console.log('✓ 所有操作完成！');
    console.log('========================================');
    
  } catch (error) {
    console.error('\n✗ 错误:', error.message);
    console.error('\n详细信息:');
    console.error(error);
    process.exit(1);
  } finally {
    if (connection) {
      await connection.end();
      console.log('\n数据库连接已关闭');
    }
  }
}

// 执行创建表操作
console.log('========================================');
console.log('创建直播有效时长数据表');
console.log('========================================\n');

createTable().catch(error => {
  console.error('执行失败:', error);
  process.exit(1);
});
