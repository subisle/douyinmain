# Electron 官方文档安装脚本
# 参考：https://www.electronjs.org/zh/docs/latest/tutorial/tutorial-first-app

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Electron 官方文档安装方法" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 步骤 1：检查环境
Write-Host "[步骤 1/5] 检查环境" -ForegroundColor Yellow
$nodeVersion = node --version
$npmVersion = npm --version
Write-Host "  ✓ Node.js 版本: $nodeVersion" -ForegroundColor Green
Write-Host "  ✓ npm 版本: $npmVersion" -ForegroundColor Green
Write-Host ""

# 步骤 2：设置镜像
Write-Host "[步骤 2/5] 设置 Electron 镜像" -ForegroundColor Yellow
$env:ELECTRON_MIRROR = "https://npmmirror.com/mirrors/electron/"
Write-Host "  ✓ 已设置 ELECTRON_MIRROR 环境变量" -ForegroundColor Green
Write-Host ""

# 步骤 3：清理旧安装
Write-Host "[步骤 3/5] 清理旧安装" -ForegroundColor Yellow
if (Test-Path "node_modules") {
    Write-Host "  正在删除 node_modules..." -ForegroundColor Gray
    Remove-Item -Recurse -Force "node_modules" -ErrorAction SilentlyContinue
    Write-Host "  ✓ node_modules 已删除" -ForegroundColor Green
}
if (Test-Path "pnpm-lock.yaml") {
    Remove-Item -Force "pnpm-lock.yaml" -ErrorAction SilentlyContinue
    Write-Host "  ✓ pnpm-lock.yaml 已删除" -ForegroundColor Green
}
if (Test-Path "package-lock.json") {
    Remove-Item -Force "package-lock.json" -ErrorAction SilentlyContinue
    Write-Host "  ✓ package-lock.json 已删除" -ForegroundColor Green
}
Write-Host ""

# 步骤 4：安装依赖（官方推荐）
Write-Host "[步骤 4/5] 使用 npm 安装依赖（官方推荐）" -ForegroundColor Yellow
Write-Host "  执行: npm install" -ForegroundColor Gray
Write-Host ""

npm install

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "  ✓ 依赖安装成功" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "  ✗ 安装失败" -ForegroundColor Red
    Write-Host "  请检查网络连接或尝试手动安装" -ForegroundColor Red
    exit 1
}
Write-Host ""

# 步骤 5：验证安装
Write-Host "[步骤 5/5] 验证 Electron 安装" -ForegroundColor Yellow
try {
    $electronPath = node -e "console.log(require('electron'))"
    Write-Host "  ✓ Electron 安装成功" -ForegroundColor Green
    Write-Host "  路径: $electronPath" -ForegroundColor Gray
} catch {
    Write-Host "  ✗ Electron 验证失败" -ForegroundColor Red
    Write-Host "  错误: $_" -ForegroundColor Red
    exit 1
}
Write-Host ""

# 完成
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  安装完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "现在可以运行以下命令启动应用：" -ForegroundColor Yellow
Write-Host "  npm run dev" -ForegroundColor White
Write-Host ""
