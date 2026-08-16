package com.rakshanet.meshchat.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A human-readable label for Nearby connection prompts, not an identity credential. */
class DeviceProfile(context: Context, defaultName: String) {
    private val preferences = context.getSharedPreferences("device_profile", Context.MODE_PRIVATE)
    private val _displayName = MutableStateFlow(
        preferences.getString(NAME_KEY, null)?.takeIf { it.isNotBlank() } ?: defaultName,
    )
    val displayName = _displayName.asStateFlow()

    fun updateDisplayName(value: String): Boolean {
        val normalized = value.trim().take(32)
        if (normalized.isBlank()) return false
        preferences.edit().putString(NAME_KEY, normalized).apply()
        _displayName.value = normalized
        return true
    }

    private companion object {
        const val NAME_KEY = "display_name"
    }
}
