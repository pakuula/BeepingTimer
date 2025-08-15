package me.pakuula.beeper

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TimerStorage {
    private const val PREFS_NAME = "timer_prefs"
    private const val KEY_TIMERS = "timers"
    private val gson = Gson()

    fun saveTimers(context: Context, timers: List<TimerPreset>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(timers)
        prefs.edit().putString(KEY_TIMERS, json).apply()
    }

    fun loadTimers(context: Context): List<TimerPreset> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_TIMERS, null)
        return if (json != null) {
            val type = object : TypeToken<List<TimerPreset>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun isFirstLaunch(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TIMERS, null) == null
    }
}

