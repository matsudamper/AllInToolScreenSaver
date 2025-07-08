package net.matsudamper.allintoolscreensaver

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.matsudamper.allintoolscreensaver.feature.setting.SettingsRepository

class FakeSettingsManager : SettingsRepository {
    private val selectedCalendarIdsFlow = MutableStateFlow<List<Long>>(listOf())
    private val imageSwitchIntervalSecondsFlow = MutableStateFlow<Int>(30)
    private val alertEnabledFlow = MutableStateFlow<Boolean>(false)
    private val alertCalendarIdsFlow = MutableStateFlow<List<Long>>(listOf())
    private var imageDirectoryUri: Uri? = null

    override val settingsFlow: Flow<Settings> = MutableStateFlow(Settings.getDefaultInstance())

    fun setSelectedCalendarIds(calendarIds: List<Long>) {
        selectedCalendarIdsFlow.value = calendarIds
    }

    override suspend fun saveImageDirectoryUri(uri: Uri) {
        imageDirectoryUri = uri
    }

    override suspend fun saveSelectedCalendarIds(calendarIds: List<Long>) {
        selectedCalendarIdsFlow.value = calendarIds
    }

    override fun getSelectedCalendarIdsFlow(): Flow<List<Long>> {
        return selectedCalendarIdsFlow.asStateFlow()
    }

    override suspend fun saveImageSwitchIntervalSeconds(seconds: Int) {
        imageSwitchIntervalSecondsFlow.value = seconds
    }

    override fun getImageSwitchIntervalSecondsFlow(): Flow<Int> {
        return imageSwitchIntervalSecondsFlow.asStateFlow()
    }

    override suspend fun saveAlertEnabled(enabled: Boolean) {
        alertEnabledFlow.value = enabled
    }

    override fun getAlertEnabledFlow(): Flow<Boolean> {
        return alertEnabledFlow.asStateFlow()
    }

    override suspend fun saveAlertCalendarIds(calendarIds: List<Long>) {
        alertCalendarIdsFlow.value = calendarIds
    }

    override fun getAlertCalendarIdsFlow(): Flow<List<Long>> {
        return alertCalendarIdsFlow.asStateFlow()
    }
}
