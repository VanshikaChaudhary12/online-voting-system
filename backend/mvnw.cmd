@REM Maven Wrapper startup script for Windows
@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

if not exist %WRAPPER_JAR% (
    echo Downloading Maven Wrapper...
    powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar' -OutFile '%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar'}"
)

java -cp %WRAPPER_JAR% "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %*
