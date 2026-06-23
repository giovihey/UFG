// Command signaling-bench is a load harness for the UFG signaling server.
//
// It speaks the server's matchmaking protocol (connect -> receive "matched" ->
// relay messages between the two peers of a room) and measures the two things the
// server is actually responsible for:
//
//  1. time-to-match  — how long from WebSocket-connected to receiving the "matched"
//     message, under a burst of 2N simultaneous joins.
//  2. relay RTT      — round-trip latency of a message relayed through a room
//     (offerer -> server -> answerer -> server -> offerer).
//
// It sweeps an increasing number of concurrent rooms so the report can show how both
// degrade under load. Results go to stdout and to a markdown file.
//
// The server assigns exactly one "offerer" and one "answerer" per room, so we let the
// offerer drive pings and the answerer echo them — no need to control which client
// pairs with which.
package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

type matchedMsg struct {
	Type string `json:"type"`
	Role string `json:"role"`
}

// levelResult holds the aggregated metrics for one sweep level (N rooms).
type levelResult struct {
	rooms        int
	clients      int
	matched      int             // clients that successfully matched (expect 2*rooms)
	matchThru    float64         // rooms matched per second
	matchLat     []time.Duration // one per matched client
	relayRTT     []time.Duration // one per ping (offerers * pings)
	dialErrs     int
	protoErrs    int
}

// aggregator is the shared sink the per-client goroutines write into.
type aggregator struct {
	mu          sync.Mutex
	matchLat    []time.Duration
	relayRTT    []time.Duration
	lastMatched time.Time
	dialErrs    int
	protoErrs   int
}

func (a *aggregator) addMatch(lat time.Duration, at time.Time) {
	a.mu.Lock()
	a.matchLat = append(a.matchLat, lat)
	if at.After(a.lastMatched) {
		a.lastMatched = at
	}
	a.mu.Unlock()
}

func (a *aggregator) addRTT(rtt time.Duration) {
	a.mu.Lock()
	a.relayRTT = append(a.relayRTT, rtt)
	a.mu.Unlock()
}

func (a *aggregator) addDialErr() {
	a.mu.Lock()
	a.dialErrs++
	a.mu.Unlock()
}

func (a *aggregator) addProtoErr() {
	a.mu.Lock()
	a.protoErrs++
	a.mu.Unlock()
}

func main() {
	url := flag.String("url", "ws://localhost:8080/ws", "signaling server WebSocket URL")
	sweep := flag.String("sweep", "1,10,50,100,250,500", "comma-separated room counts to test")
	rooms := flag.Int("rooms", 0, "single room count (overrides -sweep when > 0)")
	pings := flag.Int("pings", 20, "relay ping/pong round-trips per room")
	out := flag.String("out", "../RESULTS.md", "markdown results file (empty to skip)")
	timeout := flag.Duration("timeout", 30*time.Second, "per-level safety timeout")
	flag.Parse()

	levels, err := parseLevels(*sweep, *rooms)
	if err != nil {
		fmt.Fprintln(os.Stderr, "invalid -sweep:", err)
		os.Exit(2)
	}

	// Preflight: fail fast with a clear message if the server isn't reachable.
	if err := preflight(*url); err != nil {
		fmt.Fprintf(os.Stderr, "could not connect to signaling server at %s: %v\n", *url, err)
		fmt.Fprintln(os.Stderr, "start it first: `make signaling` or `cd signaling_server && go run .`")
		os.Exit(1)
	}

	fmt.Printf("UFG signaling benchmark\n  target : %s\n  sweep  : %v rooms\n  pings  : %d per room\n\n",
		*url, levels, *pings)

	var results []levelResult
	for _, n := range levels {
		fmt.Printf("running %d rooms (%d clients)... ", n, 2*n)
		r := runLevel(*url, n, *pings, *timeout)
		results = append(results, r)
		fmt.Printf("matched %d/%d, relay p50 %s\n", r.matched, r.clients,
			fmtDur(percentile(r.relayRTT, 50)))
	}

	fmt.Println()
	fmt.Print(renderTable(results, false))

	if *out != "" {
		if err := os.WriteFile(*out, []byte(renderMarkdown(*url, *pings, results)), 0o644); err != nil {
			fmt.Fprintln(os.Stderr, "failed to write results file:", err)
		} else {
			fmt.Printf("\nresults written to %s\n", *out)
		}
	}
}

