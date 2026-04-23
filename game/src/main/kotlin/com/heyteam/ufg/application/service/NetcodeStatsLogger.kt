package com.heyteam.ufg.application.service

import io.github.oshai.kotlinlogging.KotlinLogging

private val netcodeLog = KotlinLogging.logger("netcode")

/**
 * Accumulates per-second netcode metrics and emits one `[NETCODE]` summary line per
 * wall-clock second. Gives the "ambient pulse": rollback rate, prediction hit-rate, and
 * authoritative-input lag. Toggle [com.heyteam.ufg.infrastructure.adapter.network.FakeLagInputPort]
 * to see these numbers change live.
 */
class NetcodeStatsLogger : RollbackListener {
    private var rollbacksInWindow = 0
    private var predictionHits = 0
    private var predictionEvals = 0
    private val authLags = ArrayDeque<Int>()
    private var lastFlushNanos = System.nanoTime()

    override fun onPredictionEvaluated(hit: Boolean) {
        predictionEvals++
        if (hit) predictionHits++
    }

    override fun onRollback(event: RollbackEvent) {
        rollbacksInWindow++
        authLags.addLast(event.authLag)
    }

    override fun onFrameAdvanced(currentFrame: Long) {
        val now = System.nanoTime()
        val elapsedNanos = now - lastFlushNanos
        if (elapsedNanos < NANOS_PER_SECOND.toLong()) return
        val elapsedSec = elapsedNanos / NANOS_PER_SECOND
        val rbPerSec = rollbacksInWindow / elapsedSec
        val predictedPct =
            if (predictionEvals == 0) PERCENT_SCALE else (PERCENT_SCALE * predictionHits / predictionEvals)
        val lagAvg = if (authLags.isEmpty()) 0.0 else authLags.average()
        val lagMax = authLags.maxOrNull() ?: 0
        netcodeLog.info {
            SUMMARY_LINE_FMT.format(currentFrame, rbPerSec, predictedPct, lagAvg, lagMax)
        }
        rollbacksInWindow = 0
        predictionHits = 0
        predictionEvals = 0
        authLags.clear()
        lastFlushNanos = now
    }

    companion object {
        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private const val PERCENT_SCALE = 100

        private const val SUMMARY_LINE_FMT =
            "[NETCODE] f=%d rb/s=%.1f predicted=%d%% auth_lag_avg=%.1ff auth_lag_max=%df"
    }
}
