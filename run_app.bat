@echo off
title Freelance Agency Suite Launcher
echo ===================================================
echo   Starting Freelance Agency Suite
echo ===================================================

echo.
echo [1/2] Launching Spring Boot Backend (Port 8080)...
start cmd /k "title Backend Server && cd backend && mvn spring-boot:run"

echo.
echo [2/2] Launching React Frontend (Port 5173)...
start cmd /k "title Frontend Server && cd frontend && node ./node_modules/vite/bin/vite.js"

echo.
echo ===================================================
echo   Application Servers Initialized!
echo   -------------------------------------------------
echo   Frontend Dashboard: http://localhost:5173
echo   Backend API Server: http://localhost:8080
echo   H2 Database Console: http://localhost:8080/h2-console
echo ===================================================
pause
