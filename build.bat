@echo off
echo ========================================================
echo   Compiling TIC-TAC-TOE ARENA (Java JDK 17+)...
echo ========================================================

if not exist bin mkdir bin
if not exist data mkdir data

javac -encoding UTF-8 -d bin src\game\*.java src\history\*.java src\audio\*.java src\protocol\*.java src\server\*.java src\client\*.java src\ui\*.java src\test\*.java src\Main.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Source code compiled into bin folder!
    echo.
    echo Packaging production standalone server JAR...
    jar --create --file tictactoe-server.jar --main-class server.GameServer -C bin server -C bin protocol -C bin game -C bin history
    echo Packaging desktop client JAR...
    jar --create --file tictactoe-client.jar --main-class Main -C bin .
    echo.
    echo [ARTIFACTS GENERATED]
    echo   - tictactoe-server.jar [Headless cloud server]
    echo   - tictactoe-client.jar [Desktop game client]
    echo   - bin folder [Compiled classes]
) else (
    echo.
    echo [ERROR] Compilation failed. Please ensure JDK 17+ is installed and on PATH.
)

pause
