@echo off
setlocal enabledelayedexpansion

if not exist release mkdir release

call :build "win64"                                   "windows"
call :build "osx64,osxm1"                                       "macos"
call :build "linux64,linux-arm64"                               "linux"
call :build "win64,osx64,osxm1,linux64,linux-arm64"   "universal"
call :build ""                                                  "noffmpeg-universal"

echo.
echo ============================================
echo All variants built. Output in release\
echo ============================================
dir /b release
exit /b 0


:build
set "PLATFORMS=%~1"
set "TAG=%~2"

echo.
echo ============================================
echo Building: %TAG%   platforms=[%PLATFORMS%]
echo ============================================

if "%PLATFORMS%"=="" (
    call gradlew clean build -Pffmpeg_platforms=
) else (
    call gradlew clean build -Pffmpeg_platforms=%PLATFORMS%
)

if errorlevel 1 (
    echo [ERROR] Build failed for %TAG%
    exit /b 1
)

set "FOUND=0"
for %%F in (build\libs\*.jar) do (
    echo %%~nF | findstr /i "sources" >nul
    if errorlevel 1 (
        copy /y "%%F" "release\set-the-background-music-%TAG%.jar" >nul
        echo   -^> release\set-the-background-music-%TAG%.jar
        set "FOUND=1"
    )
)

if "!FOUND!"=="0" (
    echo [ERROR] No JAR produced for %TAG%
    exit /b 1
)

exit /b 0