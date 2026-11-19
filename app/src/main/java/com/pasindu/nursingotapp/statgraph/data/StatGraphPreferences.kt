package com.pasindu.nursingotapp.statgraph.data

import android.content.Context
import android.content.SharedPreferences

/**
 * StatGraph-local preference store for first-run introduction state.
 * Kept isolated from shared app database and global preferences.
 */
class StatGraphPreferences(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun hasSeenIntro(): Boolean = prefs.getBoolean(KEY_HAS_SEEN_INTRO, false)

    fun setHasSeenIntro(seen: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_SEEN_INTRO, seen).apply()
    }

    fun getSelectedPaletteId(): String? = prefs.getString(KEY_SELECTED_PALETTE_ID, null)

    fun setSelectedPaletteId(paletteId: String?) {
        prefs.edit().apply {
            if (paletteId == null) remove(KEY_SELECTED_PALETTE_ID)
            else putString(KEY_SELECTED_PALETTE_ID, paletteId)
        }.apply()
    }

    companion object {
        private const val PREFS_NAME = "statgraph_studio_prefs"
        private const val KEY_HAS_SEEN_INTRO = "has_seen_intro"
        private const val KEY_SELECTED_PALETTE_ID = "selected_palette_id"
    }
}
