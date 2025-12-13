package com.bingoroyale.utils

import android.content.Context

class PreferencesManager(context: Context) {

    private val prefs = context.getSharedPreferences("bingo_prefs", Context.MODE_PRIVATE)

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) = prefs.edit().putBoolean("sound", value).apply()

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration", true)
        set(value) = prefs.edit().putBoolean("vibration", value).apply()
}