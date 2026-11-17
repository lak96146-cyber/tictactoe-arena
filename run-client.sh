#!/usr/bin/env bash
# Runs the Tic-Tac-Toe Arena Client on Linux
if [ -f "tictactoe-client.jar" ]; then
    java -jar tictactoe-client.jar "$@" &
else
    java -cp bin client.GameClient "$@" &
fi
