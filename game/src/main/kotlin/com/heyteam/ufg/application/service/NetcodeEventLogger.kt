package com.heyteam.ufg.application.service

import io.github.oshai.kotlinlogging.KotlinLogging

private val netcodeLog = KotlinLogging.logger("netcode")

/**
 * Emits one `[RB]` line per rollback, plus an `[RB-CLAMP]` warning when the rewind was
 * capped by [RollbackConfig.maxRollbackFrames]. Silent on a clean run.
 */
class NetcodeEventLogger : RollbackListener {
    private val startNanos = System.nanoTime()

    override fun onRollback(event: RollbackEvent) {
        netcodeLog.info {
            ROLLBACK_LINE_FMT.format(
                elapsedSeconds(),
                event.currentFrame,
                event.rewindFrames,
                event.mispredictionFrame,
                event.predicted.mask,
                event.actual.mask,
                event.authLag,
            )
        }
        if (event.clamped) {
            netcodeLog.warn {
                "[RB-CLAMP] wanted rewind to frame=${event.mispredictionFrame} " +
                    "but capped at frame=${event.effectiveFromFrame} — visual pop possible"
            }
        }
    }

    private fun elapsedSeconds(): Double = (System.nanoTime() - startNanos) / NANOS_PER_SECOND

    companion object {
        private const val NANOS_PER_SECOND = 1_000_000_000.0

        private const val ROLLBACK_LINE_FMT =
            "[RB] t=%.2fs frame=%d rewind=%df misprediction_frame=%d " +
                "remote_predicted=0x%02X remote_actual=0x%02X auth_lag=%df"
    }
}
