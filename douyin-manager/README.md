# 主播数据管理系统

一款基于 Electron + React 的跨平台桌面应用，用于管理抖音主播的音浪数据和直播时长数据。

## 功能特性

- 数据源整合：远程MySQL数据库同步主播信息
- CSV数据导入：手动导入音浪和时长数据
- 数据统计与展示：音浪趋势图、时长趋势图
- 图表筛选：支持按天/周/月/自定义时间范围筛选
- 导出功能：支持CSV和图片导出

## 技术栈

- 前端框架: React 18 + TypeScript
- 桌面框架: Electron
- 图表库: ECharts
- 数据库: MySQL (mysql2)
- 样式: Tailwind CSS

## 安装步骤

```bash
# 1. 进入项目目录
cd douyin-manager

# 2. 安装依赖
npm install

# 3. 启动开发模式
npm run dev

# 4. 构建 Windows 安装包
npm run build:win
```

## 数据库配置

项目需要配置两个数据库连接：

1. **远程数据库** (your_mysql_host:3310) - 存储主播基础信息和统计数据
   - 数据库: your_username
   - 账号: your_username
   - 密码: your_password

2. **本地数据库** (localhost:3306) - 本地备份数据
   - 数据库: douyin_stats
   - 账号: root
   - 密码: （请根据本地MySQL配置设置）

## 项目结构

```
douyin-manager/
├── electron/           # Electron 主进程
│   ├── main.ts        # 主进程入口
│   ├── preload.ts     # 预加载脚本
│   └── database.ts   # 数据库连接
├── src/               # React 前端
│   ├── components/    # 组件
│   ├── types/        # 类型定义
│   ├── App.tsx       # 主应用
│   └── main.tsx      # 入口文件
├── package.json
└── vite.config.ts
```

## 使用说明

1. **同步主播**: 点击顶部"同步主播"按钮从远程数据库同步主播信息
2. **导入数据**: 在数据概览页面点击"导入音浪CSV"或"导入时长CSV"
3. **查看统计**: 切换到"数据统计"页面查看趋势图表
4. **导出数据**: 支持导出CSV数据和图表图片

## 构建说明

构建前请确保已安装 Node.js 和 npm。

```bash
# 安装依赖
npm install

# 开发模式
npm run dev

# 构建 Windows 安装包
npm run build:win

# 构建 Mac 安装包
npm run build:mac
```

构建完成后，安装包将位于 `release` 目录下。
