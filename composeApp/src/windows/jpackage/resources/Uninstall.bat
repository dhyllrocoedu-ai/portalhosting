@echo off
setlocal enabledelayedexpansion

set "INSTALL_DIR=%~dp0"
if "%INSTALL_DIR:~-1%"=="\" set "INSTALL_DIR=%INSTALL_DIR:~0,-1%"

echo ============================================
echo PortalHost Uninstaller
echo ============================================
echo.
echo Install folder: %INSTALL_DIR%
echo.

:: Check for admin rights
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo WARNING: Not running as administrator.
    echo Some operations may require admin rights.
    echo.
)

:: Kill any running PortalHost processes
echo [1/4] Closing PortalHost...
taskkill /F /IM PortalHost.exe /T >nul 2>&1
timeout /t 1 /nobreak >nul 2>nul
echo   Done.

:: Find and uninstall PortalHost from Windows (MSI)
echo [2/4] Uninstalling via Windows Installer...
set "UNINSTALL_STRING="
for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall" /s /f "PortalHost" 2^>nul ^| findstr /i "UninstallString"') do (
    set "line=%%a %%b"
    if not "!line!%%b" == "!line!" (
        echo   Found HKLM: %%b
        set "UNINSTALL_STRING=%%b"
    )
)
if not defined UNINSTALL_STRING (
    for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall" /s /f "PortalHost" 2^>nul ^| findstr /i "UninstallString"') do (
        echo   Found HKLMx86: %%b
        set "UNINSTALL_STRING=%%b"
    )
)
if not defined UNINSTALL_STRING (
    for /f "tokens=2*" %%a in ('reg query "HKCU\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall" /s /f "PortalHost" 2^>nul ^| findstr /i "UninstallString"') do (
        echo   Found HKCU: %%b
        set "UNINSTALL_STRING=%%b"
    )
)

if defined UNINSTALL_STRING (
    echo   Running: !UNINSTALL_STRING!
    start /wait msiexec.exe /x !UNINSTALL_STRING! /qn
    echo   Windows Installer uninstall complete.
) else (
    echo   PortalHost not found in registry (or already uninstalled).
)

:: NOTE: Your data folder (servers, configs, JDKs, database) is NEVER deleted.
:: It stays wherever it was configured so you can reinstall without losing anything.

:: Remove Start Menu shortcuts
echo [3/4] Removing Start Menu shortcuts...
set "START_MENU=%APPDATA%\Microsoft\Windows\Start Menu\Programs"
if exist "%START_MENU%\PortalHost" (
    rmdir /s /q "%START_MENU%\PortalHost" 2>nul
    echo   Removed Start Menu folder.
)
if exist "%START_MENU%\PortalHost.lnk" (
    del /q "%START_MENU%\PortalHost.lnk" 2>nul
    echo   Removed shortcut.
)

:: Remove installation folder
echo [4/4] Cleaning up installation folder...
if exist "%INSTALL_DIR%" (
    :: First delete all files recursively
    for /r "%INSTALL_DIR%" %%f in (*) do (
        del /q "%%f" 2>nul
    )
    :: Then delete all directories recursively (bottom-up)
    for /f "delims=" %%d in ('dir /ad /b /s "%INSTALL_DIR%" 2^>nul ^| sort /r') do (
        rmdir "%%d" 2>nul
    )
    :: Finally delete remaining files at root level
    for %%f in ("%INSTALL_DIR%\*") do (
        del /q "%%f" 2>nul
    )
    echo   Installation folder cleaned.
) else (
    echo   Installation folder already gone.
)

:: Clean registry
echo.
echo [Extra] Cleaning registry...
reg delete "HKCU\Software\JavaSoft\Prefs\com\portalhost" /f 2>nul
echo   Registry cleaned.

echo.
echo ============================================
echo Uninstall complete!
echo.
echo Your servers, data and Java runtimes have been preserved.
echo.
pause
