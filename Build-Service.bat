@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ============================================
REM Configure Zig path - MODIFY THIS TO YOUR PATH
REM ============================================
set ZIG_DIR=E:\bin\zig
set ZIG_EXE=%ZIG_DIR%\zig.exe

REM Alternative common locations (uncomment if needed):
REM set ZIG_DIR=%LOCALAPPDATA%\zig
REM set ZIG_EXE=%ZIG_DIR%\zig.exe

REM Source and output
set SOURCE_FILE=FolkS\Hide.zig
set OUTPUT_DIR=app\src\main\assets\Service

REM Check if Zig exists
if not exist "%ZIG_EXE%" (
    echo ========================================
    echo [ERROR] Zig compiler not found!
    echo ========================================
    echo Expected location: %ZIG_EXE%
    echo.
    echo Please install Zig or update ZIG_DIR in this script.
    echo Download from: https://ziglang.org/download/
    echo.
    goto :endlocal
)

REM Create output directory
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

echo ========================================
echo Building ARM64 Android Executable
echo ========================================
echo Zig: %ZIG_EXE%
echo Source: %SOURCE_FILE%
echo Target: aarch64-linux-android
echo Output: %OUTPUT_DIR%\Hide
echo.

echo Compiling Hide.zig with Zig...
"%ZIG_EXE%" build-exe -target aarch64-linux-android -O ReleaseSmall -static "%SOURCE_FILE%" -femit-bin="%OUTPUT_DIR%\Hide"

if %errorlevel% equ 0 (
    echo [SUCCESS] Hide built: %OUTPUT_DIR%\Hide
) else (
    echo [FAILED] Hide build error!
    goto :endlocal
)

echo.
echo ========================================
echo Building Umount.zig
echo ========================================
echo Compiling Umount.zig with Zig...
"%ZIG_EXE%" build-exe -target aarch64-linux-android -O ReleaseSmall -static "FolkS\Umount.zig" -femit-bin="%OUTPUT_DIR%\Umount"

if %errorlevel% equ 0 (
    echo [SUCCESS] Umount built: %OUTPUT_DIR%\Umount
) else (
    echo [FAILED] Umount build error!
    goto :endlocal
)

echo.
echo ========================================
echo [ALL SUCCESS] Build complete!
echo ========================================
echo Output files:
echo   - %OUTPUT_DIR%\Hide
echo   - %OUTPUT_DIR%\Umount
echo.

:endlocal
pause
