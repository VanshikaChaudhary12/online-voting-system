@echo off
echo Checking users in database...
curl http://localhost:8082/api/debug/users
echo.
echo.
echo Testing login...
curl -X POST http://localhost:8082/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@ovs.local\",\"password\":\"Password123!\"}"
echo.
pause
