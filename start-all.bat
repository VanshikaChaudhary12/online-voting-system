@echo off
echo ========================================
echo   Online Voting System - Startup
echo ========================================
echo.
echo Starting Backend Server...
start "Backend Server" cmd /k "cd backend && mvn spring-boot:run"
timeout /t 5 /nobreak >nul
echo.
echo Starting Frontend Server...
start "Frontend Server" cmd /k "cd frontend && npm install && npm run dev"
echo.
echo ========================================
echo Both servers are starting...
echo Backend: http://localhost:8082
echo Frontend: http://localhost:5173
echo ========================================
echo.
echo Demo Accounts:
echo - admin@ovs.local / Password123!
echo - orgadmin@ovs.local / Password123!
echo - voter@ovs.local / Password123!
echo.
