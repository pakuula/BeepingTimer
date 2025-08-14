package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth

// Таймерная модель
data class TimerPreset(
    val title: String, // Название пресета
    // Параметры таймера
    val secondsPerRep: Int, // Время на одно повторение в секундах
    val reps: Int, // Количество повторений
    val restSeconds: Int, // Время отдыха между повторениями в секундах
    val sets: Int, // Количество подходов
    val prepTime: Int = 7 // Время подготовки по умолчанию
)

val timerPresets = listOf(
    TimerPreset("6 сек/8 повторов/50 сек/4 подхода", 6, 8, 50, 4),
    TimerPreset("8 сек/6 повторов/50 сек/4 подхода", 8, 6, 50, 4),
    TimerPreset("6 сек/8 повторов/8 сек/8 подходов", 6, 8, 8, 8),
    TimerPreset("8 сек/6 повторов/8 сек/8 подходов", 8, 6, 8, 8),
    TimerPreset("2 сек/1 повтор/2 сек/2 подхода", 2, 1, 2, 2)
)

@Composable
fun TimerList(onPresetClick: (TimerPreset) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(16.dp)
    ) {
        timerPresets.forEach { preset ->
            Log.i("TimerList", "Preset: ${preset.title}")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPresetClick(preset) },
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(text = preset.title, modifier = Modifier.padding(16.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimerList { preset ->
                        val intent = Intent(this, ExerciseActivity::class.java).apply {
                            putExtra("secondsPerRep", preset.secondsPerRep)
                            putExtra("reps", preset.reps)
                            putExtra("restSeconds", preset.restSeconds)
                            putExtra("sets", preset.sets)
                            putExtra("prepTime", preset.prepTime)
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }
}