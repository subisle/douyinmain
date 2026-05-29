# Douyin 数据管理项目集合

这个仓库整理了抖音主播数据管理相关的桌面端、Android 端和辅助脚本。不同目录对应不同运行环境，开发前请先确认要处理的模块。

## 模块导航

| 路径 | 说明 | 技术栈 |
| --- | --- | --- |
| [`douyin-manager/`](douyin-manager/) | 桌面端主播数据管理系统 | Electron, React, TypeScript, Vite |
| [`pzz/`](pzz/) | Android 版主播音浪和直播时长管理系统 | Kotlin, Jetpack Compose, Room |
| [`pzz/scripts/`](pzz/scripts/) | 数据库辅助脚本 | Node.js, MySQL |
| [`pzz/docs/`](pzz/docs/) | Android 端测试和排查文档 | Markdown |

## 快速开始

### 桌面端

```bash
cd douyin-manager
npm install
npm run electron:dev
```

构建安装包：

```bash
npm run build:win
npm run build:mac
```

### Android 端

```bash
cd pzz
./gradlew assembleDebug
```

也可以使用 Android Studio 打开 `pzz/` 目录进行调试。

## 配置与数据安全

- 不要在公开文档、脚本或源码中提交真实数据库地址、账号、密码和令牌。
- 示例配置统一使用 `your_mysql_host`、`your_username`、`your_password` 等占位值。
- 已经公开过的数据库凭据应当视为泄露，建议立即轮换。
- 测试数据和导出 CSV 建议放在本地或私有存储，避免提交到公开仓库。

## 文档维护

- 新用户入口写在根 README。
- 子项目的运行、构建和配置细节写在各自目录的 README。
- 变更功能时同步更新对应模块 README 和 `pzz/docs/` 下的测试说明。

## License

MIT License. See [LICENSE](LICENSE).