// preflight opens and immediately closes one connection to verify reachability.
func preflight(url string) error {
	c, _, err := websocket.DefaultDialer.Dial(url, nil)
	if err != nil {
		return err
	}
	return c.Close()
}

// runLevel spawns 2N clients against the server, drives the match + relay protocol,
// and returns the aggregated metrics for this level.
func runLevel(url string, rooms, pings int, timeout time.Duration) levelResult {
	agg := &aggregator{}
	deadline := time.Now().Add(timeout)
	start := time.Now()

	var wg sync.WaitGroup
	wg.Add(2 * rooms)
	for i := 0; i < 2*rooms; i++ {
		go func() {
			defer wg.Done()
			runClient(url, pings, deadline, agg)
		}()
	}

	// Wait for all clients to finish, but never longer than the safety timeout.
	done := make(chan struct{})
	go func() { wg.Wait(); close(done) }()
	select {
	case <-done:
	case <-time.After(time.Until(deadline) + time.Second):
	}

	agg.mu.Lock()
	defer agg.mu.Unlock()

	var thru float64
	if len(agg.matchLat) > 0 {
		elapsed := agg.lastMatched.Sub(start).Seconds()
		if elapsed > 0 {
			thru = float64(rooms) / elapsed
		}
	}
	return levelResult{
		rooms:     rooms,
		clients:   2 * rooms,
		matched:   len(agg.matchLat),
		matchThru: thru,
		matchLat:  agg.matchLat,
		relayRTT:  agg.relayRTT,
		dialErrs:  agg.dialErrs,
		protoErrs: agg.protoErrs,
	}
}

// runClient connects one player, records its time-to-match, then either drives ping
// round-trips (offerer) or echoes relayed messages (answerer) until the room dissolves.
func runClient(url string, pings int, deadline time.Time, agg *aggregator) {
	connStart := time.Now()
	conn, _, err := websocket.DefaultDialer.Dial(url, nil)
	if err != nil {
		agg.addDialErr()
		return
	}
	defer conn.Close()
	_ = conn.SetReadDeadline(deadline)

	// First message is "matched" (sent directly by the server, not relayed).
	_, raw, err := conn.ReadMessage()
	if err != nil {
		agg.addProtoErr()
		return
	}
	matchedAt := time.Now()

	var m matchedMsg
	if err := json.Unmarshal(raw, &m); err != nil || m.Type != "matched" {
		agg.addProtoErr()
		return
	}
	agg.addMatch(matchedAt.Sub(connStart), matchedAt)

	switch m.Role {
	case "offerer":
		// Drive sequential ping -> (relay) -> echo -> (relay) -> pong round-trips.
		for i := 0; i < pings; i++ {
			payload := []byte("ping:" + strconv.Itoa(i))
			sent := time.Now()
			if err := conn.WriteMessage(websocket.TextMessage, payload); err != nil {
				agg.addProtoErr()
				return
			}
			if _, _, err := conn.ReadMessage(); err != nil {
				agg.addProtoErr()
				return
			}
			agg.addRTT(time.Since(sent))
		}
	default:
		// answerer: echo every relayed message straight back until the peer leaves.
		for {
			mt, msg, err := conn.ReadMessage()
			if err != nil {
				return
			}
			if err := conn.WriteMessage(mt, msg); err != nil {
				return
			}
		}
	}
}

// --- helpers ---------------------------------------------------------------

func parseLevels(sweep string, single int) ([]int, error) {
	if single > 0 {
		return []int{single}, nil
	}
	var levels []int
	for _, p := range strings.Split(sweep, ",") {
		p = strings.TrimSpace(p)
		if p == "" {
			continue
		}
		n, err := strconv.Atoi(p)
		if err != nil {
			return nil, err
		}
		if n <= 0 {
			return nil, fmt.Errorf("room count must be > 0, got %d", n)
		}
		levels = append(levels, n)
	}
	if len(levels) == 0 {
		return nil, fmt.Errorf("no room counts given")
	}
	return levels, nil
}

