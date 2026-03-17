# 鹏仔传媒数据管理系统 - Android版

## 项目简介

这是一个基于 Jetpack Compose 开发的 Android 应用，用于管理和查看主播音浪数据和直播有效时长数据。

## 功能特性

### ✅ 已实现功能

#### 音浪数据管理

1. **数据查询与展示**
   - 按日期查询音浪数据
   - 列表展示主播排名、姓名、音浪、等级
   - 根据等级显示不同背景色（S/A/B/C/D）
   - 前三名显示奖牌图标

2. **日期导航**
   - 快捷按钮：今天、昨天、前天
   - 日期选择器
   - 上一天/下一天导航

3. **数据统计**
   - 总记录数
   - 有音浪人数
   - 总音浪统计

4. **数据导出** ⭐
   - **导出为 Excel**：保存到下载文件夹
   - **导出为图片**：保存到相册，可分享到微信等应用
   - 自动请求存储权限

5. **数据导入**
   - CSV文件导入
   - 数据预览
   - 本地和远程数据库同步

#### 直播有效时长管理 🆕

1. **时间范围选择**
   - 手动选择开始和结束日期
   - 默认显示本月1号到今天
   - 支持查看任意历史时间范围

2. **数据导入**
   - 支持CSV文件导入（主播概览列表格式）
   - 自动提取"开播有效时长"字段
   - 根据主播ID匹配数据库中的主播名称
   - 导入前数据预览

3. **数据展示**
   - 按时长降序排列
   - 显示主播ID、名称、开播有效时长
   - 前三名显示金银铜牌标识
   - 统计总人数、有时长人数、未开播人数

4. **数据同步**
   - 本地Room数据库存储
   - 远程MySQL数据库同步
   - 支持删除指定时间范围数据

#### 主播管理

1. **主播信息管理**
   - 查看所有主播列表
   - 添加、编辑、删除主播
   - 从远程数据库同步主播名单

2. **本地数据库**
   - Room 数据库存储
   - 支持音浪数据和直播时长数据

### 🚧 待实现功能

- [ ] 数据统计图表
- [ ] 数据筛选和搜索
- [ ] 深色模式
- [ ] 时长趋势分析

## 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose + Material Design 3
- **架构**: MVVM
- **数据库**: Room
- **网络**: Retrofit + OkHttp
- **异步**: Kotlin Coroutines + Flow
- **Excel处理**: Apache POI
- **权限管理**: ActivityResultContracts

## 项目结构

```
app/src/main/java/com/example/myapplication/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt              # Room 数据库
│   │   ├── SoundDataDao.kt             # 音浪数据访问对象
│   │   └── DurationDataDao.kt          # 直播时长数据访问对象 🆕
│   ├── model/
│   │   ├── SoundData.kt                # 音浪数据模型
│   │   ├── DurationData.kt             # 直播时长数据模型 🆕
│   │   └── Streamer.kt                 # 主播模型
│   ├── remote/
│   │   ├── ApiService.kt               # API 接口定义
│   │   ├── RetrofitClient.kt           # Retrofit 客户端
│   │   ├── SimpleMySQLClient.kt        # MySQL客户端
│   │   └── RemoteDatabaseManagerV2.kt  # 远程数据库管理
│   └── repository/
│       ├── SoundDataRepository.kt      # 音浪数据仓库
│       └── DurationDataRepository.kt   # 直播时长数据仓库 🆕
├── ui/
│   ├── components/
│   │   ├── DateNavigationBar.kt        # 日期导航栏
│   │   ├── QuickDateButtons.kt         # 快捷日期按钮
│   │   ├── SoundDataList.kt            # 数据列表
│   │   └── StatisticsBar.kt            # 统计栏
│   ├── dialogs/
│   │   ├── ImportPreviewDialog.kt      # 导入预览对话框
│   │   ├── DurationImportDialog.kt     # 直播时长导入对话框 🆕
│   │   └── DateRangePickerDialog.kt    # 日期范围选择对话框 🆕
│   ├── screens/
│   │   ├── SettingsScreen.kt           # 设置界面
│   │   ├── StreamerManagementScreen.kt # 主播管理界面
│   │   └── DurationDataScreen.kt       # 直播时长界面 🆕
│   └── theme/
│       ├── Color.kt                    # 颜色定义
│       ├── Theme.kt                    # 主题配置
│       └── Type.kt                     # 字体配置
├── utils/
│   ├── ExportUtils.kt                  # 导出工具
│   ├── ImportUtils.kt                  # 导入工具
│   ├── DurationImportUtils.kt          # 直播时长导入工具 🆕
│   ├── GenderUtils.kt                  # 主播性别工具
│   └── TestDataGenerator.kt            # 测试数据生成器
├── viewmodel/
│   ├── SoundDataViewModel.kt           # 音浪数据ViewModel
│   └── DurationDataViewModel.kt        # 直播时长ViewModel 🆕
├── MainActivity.kt                     # 主Activity
└── PzzApplication.kt                   # Application类
```

