package net.matsudamper.allintoolscreensaver

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSettingsManager : SettingsRepository {
    private val _selectedCalendarIds = MutableStateFlow<List<Long>>(listOf())
    private var _imageDirectoryUri: Uri? = null
    private var _imageSwitchIntervalSeconds: Int = 30

    override val settingsFlow: Flow<Settings> = MutableStateFlow(Settings.getDefaultInstance())

    fun setSelectedCalendarIds(calendarIds: List<Long>) {
        _selectedCalendarIds.value = calendarIds
    }

    override suspend fun saveImageDirectoryUri(uri: Uri) {
        _imageDirectoryUri = uri
    }

    override suspend fun getImageDirectoryUri(): Uri? {
        return _imageDirectoryUri
    }

    override suspend fun saveSelectedCalendarIds(calendarIds: List<Long>) {
        _selectedCalendarIds.value = calendarIds
    }

    override suspend fun getSelectedCalendarIds(): List<Long> {
        return _selectedCalendarIds.value
    }

    override fun getSelectedCalendarIdsFlow(): Flow<List<Long>> {
        return _selectedCalendarIds.asStateFlow()
    }

    override suspend fun saveImageSwitchIntervalSeconds(seconds: Int) {
        _imageSwitchIntervalSeconds = seconds
    }

    override suspend fun getImageSwitchIntervalSeconds(): Int {
        return _imageSwitchIntervalSeconds
    }
}
