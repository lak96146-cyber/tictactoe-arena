#!/usr/bin/env bash
# Runs the Tic-Tac-Toe Arena Authoritative Server on Linux
PORT=${1:-8888}
echo "Starting Tic-Tac-Toe Arena Game Server on port $PORT (Headless mode)..."

if [ -f "tictactoe-server.jar" ]; then
    java -Djava.awt.headless=true -jar tictactoe-server.jar "$PORT"
else
    java -Djava.awt.headless=true -cp bin server.GameServer "$PORT"
fi
