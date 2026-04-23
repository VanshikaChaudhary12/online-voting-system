@echo off
echo ========================================
echo Starting Online Voting System
echo ========================================
echo.

REM Set Maven path
set PATH=%USERPROFILE%\maven\apache-maven-3.9.6\bin;%PATH%

echo Checking for existing servers...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8082') do taskkill /F /PID %%a >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173') do taskkill /F /PID %%a >nul 2>&1
echo.

echo [1/2] Starting Backend Server on port 8082...
start "Backend Server - Port 8082" cmd /k "cd /d %~dp0backend && mvn spring-boot:run"

echo Waiting for backend to initialize...
timeout /t 10 /nobreak >nul

echo.
echo [2/2] Starting Frontend Server on port 5173...
start "Frontend Server - Port 5173" cmd /k "cd /d %~dp0frontend && npm run dev"

echo.
echo ========================================
echo Both servers are starting!
echo ========================================
echo.
echo Backend:  http://localhost:8082
echo Frontend: http://localhost:5173
echo.
echo Demo Accounts:
echo - admin@ovs.local / Password123!
echo - orgadmin@ovs.local / Password123!
echo - voter@ovs.local / Password123!
echo.
echo Press any key to stop all servers...
pause >nul

echo.
echo Stopping servers...
taskkill /FI "WindowTitle eq Backend Server - Port 8082*" /T /F >nul 2>&1
taskkill /FI "WindowTitle eq Frontend Server - Port 5173*" /T /F >nul 2>&1

echo Servers stopped.
