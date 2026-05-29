# 鹏仔传媒数据管理系统（Android 版）

基于 Kotlin + Jetpack Compose 的 Android 应用，用于查看和管理主播音浪数据、直播有效时长、主播信息和数据导出。

## 功能概览

- 音浪数据：按日期查看主播排名、音浪、等级和总音浪。
- 直播时长：按时间范围导入、统计和展示有效开播时长。
- 主播管理：同步、添加、编辑和删除主播基础信息。
- 数据导入：支持 CSV 导入并在导入前预览。
- 数据导出：支持导出 Excel 和图片。
- 数据同步：本地 Room 数据库和远程 MySQL 数据同步。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 语言 | Kotlin |
| UI | Jetpack Compose, Material Design 3 |
| 架构 | MVVM |
| 本地数据 | Room |
| 远程访问 | Retrofit, OkHttp, MySQL 连接模块 |
| 异步 | Kotlin Coroutines, Flow |
| 导出 | Apache POI, Android 分享能力 |

## 环境要求

- Android Studio Hedgehog 或更高版本
- JDK 11+
- Android SDK 26+
- 可访问的 MySQL 数据库

## 快速开始

```bash
./gradlew assembleDebug
```

或使用 Android Studio 打开 `pzz/` 目录，等待 Gradle 同步完成后运行 `app`。

## 数据库配置

公开仓库只保留占位配置。实际使用时请通过本地配置、构建变量或私有文件注入真实连接信息：

```kotlin
const val DB_HOST = "your_mysql_host"
const val DB_PORT = 3306
const val DB_NAME = "your_database"
const val DB_USER = "your_username"
const val DB_PASSWORD = "your_password"
const val DB_TABLE = "streamer_data"
```

已经提交到公开仓库的真实数据库凭据应当立即轮换。

## 目录结构

```text
app/src/main/java/com/example/myapplication/
├── config/      # 应用配置
├── data/        # 本地数据库、远程访问和仓储层
├── ui/          # Compose 页面、组件和主题
├── utils/       # 导入、导出、匹配和统计工具
└── viewmodel/   # 页面状态和业务协调
```

## 辅助文档

- [`docs/`](docs/)：测试、排查和功能说明。
- [`scripts/`](scripts/)：数据库建表与迁移辅助脚本。
- [`duration_data_table.sql`](duration_data_table.sql)：直播时长表结构。

## License

MIT License. See [../LICENSE](../LICENSE).
