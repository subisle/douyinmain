# 主播数据管理系统（桌面版）

基于 Electron + React 的跨平台桌面应用，用于管理主播音浪、直播时长、数据导入、统计图表和导出报表。

## 功能概览

- 主播同步：同步远程主播基础信息。
- CSV 导入：导入音浪数据和直播时长数据。
- 数据展示：查看音浪、时长、等级和趋势统计。
- 图表筛选：按天、周、月或自定义时间范围筛选。
- 数据导出：导出 CSV 数据和统计图图片。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 桌面运行时 | Electron |
| 前端 | React 18, TypeScript, Vite |
| 图表 | ECharts |
| 数据 | MySQL, sql.js |
| 样式 | Tailwind CSS |

## 环境要求

- Node.js 18+
- npm
- 可访问的 MySQL 数据库

## 快速开始

```bash
npm install
npm run electron:dev
```

只启动前端预览：

```bash
npm run dev
```

## 构建

```bash
npm run typecheck
npm run build:win
npm run build:mac
```

构建产物默认输出到 `release/`。

## 数据库配置

公开仓库不要提交真实数据库连接信息。实际使用时请在本地配置数据库连接，并使用占位值记录示例：

```env
DB_HOST=your_mysql_host
DB_PORT=3306
DB_NAME=your_database
DB_USER=your_username
DB_PASSWORD=your_password
```

当前桌面端数据库连接逻辑位于 Electron 主进程相关模块中。发布或交付前建议改造成环境变量、私有配置文件或安全的密钥注入方式。

## 项目结构

```text
electron/       # Electron 主进程、预加载脚本和数据库连接
src/            # React 前端页面、组件和类型定义
package.json    # 项目命令和依赖
vite.config.ts  # Vite 构建配置
```

## 使用流程

1. 配置数据库连接。
2. 启动桌面端应用。
3. 同步主播信息。
4. 导入音浪或直播时长 CSV。
5. 查看统计图表并按需导出 CSV 或图片。

## License

MIT License. See [../LICENSE](../LICENSE).
