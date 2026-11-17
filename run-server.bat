@echo off
title Tic-Tac-Toe Arena — Game Server
set PORT=%1
if "%PORT%"=="" set PORT=8888

echo Starting Tic-Tac-Toe Arena Dedicated Server on port %PORT%...
if exist tictactoe-server.jar (
    java -Djava.awt.headless=true -jar tictactoe-server.jar %PORT%
) else (
    java -cp bin server.GameServer %PORT%
)
pause
