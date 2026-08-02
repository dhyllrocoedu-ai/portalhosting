@echo off
setlocal enabledelayedexpansion

set "INSTALL_DIR=%~dp0"
if "%INSTALL_DIR:~-1%"=="\" set "INSTALL_DIR=%INSTALL_DIR:~0,-1%"

if not defined LOCALAPPDATA set "LOCALAPPDATA=%USERPROFILE%\AppData\Local"
set "DATA_TARGET=%LOCALAPPDATA%\PortalHost"

echo ============================================
echo PortalHost Uninstaller
echo ============================================
echo.
echo Install folder: %INSTALL_DIR%
echo.
echo Your data (servers, JDKs, playit, database) is NEVER deleted.
if exist "%INSTALL_DIR%\servers\" if not exist "%DATA_TARGET%\servers\" (
    echo NOTE: Your data was detected inside the install folder and will
    echo be moved to:
    echo   %DATA_TARGET%
)
echo.

:: Check for admin rights
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo WARNING: Not running as administrator.
    echo Some operations may require admin rights.
    echo.
)

:: [1/4] Kill any running PortalHost processes
echo [1/4] Closing PortalHost...
taskkill /F /IM PortalHost.exe /T >nul 2>&1
timeout /t 1 /nobreak >nul 2>nul
echo   Done.

:: [2/4] Preserve user data (only if it lives inside the install folder).
:: The data folders are moved OUT of the install folder BEFORE the Windows
:: installer removes it, so servers, JDKs and playit are never deleted.
echo [2/4] Preserving your data...
set "MOVED=0"
set "FOUND=0"
if not exist "%DATA_TARGET%\" mkdir "%DATA_TARGET%"
for %%d in (servers jdks playit backups temp) do (
    if exist "%INSTALL_DIR%\%%d\" (
        set "FOUND=1"
        if not exist "%DATA_TARGET%\%%d\" (
            move "%INSTALL_DIR%\%%d" "%DATA_TARGET%\%%d" >nul 2>nul
            if exist "%DATA_TARGET%\%%d\" (
                echo   Preserved: %%d -^> %DATA_TARGET%\%%d
                set "MOVED=1"
            ) else (
                echo   WARNING: Could not move %%d. It will be left in place.
            )
        ) else (
            echo   Note: %%d already exists in %DATA_TARGET% - left untouched.
        )
    )
)
if exist "%INSTALL_DIR%\portalhost.db" (
    set "FOUND=1"
    if not exist "%DATA_TARGET%\portalhost.db" (
        move "%INSTALL_DIR%\portalhost.db" "%DATA_TARGET%\portalhost.db" >nul 2>nul
        if exist "%DATA_TARGET%\portalhost.db" (
            echo   Preserved: portalhost.db -^> %DATA_TARGET%\portalhost.db
            set "MOVED=1"
        )
    )
)
if "!FOUND!"=="0" echo   No PortalHost data found inside the install folder.

:: [3/4] Find and uninstall PortalHost from Windows.
:: Search the registry uninstall keys for a DisplayName matching PortalHost
:: and run that entry's uninstaller (MSI product code or uninstaller EXE).
echo [3/4] Uninstalling PortalHost from Windows...
set "FOUND_KEY="
for %%H in (
    "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall"
    "HKLM\SOFTWARE\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall"
    "HKCU\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall"
) do (
    if not defined FOUND_KEY (
        for /f "delims=" %%L in ('reg query %%H /s /f "PortalHost" 2^>nul') do (
            set "LINE=%%L"
            set "FIRST=!LINE:~0,1!"
            if "!FIRST!"=="H" if not defined FOUND_KEY (
                set "FOUND_KEY=%%L"
            )
        )
    )
)

set "UNINSTALL_STRING="
if defined FOUND_KEY (
    echo   Found registry entry: !FOUND_KEY!
    for /f "tokens=2*" %%a in ('reg query "!FOUND_KEY!" /v UninstallString 2^>nul') do (
        set "UNINSTALL_STRING=%%b"
    )
)