## 快速开始

### 1. 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 11 或更高版本
- Android SDK 26 或更高版本
- MySQL 数据库（用于远程数据同步）

### 2. 数据库配置

#### 创建MySQL数据库表

```sql
-- 音浪数据表（已存在）
-- 参考现有数据库结构

-- 直播时长数据表（新增）
source duration_data_table.sql;
```

#### 配置数据库连接

修改 `AppConfig.kt` 中的数据库配置：

```kotlin
object AppConfig {
    const val DB_HOST = "your-mysql-host"
    const val DB_PORT = 3306
    const val DB_NAME = "your-database-name"
    const val DB_USER = "your-username"
    const val DB_PASSWORD = "your-password"
    const val DB_TABLE = "streamer_data"
}
```

### 3. 构建项目

```bash
# 克隆项目
cd pzz

# 使用 Android Studio 打开项目
# 或使用命令行构建
./gradlew assembleDebug
```

### 4. 运行应用

1. 连接 Android 设备或启动模拟器
2. 在 Android Studio 中点击 Run 按钮
3. 或使用命令行：`./gradlew installDebug`

### 5. 功能测试

#### 测试音浪数据导入
1. 准备CSV文件（主播榜格式）
2. 点击底部"导入"按钮
3. 选择日期和CSV文件
4. 预览并确认导入

#### 测试直播时长导入
1. 准备CSV文件（主播概览列表格式）
2. 点击底部"时长"按钮
3. 点击右上角"+"按钮
4. 选择时间范围和CSV文件
5. 预览并确认导入

## 使用指南

### 音浪数据管理

1. **查看数据**
   - 使用快捷按钮选择日期（今天/昨天/前天）
   - 或使用日期选择器选择任意日期
   - 使用左右箭头切换日期

2. **导入数据**
   - 点击底部"导入"按钮
   - 选择导入日期
   - 选择CSV文件（主播榜格式）
   - 预览数据后确认导入

3. **导出数据**
   - 点击底部"导出"按钮
   - 选择导出格式（Excel/图片）
   - 查看导出结果

### 直播时长管理 🆕

1. **进入时长界面**
   - 点击底部"时长"按钮

2. **选择时间范围**
   - 点击顶部时间范围卡片
   - 选择开始和结束日期
   - 确认应用

3. **导入数据**
   - 点击右上角"+"按钮
   - 选择时间范围
   - 选择CSV文件（主播概览列表格式）
   - 预览数据后确认导入

4. **查看统计**
   - 查看总人数、有时长人数、未开播人数
   - 查看主播排名和时长详情

### 主播管理

1. **查看主播列表**
   - 点击顶部主播管理图标
   - 查看所有主播信息

2. **同步主播名单**
   - 在主播管理界面点击"同步"
   - 从远程数据库同步最新主播信息

3. **编辑主播信息**
   - 点击主播卡片的编辑按钮
   - 修改主播信息
   - 保存更改

## 权限说明

应用需要以下权限：

