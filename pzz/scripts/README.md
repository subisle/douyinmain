# 数据库脚本说明

## 创建直播时长表

### 方法1：使用Node.js脚本（推荐）

```bash
# 进入pzz目录
cd pzz

# 安装依赖（如果还没安装）
npm install mysql2

# 运行脚本
node scripts/create-duration-table.js
```

### 方法2：使用MySQL命令行

```bash
# 登录MySQL
mysql -u root -p

# 选择数据库
USE xs;

# 执行SQL文件
source /path/to/pzz/duration_data_table.sql;

# 或者直接执行
mysql -u root -p xs < /path/to/pzz/duration_data_table.sql
```

### 方法3：使用MySQL Workbench

1. 打开MySQL Workbench
2. 连接到数据库
3. 打开 `duration_data_table.sql` 文件
4. 点击执行按钮

## 验证表是否创建成功

```sql
-- 查看表结构
DESCRIBE duration_data;

-- 查看索引
SHOW INDEX FROM duration_data;

-- 查看表信息
SHOW CREATE TABLE duration_data;
```

## 删除表（如果需要重新创建）

```sql
DROP TABLE IF EXISTS duration_data;
```

## 常见问题

### Q: 连接数据库失败
A: 检查数据库配置是否正确：
- 主机地址：127.0.0.1
- 端口：3306
- 数据库名：xs
- 用户名：root
- 密码：mysql_QmsGFC

### Q: 表已存在
A: 脚本使用 `CREATE TABLE IF NOT EXISTS`，如果表已存在不会报错。如需重新创建，先删除表。

### Q: 权限不足
A: 确保MySQL用户有CREATE TABLE权限。
