@echo off
echo Downloading Apache Maven...

set MAVEN_VERSION=3.9.6
set MAVEN_URL=https://dlcdn.apache.org/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip
set INSTALL_DIR=%USERPROFILE%\maven

echo Creating installation directory...
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"

echo Downloading Maven %MAVEN_VERSION%...
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%TEMP%\maven.zip'}"

echo Extracting Maven...
powershell -Command "& {Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%INSTALL_DIR%' -Force}"

echo Cleaning up...
del "%TEMP%\maven.zip"

echo.
echo Maven installed to: %INSTALL_DIR%\apache-maven-%MAVEN_VERSION%
echo.
echo Adding Maven to PATH for this session...
set PATH=%INSTALL_DIR%\apache-maven-%MAVEN_VERSION%\bin;%PATH%

echo.
echo Testing Maven installation...
mvn -version

echo.
echo ========================================
echo Maven installation complete!
echo ========================================
echo.
echo To use Maven permanently, add this to your system PATH:
echo %INSTALL_DIR%\apache-maven-%MAVEN_VERSION%\bin
echo.
echo Or run this command as Administrator:
echo setx /M PATH "%%PATH%%;%INSTALL_DIR%\apache-maven-%MAVEN_VERSION%\bin"
echo.
pause