if defined UNINSTALL_STRING (
    set "PRODUCT_ID="
    for /f "tokens=2 delims={}" %%g in ("!UNINSTALL_STRING!") do set "PRODUCT_ID={%%g}"
    if defined PRODUCT_ID (
        echo   Found MSI product code: !PRODUCT_ID!
        start /wait "" msiexec.exe /x !PRODUCT_ID! /qn
    ) else (
        echo   Running uninstaller: !UNINSTALL_STRING!
        start /wait "" !UNINSTALL_STRING!
    )
    echo   Uninstaller finished.
) else (
    echo   PortalHost not found in the Windows registry.
)

:: [4/4] Remove installation folder (program files only - never user data)
echo [4/4] Cleaning up installation folder...
if exist "%INSTALL_DIR%" (
    if exist "%INSTALL_DIR%\PortalHost.exe" del /f /q "%INSTALL_DIR%\PortalHost.exe" 2>nul
    if exist "%INSTALL_DIR%\PortalHost.exe.cfg" del /f /q "%INSTALL_DIR%\PortalHost.exe.cfg" 2>nul
    if exist "%INSTALL_DIR%\Uninstall PortalHost.exe" del /f /q "%INSTALL_DIR%\Uninstall PortalHost.exe" 2>nul
    if exist "%INSTALL_DIR%\app" rmdir /s /q "%INSTALL_DIR%\app" 2>nul
    if exist "%INSTALL_DIR%\runtime" rmdir /s /q "%INSTALL_DIR%\runtime" 2>nul
    if exist "%INSTALL_DIR%\lib" rmdir /s /q "%INSTALL_DIR%\lib" 2>nul
    if exist "%INSTALL_DIR%\resources" rmdir /s /q "%INSTALL_DIR%\resources" 2>nul
    if exist "%INSTALL_DIR%\bin" rmdir /s /q "%INSTALL_DIR%\bin" 2>nul
    if exist "%INSTALL_DIR%\icons" rmdir /s /q "%INSTALL_DIR%\icons" 2>nul
    if exist "%INSTALL_DIR%\legal" rmdir /s /q "%INSTALL_DIR%\legal" 2>nul

    :: Remove the folder only if it is now empty (keeps user data intact)
    set "HAS_CONTENT="
    for /f "delims=" %%i in ('dir /b "%INSTALL_DIR%" 2^>nul') do set "HAS_CONTENT=1"
    if not defined HAS_CONTENT (
        rmdir "%INSTALL_DIR%" 2>nul
        if not exist "%INSTALL_DIR%" (
            echo   Installation folder removed.
        ) else (
            echo   Installation folder kept - this script is running from it.
        )
    ) else (
        echo   Installation folder kept - it contains files not created by PortalHost.
    )
) else (
    echo   Installation folder already removed by the uninstaller.
)

:: Remove Start Menu shortcuts
echo Removing Start Menu shortcuts...
set "START_MENU=%APPDATA%\Microsoft\Windows\Start Menu\Programs"
if exist "%START_MENU%\PortalHost" rmdir /s /q "%START_MENU%\PortalHost" 2>nul
if exist "%START_MENU%\PortalHost.lnk" del /f /q "%START_MENU%\PortalHost.lnk" 2>nul

:: If data was moved out of the install folder, drop the stale data-directory
:: override so the app uses the default data folder (where the data now lives)
if "!MOVED!"=="1" (
    reg delete "HKCU\Software\JavaSoft\Prefs\com\portalhost" /v "data/Directory" /f >nul 2>nul
)

echo.
echo ============================================
echo Uninstall complete!
echo.
echo Your servers, Java runtimes and playit data have been preserved.
echo.
if "!MOVED!"=="1" (
    echo IMPORTANT: Your data was stored inside the install folder and has
    echo been moved to:
    echo   %DATA_TARGET%
    echo After reinstalling PortalHost it will load your data from there
    echo automatically.
) else (
    echo Your data folder was not inside the install folder and was left
    echo untouched at its current location.
)
echo.
pause
endlocal
