@echo off
title Tic-Tac-Toe Arena Launcher
if exist tictactoe-client.jar (
    java -jar tictactoe-client.jar
) else (
    java -cp bin Main
)
