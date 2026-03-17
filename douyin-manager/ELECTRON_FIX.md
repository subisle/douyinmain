# Electron 安装问题修复指南

## 问题原因
Electron 二进制文件下载失败，导致无法启动应用。

## 解决方案

### 方案 1：使用国内镜像重新安装（推荐）

在 PowerShell 中执行以下命令：

```powershell
# 设置环境变量使用淘宝镜像
$env:ELECTRON_MIRROR="https://npmmirror.com/mirrors/electron/"
$env:ELECTRON_CUSTOM_DIR="v31.7.7"

# 删除并重新安装 Electron
cd "D:\IT\openclaw douyin\01\douyin-manager"
Remove-Item -Recurse -Force node_modules\electron
pnpm install electron --force
```

### 方案 2：手动下载 Electron

1. 访问淘宝镜像：https://npmmirror.com/mirrors/electron/31.7.7/
2. 下载对应系统的文件：
   - Windows: `electron-v31.7.7-win32-x64.zip`
3. 解压到：`node_modules/electron/dist/`
4. 在 `node_modules/electron/` 创建 `path.txt` 文件，内容为：`dist\electron.exe`

### 方案 3：完全重装依赖

```powershell
cd "D:\IT\openclaw douyin\01\douyin-manager"

# 设置镜像
$env:ELECTRON_MIRROR="https://npmmirror.com/mirrors/electron/"

# 删除 node_modules 和 lock 文件
Remove-Item -Recurse -Force node_modules
Remove-Item pnpm-lock.yaml

# 重新安装
pnpm install
```

### 方案 4：使用 npm 安装

如果 pnpm 持续有问题，可以尝试使用 npm：

```powershell
# 先删除 pnpm 相关文件
cd "D:\IT\openclaw douyin\01\douyin-manager"
Remove-Item -Recurse -Force node_modules
Remove-Item pnpm-lock.yaml

# 使用 npm 安装
$env:ELECTRON_MIRROR="https://npmmirror.com/mirrors/electron/"
npm install
```

## 验证安装

安装完成后，运行以下命令验证：

```powershell
node -e "console.log(require('electron'))"
```

如果输出了 Electron 的路径，说明安装成功。

## 启动应用

安装成功后，运行：

```powershell
npm run dev
```

## 注意事项

1. 确保网络连接正常
2. 如果使用公司网络，可能需要配置代理
3. 国内用户建议使用淘宝镜像
4. Electron 二进制文件约 80MB，下载可能需要几分钟

## 相关链接

- Electron 官方文档：https://www.electronjs.org/
- 淘宝镜像：https://npmmirror.com/
- pnpm 文档：https://pnpm.io/
