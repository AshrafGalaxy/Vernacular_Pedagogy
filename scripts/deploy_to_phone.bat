@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
cd /d "%~dp0\..\android"

echo ========================================================
echo [1/3] Compiling and installing Palash-Setu to device...
echo ========================================================
call gradlew.bat installDebug
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Build or installation failed!
    exit /b %ERRORLEVEL%
)

echo.
echo ========================================================
echo [2/3] Launching Palash-Setu on device...
echo ========================================================
"C:\Users\Ashraf\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n com.example.palashsetu/.MainActivity
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to start activity via ADB!
    exit /b %ERRORLEVEL%
)

echo.
echo ========================================================
echo [3/3] SUCCESS: Palash-Setu is now running on your phone!
echo ========================================================
