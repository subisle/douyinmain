/**
 * 为 duration_data 表添加 gender 字段
 */

const mysql = require('mysql2/promise');

const config = {
  host: process.env.DB_HOST || 'your_mysql_host',
  port: parseInt(process.env.DB_PORT || '3306', 10),
  user: process.env.DB_USER || 'your_username',
  password: process.env.DB_PASSWORD || 'your_password',
  database: process.env.DB_NAME || 'your_database'
};

async function addGenderColumn() {
  let connection;
  
  try {
    console.log('连接到数据库...');
    connection = await mysql.createConnection(config);
    console.log('✓ 数据库连接成功');
    
    // 检查 gender 字段是否已存在
    const [columns] = await connection.query(`
      SELECT COLUMN_NAME 
      FROM INFORMATION_SCHEMA.COLUMNS 
      WHERE TABLE_SCHEMA = 'your_database'
        AND TABLE_NAME = 'duration_data' 
        AND COLUMN_NAME = 'gender'
    `);
    
    if (columns.length > 0) {
      console.log('✓ gender 字段已存在，无需添加');
      return;
    }
    
    // 添加 gender 字段
    console.log('添加 gender 字段...');
    await connection.query(`
      ALTER TABLE duration_data 
      ADD COLUMN gender VARCHAR(10) NOT NULL DEFAULT '' COMMENT '性别：男/女' 
      AFTER end_date
    `);
    console.log('✓ gender 字段添加成功');
    
    // 验证字段
    const [result] = await connection.query(`
      SHOW COLUMNS FROM duration_data LIKE 'gender'
    `);
    
    if (result.length > 0) {
      console.log('✓ 验证成功：gender 字段已添加');
      console.log('字段信息：', result[0]);
    }
    
  } catch (error) {
    console.error('✗ 错误：', error.message);
    throw error;
  } finally {
    if (connection) {
      await connection.end();
      console.log('数据库连接已关闭');
    }
  }
}

// 执行
addGenderColumn()
  .then(() => {
    console.log('\n✓ 所有操作完成');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n✗ 操作失败：', error.message);
    process.exit(1);
  });
