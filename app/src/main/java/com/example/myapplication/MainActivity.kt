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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.ui.unit.dp

// Таймерная модель
data class TimerPreset(
    val title: String,
    val secondsPerRep: Int,
    val reps: Int,
    val restSeconds: Int,
    val sets: Int
)

val timerPresets = listOf(
    TimerPreset("6 сек/8 повторов/50 сек/4 подхода", 6, 8, 50, 4),
    TimerPreset("8 сек/6 повторов/50 сек/4 подхода", 8, 6, 50, 4),
    TimerPreset("6 сек/8 повторов/8 сек/8 подходов", 6, 8, 8, 8),
    TimerPreset("8 сек/6 повторов/8 сек/8 подходов", 8, 6, 8, 8)
)

@Composable
fun TimerList(onPresetClick: (TimerPreset) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        timerPresets.forEach { preset ->
            Card(
                modifier = Modifier
                    .fillMaxSize()
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
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }
}