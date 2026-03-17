# Electron 官方安装指南

## 官方推荐安装方法

### 方法 1：使用 npm（官方推荐）

```powershell
# 1. 进入项目目录
cd "D:\IT\openclaw douyin\01\douyin-manager"

# 2. 删除旧安装
Remove-Item -Recurse -Force node_modules -ErrorAction SilentlyContinue
Remove-Item pnpm-lock.yaml -ErrorAction SilentlyContinue

# 3. 使用 npm 安装（官方推荐）
npm install
```

### 方法 2：pnpm + 手动批准构建脚本

```powershell
# 1. 进入项目目录
cd "D:\IT\openclaw douyin\01\douyin-manager"

# 2. 运行 pnpm 的构建脚本批准命令
pnpm approve-builds

# 3. 选择允许以下包运行脚本：
#    - electron@31.7.7
#    - esbuild@0.21.5

# 4. 重新安装
pnpm install --force
```

### 方法 3：手动下载并配置（离线安装）

如果网络问题持续存在，可以手动下载：

#### 步骤 1：下载 Electron 二进制文件

访问淘宝镜像（推荐）：
- URL: https://npmmirror.com/mirrors/electron/31.7.7/
- 下载文件: `electron-v31.7.7-win32-x64.zip`（约 80MB）

或者使用官方 GitHub Release：
- URL: https://github.com/electron/electron/releases/tag/v31.7.7
- 下载文件: `electron-v31.7.7-win32-x64.zip`

#### 步骤 2：解压并配置

```powershell
# 1. 解压下载的 zip 文件到：
#    node_modules\electron\dist\

# 2. 创建 path.txt 文件
# 在 node_modules\electron\ 目录下创建 path.txt
# 内容为：dist\electron.exe

# 示例命令：
cd "D:\IT\openclaw douyin\01\douyin-manager"
echo dist\electron.exe > node_modules\electron\path.txt
```

### 方法 4：使用代理（公司网络环境）

```powershell
# 设置代理
$env:HTTP_PROXY="http://your-proxy:port"
$env:HTTPS_PROXY="http://your-proxy:port"

# 然后重新安装
npm install
```

### 方法 5：使用 yarn

```powershell
# 1. 进入项目目录
cd "D:\IT\openclaw douyin\01\douyin-manager"

# 2. 删除旧安装
Remove-Item -Recurse -Force node_modules
Remove-Item pnpm-lock.yaml

# 3. 使用 yarn 安装
yarn install
```

## 推荐解决方案（按优先级）

### 🥇 首选：切换到 npm
npm 默认会运行 postinstall 脚本，兼容性最好。

### 🥈 次选：pnpm approve-builds
保留 pnpm，但手动批准 Electron 的构建脚本。

### 🥉 最后：手动下载
适用于网络完全不通的环境。

## 验证安装

安装成功后运行：

```powershell
node -e "console.log(require('electron'))"
```

应该输出类似：`D:\IT\openclaw douyin\01\douyin-manager\node_modules\electron\dist\electron.exe`

## 启动应用

```powershell
npm run dev
```

## 常见问题

### Q: 为什么 pnpm 会忽略构建脚本？
A: pnpm 出于安全考虑，默认不会运行 postinstall 脚本。需要手动批准。

### Q: 下载速度慢怎么办？
A: 使用淘宝镜像（已在 .npmrc 中配置）或使用方法 3 手动下载。

### Q: Node.js 版本要求？
A: Electron 31.7.7 需要 Node.js >= 12.20.55，你当前是 v24.11.1，符合要求。

## 相关链接

- Electron 官方文档: https://www.electronjs.org/docs/latest/tutorial/installation
- 淘宝镜像: https://npmmirror.com/mirrors/electron/
- Electron Releases: https://github.com/electron/electron/releases
