package main

import (
	"fmt"
	"log"
	"net/http"

	"github.com/gorilla/websocket"
)

// upgrader converts an HTTP connection to a WebSocket
var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true // allow all origins for dev
	},
}

func handleWebSocket(w http.ResponseWriter, r *http.Request) {
	// 1. Upgrade HTTP → WebSocket
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println("upgrade error:", err)
		return
	}
	defer conn.Close()

	log.Println("client connected")

	// 2. Read loop — runs until client disconnects
	for {
		// Read a message (blocks until one arrives)
		messageType, message, err := conn.ReadMessage()
		if err != nil {
			log.Println("client disconnected:", err)
			break
		}

		log.Printf("received: %s", message)

		// 3. Echo it back
		err = conn.WriteMessage(messageType, message)
		if err != nil {
			log.Println("write error:", err)
			break
		}
	}
}

func main() {
	http.HandleFunc("/ws", handleWebSocket)

	fmt.Println("signaling server running on :8080")
	log.Fatal(http.ListenAndServe(":8080", nil))
}
