package com.heyteam.ufg.application.service

import com.heyteam.ufg.domain.entity.World

/**
 * Canonical 64-bit hash of a [World], over a stable subset of fields:
 *
 *  - `frameNumber`, `gameStatus.ordinal`, `roundTimer`
 *  - per player (iterated in `players.keys.sorted()` order):
 *      `id`, `position.{x,y}`, `health.current`,
 *      `physicsState.{state.ordinal, hitstunFramesRemaining, isCrouching, facing.ordinal}`,
 *      `attackState != null`, `attackState?.attack.name`,
 *      `attackState?.currentFrame`, `attackState?.hasLanded`
 *
 * Critically, this is **not** `World.hashCode()`. Kotlin's auto-generated `hashCode` includes
 * every field — adding a purely-cosmetic field (e.g. an animation timer that rounds the same
 * way on both peers but isn't sim-load-bearing) would change the hash and produce false
 * desync alarms. By picking the fields explicitly we control exactly what counts as "the
 * sim's true state" for the purpose of cross-peer comparison.
 *
 * The algorithm is **FNV-1a 64-bit**: simple, fast, no dependencies, deterministic across
 * JVMs. We feed in `Long`s (one per primitive) by mixing each byte. `Double` values are
 * fed via `toRawBits()` which is bit-identical across IEEE 754 JVMs — same constraint we
 * already rely on for sim determinism, so no new requirement.
 *
 * Two peers that have stayed in sync will produce identical hashes for the same frame; any
 * difference indicates a desync bug (or, more rarely, that we forgot a field here that
 * actually does affect the sim — in which case the fix is to add it to the hash, not to
 * silence the alarm).
 */
object WorldHash {
    private const val FNV_OFFSET_BASIS: Long = -3750763034362895579L // 0xcbf29ce484222325
    private const val FNV_PRIME: Long = 1099511628211L // 0x100000001b3
    private const val BYTE_MASK_INT: Int = 0xFF
    private const val BYTE_MASK_LONG: Long = 0xFFL

    fun hash(world: World): Long {
        var h = FNV_OFFSET_BASIS
        h = mixLong(h, world.frameNumber)
        h = mixInt(h, world.gameStatus.ordinal)
        h = mixInt(h, world.roundTimer)

        for (id in world.players.keys.sorted()) {
            val p = world.players[id] ?: continue
            h = mixInt(h, p.id)
            h = mixDouble(h, p.position.x)
            h = mixDouble(h, p.position.y)
            h = mixInt(h, p.health.current)

            val ps = p.physicsState
            h = mixInt(h, ps.state.ordinal)
            h = mixInt(h, ps.hitstunFramesRemaining)
            h = mixInt(h, if (ps.isCrouching) 1 else 0)
            h = mixInt(h, ps.facing.ordinal)

            val a = p.attackState
            if (a == null) {
                h = mixInt(h, 0)
            } else {
                h = mixInt(h, 1)
                h = mixString(h, a.attack.name)
                h = mixInt(h, a.currentFrame)
                h = mixInt(h, if (a.hasLanded) 1 else 0)
            }
        }
        return h
    }

    private fun mixByte(
        h: Long,
        b: Int,
    ): Long = (h xor (b.toLong() and BYTE_MASK_LONG)) * FNV_PRIME

    private fun mixInt(
        h: Long,
        v: Int,
    ): Long {
        var acc = h
        var i = 0
        while (i < Int.SIZE_BYTES) {
            acc = mixByte(acc, (v ushr (i * Byte.SIZE_BITS)) and BYTE_MASK_INT)
            i++
        }
        return acc
    }

    private fun mixLong(
        h: Long,
        v: Long,
    ): Long {
        var acc = h
        var i = 0
        while (i < Long.SIZE_BYTES) {
            acc = mixByte(acc, ((v ushr (i * Byte.SIZE_BITS)) and BYTE_MASK_LONG).toInt())
            i++
        }
        return acc
    }

    private fun mixDouble(
        h: Long,
        v: Double,
    ): Long = mixLong(h, v.toRawBits())

    private fun mixString(
        h: Long,
        s: String,
    ): Long {
        var acc = mixInt(h, s.length)
        for (i in s.indices) acc = mixInt(acc, s[i].code)
        return acc
    }
}
