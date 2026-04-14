@echo off
echo ================================
echo Plate Recognition Service
echo ================================
echo.

cd /d "%~dp0plate-recognition-service"

set PYTHON_PATH=%~dp0..\CarPlateDetection_env\python.exe

echo Starting service...
echo Python: %PYTHON_PATH%
echo.

"%PYTHON_PATH%" app.py

pause
