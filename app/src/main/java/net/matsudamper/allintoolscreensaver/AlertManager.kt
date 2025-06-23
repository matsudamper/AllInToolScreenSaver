package net.matsudamper.allintoolscreensaver

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class AlertManager(private val context: Context) {
    private val settingsManager = SettingsManager(context)
    private val calendarManager = CalendarManager(context)
    private var alertJob: Job? = null
    private var toneGenerator: ToneGenerator? = null
    private val alertScope = CoroutineScope(Dispatchers.Main)
    private val alreadyTriggeredEvents = mutableSetOf<Long>()

    var onAlertTriggered: ((CalendarEvent) -> Unit)? = null

    init {
        initializeToneGenerator()
    }

    private fun initializeToneGenerator() {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: RuntimeException) {
            // ToneGeneratorの初期化に失敗した場合
        }
    }

    fun startAlertMonitoring() {
        stopAlertMonitoring()
        
        alertJob = alertScope.launch {
            while (true) {
                val currentTime = System.currentTimeMillis()
                checkCalendarAlerts(currentTime)
                delay(60000) // 1分ごとにチェック
            }
        }
    }

    fun stopAlertMonitoring() {
        alertJob?.cancel()
        alertJob = null
    }

    private suspend fun checkCalendarAlerts(currentTime: Long) {
        val selectedCalendarIds = settingsManager.getSelectedCalendarIds()
        if (selectedCalendarIds.isEmpty()) return

        // 現在時刻から5分後までのイベントを取得
        val endTime = currentTime + (5 * 60 * 1000)
        val events = calendarManager.getEventsForTimeRange(selectedCalendarIds, currentTime, endTime)
        
        for (event in events) {
            // イベント開始時刻の前後1分以内かつ、まだトリガーされていないイベントをチェック
            val timeDiff = kotlin.math.abs(event.startTime - currentTime)
            if (timeDiff <= 60000 && !alreadyTriggeredEvents.contains(event.id)) {
                triggerAlert(event)
                alreadyTriggeredEvents.add(event.id)
            }
        }
        
        // 古いトリガー済みイベントIDをクリーンアップ（1時間以上前のものを削除）
        cleanupOldTriggeredEvents(currentTime)
    }

    private fun cleanupOldTriggeredEvents(currentTime: Long) {
        // 1時間前より古いイベントIDを削除（簡易的に全クリア）
        if (alreadyTriggeredEvents.size > 100) {
            alreadyTriggeredEvents.clear()
        }
    }

    private fun triggerAlert(event: CalendarEvent) {
        playAlertSound()
        onAlertTriggered?.invoke(event)
    }

    fun playAlertSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000)
        } catch (e: Exception) {
            // 音の再生に失敗した場合
        }
    }

    fun cleanup() {
        stopAlertMonitoring()
        toneGenerator?.release()
        toneGenerator = null
    }
} 