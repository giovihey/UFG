.PHONY: signaling game channel all clean down

ifeq ($(OS), Windows_NT)
    DETECTED_OS := Windows
else
    DETECTED_OS := $(shell uname -s)
endif

all: signaling game

signaling:
	docker compose up --build -d

host:
ifeq ($(DETECTED_OS), Windows)
	cd game && .\gradlew run --args='--host'
else
	cd game && ./gradlew run --args='--host'
endif

game:
ifeq ($(DETECTED_OS), Windows)
	cd game && .\gradlew run
else
	cd game && ./gradlew run
endif

channel:
ifeq ($(DETECTED_OS), Windows)
	cd channel && cmake -B build && cmake --build build --config Release
else
    cd channel && cmake -B build && cmake --build build
endif

down:
	docker compose down

clean:
	docker compose down
ifeq ($(DETECTED_OS), Windows)
	cd game && .\gradlew clean
else
	cd game && ./gradlew clean
endif