package com.example.nori_tura.data

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class AutocompleteSelectionCacheTest {

    @Test
    fun recordedSelectionIsSuggestedFirstOnNextQuery() = runBlocking {
        val cache = AutocompleteSelectionCache
        cache.setTestSettings(TestMapSettings())

        val fieldType = "smoke_${hashCode()}"

        cache.recordSelection("vesicostomy", fieldType)
        cache.recordSelection("vesicoureteral reflux", fieldType)
        // Selecting the same term again should boost its count.
        cache.recordSelection("vesicostomy", fieldType)

        val suggestions = cache.getSuggestions("ves", fieldType, 5)

        assertEquals(
            expected = listOf("vesicostomy", "vesicoureteral reflux"),
            actual = suggestions
        )
    }
}
