package net.matsudamper.allintoolscreensaver.feature.setting

import android.content.Context
import android.net.Uri
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.protobuf.InvalidProtocolBufferException
import net.matsudamper.allintoolscreensaver.Settings

interface SettingsRepository {
    val settingsFlow: Flow<Settings>
    suspend fun saveImageDirectoryUri(uri: Uri)
    suspend fun saveSelectedCalendarIds(calendarIds: List<Long>)
    fun getSelectedCalendarIdsFlow(): Flow<List<Long>>
    suspend fun saveImageSwitchIntervalSeconds(seconds: Int)
    fun getImageSwitchIntervalSecondsFlow(): Flow<Int>
    suspend fun saveAlertEnabled(enabled: Boolean)
    fun getAlertEnabledFlow(): Flow<Boolean>
    suspend fun saveAlertCalendarIds(calendarIds: List<Long>)
    fun getAlertCalendarIdsFlow(): Flow<List<Long>>
    suspend fun saveNotificationDisplayDuration(duration: Duration)
    fun getNotificationDisplayDurationFlow(): Flow<Duration>
}

object SettingsSerializer : Serializer<Settings> {
    override val defaultValue: Settings = Settings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Settings {
        try {
            return Settings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: Settings, output: OutputStream) = t.writeTo(output)
}

private val Context.settingsDataStore: DataStore<Settings> by dataStore(
    fileName = "settings.pb",
    serializer = SettingsSerializer,
)

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {
    private val dataStore = context.settingsDataStore

    override val settingsFlow: Flow<Settings> = dataStore.data

    override suspend fun saveImageDirectoryUri(uri: Uri) {
        dataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setImageDirectoryUri(uri.toString())
                .build()
        }
    }

    override suspend fun saveSelectedCalendarIds(calendarIds: List<Long>) {
        dataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .clearSelectedCalendarIds()
                .addAllSelectedCalendarIds(calendarIds)
                .build()
        }
    }

    override fun getSelectedCalendarIdsFlow(): Flow<List<Long>> = dataStore.data.map { settings ->
        settings.selectedCalendarIdsList
    }

    override suspend fun saveImageSwitchIntervalSeconds(seconds: Int) {
        dataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setImageSwitchIntervalSeconds(seconds)
                .build()
        }
    }

    override fun getImageSwitchIntervalSecondsFlow(): Flow<Int> = dataStore.data.map { settings ->
        if (settings.imageSwitchIntervalSeconds == 0) {
            30
        } else {
            settings.imageSwitchIntervalSeconds
        }
    }

    override suspend fun saveAlertEnabled(enabled: Boolean) {
        dataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setAlertEnabled(enabled)
                .build()
        }
    }

    override fun getAlertEnabledFlow(): Flow<Boolean> = dataStore.data.map { settings ->
        settings.alertEnabled
    }

    override suspend fun saveAlertCalendarIds(calendarIds: List<Long>) {
        dataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .clearAlertCalendarIds()
                .addAllAlertCalendarIds(calendarIds)
                .build()
        }
    }

    override fun getAlertCalendarIdsFlow(): Flow<List<Long>> = dataStore.data.map { settings ->
        settings.alertCalendarIdsList
    }

    override suspend fun saveNotificationDisplayDuration(duration: Duration) {
        dataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setNotificationDisplayDurationMillis(duration.inWholeMilliseconds)
                .build()
        }
    }

    override fun getNotificationDisplayDurationFlow(): Flow<Duration> = dataStore.data.map { settings ->
        when (settings.notificationDisplayDurationMillis) {
            0L -> 5.seconds
            Long.MAX_VALUE -> Duration.INFINITE
            else -> settings.notificationDisplayDurationMillis.milliseconds
        }
    }
}
