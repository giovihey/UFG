package main

import (
	"fmt"
	"log"
	"net/http"
	"sync"

	"github.com/gorilla/websocket"
)

// upgrader converts an HTTP connection to a WebSocket
var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true // allow all origins for dev
	},
}

var (
	clients [2]*websocket.Conn
	mu      sync.Mutex
)

func handleWebSocket(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println("upgrade error:", err)
		return
	}
	defer conn.Close()

	mu.Lock()
	slot := -1
	for i := 0; i < 2; i++ {
		if clients[i] == nil {
			clients[i] = conn
			slot = i
			break
		}
	}
	mu.Unlock()

	if slot == -1 {
		log.Println("room full, rejecting client")
		return
	}

	log.Printf("player %d connected", slot+1)

	defer func() {
		mu.Lock()
		clients[slot] = nil
		mu.Unlock()
	}()

	for {
		messageType, message, err := conn.ReadMessage()
		if err != nil {
			log.Printf("player %d disconnected: %v", slot+1, err)
			break
		}

		log.Printf("player %d sent: %s", slot+1, message)

		// Forward to the other player
		mu.Lock()
		other := clients[1-slot]
		mu.Unlock()

		if other != nil {
			err = other.WriteMessage(messageType, message)
			if err != nil {
				log.Printf("error forwarding to player %d: %v", 2-slot, err)
			}
		}
	}
}

func main() {
	http.HandleFunc("/ws", handleWebSocket)

	fmt.Println("signaling server running on :8080")
	log.Fatal(http.ListenAndServe(":8080", nil))
}
