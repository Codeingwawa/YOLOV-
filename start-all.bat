@echo off
chcp 65001 >nul
echo ================================
echo 停车场管理系统启动脚本
echo ================================
echo.

echo [1/3] 启动车牌识别服务...
start cmd /k "cd /d %~dp0plate-recognition-service && ..\..\CarPlateDetection_env\Scripts\python.exe app.py"
timeout /t 5 /nobreak > nul

echo [2/3] 启动Spring Boot后端...
start cmd /k "cd /d %~dp0parking-backend && mvn spring-boot:run -DskipTests"
timeout /t 10 /nobreak > nul

echo [3/3] 启动Vue前端...
start cmd /k "cd /d %~dp0parking-frontend && npm run dev"

echo.
echo ================================
echo 所有服务启动完成！
echo ================================
echo.
echo 前端地址: http://localhost:3000
echo 后端地址: http://localhost:8080
echo 识别服务: http://localhost:5000
echo.
pause
