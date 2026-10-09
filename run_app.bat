@echo off
title Freelance Agency Suite Launcher
echo ===================================================
echo   Starting Freelance Agency Suite (local H2 mode)
echo ===================================================

echo.
echo [1/2] Launching Spring Boot Backend (Port 8080)...
start "Backend Server" "%~dp0backend\run-dev.bat"

echo.
echo [2/2] Launching React Frontend (Port 5173)...
start "Frontend Server" "%~dp0frontend\run-dev.bat"

echo.
echo ===================================================
echo   Frontend Dashboard: http://localhost:5173
echo   Backend API Server: http://localhost:8080
echo   H2 Database Console: http://localhost:8080/h2-console
echo   Local mode always uses disposable H2, not Neon.
echo ===================================================
pause
