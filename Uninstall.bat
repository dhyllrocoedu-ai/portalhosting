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
echo [1/5] Closing PortalHost...
taskkill /F /IM PortalHost.exe /T >nul 2>&1
timeout /t 1 /nobreak >nul 2>nul
echo   Done.

:: Find and uninstall PortalHost from Windows (MSI)
echo [2/5] Uninstalling via Windows Installer...
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

:: Clean app data (keep servers and playit folders)
echo [3/5] Cleaning app data...
set "APPDATA_PATH=%USERPROFILE%\.portalhost"
if exist "%APPDATA_PATH%" (
    for /d %%d in ("%APPDATA_PATH%\*") do (
        set "folderName=%%~nxd"
        if /i "!folderName!"=="servers" (
            echo   Keeping: !folderName!
        ) else if /i "!folderName!"=="playit" (
            echo   Keeping: !folderName!
        ) else (
            echo   Removing: !folderName!
            rmdir /s /q "%APPDATA_PATH%\!folderName!" 2>nul
        )
    )
    for %%f in ("%APPDATA_PATH%\*") do (
        set "fileName=%%~nxf"
        echo   Removing file: !fileName!
        del /q "%%f" 2>nul
    )
    echo   App data cleaned.
) else (
    echo   No app data folder found.
)

:: Remove Start Menu shortcuts
echo [4/5] Removing Start Menu shortcuts...
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
echo [5/5] Cleaning up installation folder...
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
reg delete "HKCU\Software\PortalHost" /f 2>nul
reg delete "HKLM\SOFTWARE\PortalHost" /f 2>nul
echo   Registry cleaned.

echo.
echo ============================================
echo Uninstall complete!
echo.
echo NOTE: The 'servers' and 'playit' folders in %USERPROFILE%\.portalhost
echo have been preserved. You can delete them manually if needed.
echo.
pause