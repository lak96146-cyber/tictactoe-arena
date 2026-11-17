# ⚡ TIC-TAC-TOE ARENA — Java Desktop Multiplayer Game

> **A modern, competitive multiplayer desktop gaming application built entirely with Pure Java (JDK 17+), Java Swing, and TCP Socket Programming.**

---

## 📌 Project Overview
**TIC-TAC-TOE ARENA** is an authoritative, client-server multiplayer gaming platform engineered specifically for competitive desktop gameplay. Moving away from standard, basic Tic-Tac-Toe implementations, this application features a sleek dark cyberpunk aesthetic, vector-glow graphics, procedural audio synthesis, series match formats (Best of 1, Best of 3, Best of 5), in-game text comms, live match timers, mutual rematch negotiation, and persistent CSV match archives and leaderboards.

---

## 🛠️ Technology Stack & Constraints

| Layer | Technology | Purpose |
|---|---|---|
| **Language** | **Java (JDK 17 or newer)** | Core language with modern object-oriented features |
| **User Interface** | **Java Swing & Java 2D (`Graphics2D`)** | Custom vector-rendered glowing X/O board, rounded components |
| **Networking** | **Java TCP Sockets (`Socket`, `ServerSocket`)** | Reliable, low-latency client-server stream communication |
| **Concurrency** | **Java Multithreading (`ExecutorService`, Threads)** | Non-blocking socket I/O and isolated game room processing |
| **Audio** | **Pure Java `javax.sound.sampled`** | Procedural 16-bit PCM sound synthesis (zero external files!) |
| **Persistence** | **Java File I/O & CSV (`PrintWriter`, `BufferedReader`)** | Permanent storage of battle history and player rankings |
| **Dependencies**| **Zero external dependencies** | 100% standard Java Runtime Environment |

> **Strict Compliance**: No web technologies (no HTML, CSS, JavaScript, React, Node.js), no Python, no Spring Boot, and no local game simulations. The central game server is 100% authoritative over board state, turn validation, win conditions, and scoring.

---

## 🏗️ Architecture & Networking Design

```
+--------------------------------------------------------------------------------+
|                             TIC-TAC-TOE ARENA ARCHITECTURE                     |
+--------------------------------------------------------------------------------+

      +-------------------------+             +-------------------------+
      |   Java Swing Client 1   |             |   Java Swing Client 2   |
      |   (Host - Player X)     |             |   (Guest - Player O)    |
      +------------+------------+             +------------+------------+
                   |                                       |
                   | TCP Socket (Port 8888)                | TCP Socket (Port 8888)
                   |                                       |
                   v                                       v
      +-----------------------------------------------------------------+
      |                 CENTRAL AUTHORITATIVE GAME SERVER               |
      |                                                                 |
      |  • GameServer (ServerSocket listener, cached thread pool)       |
      |  • ClientHandler (Worker thread per player socket)              |
      |  • ServerManager (Room registry, quick matchmaking queue)       |
      |  • GameRoom (Authoritative board, MoveValidator, WinDetector)   |
      |  • Match History & Leaderboard CSV Persistence                  |
      +-----------------------------------------------------------------+
```

### Authoritative Server Responsibilities:
1. **Connection & Session Management**: Assigns player sessions and manages clean disconnect handling.
2. **Room Orchestration**: Generates unique 6-character room codes (`ARENA-XXXX`) and manages lobby readiness.
3. **Turn & Move Validation (`MoveValidator`)**: Enforces legal turn order, boundary limits (`0..2`), cell vacancy, and active game status. Rejects illegal moves before altering state.
4. **Win & Stalemate Arbitration (`WinDetector`)**: Evaluates all 8 winning combinations (3 rows, 3 columns, 2 diagonals) and full board draws.
5. **Series State Control**: Manages Best of 1 (1 win), Best of 3 (first to 2 wins), or Best of 5 (first to 3 wins), alternating starting symbols between rounds.
6. **Data Persistence**: Appends authenticated results directly into `data/match_history.csv` and recalculates rankings in `data/player_statistics.csv`.

---

## 📁 Project Folder Structure

