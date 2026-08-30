package com.example.nori_tura.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Local, per-field autocomplete selection cache.
 *
 * Remembers terms the user selects per [fieldType] and boosts them to the top
 * of future suggestions.  The cache is persisted in `Settings` as a single JSON
 * blob under [SETTINGS_KEY].
 *
 * - Cache is prioritised over server suggestions.
 * - Each field keeps at most [MAX_TERMS_PER_FIELD] entries, ranked by count.
 * - Entries unused for [STALE_DAYS] are evicted on every save.
 * - All operations are synchronous reads plus a suspending write/record path
 *   guarded by a mutex so concurrent selections do not corrupt the backing map.
 */
object AutocompleteSelectionCache {

    private const val SETTINGS_KEY = "autocomplete_selection_cache"
    private const val MAX_TERMS_PER_FIELD = 50
    private const val STALE_DAYS = 90L
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    private val settingsDelegate = lazy { Settings() }
    private val settings: Settings get() = settingsOverride ?: settingsDelegate.value
    private var settingsOverride: Settings? = null
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    private val lock = Mutex()
    private val selections = mutableMapOf<String, MutableList<AutocompleteSelection>>()
    private var loaded = false

    /**
     * Record that the user selected [term] for [fieldType].
     *
     * Blank [term] or [fieldType] values are ignored so ad-hoc calls from fields
     * without a field type are harmless.
     */
    suspend fun recordSelection(term: String, fieldType: String?) {
        val normalisedTerm = term.trim()
        val normalisedField = normaliseFieldType(fieldType)
        if (normalisedTerm.isBlank() || normalisedField.isBlank()) return

        lock.withLock {
            loadOnce()
            val list = selections.getOrPut(normalisedField) { mutableListOf() }
            val index = list.indexOfFirst { it.term.equals(normalisedTerm, ignoreCase = true) }
            val now = Clock.System.now().toEpochMilliseconds()

            if (index >= 0) {
                val existing = list[index]
                list[index] = existing.copy(
                    count = existing.count + 1,
                    selectedAtMillis = now
                )
            } else {
                list.add(
                    AutocompleteSelection(
                        term = normalisedTerm,
                        fieldType = normalisedField,
                        selectedAtMillis = now,
                        count = 1
                    )
                )
            }

            persistLocked()
        }
    }

    /**
     * Return cached suggestions for the current [query] and [fieldType].
     *
     * Results are filtered by prefix (case-insensitive), ranked by count then
     * recency, and capped at [limit].
     */
    suspend fun getSuggestions(query: String, fieldType: String?, limit: Int): List<String> {
        val normalisedField = normaliseFieldType(fieldType)
        val normalisedQuery = query.trim()
        if (normalisedQuery.isBlank() || normalisedField.isBlank() || limit <= 0) {
            return emptyList()
        }

        return lock.withLock {
            loadOnce()
            val list = selections[normalisedField] ?: return@withLock emptyList()
            list.asSequence()
                .filter { it.term.startsWith(normalisedQuery, ignoreCase = true) }
                .sortedWith(
                    compareByDescending<AutocompleteSelection> { it.count }
                        .thenByDescending { it.selectedAtMillis }
                )
                .map { it.term }
                .take(limit)
                .toList()
        }
    }

    private fun normaliseFieldType(fieldType: String?): String {
        return fieldType?.trim()?.lowercase() ?: ""
    }

    private fun loadOnce() {
        if (loaded) return
        val raw: String? = settings[SETTINGS_KEY]
        if (!raw.isNullOrBlank()) {
            try {
                val parsed: Map<String, List<AutocompleteSelection>> = json.decodeFromString(raw)
                selections.clear()
                selections.putAll(parsed.mapValues { it.value.toMutableList() })
            } catch (_: Exception) {
                // Corrupted cache: start empty.
                selections.clear()
            }
        }
        loaded = true
    }

    private fun persistLocked() {
        pruneLocked()
        val snapshot = selections.mapValues { (_, list) ->
            list.sortedWith(
                compareByDescending<AutocompleteSelection> { it.count }
                    .thenByDescending { it.selectedAtMillis }
            ).take(MAX_TERMS_PER_FIELD)
        }
        settings[SETTINGS_KEY] = json.encodeToString(snapshot)
    }

    private fun pruneLocked() {
        val cutoff = Clock.System.now().toEpochMilliseconds() - (STALE_DAYS * MILLIS_PER_DAY)
        selections.values.forEach { list ->
            list.removeAll { it.selectedAtMillis < cutoff }
        }
        selections.entries.removeAll { it.value.isEmpty() }
    }

    /**
     * Replace the backing [Settings] instance.  Intended for unit tests only.
     */
    internal fun setTestSettings(testSettings: Settings) {
        settingsOverride = testSettings
        loaded = false
        selections.clear()
    }
}
