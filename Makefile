.PHONY:	signaling game channel all clean down

all: signaling game

signaling:
	docker compose up --build -d

game:
	cd game && ./gradlew run

channel:
	cd channel && cmake -B build && cmake --build build 

down:
	docker compose down

clean:
	docker compose down 
	cd game && ./gradlew clean

Usage:

# make signaling	# run the signaling server, make sure to run this before starting the game
# make game	# run the game, make sure signaling server is running first
# make all 	# both game and signaling
# make down	# stop containers
# make clean	# clean everything, including docker containers and gradle build files
