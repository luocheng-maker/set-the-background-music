@echo off
setlocal

:: 保存原代码页
for /f "tokens=2 delims=:" %%a in ('chcp') do set "OLDCP=%%a"
chcp 65001 >nul

if "%~1"=="" (
    echo Usage: chver.bat ^<new-version^>
    echo Example: chver.bat 1.0.2
    exit /b 1
)

set "NEWVER=%~1"

:: 校验 X.Y.Z 格式
powershell -NoProfile -Command "if ('%NEWVER%' -notmatch '^[0-9]+\.[0-9]+\.[0-9]+$') { exit 1 }"
if errorlevel 1 (
    echo [ERROR] Invalid version format. Expected X.Y.Z
    exit /b 1
)

:: 备份
copy /y build.gradle build.gradle.bak >nul
copy /y gradle.properties gradle.properties.bak >nul

:: 改 build.gradle（显式 UTF-8 读写）
powershell -NoProfile -Command ^
  "$q=[char]34;" ^
  "$p=(Resolve-Path build.gradle).Path;" ^
  "$c=[IO.File]::ReadAllText($p,[Text.Encoding]::UTF8);" ^
  "$c=$c -replace ('version = '+$q+'[0-9.]+'+$q+' \+ versionSuffix'),('version = '+$q+'%NEWVER%'+$q+' + versionSuffix');" ^
  "[IO.File]::WriteAllText($p,$c,[Text.UTF8Encoding]::new($false))"

:: 改 gradle.properties（显式 UTF-8 读写）
powershell -NoProfile -Command ^
  "$p=(Resolve-Path gradle.properties).Path;" ^
  "$c=[IO.File]::ReadAllText($p,[Text.Encoding]::UTF8);" ^
  "$c=$c -replace 'mod_version=[0-9.]+','mod_version=%NEWVER%';" ^
  "[IO.File]::WriteAllText($p,$c,[Text.UTF8Encoding]::new($false))"

echo.
echo ============================================
echo  Version bumped to %NEWVER%
echo ============================================

:: 只显示版本那一行（/c: 表示精确字符串匹配）
findstr /n /c:"version = " build.gradle | findstr /c:"versionSuffix"
findstr /n /c:"mod_version" gradle.properties

echo.
echo  Backups: build.gradle.bak / gradle.properties.bak
echo  Delete them after confirming.

:: 恢复原代码页
chcp %OLDCP% >nul
endlocal
pause