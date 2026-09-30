package com.example.xuper.util

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator

/**
 * Factory optimizado para crear instancias de ExoPlayer
 * Gestiona configuración de memoria, buffering y track selection
 */
object ExoPlayerFactory {

    // Pool de ExoPlayer para reutilización
    private var cachedPlayer: ExoPlayer? = null
    private var lastContext: Context? = null

    /**
     * Obtener o crear ExoPlayer optimizado
     */
    fun getOrCreate(context: Context, reuseIfPossible: Boolean = true): ExoPlayer {
        // Si el contexto cambió, liberar el player anterior
        if (lastContext != null && lastContext != context.applicationContext) {
            releasePlayer()
        }

        // Reutilizar si está disponible y se permite
        if (reuseIfPossible && cachedPlayer != null) {
            return cachedPlayer!!
        }

        // Crear nuevo player optimizado
        val player = createOptimizedPlayer(context)
        cachedPlayer = player
        lastContext = context.applicationContext

        return player
    }

    /**
     * Crear ExoPlayer con configuración optimizada
     */
    private fun createOptimizedPlayer(context: Context): ExoPlayer {
        // Track Selector optimizado para streaming
        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                // Preferir calidad adaptativa
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowAudioMixedMimeTypeAdaptiveness(true)
                // Limitar resolución en móviles para ahorrar memoria
                .setMaxVideoSize(1920, 1080)
                // Priorizar audio en el idioma del sistema
                .setPreferredAudioLanguage(context.resources.configuration.locales[0].language)
                .build()
        }

        // Load Control optimizado para IPTV/Streaming
        val loadControl = createOptimizedLoadControl()

        // Data Source Factory con headers customizados
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Xuper/1.0 (Android)")
            .setConnectTimeoutMs(15_000) // 15 segundos
            .setReadTimeoutMs(30_000)    // 30 segundos
            .setAllowCrossProtocolRedirects(true)

        // Media Source Factory
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        // Construir ExoPlayer
        return ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            // Optimizaciones de renderizado
            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
            .setHandleAudioBecomingNoisy(true) // Pausar si desconectan audífonos
            .setWakeMode(C.WAKE_MODE_NETWORK) // Mantener CPU despierta
            .build().apply {
                // Configuraciones adicionales
                playWhenReady = false
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    /**
     * Load Control optimizado para streaming en vivo
     */
    private fun createOptimizedLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setAllocator(
                DefaultAllocator(
                    true, // Usar C.DEFAULT_BUFFER_SEGMENT_SIZE
                    C.DEFAULT_BUFFER_SEGMENT_SIZE
                )
            )
            // Buffer mínimo antes de empezar (3 segundos)
            .setBufferDurationsMs(
                3_000,   // minBufferMs - Mínimo para empezar
                15_000,  // maxBufferMs - Máximo en memoria
                1_500,   // bufferForPlaybackMs - Para empezar reproducción
                2_000    // bufferForPlaybackAfterRebufferMs - Después de rebuffering
            )
            // Priorizar playback sobre buffering
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    /**
     * Liberar player y recursos
     */
    fun releasePlayer() {
        cachedPlayer?.apply {
            stop()
            clearMediaItems()
            release()
        }
        cachedPlayer = null
        lastContext = null
    }

    /**
     * Reset player (mantener instancia pero limpiar estado)
     */
    fun resetPlayer() {
        cachedPlayer?.apply {
            stop()
            clearMediaItems()
            seekTo(0)
        }
    }
}

/**
 * Extensiones para ExoPlayer
 */
object ExoPlayerExtensions {

    /**
     * Verificar si el player está buffering
     */
    fun ExoPlayer.isBuffering(): Boolean {
        return playbackState == Player.STATE_BUFFERING
    }

    /**
     * Verificar si el player está listo
     */
    fun ExoPlayer.isReady(): Boolean {
        return playbackState == Player.STATE_READY
    }

    /**
     * Obtener estado como string legible
     */
    fun ExoPlayer.getStateString(): String {
        return when (playbackState) {
            Player.STATE_IDLE -> "Idle"
            Player.STATE_BUFFERING -> "Buffering"
            Player.STATE_READY -> "Ready"
            Player.STATE_ENDED -> "Ended"
            else -> "Unknown"
        }
    }

    /**
     * Obtener información de la pista actual
     */
    fun ExoPlayer.getCurrentTrackInfo(): String {
        val format = videoFormat ?: return "No video"
        return "${format.width}x${format.height} @ ${format.frameRate}fps"
    }

    /**
     * Obtener velocidad de download estimada
     */
    fun ExoPlayer.getEstimatedBandwidth(): Long {
        // Requiere acceso al BandwidthMeter del TrackSelector
        // Por ahora retornar 0
        return 0L
    }
}
