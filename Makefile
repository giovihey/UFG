.PHONY: signaling game channel host all clean down

ifeq ($(OS), Windows_NT)
DETECTED_OS := Windows
else
DETECTED_OS := $(shell uname -s)
endif

LIBDATACHANNEL_PREFIX ?= C:/libdatachannel
OPENSSL_PREFIX ?= C:/Program Files/OpenSSL-Win64

WIN_RUNTIME_PATH = %CD%\channel\build\Release;$(subst /,\,$(LIBDATACHANNEL_PREFIX))\bin;$(subst /,\,$(OPENSSL_PREFIX))\bin

all: signaling game

signaling:
	docker compose up --build -d

host:
ifeq ($(DETECTED_OS), Windows)
	set "PATH=$(WIN_RUNTIME_PATH);%PATH%" && cd game && .\gradlew run --args='--host'
else
	cd game && ./gradlew run --args='--host'
endif

game:
ifeq ($(DETECTED_OS), Windows)
	set "PATH=$(WIN_RUNTIME_PATH);%PATH%" && cd game && .\gradlew run
else
	cd game && ./gradlew run
endif

channel:
ifeq ($(DETECTED_OS), Windows)
	cd channel && cmake -B build -DCMAKE_PREFIX_PATH="$(LIBDATACHANNEL_PREFIX);$(OPENSSL_PREFIX)" && cmake --build build --config Release
else
	cd channel && cmake -B build && cmake --build build
endif

test:
ifeq ($(DETECTED_OS), Windows)
	set "PATH=$(WIN_RUNTIME_PATH);%PATH%" && cd game && .\gradlew build && .\gradlew test
else
	cd game && ./gradlew build && ./gradlew test
endif


down:
	docker compose down

clean:
	docker compose down
ifeq ($(DETECTED_OS), Windows)
	cmd /c "cd game && gradlew.bat clean"
else
	cd game && ./gradlew clean
endif
