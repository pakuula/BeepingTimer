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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

suspend fun doExercise(
    secondsPerRep: Int,
    reps: Int,
    toneGen: ToneGenerator,
    onRepEnd: () -> Unit,
    onSetEnd: () -> Unit,
    isPaused: () -> Boolean,
    onTimeLeftChange: (Int) -> Unit
) {
    // Начало подхода: длинный громкий сигнал
    toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
    for (rep in 1..reps) {
        var timeLeft = secondsPerRep
        while (timeLeft > 0) {
            // Двойной бип в начале повторения
            if (timeLeft == secondsPerRep) {
                repeat(2) { toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100); kotlinx.coroutines.delay(100) }
            } else if (timeLeft == secondsPerRep / 2) {
                // Двойной бип в середине повторения
                repeat(2) { toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100); kotlinx.coroutines.delay(100) }
            } else {
                // Обычный бип каждую секунду
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            }
            onTimeLeftChange(timeLeft)
            kotlinx.coroutines.delay(1000)
            if (isPaused()) {
                doPauseInExercise(isPaused, timeLeft, onTimeLeftChange)
            }
            timeLeft--
        }
        onRepEnd()
    }
    // Конец подхода: длинный бип
    toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
    onSetEnd()
}

suspend fun doPauseInExercise(isPaused: () -> Boolean, timeLeft: Int, onTimeLeftChange: (Int) -> Unit) {
    while (isPaused()) {
        onTimeLeftChange(timeLeft)
        kotlinx.coroutines.delay(100)
    }
}

suspend fun doRest(
    restSeconds: Int,
    toneGen: ToneGenerator,
    isPaused: () -> Boolean,
    onTimeLeftChange: (Int) -> Unit
) {
    var timeLeft = restSeconds
    while (timeLeft > 0) {
        if (timeLeft <= 5) {
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
        }
        onTimeLeftChange(timeLeft)
        kotlinx.coroutines.delay(1000)
        if (isPaused()) {
            doPauseInRest(isPaused)
        }
        timeLeft--
        // Можно добавить сигналы для отдыха, если потребуется
    }
}

suspend fun doPauseInRest(isPaused: () -> Boolean) {
    while (isPaused()) {
        kotlinx.coroutines.delay(100)
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

    LaunchedEffect(currentSet, isRest, isPaused, finished) {
        if (!finished) {
            if (!isRest) {
                doExercise(
                    secondsPerRep,
                    reps,
                    toneGen,
                    onRepEnd = { currentRep = (currentRep % reps) + 1 },
                    onSetEnd = {
                        if (currentSet < sets) {
                            currentSet++
                            isRest = true
                            currentRep = 1
                        } else {
                            finished = true
                        }
                    },
                    isPaused = { isPaused },
                    onTimeLeftChange = { timeLeft = it }
                )
            } else {
                doRest(
                    restSeconds,
                    toneGen,
                    isPaused = { isPaused },
                    onTimeLeftChange = { timeLeft = it }
                )
                isRest = false
                timeLeft = secondsPerRep
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
        val bigText = TextStyle(fontSize = 36.sp)
        val mediumText = TextStyle(fontSize = 28.sp)
        if (finished) {
            Text(text = "Тренировка завершена!", color = Color.Red, style = bigText)
        } else {
            Text(text = "Подход: $currentSet / $sets", style = bigText)
            Text(text = "Повторение: $currentRep / $reps", style = bigText)
            Text(text = if (isRest) "Отдых" else "Выполнение", style = mediumText)
            Text(text = "Осталось секунд: $timeLeft", style = bigText)
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
                Text(text = if (isPaused) "Возобновить" else "Пауза", style = mediumText)
            }
        }
    }
}
