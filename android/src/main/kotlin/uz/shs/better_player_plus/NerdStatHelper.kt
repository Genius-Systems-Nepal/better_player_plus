package uz.shs.better_player_plus

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import java.util.Locale
import kotlin.math.max
import kotlin.math.pow

internal class NerdStatHelper(
    private val exoPlayer: ExoPlayer?,
    private val eventSink: QueuingEventSink,
    private val context: Context,
) : AnalyticsListener {
    private val statsHandler = Handler(Looper.getMainLooper())
    private var started = false
    private var bitrateEstimateValue: Long = 0L

    private val statsRunnable: Runnable = object : Runnable {
        override fun run() {
            emitNerdStat()
            if (started) {
                statsHandler.postDelayed(this, 1000)
            }
        }
    }

    fun init() {
        if (started) return
        started = true
        exoPlayer?.addAnalyticsListener(this)
        statsHandler.post(statsRunnable)
    }

    fun onStop() {
        if (!started) return
        started = false
        exoPlayer?.removeAnalyticsListener(this)
        statsHandler.removeCallbacks(statsRunnable)
    }

    override fun onBandwidthEstimate(
        eventTime: AnalyticsListener.EventTime,
        totalLoadTimeMs: Int,
        totalBytesLoaded: Long,
        bitrateEstimate: Long,
    ) {
        bitrateEstimateValue = bitrateEstimate
    }

    private fun emitNerdStat() {
        val player = exoPlayer ?: return
        val videoFormat = player.videoFormat
        val audioFormat = player.audioFormat

        val bufferedMs = max(0L, (player.bufferedPosition - player.currentPosition))
        val bufferSec = bufferedMs / 1000.0

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volumePercent = if (maxVolume == 0) 0 else (100 * currentVolume / maxVolume)

        val videoDesc = if (videoFormat != null) {
            val mime = (videoFormat.sampleMimeType ?: "---").replace("video/", "")
            "${videoFormat.width}x${videoFormat.height} / $mime"
        } else {
            "---"
        }

        val audioDesc = if (audioFormat != null) {
            val mime = (audioFormat.sampleMimeType ?: "---").replace("audio/", "")
            "$volumePercent% / $mime"
        } else {
            "$volumePercent% / ---"
        }

        val decoderCounters = player.videoDecoderCounters
        decoderCounters?.ensureUpdated()
        val droppedFrames = decoderCounters?.droppedBufferCount ?: 0
        val renderedFrames = decoderCounters?.renderedOutputBufferCount ?: 0

        val data = buildString {
            append("Buffer Health: ${formatDouble(bufferSec)} s")
            append("\nConn Speed: ${humanReadableBitrate(bitrateEstimateValue)}ps")
            append("\nVideo: $videoDesc")
            append("\nAudio: $audioDesc")
            append("\nCurrent: ---")
            append("\nFrames: $droppedFrames dropped of $renderedFrames")
        }

        eventSink.success(
            mutableMapOf<String, Any>(
                "event" to "nerdStat",
                "values" to data,
            ),
        )
    }

    private fun formatDouble(value: Double): String = String.format(Locale.US, "%.1f", value)

    private fun humanReadableBitrate(bitsPerSecond: Long): String {
        if (bitsPerSecond <= 0) return "0 b"
        val unit = 1000.0
        val exp = (kotlin.math.ln(bitsPerSecond.toDouble()) / kotlin.math.ln(unit)).toInt().coerceAtLeast(0)
        if (exp == 0) return "$bitsPerSecond b"
        val prefixes = "kMGTPE"
        val pre = prefixes[exp - 1]
        val value = bitsPerSecond / unit.pow(exp.toDouble())
        return String.format(Locale.US, "%.1f %sb", value, pre)
    }
}
