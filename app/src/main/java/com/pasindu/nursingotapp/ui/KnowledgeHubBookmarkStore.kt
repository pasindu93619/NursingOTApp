package com.pasindu.nursingotapp.ui

import android.content.Context

class KnowledgeHubBookmarkStore(context: Context) {

    private val preferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getBookmarkedIds(): Set<String> =
        preferences.getStringSet(KEY_BOOKMARKED_IDS, emptySet()).orEmpty()

    fun setBookmarked(id: String, bookmarked: Boolean) {
        val ids = getBookmarkedIds().toMutableSet()
        if (bookmarked) {
            ids.add(id)
        } else {
            ids.remove(id)
        }

        preferences.edit()
            .putStringSet(KEY_BOOKMARKED_IDS, ids)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "knowledge_hub_bookmarks"
        const val KEY_BOOKMARKED_IDS = "bookmarked_circular_ids"
    }
}
