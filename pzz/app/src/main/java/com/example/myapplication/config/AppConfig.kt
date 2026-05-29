package com.example.myapplication.config

/**
 * 应用配置
 */
object AppConfig {
    /**
     * 远程数据库配置
     * 
     * 使用说明：
     * 1. 配置你的远程MySQL数据库信息
     * 2. 确保数据库允许远程连接
     * 3. 确保网络可以访问数据库服务器
     * 
     * 安全建议：
     * - 使用只读账户
     * - 限制IP访问
     * - 使用SSL连接（如果可能）
     */
    
    // 数据库连接信息
    const val DB_HOST = "your_mysql_host"      // 远程数据库地址
    const val DB_PORT = 3306                   // 数据库端口
    const val DB_NAME = "your_database"        // 数据库名称
    const val DB_USER = "your_username"        // 数据库用户名
    const val DB_PASSWORD = "your_password"    // 数据库密码
    const val DB_TABLE = "streamer_data"       // 表名（与Python项目一致）
    
    /**
     * 是否启用远程数据库同步
     * 
     * 使用SimpleMySQLClient直接实现MySQL协议，绕过JDBC
     * 解决了Android ICU4J正则表达式引擎不兼容问题
     */
    const val ENABLE_REMOTE_DB = true  // 启用自定义MySQL客户端
    
    /**
     * 数据库连接超时时间（秒）
     */
    const val DB_TIMEOUT = 10
    
    /**
     * 服务器配置（HTTP REST API）
     * 
     * 配置说明：
     * 1. 在PC端运行: python api_server.py
     * 2. 将下面的IP地址改为PC的局域网IP
     * 3. 确保手机和PC在同一网络
     * 4. 确保PC防火墙允许5000端口
     * 
     * 查看PC IP地址:
     * - Windows: ipconfig
     * - Mac/Linux: ifconfig
     */
    const val BASE_URL = "http://192.168.1.100:5000/api/v1/"  // 修改为你的PC IP地址
    
    /**
     * 是否启用网络同步功能（通过HTTP API）
     * 
     * 备用方案：如果SimpleMySQLClient不工作，可以使用HTTP API
     * 需要在PC端运行api_server.py
     */
    const val ENABLE_NETWORK_SYNC = false  // 禁用HTTP API（使用直连）
    
    /**
     * 网络请求超时时间（秒）
     */
    const val NETWORK_TIMEOUT = 30L
    
    /**
     * 是否自动生成测试数据
     * 
     * true: 首次启动时自动生成测试数据（用于测试）
     * false: 不生成测试数据，使用远程数据库数据
     */
    const val AUTO_GENERATE_TEST_DATA = false  // 禁用，使用远程数据
    
    /**
     * 使用哪个版本的远程数据库管理器
     * 
     * V1: 使用JDBC（在Android上不兼容）
     * V2: 使用SimpleMySQLClient（自定义MySQL协议实现）
     */
    const val USE_REMOTE_DB_V2 = true  // 使用V2版本
    
    /**
     * 测试数据生成天数
     */
    const val TEST_DATA_DAYS = 3
    
    /**
     * 每天生成的测试数据条数
     */
    const val TEST_DATA_COUNT = 30
}
