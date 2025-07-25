@echo off
REM 크로스 플랫폼 진입점 스크립트 (Windows Batch)

echo 🐳 TechWikiPlus Docker Manager
echo OS detected: Windows (Batch)
echo.

REM PowerShell 스크립트 실행
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1"

pause