package com.trueprint.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.trueprint.app.model.PrinterProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.printerProfileDataStore by preferencesDataStore(name = "printer_profile")

interface PrinterProfileRepository {
    val profile: Flow<PrinterProfile>

    suspend fun save(profile: PrinterProfile)
    suspend fun resetToDefaults()
}

/** Local-only configuration storage. It intentionally has no network behavior. */
class DataStorePrinterProfileRepository(private val context: Context) : PrinterProfileRepository {
    override val profile: Flow<PrinterProfile> = context.printerProfileDataStore.data.map { preferences ->
        PrinterProfile(
            displayName = preferences[PrinterName] ?: PrinterProfile.Default.displayName,
            protocol = preferences[Protocol] ?: PrinterProfile.Default.protocol,
            server = preferences[Server] ?: PrinterProfile.Default.server,
            port = preferences[Port] ?: PrinterProfile.Default.port,
            queue = preferences[Queue] ?: PrinterProfile.Default.queue
        )
    }

    override suspend fun save(profile: PrinterProfile) {
        context.printerProfileDataStore.edit { preferences ->
            preferences[PrinterName] = profile.displayName
            preferences[Protocol] = profile.protocol
            preferences[Server] = profile.server
            preferences[Port] = profile.port
            preferences[Queue] = profile.queue
        }
    }

    override suspend fun resetToDefaults() {
        save(PrinterProfile.Default)
    }

    private companion object {
        val PrinterName = stringPreferencesKey("printer_name")
        val Protocol = stringPreferencesKey("protocol")
        val Server = stringPreferencesKey("server")
        val Port = intPreferencesKey("port")
        val Queue = stringPreferencesKey("queue")
    }
}