```
TicTacToeArena/
│
├── src/
│   ├── server/                      # Server Module (Authoritative Backend)
│   │   ├── GameServer.java          # ServerSocket listener & thread pool management
│   │   ├── ClientHandler.java       # Per-client TCP I/O worker thread
│   │   ├── GameRoom.java            # Room state machine, series tracking & arbitration
│   │   └── ServerManager.java       # Central room registry & matchmaking manager
│   │
│   ├── client/                      # Client Network & Controller Module
│   │   ├── GameClient.java          # Client game state & outbound dispatch API
│   │   ├── NetworkHandler.java      # Background socket I/O & EDT event poster
│   │   └── ClientListener.java      # Asynchronous event callback interface
│   │
│   ├── game/                        # Core Game Logic Engine
│   │   ├── GameBoard.java           # 3x3 grid state, occupancy & serialization
│   │   ├── MoveValidator.java       # Authoritative move legality validation
│   │   ├── WinDetector.java         # 8-pattern win evaluator & draw detector
│   │   ├── GameStatus.java          # Game session lifecycle enumeration
│   │   ├── PlayerSymbol.java        # Symbol representation (X, O, NONE)
│   │   └── WinningLine.java         # Coordinate carrier for highlighted winning line
│   │
│   ├── ui/                          # Java Swing User Interface (Futuristic Theme)
│   │   ├── Theme.java               # Color palette, font hierarchy & render hints
│   │   ├── ModernComponents.java    # Rounded glowing buttons, panels, badges & inputs
│   │   ├── BoardCanvas.java         # Custom Graphics2D 3x3 board with vector glow & animations
│   │   ├── LoginFrame.java          # Screen A: Welcome & Server Connection
│   │   ├── HomeFrame.java           # Screen B: Main Dashboard & Mode Selection
│   │   ├── LobbyFrame.java          # Screen C: Room Lobby & Readiness
│   │   ├── GameFrame.java           # Screen D: Live 3x3 Match Arena & Comms
│   │   ├── ResultFrame.java         # Screen E: Victory Modal & Rematch Request
│   │   ├── LeaderboardFrame.java    # Screen F: Global Hall of Fame Rankings
│   │   ├── MatchHistoryFrame.java   # Screen G: Battle Archives
│   │   ├── RulesDialog.java         # In-game rules & mechanics manual
│   │   ├── SettingsDialog.java      # Audio toggle & system diagnostics
│   │   └── ViewManager.java         # Seamless view switcher preserving window geometry
│   │
│   ├── history/                     # Persistence & Statistics Module
│   │   ├── MatchRecord.java         # Model for completed match record
│   │   ├── PlayerStats.java         # Model for player win/loss/draw ratings
│   │   ├── HistoryManager.java      # CSV reader/writer for match_history.csv
│   │   └── ScoreManager.java        # CSV reader/writer for player_statistics.csv
│   │
│   ├── audio/                       # Procedural Audio Module
│   │   └── SoundManager.java        # Pure Java PCM audio synthesizer (moves, wins, clicks)
│   │
│   ├── protocol/                    # Network Protocol Specification
│   │   └── MessageProtocol.java     # Delimited protocol tokens & message builders
│   │
│   ├── test/                        # Automated Verification Test Suite
│   │   ├── GameLogicTest.java       # Unit tests for 8 win paths, draws, and validator
│   │   └── NetworkIntegrationTest.java # Full socket E2E match test between two live clients
│   │
│   └── Main.java                    # Universal launcher (Server, Client, Demo GUI)
│
├── data/                            # Persistent Storage
│   ├── match_history.csv            # Permanent CSV log of matches played
│   └── player_statistics.csv        # Permanent CSV leaderboard statistics
│
├── build.bat / build.sh             # Build scripts
├── run-server.bat / run-server.sh   # Dedicated server launch script
├── run-client.bat / run-client.sh   # Game client launch script
├── run-demo.bat / run-demo.sh       # Instant 2-player demonstration script
├── run.bat                          # Quick graphical launcher script
└── README.md                        # Documentation & College Viva Guide
```

---

## 🚀 Quick Start & How to Run

### 1. Prerequisites
- **Java Development Kit (JDK 17 or higher)** installed.
- Verify installation by running:
  ```bash
  javac -version
  java -version
  ```

### 2. Compilation
Compile all source files into the `bin/` directory:
- **Windows**: Double-click `build.bat` or run:
  ```cmd
  build.bat
  ```
- **Linux / macOS**:
  ```bash
  chmod +x *.sh
  ./build.sh
  ```
