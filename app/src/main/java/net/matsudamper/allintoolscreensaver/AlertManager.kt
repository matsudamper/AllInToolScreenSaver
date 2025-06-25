package net.matsudamper.allintoolscreensaver

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.Job

class AlertManager(
    @Suppress("UNUSED_PARAMETER") context: Context,
) {
//    private val settingsManager = SettingsManager(context)
//    private val calendarManager = CalendarManager(context)
    private var alertJob: Job? = null
    private var toneGenerator: ToneGenerator? = null
//    private val alertScope = CoroutineScope(Dispatchers.Main)
//    private val alreadyTriggeredEvents = mutableSetOf<Long>()

    var onAlertTriggered: ((CalendarEvent) -> Unit)? = null

    init {
        initializeToneGenerator()
    }

    private fun initializeToneGenerator() {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (_: RuntimeException) {
            // ToneGeneratorの初期化に失敗した場合
        }
    }

    fun startAlertMonitoring() {
        stopAlertMonitoring()
        // TODO
    }

    fun stopAlertMonitoring() {
        alertJob?.cancel()
        alertJob = null
    }

    fun cleanup() {
        stopAlertMonitoring()
        toneGenerator?.release()
        toneGenerator = null
    }
}
