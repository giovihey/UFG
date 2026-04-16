# User Guide

## Controls

| Key | Action |
|-----|--------|
| **W** | Up |
| **A** | Move left |
| **S** | Down |
| **D** | Move right |
| **Space** | Jump |
| **P** | Punch (JAB) |
| **K** | Kick (not yet implemented) |

## Gameplay

### Starting a Match

1. Launch the game (see [Deployment](deployment.md))
2. Once two players are connected via WebRTC, the match starts automatically
3. Both players spawn on opposite sides of the stage

### Combat

- **Move** with A/D to approach or retreat
- **Jump** with Space — you can't double-jump
- **Punch** with P to throw a JAB. The JAB has:
    - 4 frames of startup (you're committed, can't cancel)
    - 3 frames where the hitbox is active (can damage)
    - 8 frames of recovery (you're vulnerable)
- **Hits** deal damage and cause **hitstun** — the opponent can't act for a brief moment and gets pushed away (knockback)

### Winning

A round ends when:

- A player's HP reaches **0**, or
- The **timer** reaches 0 — the player with more HP wins

### HUD

- **Health bars** at the top — P1 (red, left), P2 (blue, right)
- **Timer** centered at the top — counts down from 99 seconds

### Visual Cues

- **Blue rectangle** = Player 1
- **Red rectangle** = Player 2
- **Yellow outline** = active attack hitbox (only visible during the active frames)