- **INTERNET**: 网络访问（用于数据同步）
- **ACCESS_NETWORK_STATE**: 网络状态检查
- **READ_EXTERNAL_STORAGE**: 读取存储（Android 12 及以下）
- **WRITE_EXTERNAL_STORAGE**: 写入存储（Android 12 及以下）
- **READ_MEDIA_IMAGES**: 读取图片（Android 13+）

应用会在启动时自动请求必要的权限。

## 配置服务器

如需启用网络同步功能，请修改 `RetrofitClient.kt` 中的服务器地址：

```kotlin
private const val BASE_URL = "http://your-server-ip:5000/api/v1/"
```

## 导出功能详解

### Excel 导出

- 使用 Apache POI 库生成 Excel 文件
- 包含完整的数据字段：排名、主播ID、姓名、音浪、等级等
- 自动调整列宽
- 标题行使用灰色背景

### 图片导出

- 使用 Canvas 绘制数据表格
- 包含应用标题和日期
- 根据等级显示不同背景色
- 前三名显示奖牌图标
- 最多显示前50条数据
- 图片尺寸：1080px 宽，高度自适应

### 分享到微信

导出图片后，可以通过以下方式分享到微信：

1. 打开相册
2. 找到导出的图片
3. 点击分享按钮
4. 选择微信

## 常见问题

### Q: 导出失败怎么办？

A: 请确保已授予存储权限。可以在系统设置中手动授予权限。

### Q: 为什么看不到数据？

A: 首次使用需要导入CSV数据。点击"导入"按钮选择CSV文件进行导入。

### Q: 如何连接真实服务器？

A: 修改 `AppConfig.kt` 中的数据库配置，填入正确的MySQL服务器信息。

### Q: 支持哪些 Android 版本？

A: 最低支持 Android 8.0 (API 26)，推荐 Android 10 (API 29) 及以上。

### Q: 直播时长数据和音浪数据有什么区别？

A: 
- **音浪数据**：按单日记录，显示每日音浪收入
- **直播时长数据**：按时间范围记录，显示一段时间内的累计开播时长
- 两者独立管理，互不影响

### Q: CSV文件格式要求是什么？

A:
- **音浪数据**：使用"主播榜"格式，包含主播ID、名称、音浪等字段
- **直播时长数据**：使用"主播概览列表"格式，第17列为"开播有效时长"

### Q: 数据导入失败怎么办？

A: 
1. 检查CSV文件格式是否正确
2. 检查网络连接是否正常
3. 检查数据库配置是否正确
4. 查看错误提示信息

## 开发计划

- [x] 音浪数据管理
- [x] 数据导入导出
- [x] 主播信息管理
- [x] 直播时长管理 🆕
- [ ] 数据统计图表
- [ ] 数据筛选和搜索
- [ ] 深色模式
- [ ] 时长趋势分析
- [ ] 多时间范围对比

## 更新日志

### v1.1.0 (2026-01-19) 🆕
- ✨ 新增直播有效时长管理功能
- ✨ 支持时间范围选择和数据导入
- ✨ 新增时长统计和排名展示
- 🔧 数据库版本升级到v2
- 📚 完善文档和使用指南

### v1.0.0
- ✨ 音浪数据管理
- ✨ 数据导入导出
- ✨ 主播信息管理

详细更新日志请查看 [CHANGELOG-直播时长功能.md](./CHANGELOG-直播时长功能.md)

## 相关文档

### 功能说明
- [直播有效时长功能说明](./直播有效时长功能说明.md)
- [应用功能说明文档](./应用功能说明文档.md)

### 开发文档
- [直播时长功能实现总结](./直播时长功能实现总结.md)
- [MainActivity集成指南](./MainActivity集成指南.md)
- [快速开始-直播时长功能](./快速开始-直播时长功能.md)

### 数据库
- [数据库表创建脚本](./duration_data_table.sql)

## 许可证

Copyright © 2026 鹏仔传媒

## 联系方式

如有问题或建议，请联系开发团队。
