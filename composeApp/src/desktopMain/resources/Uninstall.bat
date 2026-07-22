@echo off
setlocal enabledelayedexpansion

echo ============================================
echo PortalHost Uninstaller
echo ============================================
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
timeout /t 1 /nobreak >nul

:: Find and uninstall PortalHost from Windows
echo [2/4] Uninstalling PortalHost...

:: Search registry for PortalHost uninstaller
for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall" /s /f "PortalHost" 2^>nul ^| findstr "UninstallString"') do (
    set "UNINSTALL_STRING=%%b"
)

:: Also check 32-bit registry on 64-bit Windows
if not defined UNINSTALL_STRING (
    for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall" /s /f "PortalHost" 2^>nul ^| findstr "UninstallString"') do (
        set "UNINSTALL_STRING=%%b"
    )
)

:: Also check current user registry
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

:: Keep servers and playit folders but delete everything else
echo [3/4] Cleaning app data (keeping servers and playit folders)...
set "APPDATA_PATH=%USERPROFILE%\.portalhost"

if exist "%APPDATA_PATH%" (
    echo Preserving folders: servers, playit
    
    :: Delete all items EXCEPT servers and playit
    for /d %%d in ("%APPDATA_PATH%\*") do (
        set "folderName=%%~nxd"
        if /i not "!folderName!"=="servers" (
            if /i not "!folderName!"=="playit" (
                echo   Removing: !folderName!
                rmdir /s /q "%%d" 2>nul
            ) else (
                echo   Keeping: !folderName!
            )
        )
    )
    
    :: Delete files (not folders) in the root appdata folder
    for %%f in ("%APPDATA_PATH%\*") do (
        if not "%%~ax"=="/d" (
            echo   Removing file: %%~nxf
            del /q "%%f" 2>nul
        )
    )
    
    echo App data cleaned (servers and playit preserved).
) else (
    echo No app data folder found.
)

:: Try to remove installation folder (may require admin)
echo [4/4] Cleaning up installation folder...
if exist "%ProgramFiles%\PortalHost" (
    takeown /f "%ProgramFiles%\PortalHost" /r /d y >nul 2>&1
    icacls "%ProgramFiles%\PortalHost" /grant administrators:F /t >nul 2>&1
    rmdir /s /q "%ProgramFiles%\PortalHost" 2>nul
    if exist "%ProgramFiles%\PortalHost" (
        echo Could not remove Program Files folder (may need admin).
    ) else (
        echo Installation folder removed.
    )
)

if exist "%ProgramFiles(x86)%\PortalHost" (
    takeown /f "%ProgramFiles(x86)%\PortalHost" /r /d y >nul 2>&1
    icacls "%ProgramFiles(x86)%\PortalHost" /grant administrators:F /t >nul 2>&1
    rmdir /s /q "%ProgramFiles(x86)%\PortalHost" 2>nul
)

echo.
echo ============================================
echo Uninstall complete!
echo.
echo NOTE: The 'servers' and 'playit' folders in %USERPROFILE%\.portalhost
echo have been preserved. You can delete them manually if needed.
echo.
pause