#!/usr/bin/env bash
# Build script for Linux (Ubuntu, Debian, CentOS, etc.)
set -e

echo "========================================================"
echo "  Compiling TIC-TAC-TOE ARENA on Linux (Java JDK 17+)"
echo "========================================================"

mkdir -p bin
mkdir -p data

javac -encoding UTF-8 -d bin $(find src -name "*.java")

echo "[SUCCESS] Source code compiled into 'bin' folder!"
echo "Packaging standalone server JAR (Headless Linux)..."
jar --create --file tictactoe-server.jar --main-class server.GameServer -C bin server -C bin protocol -C bin game -C bin history

echo "Packaging desktop client JAR..."
jar --create --file tictactoe-client.jar --main-class Main -C bin .

echo ""
echo "[ARTIFACTS READY]"
echo "  - tictactoe-server.jar (Run on server: java -Djava.awt.headless=true -jar tictactoe-server.jar)"
echo "  - tictactoe-client.jar (Run on desktop: java -jar tictactoe-client.jar)"
echo "  - bin/ (Class files)"
