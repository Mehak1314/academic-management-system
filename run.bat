@echo off
cd /d "%~dp0"

set H2=%USERPROFILE%\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar
set SERV=%USERPROFILE%\.m2\repository\javax\servlet\javax.servlet-api\4.0.1\javax.servlet-api-4.0.1.jar
set MYSQL=%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar
set CP=target\classes;%H2%;%SERV%;%MYSQL%

echo === Compiling Java files... ===
mkdir target\classes 2>nul
copy src\main\resources\* target\classes\ >nul 2>&1

for /r src\main\java %%f in (*.java) do set SOURCES=!SOURCES! "%%f"
setlocal enabledelayedexpansion

set SOURCES=
for /r src\main\java %%f in (*.java) do set SOURCES=!SOURCES! "%%f"

javac -cp "%CP%" -d target\classes %SOURCES%
if %errorlevel% neq 0 (
    echo COMPILATION FAILED!
    pause
    exit /b 1
)

echo.
echo === Compilation successful! Starting server... ===
echo.
echo   Open your browser at:  http://localhost:8080/login
echo.
echo   Admin login    - Username: admin  ^|  Password: admin123
echo   Student login  - Roll Number + Registered Email
echo.
echo   Press Ctrl+C to stop the server.
echo.

java -cp "%CP%" com.studentmanagement.StandaloneAppRunner
pause
