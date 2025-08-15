package me.pakuula.beeper

import android.content.Context
import android.content.SharedPreferences

// Класс для хранения настроек приложения
 data class Settings(
    val volume: Int = 100,
    val language: String = "ru",
    val voice: String = "default",
    val prepTime: Int = 7,
    val beepsBeforeStart: Int = 5,
    val beepsBeforeSet: Int = 3
)

object SettingsStorage {
    private const val PREFS_NAME = "settings"
    private const val VOLUME = "volume"
    private const val LANGUAGE = "language"
    private const val VOICE = "voice"
    private const val PREP_TIME = "prepTime"
    private const val BEEPS_BEFORE_START = "beepsBeforeStart"
    private const val BEEPS_BEFORE_SET = "beepsBeforeSet"

    fun save(context: Context, settings: Settings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(VOLUME, settings.volume)
            .putString(LANGUAGE, settings.language)
            .putString(VOICE, settings.voice)
            .putInt(PREP_TIME, settings.prepTime)
            .putInt(BEEPS_BEFORE_START, settings.beepsBeforeStart)
            .putInt(BEEPS_BEFORE_SET, settings.beepsBeforeSet)
            .apply()
    }

    fun load(context: Context): Settings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Settings(
            volume = prefs.getInt(VOLUME, 100),
            language = prefs.getString(LANGUAGE, "ru") ?: "ru",
            voice = prefs.getString(VOICE, "default") ?: "default",
            prepTime = prefs.getInt(PREP_TIME, 7),
            beepsBeforeStart = prefs.getInt(BEEPS_BEFORE_START, 5),
            beepsBeforeSet = prefs.getInt(BEEPS_BEFORE_SET, 3)
        )
    }
}

