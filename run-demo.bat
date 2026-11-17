@echo off
title Tic-Tac-Toe Arena — Live 2-Player Demo
echo ========================================================
echo   Launching TIC-TAC-TOE ARENA 2-Player Live Demo...
echo   (Starts Game Server + Spawns 2 Game Clients)
echo ========================================================
if exist tictactoe-client.jar (
    java -jar tictactoe-client.jar demo
) else (
    java -cp bin Main demo
)
pause
