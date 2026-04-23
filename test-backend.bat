@echo off
echo Testing backend API...
echo.

echo Testing health endpoint:
curl -X GET http://localhost:8082/api/health
echo.
echo.

echo Testing login with admin account:
curl -X POST http://localhost:8082/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@ovs.local\",\"password\":\"Password123!\"}"
echo.
echo.

pause
