package com.example.myapplication

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.ui.input.pointer.pointerInput

class ExerciseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val secondsPerRep = intent.getIntExtra("secondsPerRep", 6)
        val reps = intent.getIntExtra("reps", 8)
        val restSeconds = intent.getIntExtra("restSeconds", 50)
        val sets = intent.getIntExtra("sets", 4)
        setContent {
            MyApplicationTheme {
                ExerciseScreen(
                    secondsPerRep = secondsPerRep,
                    reps = reps,
                    restSeconds = restSeconds,
                    sets = sets
                )
            }
        }
    }
}

@Composable
fun ExerciseScreen(
    secondsPerRep: Int,
    reps: Int,
    restSeconds: Int,
    sets: Int
) {
    var currentSet by remember { mutableStateOf(1) }
    var currentRep by remember { mutableStateOf(1) }
    var isRest by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(secondsPerRep) }
    var finished by remember { mutableStateOf(false) }
    val phaseColor = if (isRest) Color(0xFF90CAF9) else Color(0xFFA5D6A7)
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    val interactionSource = remember { MutableInteractionSource() }

    // Таймерная логика
    LaunchedEffect(currentSet, currentRep, isRest, isPaused) {
        if (!isPaused && !finished) {
            while (timeLeft > 0) {
                kotlinx.coroutines.delay(1000)
                if (isPaused) break
                timeLeft--
                // Звуковой сигнал каждую секунду при выполнении повторения
                if (!isRest) toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                // Особый сигнал в середине повторения
                if (!isRest && timeLeft == secondsPerRep / 2) toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                // Особый сигнал в конце повторения
                if (!isRest && timeLeft == 1) toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
                // Во время отдыха: сигнал каждые 10 секунд, в последние 5 секунд — каждую секунду
                if (isRest) {
                    if (timeLeft <= 5 || timeLeft % 10 == 0) toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                }
            }
            if (!isPaused && !finished) {
                if (!isRest) {
                    // Конец повторения
                    if (currentRep < reps) {
                        currentRep++
                        timeLeft = secondsPerRep
                    } else {
                        // Конец подхода
                        toneGen.startTone(ToneGenerator.TONE_SUP_RINGTONE, 500)
                        if (currentSet < sets) {
                            currentSet++
                            currentRep = 1
                            isRest = true
                            timeLeft = restSeconds
                        } else {
                            finished = true
                        }
                    }
                } else {
                    // Конец отдыха
                    isRest = false
                    timeLeft = secondsPerRep
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(phaseColor)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (finished) {
            Text(text = "Тренировка завершена!", color = Color.Red)
        } else {
            Text(text = "Подход: $currentSet / $sets")
            Text(text = "Повторение: $currentRep / $reps")
            Text(text = if (isRest) "Отдых" else "Выполнение")
            Text(text = "Осталось секунд: $timeLeft")
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { isPaused = !isPaused },
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            finished = true
                        }
                    )
                },
                interactionSource = interactionSource
            ) {
                Text(text = if (isPaused) "Возобновить" else "Пауза")
            }
        }
    }
}
