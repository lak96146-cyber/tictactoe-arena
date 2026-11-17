@echo off
title Tic-Tac-Toe Arena — Game Client
echo Launching Tic-Tac-Toe Arena Client...
if exist tictactoe-client.jar (
    start "Tic-Tac-Toe Arena Client" javaw -jar tictactoe-client.jar
) else (
    start "Tic-Tac-Toe Arena Client" javaw -cp bin client.GameClient
)
