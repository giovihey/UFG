package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"sync"

	"github.com/gorilla/websocket"
)

// upgrader converts an HTTP connection to a WebSocket.
var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true // allow all origins for dev
	},
}

// client wraps a single player's WebSocket connection. gorilla/websocket forbids
// concurrent writes to the same connection, so every write goes through writeMu — both
// peers' read loops forward into each other, so a client can be written to from the other
// peer's goroutine.
type client struct {
	conn    *websocket.Conn
	writeMu sync.Mutex
	room    *room
	role    string // "offerer" or "answerer", assigned at match time
}

func (c *client) send(messageType int, data []byte) error {
	c.writeMu.Lock()
	defer c.writeMu.Unlock()
	return c.conn.WriteMessage(messageType, data)
}

// room pairs exactly two matched players. SDP/ICE/ready/start traffic is relayed only
// between the two peers of the same room, so many rooms run concurrently in isolation.
type room struct {
	id    int
	peers [2]*client
}

func (r *room) other(c *client) *client {
	if r.peers[0] == c {
		return r.peers[1]
	}
	return r.peers[0]
}

var (
	mu sync.Mutex
	// waiting holds the single player currently in the matchmaking queue, or nil when the
	// queue is empty. Pairing-on-arrival keeps the queue at most one deep: the Nth arrival
	// either waits (odd) or pairs with the waiter and opens a new room (even). This scales
	// to unlimited concurrent rooms with no per-room configuration.
	waiting    *client
	nextRoomID int
)

func matchedMsg(role string) []byte {
	b, _ := json.Marshal(map[string]string{"type": "matched", "role": role})
	return b
}

func peerLeftMsg() []byte {
	b, _ := json.Marshal(map[string]string{"type": "peer_left"})
	return b
}

func handleWebSocket(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println("upgrade error:", err)
		return
	}
	defer conn.Close()

	c := &client{conn: conn}

	mu.Lock()
	if waiting == nil {
		// No one waiting — enqueue and let the next arrival pair with us.
		waiting = c
		mu.Unlock()
		log.Println("player queued, waiting for an opponent")
	} else {
		// Someone is waiting — pair up, open a room, and assign roles. The earlier player
		// becomes the offerer (creates the WebRTC offer); the new arrival answers.
		peer := waiting
		waiting = nil
		nextRoomID++
		rm := &room{id: nextRoomID, peers: [2]*client{peer, c}}
		peer.room, c.room = rm, rm
		peer.role, c.role = "offerer", "answerer"
		mu.Unlock()

		log.Printf("room %d created: two players matched", rm.id)
		if err := peer.send(websocket.TextMessage, matchedMsg(peer.role)); err != nil {
			log.Printf("room %d: failed to notify offerer: %v", rm.id, err)
		}
		if err := c.send(websocket.TextMessage, matchedMsg(c.role)); err != nil {
			log.Printf("room %d: failed to notify answerer: %v", rm.id, err)
		}
	}

	defer cleanup(c)

	for {
		messageType, message, err := conn.ReadMessage()
		if err != nil {
			log.Printf("player disconnected: %v", err)
			break
		}

		mu.Lock()
		rm := c.room
		mu.Unlock()
		if rm == nil {
			// Not matched yet — clients should not signal before "matched", so ignore.
			continue
		}

		// Relay to the only other peer in this room.
		if other := rm.other(c); other != nil {
			if err := other.send(messageType, message); err != nil {
				log.Printf("room %d: relay failed: %v", rm.id, err)
			}
		}
	}
}

// cleanup removes a disconnecting client from the queue or dissolves its room, notifying
// the surviving peer so it can stop waiting on a dead connection.
func cleanup(c *client) {
	mu.Lock()
	if waiting == c {
		waiting = nil
		mu.Unlock()
		log.Println("queued player left before matching")
		return
	}
	rm := c.room
	if rm == nil {
		mu.Unlock()
		return
	}
	other := rm.other(c)
	rm.peers[0].room = nil
	rm.peers[1].room = nil
	mu.Unlock()

	if other != nil {
		if err := other.send(websocket.TextMessage, peerLeftMsg()); err != nil {
			log.Printf("room %d: failed to notify surviving peer: %v", rm.id, err)
		}
	}
	log.Printf("room %d dissolved (a player disconnected)", rm.id)
}

func main() {
	http.HandleFunc("/ws", handleWebSocket)

	fmt.Println("signaling server running on :8080 (auto-queue matchmaking)")
	log.Fatal(http.ListenAndServe(":8080", nil))
}