// percentile returns the nearest-rank percentile of a duration sample.
func percentile(d []time.Duration, p float64) time.Duration {
	if len(d) == 0 {
		return 0
	}
	s := make([]time.Duration, len(d))
	copy(s, d)
	sort.Slice(s, func(i, j int) bool { return s[i] < s[j] })
	rank := int((p/100)*float64(len(s)) + 0.5)
	if rank < 1 {
		rank = 1
	}
	if rank > len(s) {
		rank = len(s)
	}
	return s[rank-1]
}

func fmtDur(d time.Duration) string {
	if d == 0 {
		return "-"
	}
	return d.Round(time.Microsecond).String()
}

// renderTable formats the results as an aligned text (or markdown) table.
func renderTable(rs []levelResult, md bool) string {
	header := []string{"Rooms", "Clients", "Matched", "Match/s",
		"Match p50", "Match p95", "Match p99",
		"Relay p50", "Relay p95", "Relay p99", "Errors"}

	rows := make([][]string, 0, len(rs))
	for _, r := range rs {
		rows = append(rows, []string{
			strconv.Itoa(r.rooms),
			strconv.Itoa(r.clients),
			fmt.Sprintf("%d/%d", r.matched, r.clients),
			fmt.Sprintf("%.0f", r.matchThru),
			fmtDur(percentile(r.matchLat, 50)),
			fmtDur(percentile(r.matchLat, 95)),
			fmtDur(percentile(r.matchLat, 99)),
			fmtDur(percentile(r.relayRTT, 50)),
			fmtDur(percentile(r.relayRTT, 95)),
			fmtDur(percentile(r.relayRTT, 99)),
			strconv.Itoa(r.dialErrs + r.protoErrs),
		})
	}

	if md {
		var b strings.Builder
		b.WriteString("| " + strings.Join(header, " | ") + " |\n")
		sep := make([]string, len(header))
		for i := range sep {
			sep[i] = "---"
		}
		b.WriteString("| " + strings.Join(sep, " | ") + " |\n")
		for _, row := range rows {
			b.WriteString("| " + strings.Join(row, " | ") + " |\n")
		}
		return b.String()
	}

	// Aligned plain-text table.
	width := make([]int, len(header))
	for i, h := range header {
		width[i] = len(h)
	}
	for _, row := range rows {
		for i, c := range row {
			if len(c) > width[i] {
				width[i] = len(c)
			}
		}
	}
	var b strings.Builder
	writeRow := func(cells []string) {
		for i, c := range cells {
			b.WriteString(fmt.Sprintf("%-*s", width[i]+2, c))
		}
		b.WriteString("\n")
	}
	writeRow(header)
	for _, row := range rows {
		writeRow(row)
	}
	return b.String()
}

func renderMarkdown(url string, pings int, rs []levelResult) string {
	var b strings.Builder
	b.WriteString("# Signaling server benchmark\n\n")
	b.WriteString(fmt.Sprintf("Generated: %s  \n", time.Now().Format(time.RFC3339)))
	b.WriteString(fmt.Sprintf("Target: `%s`  \n", url))
	b.WriteString(fmt.Sprintf("Relay round-trips per room: %d\n\n", pings))
	b.WriteString("Each level spawns `2 x Rooms` WebSocket clients that the server pairs into ")
	b.WriteString("rooms. **Match** columns are time from connect to the `matched` message; ")
	b.WriteString("**Relay** columns are message round-trip latency through a room ")
	b.WriteString("(offerer -> server -> answerer -> server -> offerer). ")
	b.WriteString("**Match/s** is matched rooms per second.\n\n")
	b.WriteString(renderTable(rs, true))
	b.WriteString("\n> Numbers are localhost measurements; they isolate server overhead and ")
	b.WriteString("exclude real WAN/NAT latency.\n")
	return b.String()
}
