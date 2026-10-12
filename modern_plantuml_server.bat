@echo off
setlocal

rem Change to the directory of this script
cd /d "%~dp0"

rem Set console code page to UTF-8
chcp 65001 >nul

rem Default Port: 6080
if not defined PLANTUML_SERVER_PORT (
    set "PLANTUML_SERVER_PORT=6080"
)

rem Default ContextPath: /plantuml
if not defined PLANTUML_SERVER_CONTEXTPATH (
    set "PLANTUML_SERVER_CONTEXTPATH=/plantuml"
)

rem Ensure ContextPath starts with slash
set "FIRST_CHAR=%PLANTUML_SERVER_CONTEXTPATH:~0,1%"
if not "%FIRST_CHAR%"=="/" (
    set "PLANTUML_SERVER_CONTEXTPATH=/%PLANTUML_SERVER_CONTEXTPATH%"
)

rem FONT_PATH: if not set or directory does not exist, OS fonts will be used
rem if not defined FONT_PATH set "FONT_PATH="

rem SAMPLE_PATH: if not set or directory does not exist, internal samples will be used
rem if not defined SAMPLE_PATH set "SAMPLE_PATH="

rem Determine Java command
rem Linux service default: /usr/lib/jvm/java-17-openjdk-17.0.16.0.8-2.el9.x86_64
set "JAVA_CMD="

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    )
)

if not defined JAVA_CMD (
    if exist "C:\java\jdk-17.0.12\bin\java.exe" (
        set "JAVA_CMD=C:\java\jdk-17.0.12\bin\java.exe"
    ) else if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
        set "JAVA_CMD=C:\Program Files\Java\jdk-17\bin\java.exe"
    ) else if exist "C:\java\jdk-17\bin\java.exe" (
        set "JAVA_CMD=C:\java\jdk-17\bin\java.exe"
    )
)

if not defined JAVA_CMD (
    for /f "tokens=*" %%i in ('where java 2^>nul') do (
        if not defined JAVA_CMD set "JAVA_CMD=%%i"
    )
)

if not defined JAVA_CMD (
    echo [ERROR] Java was not found. Please install Java or set JAVA_HOME.
    exit /b 1
)

rem Locate JAR file
set "JAR_FILE=%~dp0modern_plantuml_server.jar"
if not exist "%JAR_FILE%" (
    if exist "%~dp0build\libs\modern_plantuml_server.jar" (
        set "JAR_FILE=%~dp0build\libs\modern_plantuml_server.jar"
    ) else (
        echo [ERROR] JAR file not found: %JAR_FILE%
        echo Please run '.\gradlew.bat bootJar' or place modern_plantuml_server.jar in this directory.
        exit /b 1
    )
)

echo Starting Modern PlantUML Server...
echo Java        : "%JAVA_CMD%"
echo JAR         : "%JAR_FILE%"
echo Port        : %PLANTUML_SERVER_PORT%
echo ContextPath : %PLANTUML_SERVER_CONTEXTPATH%
if defined FONT_PATH (
    echo Font Path   : %FONT_PATH%
) else (
    echo Font Path   : ^(OS Fonts^)
)
if defined SAMPLE_PATH (
    echo Sample Path : %SAMPLE_PATH%
) else (
    echo Sample Path : ^(Internal resources^)
)

rem Execute with options from modern_plantuml_server.service
"%JAVA_CMD%" -Djava.awt.headless=true -Duser.timezone=Asia/Tokyo -jar "%JAR_FILE%" %*

endlocal
