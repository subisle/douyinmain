@echo off
echo 正在按照官方文档重新安装 Electron...

echo.
echo 步骤 1: 清理旧安装
if exist node_modules\electron (
    echo 删除旧的 Electron 安装...
    rmdir /s /q node_modules\electron
)

if exist node_modules\.pnpm\electron@31.7.7 (
    echo 删除 pnpm Electron 缓存...
    rmdir /s /q node_modules\.pnpm\electron@31.7.7
)

echo.
echo 步骤 2: 安装依赖
echo 正在安装所有依赖（包括 Electron）...
call pnpm install --force

echo.
echo 步骤 3: 验证安装
echo 验证 Electron 是否正确安装...
node -e "try { const e = require('electron'); console.log('✓ Electron 安装成功'); console.log('路径:', e); } catch(err) { console.error('✗ 安装失败:', err.message); process.exit(1); }"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   安装成功！现在可以运行 npm run dev
    echo ========================================
) else (
    echo.
    echo ========================================
    echo   安装失败，请检查网络连接或尝试手动安装
    echo ========================================
)

pause
