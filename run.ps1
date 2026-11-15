$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot

$h2Jar    = "$env:USERPROFILE\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar"
$servJar  = "$env:USERPROFILE\.m2\repository\javax\servlet\javax.servlet-api\4.0.1\javax.servlet-api-4.0.1.jar"
$mysqlJar = "$env:USERPROFILE\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar"
$cp       = "target\classes;$h2Jar;$servJar;$mysqlJar"

# Always recompile to pick up any changes
Write-Host "=== Compiling Java files... ===" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path "target\classes" | Out-Null
Copy-Item -Path "src\main\resources\*" -Destination "target\classes\" -Force

$sources = Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }

javac -cp $cp -d "target\classes" $sources
if ($LASTEXITCODE -ne 0) {
    Write-Host "COMPILATION FAILED. Please check errors above." -ForegroundColor Red
    exit 1
}

Write-Host "=== Compilation successful! Starting server... ===" -ForegroundColor Green
Write-Host ""
Write-Host "  Open your browser at:  http://localhost:8080/login" -ForegroundColor Yellow
Write-Host ""
Write-Host "  Admin login -> Username: admin  |  Password: admin123" -ForegroundColor Magenta
Write-Host "  Student login -> Use Roll Number + Registered Email" -ForegroundColor Magenta
Write-Host ""
Write-Host "  Press Ctrl+C to stop the server." -ForegroundColor Gray
Write-Host ""

java -cp $cp com.studentmanagement.StandaloneAppRunner
