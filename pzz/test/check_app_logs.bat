@echo off
REM 查看应用关键日志的脚本（Windows版本）

echo ==========================================
echo 查看应用关键日志
echo ==========================================
echo.
echo 使用方法：
echo   check_app_logs.bat
echo.
echo 或者直接运行以下命令：
echo.
echo # 查看SimpleMySQLClient日志
echo adb logcat -s SimpleMySQLClient:D
echo.
echo # 查看RemoteDatabaseManagerV2日志
echo adb logcat -s RemoteDatabaseManagerV2:D
echo.
echo # 查看SoundDataViewModel日志
echo adb logcat -s SoundDataViewModel:D
echo.
echo # 查看所有应用日志（过滤系统噪音）
echo adb logcat ^| findstr "SimpleMySQLClient RemoteDatabaseManagerV2 SoundDataViewModel MainActivity"
echo.
echo ==========================================
echo 开始监听日志...
echo ==========================================
echo.

REM 清空之前的日志
adb logcat -c

REM 监听关键日志
adb logcat | findstr "SimpleMySQLClient RemoteDatabaseManagerV2 SoundDataViewModel MainActivity"
