package me.pakuula.beeper

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.pakuula.beeper.theme.BeeperTheme


val timerPresets = listOf(
    TimerPreset("6 сек/8 повторов/50 сек/4 подхода", 6, 8, 50, 4),
    TimerPreset("8 сек/6 повторов/50 сек/4 подхода", 8, 6, 50, 4),
    TimerPreset("6 сек/8 повторов/8 сек/8 подходов", 6, 8, 8, 8),
    TimerPreset("8 сек/6 повторов/8 сек/8 подходов", 8, 6, 8, 8),
    TimerPreset("2 сек/1 повтор/2 сек/2 подхода", 2, 1, 2, 2)
)

@Composable
fun TimerList(onPresetClick: (TimerPreset) -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(16.dp)
    ) {
        timerPresets.forEach { preset ->
            TimerPresetWidget(
                preset = preset,
                onStart = { onPresetClick(it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeeperTheme {
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