@echo off
setlocal enabledelayedexpansion

:: Self-locate: get the directory where this batch file lives
set "INSTALL_DIR=%~dp0"
:: Remove trailing backslash
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
echo [1/6] Closing PortalHost...
taskkill /F /IM PortalHost.exe /T >nul 2>&1
timeout /t 2 /nobreak >nul

:: Remove Start Menu shortcuts
echo [2/6] Removing Start Menu shortcuts...
set "START_MENU=%APPDATA%\Microsoft\Windows\Start Menu\Programs"
if exist "%START_MENU%\PortalHost" (
    rmdir /s /q "%START_MENU%\PortalHost" 2>nul
    echo   Removed Start Menu folder.
)
if exist "%START_MENU%\PortalHost.lnk" (
    del /q "%START_MENU%\PortalHost.lnk" 2>nul
    echo   Removed Start Menu shortcut.
)

:: Find and uninstall PortalHost from Windows
echo [3/6] Uninstalling PortalHost via Windows Installer...

:: Search registry for PortalHost uninstaller
for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall" /s /f "PortalHost" 2^>nul ^| findstr "UninstallString"') do (
    set "UNINSTALL_STRING=%%b"
)

if not defined UNINSTALL_STRING (
    for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall" /s /f "PortalHost" 2^>nul ^| findstr "UninstallString"') do (
        set "UNINSTALL_STRING=%%b"
    )
)

if not defined UNINSTALL_STRING (
    for /f "tokens=2*" %%a in ('reg query "HKCU\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall" /s /f "PortalHost" 2^>nul ^| findstr "UninstallString"') do (
        set "UNINSTALL_STRING=%%b"
    )
)

if defined UNINSTALL_STRING (
    echo Found uninstaller: !UNINSTALL_STRING!
    echo Running uninstaller (this may take a moment)...
    start /wait cmd /c "!UNINSTALL_STRING!"
) else (
    echo PortalHost not found in Windows registry.
    echo Attempting to remove installation folder anyway...
)

:: Clean Java Preferences registry entries
echo [4/6] Cleaning registry preferences...
reg delete "HKCU\Software\JavaSoft\Prefs\com\portalhost" /f >nul 2>&1
if %errorLevel% equ 0 (
    echo   Removed Java Preferences registry entry.
) else (
    echo   No Java Preferences registry entry found.
)

:: Prompt user about data removal
echo.
echo ============================================
echo Data Folder Cleanup
echo ============================================
echo.
echo PortalHost data is stored in: %USERPROFILE%\.portalhost
echo This folder contains server files, JDK installations, and configurations.
echo.
echo Do you want to remove ALL data? (THIS CANNOT BE UNDONE)
echo.
echo   [Y] YES - Remove everything (servers, configs, JDKs)
echo   [N] NO  - Keep all data (you can delete it manually later)
echo   [K] KEEP SERVERS - Remove JDKs and configs, but keep server folders
echo.
set /p "DATA_CHOICE=Enter your choice (Y/N/K): "

if /i "%DATA_CHOICE%"=="Y" (
    echo [5/6] Removing all data...
    set "APPDATA_PATH=%USERPROFILE%\.portalhost"
    if exist "!APPDATA_PATH!" (
        rmdir /s /q "!APPDATA_PATH!" 2>nul
        if exist "!APPDATA_PATH!" (
            echo   WARNING: Could not remove all files. Some files may be in use.
        ) else (
            echo   All data removed successfully.
        )
    ) else (
        echo   No data folder found.
    )
) else if /i "%DATA_CHOICE%"=="K" (
    echo [5/6] Removing JDKs and configs, keeping servers...
    set "APPDATA_PATH=%USERPROFILE%\.portalhost"
    if exist "!APPDATA_PATH!" (
        :: Delete all items EXCEPT servers folder
        for /d %%d in ("!APPDATA_PATH!\*") do (
            set "folderName=%%~nxd"
            if /i not "!folderName!"=="servers" (
                echo   Removing: !folderName!
                rmdir /s /q "%%d" 2>nul
            ) else (
                echo   Keeping: !folderName!
            )
        )
        :: Delete files in the root appdata folder
        for %%f in ("!APPDATA_PATH!\*") do (
            if not "%%~af"=="" (
                echo   Removing file: %%~nxf
                del /q "%%f" 2>nul
            )
        )
        echo   JDKs and configs removed. Servers preserved.
    ) else (
        echo   No data folder found.
    )
) else (
    echo [5/6] Keeping all data. You can delete it manually from:
    echo   %USERPROFILE%\.portalhost
)

:: Remove installation folder
echo [6/6] Cleaning up installation folder...
if exist "%INSTALL_DIR%" (
    :: Try to remove everything except this batch file itself
    for %%f in ("%INSTALL_DIR%\*") do (
        if /i not "%%~nxf"=="Uninstall.bat" (
            if "%%~af"=="" (
                rmdir /s /q "%%f" 2>nul
            ) else (
                del /q "%%f" 2>nul
            )
        )
    )
    for /d %%d in ("%INSTALL_DIR%\*") do (
        rmdir /s /q "%%d" 2>nul
    )
    echo   Installation folder cleaned.
)

echo.
echo ============================================
echo Uninstall complete!
echo.
echo Thank you for using PortalHost.
echo ============================================
echo.

:: Self-delete: remove this batch file
timeout /t 2 /nobreak >nul
del /q "%~f0" 2>nul
