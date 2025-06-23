package net.matsudamper.allintoolscreensaver

import android.content.Context
import android.net.Uri
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

class SettingsManager(private val context: Context) {
    private val dataStore = context.settingsDataStore

    val settingsFlow: Flow<Settings> = dataStore.data

    suspend fun saveImageDirectoryUri(uri: Uri) {
        dataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setImageDirectoryUri(uri.toString())
                .build()
        }
    }

    suspend fun getImageDirectoryUri(): Uri? {
        val settings = dataStore.data.first()
        return if (settings.imageDirectoryUri.isNotEmpty()) {
            Uri.parse(settings.imageDirectoryUri)
        } else {
            null
        }
    }

    suspend fun saveSelectedCalendarIds(calendarIds: List<Long>) {
        dataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .clearSelectedCalendarIds()
                .addAllSelectedCalendarIds(calendarIds)
                .build()
        }
    }

    suspend fun getSelectedCalendarIds(): List<Long> {
        val settings = dataStore.data.first()
        return settings.selectedCalendarIdsList
    }

    fun getSelectedCalendarIdsFlow(): Flow<List<Long>> = dataStore.data.map { settings ->
        settings.selectedCalendarIdsList
    }
} 
