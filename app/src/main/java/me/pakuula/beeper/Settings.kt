package me.pakuula.beeper

import android.content.Context
import androidx.core.content.edit

// Класс для хранения настроек приложения
data class Settings(
    val volume: Int = 100,
    val language: String = "ru",
    val voice: String = "default",
    val prepTime: Int = Defaults.PREPARATION_SECONDS,
    val beepsBeforeStart: Int = 5,
    val beepsBeforeSet: Int = 3,
    val reverseRepCount: Boolean = true, // Новый флаг
    val mute: Boolean = false, // Новый флаг для отключения голоса
    // Если true, то вперед при свайпе справа налево.
    // Если false, то вперед при свайпе слева направо.
    // По умолчанию true, т.е. вперед при свайпе справа налево.
    val swipeRightToLeft: Boolean = true
)

object SettingsStorage {
    private const val PREFS_NAME = "settings"
    private const val VOLUME = "volume"
    private const val LANGUAGE = "language"
    private const val VOICE = "voice"
    private const val PREP_TIME = "prepTime"
    private const val BEEPS_BEFORE_START = "beepsBeforeStart"
    private const val BEEPS_BEFORE_SET = "beepsBeforeSet"
    private const val REVERSE_REP_COUNT = "reverseRepCount" // Новый ключ
    private const val MUTE = "mute" // Новый ключ для mute
    private const val SWIPE_RIGHT_TO_LEFT = "swipeRightToLeft"

    fun save(context: Context, settings: Settings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putInt(VOLUME, settings.volume)
            putString(LANGUAGE, settings.language)
            putString(VOICE, settings.voice)
            putInt(PREP_TIME, settings.prepTime)
            putInt(BEEPS_BEFORE_START, settings.beepsBeforeStart)
            putInt(BEEPS_BEFORE_SET, settings.beepsBeforeSet)
            putBoolean(REVERSE_REP_COUNT, settings.reverseRepCount) // Сохраняем флаг
            putBoolean(MUTE, settings.mute) // Сохраняем mute
            putBoolean(
                SWIPE_RIGHT_TO_LEFT,
                settings.swipeRightToLeft
            ) // Сохраняем значение по умолчанию
        }
    }

    fun load(context: Context): Settings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Settings(
            prepTime = prefs.getInt(PREP_TIME, Defaults.PREPARATION_SECONDS),
            beepsBeforeStart = prefs.getInt(BEEPS_BEFORE_START, Defaults.BEEPS_BEFORE_START),
            beepsBeforeSet = prefs.getInt(BEEPS_BEFORE_SET, Defaults.BEEPS_BEFORE_SET),

            volume = prefs.getInt(VOLUME, 100),

            language = prefs.getString(LANGUAGE, "ru") ?: "ru",
            voice = prefs.getString(VOICE, "default") ?: "default",
            reverseRepCount = prefs.getBoolean(REVERSE_REP_COUNT, true),
            mute = prefs.getBoolean(MUTE, false) ,

            swipeRightToLeft = prefs.getBoolean(
                SWIPE_RIGHT_TO_LEFT,
                true
            )
        )
    }
}
