# Electron 官方教程 - 第一个应用

## 官方文档来源
https://www.electronjs.org/zh/docs/latest/tutorial/tutorial-first-app

## 一、基本要求

### 1. 检测环境
```powershell
node --version
npm --version
```

你的环境：
- Node.js: v24.11.1 ✓
- npm: 需确认版本
- pnpm: 10.32.1（但官方推荐用 npm）

## 二、创建您的第一个应用程序（官方步骤）

### 步骤 1：初始化 npm 项目
官方推荐方式：
```bash
mkdir my-electron-app
cd my-electron-app
npm init
```

### 步骤 2：安装 Electron
官方推荐命令：
```bash
npm install electron --save-dev
```

**重点：官方明确使用 npm install electron --save-dev**

### 步骤 3：创建主脚本文件
创建 `main.js` 文件（Electron 主进程入口）

### 步骤 4：修改 package.json
```json
{
  "name": "my-electron-app",
  "version": "1.0.0",
  "main": "main.js",
  "scripts": {
    "start": "electron ."
  }
}
```

### 步骤 5：运行应用
```bash
npm start
```

## 三、官方安装要点

### ⚠️ 重要提示
1. **官方推荐使用 npm**，不是 pnpm
2. 安装后会生成：
   - `node_modules/` 文件夹（包含 Electron 可执行文件）
   - `package-lock.json` 文件
3. Electron 会作为 devDependencies 安装

### 🔍 为什么不用 pnpm？
根据官方文档示例，所有安装命令都使用 npm：
- `npm init` - 初始化项目
- `npm install electron --save-dev` - 安装 Electron

pnpm 默认会忽略 postinstall 脚本，导致 Electron 二进制文件无法下载。

## 四、针对你的项目的解决方案

你的项目已经有配置文件，只需要：

### 方案 A：切换到 npm（推荐）

```powershell
# 1. 进入项目目录
cd "D:\IT\openclaw douyin\01\douyin-manager"

# 2. 备份并删除 node_modules
Remove-Item -Recurse -Force node_modules
Remove-Item pnpm-lock.yaml -ErrorAction SilentlyContinue

# 3. 使用 npm 安装（官方推荐）
npm install

# 4. 验证 Electron 安装
node -e "console.log(require('electron'))"

# 5. 启动应用
npm run dev
```

### 方案 B：pnpm + 手动批准

如果坚持用 pnpm：

```powershell
# 1. 允许运行构建脚本
pnpm approve-builds

# 在交互界面选择：
# ✓ electron@31.7.7
# ✓ esbuild@0.21.5

# 2. 重新安装
pnpm install --force
```

## 五、官方文档关键摘录

> "安装完 Electron 后，文件夹中会出现一个 node_modules 文件夹，其中包含了 Electron 可执行文件"

这意味着安装脚本必须成功运行，Electron 二进制文件才会被下载。

## 六、常见问题（官方文档提及）

### Q: 安装时网络错误怎么办？
官方教程提到的解决方案：
1. 使用国内镜像
2. 手动下载二进制文件

你的项目已配置 `.npmrc` 使用淘宝镜像，应该可以正常下载。

## 七、立即执行（推荐）

复制以下命令到 PowerShell 执行：

```powershell
cd "D:\IT\openclaw douyin\01\douyin-manager"; Remove-Item -Recurse -Force node_modules -ErrorAction SilentlyContinue; Remove-Item pnpm-lock.yaml -ErrorAction SilentlyContinue; npm install
```

安装完成后：
```powershell
npm run dev
```

## 八、参考资源

- 官方教程：https://www.electronjs.org/zh/docs/latest/tutorial/tutorial-first-app
- API 文档：https://www.electronjs.org/docs/latest/api/app
- 示例代码：https://github.com/electron/electron-quick-start
