package com.trueprint.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.jobHistoryDataStore by preferencesDataStore(name = "job_history")

data class PrintHistoryEntry(
    val fileName: String,
    val documentType: String,
    val timestamp: Long,
    val printer: String,
    val copies: Int,
    val paperSize: String,
    val pageSpecification: String,
    val pagesPerSheet: Int,
    val duplex: String,
    val orientation: String,
    val paperType: String,
    val jobId: Int?,
    val status: String,
    val failureReason: String? = null
)

interface JobHistoryRepository {
    val entries: Flow<List<PrintHistoryEntry>>
    suspend fun add(entry: PrintHistoryEntry)
    suspend fun clear()
}

/** Stores only small, local metadata; source document bytes and URIs are never retained. */
class DataStoreJobHistoryRepository(private val context: Context) : JobHistoryRepository {
    override val entries: Flow<List<PrintHistoryEntry>> = context.jobHistoryDataStore.data.map { prefs ->
        decode(prefs[Entries].orEmpty())
    }

    override suspend fun add(entry: PrintHistoryEntry) {
        context.jobHistoryDataStore.edit { prefs ->
            prefs[Entries] = encode((decode(prefs[Entries].orEmpty()) + entry).takeLast(MaxEntries).reversed())
        }
    }

    override suspend fun clear() { context.jobHistoryDataStore.edit { it.remove(Entries) } }

    private fun encode(entries: List<PrintHistoryEntry>) = JSONArray().apply {
        entries.forEach { entry -> put(JSONObject().apply {
            put("fileName", entry.fileName); put("documentType", entry.documentType); put("timestamp", entry.timestamp)
            put("printer", entry.printer); put("copies", entry.copies); put("paperSize", entry.paperSize)
            put("pageSpecification", entry.pageSpecification); put("pagesPerSheet", entry.pagesPerSheet); put("duplex", entry.duplex); put("orientation", entry.orientation); put("paperType", entry.paperType)
            put("jobId", entry.jobId); put("status", entry.status); put("failureReason", entry.failureReason)
        }) }
    }.toString()

    private fun decode(value: String): List<PrintHistoryEntry> = runCatching {
        JSONArray(value).let { array -> List(array.length()) { index -> array.getJSONObject(index).let { item ->
            PrintHistoryEntry(item.getString("fileName"), item.getString("documentType"), item.getLong("timestamp"),
                item.getString("printer"), item.getInt("copies"), item.getString("paperSize"), item.getString("pageSpecification"),
                item.getInt("pagesPerSheet"), item.getString("duplex"), item.optString("orientation", "Portrait"), item.optString("paperType", "Plain Paper"), item.optInt("jobId").takeIf { item.has("jobId") && !item.isNull("jobId") },
                item.getString("status"), item.optString("failureReason").takeIf { item.has("failureReason") && !item.isNull("failureReason") })
        } } }
    }.getOrDefault(emptyList())

    private companion object { val Entries = stringPreferencesKey("entries"); const val MaxEntries = 50 }
}
