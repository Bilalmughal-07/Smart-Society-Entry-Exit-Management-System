@echo off
setlocal enabledelayedexpansion
echo ============================================
echo  Smart Society Entry Management System
echo  Compile and Run Script
echo ============================================

REM --- Configuration ---
SET JAVA_HOME=C:\Program Files\Java\jdk-23
SET PATH_TO_FX=C:\javafx-sdk-21.0.11\lib

SET SRC_DIR=src
SET OUT_DIR=out
SET LIB_DIR=lib
SET RES_DIR=resources

echo.
echo [1/3] Cleaning output directory...
if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%"

echo [2/3] Compiling Java sources...
if exist sources.txt del sources.txt

REM CRITICAL FIX: Loop through files and replace \ with \\ so javac doesn't eat the backslashes
for /f "tokens=*" %%f in ('dir /s /b "%SRC_DIR%\*.java"') do (
    set "FILE_PATH=%%f"
    set "FILE_PATH=!FILE_PATH:\=\\!"
    echo "!FILE_PATH!" >> sources.txt
)

REM Build classpath from lib directory
SET CP=
for %%f in ("%LIB_DIR%\*.jar") do (
    SET CP=!CP!%%f;
)

REM Compile with classpath using the direct Java 23 path
"%JAVA_HOME%\bin\javac" -d "%OUT_DIR%" -cp "!CP!" --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml,javafx.swing @sources.txt

if %ERRORLEVEL% neq 0 (
    echo.
    echo ❌ Compilation failed! Check errors above.
    pause
    exit /b 1
)

echo [3/3] Copying resources...
if exist "%RES_DIR%" (
    xcopy /s /q /y "%RES_DIR%\*" "%OUT_DIR%\" >nul 2>&1
)

echo.
echo ✅ Compilation successful!
echo.
echo Running application...
echo.

"%JAVA_HOME%\bin\java" -cp "%OUT_DIR%;!CP!" --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml,javafx.swing com.smartsociety.Main

del sources.txt 2>nul
pause