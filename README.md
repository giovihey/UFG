# UFG - Ultimate Fighting Game

A fighting game built with Kotlin/Compose Desktop, WebRTC peer-to-peer networking, and a Go signaling server.

[Official Documentation](https://giovihey.github.io/UFG/)

## Project Structure

```
game/              Kotlin/Compose Desktop game client
channel/           C++ WebRTC wrapper (JNI bridge via libdatachannel)
signaling_server/  Go WebSocket signaling server
docs/              MkDocs documentation site
```

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| JDK | 17+ | Game (Kotlin) + JNI headers for channel |
| CMake | 3.16+ | Build C++ channel |
| Go | 1.22+ | Signaling server |
| Docker | any | Run signaling server easily |

### macOS

```bash
brew install cmake openjdk@17 go docker

export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Build libdatachannel from source
git clone --recurse-submodules https://github.com/paullouisageneau/libdatachannel.git
cd libdatachannel
cmake -B build -DCMAKE_INSTALL_PREFIX=/usr/local -DOPENSSL_ROOT_DIR=$(brew --prefix openssl)
cmake --build build -j$(sysctl -n hw.ncpu)
sudo cmake --install build
```

### Linux (Ubuntu/Debian)

```bash
sudo apt-get update
sudo apt-get install -y cmake g++ libssl-dev openjdk-17-jdk-headless golang docker.io docker-compose

# Build libdatachannel from source
git clone --recurse-submodules https://github.com/paullouisageneau/libdatachannel.git
cd libdatachannel
cmake -B build -DCMAKE_INSTALL_PREFIX=/usr/local
cmake --build build -j$(nproc)
sudo cmake --install build
```

### Windows

Install [Visual Studio Build Tools 2022](https://visualstudio.microsoft.com/downloads/), [CMake](https://cmake.org/download/), [JDK 17](https://adoptium.net/), [Go 1.22+](https://go.dev/dl/), and [Docker Desktop](https://www.docker.com/products/docker-desktop/).

You will also need `make` — install it via `choco install make` or use the underlying commands directly.

```bash
# Build libdatachannel from source (Git Bash or Developer Command Prompt)
git clone --recurse-submodules https://github.com/paullouisageneau/libdatachannel.git
cd libdatachannel
cmake -B build -DCMAKE_INSTALL_PREFIX=C:/libdatachannel
cmake --build build --config Release
cmake --install build --config Release
```

Alternatively, use vcpkg (your cats here):

```bash
vcpkg install libdatachannel:x64-windows
```

## Build and Run

```bash
# 1. Build the C++ WebRTC bridge
make channel
# Windows only — override defaults if installed at a different path:
#  LIBDATACHANNEL_PREFIX  defaults to C:/libdatachannel
#  OPENSSL_PREFIX         defaults to C:/Program Files/OpenSSL-Win64
make channel LIBDATACHANNEL_PREFIX=path/to/libdatachannel OPENSSL_PREFIX=path/to/OpenSSL-Win64

# 2. Start the signaling server
make signaling

# 3. Run the game
make game
# Windows only — pass the same overrides used in make channel:
make host LIBDATACHANNEL_PREFIX=path/to/libdatachannel OPENSSL_PREFIX=path/to/OpenSSL-Win64
```

### Running two players locally

```bash
# Terminal 1 — signaling server
make signaling

# Terminal 2 — player 1 (host, initiates WebRTC connection)
make host
# Windows only — pass the same overrides used in make channel:
make host LIBDATACHANNEL_PREFIX=path/to/libdatachannel OPENSSL_PREFIX=path/to/OpenSSL-Win64

# Terminal 3 — player 2
make game
# Windows only — pass the same overrides used in make channel:
make game LIBDATACHANNEL_PREFIX=path/to/libdatachannel OPENSSL_PREFIX=path/to/OpenSSL-Win64
```
```

## Makefile Targets

| Command | Description |
|---------|-------------|
| `make signaling` | Start signaling server (Docker) |
| `make game` | Run the game client |
| `make host` | Run the game as host |
| `make channel` | Build the C++ WebRTC wrapper |
| `make all` | Start signaling + game |
| `make down` | Stop Docker containers |
| `make clean` | Full cleanup |

## Verify Your Setup

```bash
java -version      # 17+
cmake --version    # 3.16+
go version         # 1.22+
docker --version   # any recent version
```
