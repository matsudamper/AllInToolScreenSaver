package net.matsudamper.allintoolscreensaver

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.google.protobuf.InvalidProtocolBufferException

interface SettingsRepository {
    val settingsFlow: Flow<Settings>
    suspend fun saveImageDirectoryUri(uri: Uri)
    suspend fun getImageDirectoryUri(): Uri?
    suspend fun saveSelectedCalendarIds(calendarIds: List<Long>)
    suspend fun getSelectedCalendarIds(): List<Long>
    fun getSelectedCalendarIdsFlow(): Flow<List<Long>>
    suspend fun saveImageSwitchIntervalSeconds(seconds: Int)
    suspend fun getImageSwitchIntervalSeconds(): Int
    fun getImageSwitchIntervalSecondsFlow(): Flow<Int>
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

    override suspend fun getImageDirectoryUri(): Uri? {
        val settings = dataStore.data.first()
        return if (settings.imageDirectoryUri.isNotEmpty()) {
            settings.imageDirectoryUri.toUri()
        } else {
            null
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

    override suspend fun getSelectedCalendarIds(): List<Long> {
        val settings = dataStore.data.first()
        return settings.selectedCalendarIdsList
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

    override suspend fun getImageSwitchIntervalSeconds(): Int {
        val settings = dataStore.data.first()
        return if (settings.imageSwitchIntervalSeconds == 0) {
            30
        } else {
            settings.imageSwitchIntervalSeconds
        }
    }

    override fun getImageSwitchIntervalSecondsFlow(): Flow<Int> = dataStore.data.map { settings ->
        if (settings.imageSwitchIntervalSeconds == 0) {
            30
        } else {
            settings.imageSwitchIntervalSeconds
        }
    }
}
