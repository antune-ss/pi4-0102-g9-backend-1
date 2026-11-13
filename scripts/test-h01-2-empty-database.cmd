@echo off
setlocal
rem Launch the companion PowerShell script with a process-only execution-policy bypass.
rem This does NOT change the user's permanent PowerShell execution policy.
set "SCRIPT=%~dp0test-h01-2-empty-database.ps1"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" %*
set "RC=%ERRORLEVEL%"
exit /b %RC%