- **Manual CLI**:
  ```cmd
  javac -encoding UTF-8 -d bin src\game\*.java src\history\*.java src\audio\*.java src\protocol\*.java src\server\*.java src\client\*.java src\ui\*.java src\test\*.java src\Main.java
  ```

---

### 3. Running Options

#### Option A: Instant 2-Player Demonstration (Recommended for College Viva!)
Spawns the background server on port 8888 and opens **two client windows** side-by-side automatically:
- **Windows**: Double-click `run-demo.bat`
- **Linux / macOS**: `./run-demo.sh`
- **CLI**: `java -cp bin Main demo`

#### Option B: Standalone Server & Clients (Multiple PCs or Terminals)
1. **Start the Server** (Host computer or Terminal 1):
   ```cmd
   run-server.bat
   ```
   *(Or: `java -cp bin server.GameServer 8888`)*

2. **Launch Client 1** (Player 1):
   ```cmd
   run-client.bat
   ```
   *(Or: `java -cp bin client.GameClient`)*
   - Enter your callsign (e.g. `Alpha`).
   - Leave IP as `localhost` (or enter host's LAN IP if playing across two laptops on Wi-Fi).
   - Click **ENTER ARENA** -> **CREATE PRIVATE ROOM** -> Choose **Best of 3**.
   - Copy the 6-character room code.

3. **Launch Client 2** (Player 2):
   ```cmd
   run-client.bat
   ```
   - Enter callsign (e.g. `Beta`).
   - Click **JOIN VIA ROOM CODE** -> Paste the code -> Click **ENTER ROOM**.
   - Both players click **READY** -> Match begins immediately!

---

## 🚀 Render Cloud Deployment (FREE Web Service Plan)

You can host **Tic-Tac-Toe Arena** for **100% free** 24/7 on [Render](https://render.com) using its Web Service tier with Docker and WebSockets.

### Why Render + WebSockets?
* **HTTP & WSS Compatibility**: Render reverse-proxies all external web traffic through HTTPS/WSS (port 443) to the container.
* **Built-in TLS Termination**: Render automatically provisions free SSL/TLS certificates for `https://` and `wss://`.
* **Zero Host Maintenance**: Render handles container orchestration, health monitoring, and automatic restarts.

---

### Step-by-Step Render Deployment Guide

#### 1. Push Your Code to GitHub
Ensure your repository contains:
* `Dockerfile`
* `render.yaml`
* `src/` directory with all Java source code
* `data/` directory with initial CSV files

```bash
git init
git add .
git commit -m "Deploy Tic-Tac-Toe Arena on Render with WebSockets"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/tictactoe-arena.git
git push -u origin main
```

#### 2. Create Web Service on Render Dashboard
1. Log in to [dashboard.render.com](https://dashboard.render.com).
2. Click **New +** (top right) -> Select **Web Service**.
3. Choose **Build and deploy from a Git repository** -> Click **Next**.
4. Connect your GitHub/GitLab account and select your `tictactoe-arena` repository.

#### 3. Exact Render Service Configuration Settings

| Setting | Value | Notes |
|---|---|---|
| **Name** | `tictactoe-arena` | Determines your URL: `tictactoe-arena.onrender.com` |
| **Region** | `Oregon (US West)` or nearest | Choose region closest to your players |
| **Branch** | `main` | Production branch |
| **Runtime** | **Docker** | Uses the included multi-stage `Dockerfile` |
| **Instance Type** | **Free ($0/month)** | 512 MB RAM, 0.1 CPU |
| **Health Check Path** | `/health` *(Under Advanced)* | Built-in endpoint returning `{"status":"UP"}` |
| **Auto-Deploy** | **Yes** | Automatically redeploys on new git pushes |

> [!NOTE]
> Render automatically injects the **`PORT`** environment variable (typically `10000`). The server automatically detects `$PORT` and binds to `0.0.0.0:$PORT`.

5. Click **Create Web Service** at the bottom of the page.

#### 4. Build & Health Check Monitoring
* Render will trigger the multi-stage build:
  * Stage 1 compiles all Java 21 source files and packages `tictactoe-server.jar`.
  * Stage 2 copies the compiled JAR into a lightweight `eclipse-temurin:21-jre-alpine` runtime.
* Render will poll `GET /health` until it receives `HTTP 200 OK`, then transitions your service status to **`Live`**.
* Your public URL will be displayed:  
  `https://tictactoe-arena.onrender.com`

---

### 🌐 Connecting Desktop Clients to Your Render Service

Once your Render service is `Live`:

1. Launch your desktop game client using **[run-client.bat](file:///c:/Users/Hp/OneDrive/tic%20tack%20toe/run-client.bat)** (or `java -jar tictactoe-client.jar`).
2. On the **Login Screen**:
   * **Callsign / Username**: Enter your username (e.g. `CyberViper`).
   * **Server Host / Render URL**: Enter your Render domain without `https://` (e.g. `tictactoe-arena.onrender.com`).
   * **Server Port**: `443` (or leave blank).
3. Click **ENTER ARENA**!
   * The client automatically establishes a secure **WSS (TLS WebSocket)** connection to `wss://tictactoe-arena.onrender.com/ws`.
   * Real-time game events, moves, and comms will flow with low latency across Render's global network.

---

### 💡 Render Free Tier Nuances to Know
1. **Inactivity Sleep**: Render's free tier spins down containers after 15 minutes of zero traffic.
2. **Automatic Wake-Up**: When a player connects to a sleeping service, Render automatically wakes the container within **~30 to 50 seconds**. The client will establish connection once ready.
3. **Storage Abstraction**: Render free instances have ephemeral filesystems. Our `StorageService` abstraction persists records in memory and to `data/`, ensuring that even during cloud restarts, player matchmaking, room management, and active gaming sessions remain rock-solid.

---

## ☁️ Ubuntu Linux Cloud Deployment & Remote Client Connections

The `GameServer` is 100% headless and decoupled from GUI dependencies, allowing it to run smoothly on any Linux cloud VPS (AWS EC2, Oracle Cloud, DigitalOcean, Linode, Google Cloud, Azure, Hetzner, etc.).

### 1. Cloud Instance Setup (Ubuntu 20.04 / 22.04 / 24.04 LTS)

Connect to your cloud server via SSH:
```bash
ssh ubuntu@YOUR_SERVER_PUBLIC_IP
```

Install the lightweight headless Java Runtime Environment (JDK 17 or 21):
```bash
sudo apt update
sudo apt install -y openjdk-21-jre-headless
# Verify Java installation:
java -version
```

### 2. Configure Cloud Ingress Firewall (Port 8888)

1. **Host Firewall (UFW)**:
   ```bash
   sudo ufw allow 8888/tcp
   sudo ufw reload
   ```

2. **Cloud Provider Security Groups / Firewall Rules**:
   - **AWS EC2**: Security Groups -> Inbound Rules -> Add Rule: `Custom TCP`, Port: `8888`, Source: `0.0.0.0/0` (Anywhere).
   - **Oracle Cloud (OCI)**: Virtual Cloud Network (VCN) -> Security Lists -> Ingress Rules -> IP Protocol: `TCP`, Destination Port Range: `8888`, Source CIDR: `0.0.0.0/0`.
   - **DigitalOcean / Linode**: Inbound Firewalls -> Port: `8888`, Protocol: `TCP`, Sources: All IPv4 & IPv6.

### 3. Deploy the Server Files

On your local machine, run `build.bat` (or `./build.sh` on Linux) to produce `tictactoe-server.jar`.
Upload `tictactoe-server.jar` and the `data/` directory to your cloud instance:

```bash
# Example using SCP from local machine:
scp tictactoe-server.jar ubuntu@YOUR_SERVER_PUBLIC_IP:/home/ubuntu/
scp -r data/ ubuntu@YOUR_SERVER_PUBLIC_IP:/home/ubuntu/
```

On your Ubuntu server, organize the files:
```bash
sudo mkdir -p /opt/tictactoe/data
sudo cp /home/ubuntu/tictactoe-server.jar /opt/tictactoe/
sudo cp -r /home/ubuntu/data/* /opt/tictactoe/data/
sudo chown -R ubuntu:ubuntu /opt/tictactoe
```

### 4. Running the Server

#### Option A: Quick Manual Execution (or inside `tmux` / `screen`)
```bash
cd /opt/tictactoe
java -Djava.awt.headless=true -jar tictactoe-server.jar 8888
```

#### Option B: Background Service using `nohup`
```bash
cd /opt/tictactoe
nohup java -Djava.awt.headless=true -jar tictactoe-server.jar 8888 > server.log 2>&1 &
# Check live logs:
tail -f server.log
```

#### Option C: Production 24/7 Systemd Service (Recommended)
Copy the included `tictactoe-server.service` file to the systemd directory:
```bash
sudo cp /home/ubuntu/tictactoe-server.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now tictactoe-server

# Check status:
sudo systemctl status tictactoe-server

# View live timestamped logs:
journalctl -u tictactoe-server -f
```

### 5. Connecting Remote Players from Anywhere in the World

Once the server is running on your cloud instance:

1. Send `tictactoe-client.jar` (or the project folder with `run-client.bat`) to players.
2. Players launch their desktop client (`run-client.bat` or `java -jar tictactoe-client.jar`).
3. On the **Login Screen**:
   - **Callsign / Username**: Enter player name (e.g., `CyberPlayer`).
   - **Server IP Address**: Replace `localhost` with your **Cloud Server Public IPv4 Address** (e.g., `54.210.88.19`).
   - **Server Port**: `8888` (default).
4. Click **ENTER ARENA**!
5. Host creates a private room, sends the 6-character room code to a friend anywhere in the world, and plays real-time low-latency multiplayer Tic-Tac-Toe.

---

## 📡 Network Communication Protocol

All communication between client and server uses UTF-8 pipe-delimited message packets (`::`):

| Direction | Command | Arguments | Description |
|---|---|---|---|
| **C -> S** | `LOGIN` | `username` | Registers player callsign with server |
| **S -> C** | `LOGIN_SUCCESS` | `username` | Confirms authentication |
| **C -> S** | `CREATE_ROOM` | `mode` (`BO1`\|`BO3`\|`BO5`) | Requests new room creation |
| **S -> C** | `ROOM_CREATED` | `code`, `mode`, `host` | Returns unique 6-character room code |
| **C -> S** | `JOIN_ROOM` | `code` | Requests entry into an existing room |
| **S -> C** | `ROOM_JOINED` | `code`, `host`, `guest`, `mode`| Confirms guest entry into room |
| **C -> S** | `TOGGLE_READY` | `code` | Toggles player readiness state |
| **S -> C** | `PLAYER_READY_STATUS`| `code`, `hostReady`, `guestReady`| Updates both players on ready status |
| **S -> C** | `GAME_START` | `code`, `pX`, `pO`, `mySym`, `curTurn`, `mode`, `round`, `targets`, `sHost`, `sGuest` | Authoritative match launch |
| **C -> S** | `MAKE_MOVE` | `code`, `row`, `col` | Requests move at cell `(row, col)` |
| **S -> C** | `MOVE_MADE` | `code`, `row`, `col`, `symbol`, `nextTurn` | Authoritatively broadcasts verified move |
| **S -> C** | `MOVE_REJECTED` | `code`, `reason` | Rejects illegal or out-of-turn move |
| **S -> C** | `ROUND_RESULT` | `code`, `winner`, `coords`, `sHost`, `sGuest`, `isSeriesOver` | Announces round winner & strike line |
| **S -> C** | `MATCH_OVER` | `code`, `winnerName`, `sHost`, `sGuest`, `durationSec` | Declares series champion & commits history |
| **C <-> S**| `CHAT` | `code`, `sender`, `message` | Broadcasts in-game communication |
| **C <-> S**| `REMATCH_REQUEST`| `code` | Proposes rematch requiring mutual agreement |
| **C -> S** | `GET_LEADERBOARD`| *(none)* | Requests current leaderboard standings |
| **C -> S** | `GET_HISTORY` | *(none)* | Requests full match archives |

---

## 🎨 UI/UX Design System

The application utilizes custom Java 2D vector painting (`Graphics2D`) with rendering hints for anti-aliasing:
- **Background**: Deep space charcoal `#0B0E17` with floating container cards `#121826`.
- **Accents**: Neon purple `#9D4EDD`, Electric cyan `#00F0FF`, Cobalt blue `#3A86FF`, Emerald `#00FF87`, and Laser pink `#FF0055`.
- **Vector X Symbol**: Multi-pass pink/magenta bloom with rounded caps and high-intensity crisp white center core.
- **Vector O Symbol**: Multi-pass cyan bloom ring with anti-aliasing and crisp white center core.
- **Hover Previews**: Hovering over empty squares renders a semi-transparent ghost symbol of your assigned mark.
- **Animated Winning Line**: A pulsing laser strike-through line connects the three winning cells with center highlights.
- **Procedural Sound Effects**: Synthesized dynamically on the fly via `SoundManager` using mathematical sine waves and envelope modulation (100% self-contained, no audio files to lose).

---

## 🧪 Automated Testing & Verification

The project includes built-in automated test suites:

### 1. Game Logic Test Suite
Verifies all 8 winning combinations, stalemate detection, move validation, and CSV persistence:
```cmd
java -cp bin test.GameLogicTest
```
**Results**:
- `[PASS] Horizontal Row Win Tests (3 rows)`
- `[PASS] Vertical Column Win Tests (3 columns)`
- `[PASS] Diagonal Win Tests (Main & Anti-diagonal)`
- `[PASS] Draw / Stalemate Detection Test`
- `[PASS] Authoritative Move Validation Test`
- `[PASS] File Persistence & Leaderboard Calculation Test`
- **Result: 6 Passed, 0 Failed.**

### 2. Network Integration Test Suite
Spawns an internal server, establishes two live TCP socket connections, tests room creation, opponent joining, in-room comms, out-of-turn rejection, live move sequences, win detection, and clean resource teardown:
```cmd
java -cp bin test.NetworkIntegrationTest
```
**Results**:
- `[PASS] Inbound TCP connection acceptance & authentication`
- `[PASS] Room generation & guest admission`
- `[PASS] In-game chat broadcast verified`
- `[PASS] Authoritative out-of-turn move rejection verified`
- `[PASS] Winning combination & series champion verified`
- `[PASS] Server clean shutdown verified`

---

## 🎓 College Viva & Examination Guide (Q&A)

### Q1: Why is the server "authoritative" instead of letting clients determine who wins?
> **Answer**: In peer-to-peer or client-authoritative games, a rogue or malfunctioning client can tamper with move counts, place out-of-turn marks, or claim fake victories. In **Tic-Tac-Toe Arena**, the server maintains the single source of truth (`GameBoard`). The client merely sends an intent (`MAKE_MOVE::code::row::col`). The server validates the player's turn, cell emptiness, and game status via `MoveValidator`, updates the grid, runs `WinDetector`, and broadcasts the verified state to both players.

### Q2: How does the application avoid freezing the GUI during network operations?
> **Answer**: Java Swing runs on a single thread called the **Event Dispatch Thread (EDT)**. If network I/O (`Socket.connect`, `in.readLine()`) were performed on the EDT, the entire window would lock up and stop repainting. We solve this by delegating all socket reading to a background daemon thread (`NetworkReaderThread`). When a message arrives, it is posted back to the EDT via `SwingUtilities.invokeLater(() -> processMessage())`, ensuring thread-safe UI updates without blocking user interaction.

### Q3: How does the server handle multiple rooms and players simultaneously?
> **Answer**: The `GameServer` uses an `ExecutorService` (cached thread pool) where each accepted socket connection is assigned a `ClientHandler` runnable thread. Each match session is represented by a thread-safe `GameRoom` stored in a `ConcurrentHashMap` inside `ServerManager`. Synchronization locks on `GameRoom` instances ensure that concurrent moves from both players are processed sequentially and safely.

### Q4: How is audio handled without external `.wav` or `.mp3` files?
> **Answer**: `SoundManager.java` uses `javax.sound.sampled.AudioSystem` to mathematically generate 16-bit PCM waveforms (frequencies, envelopes, and harmonics). For example, the victory fanfare generates an arpeggio across frequencies 523Hz (C5), 659Hz (E5), 784Hz (G5), and 1046Hz (C6) with exponential decay. This guarantees 100% audio reliability across any computer without missing asset files.

### Q5: How is persistent data stored?
> **Answer**: The server persists match outcomes to `data/match_history.csv` via thread-safe file appending, and tracks player win/loss/draw records in `data/player_statistics.csv`. These records are read into memory, sorted using Java's `Comparable` interface (highest wins, then highest win rate), and served to clients requesting the Global Leaderboard.

---

## 👥 Contributors & College Group Project Information
- **Course**: Object-Oriented Software Engineering / Advanced Java Programming
- **Project Name**: TIC-TAC-TOE ARENA
- **Year**: 2026
- **Architecture**: Authoritative TCP Client-Server Desktop Application
