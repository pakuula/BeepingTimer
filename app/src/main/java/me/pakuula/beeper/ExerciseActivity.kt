package me.pakuula.beeper

//noinspection SuspiciousImport
import android.R
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.pakuula.beeper.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class ExerciseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val secondsPerRep = intent.getIntExtra("secondsPerRep", 6)
        val reps = intent.getIntExtra("reps", 8)
        val restSeconds = intent.getIntExtra("restSeconds", 50)
        val sets = intent.getIntExtra("sets", 4)
        val prepTime = intent.getIntExtra("prepTime", 7)
        setContent {
            MyApplicationTheme {
                ExerciseScreen(
                    secondsPerRep = secondsPerRep,
                    reps = reps,
                    restSeconds = restSeconds,
                    sets = sets,
                    prepTime = prepTime
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
                repeat(2) {
                    toneGen.startTone(
                        ToneGenerator.TONE_PROP_BEEP,
                        100
                    ); delay(100)
                }
            } else if (timeLeft == secondsPerRep / 2) {
                // Двойной бип в середине повторения
                repeat(2) {
                    toneGen.startTone(
                        ToneGenerator.TONE_PROP_BEEP,
                        100
                    ); delay(100)
                }
            } else {
                // Обычный бип каждую секунду
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            }
            onTimeLeftChange(timeLeft)
            delay(1000)
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

suspend fun doPauseInExercise(
    isPaused: () -> Boolean,
    timeLeft: Int,
    onTimeLeftChange: (Int) -> Unit
) {
    while (isPaused()) {
        onTimeLeftChange(timeLeft)
        delay(100)
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
        delay(1000)
        if (isPaused()) {
            timeLeft = doPauseInRest(isPaused, timeLeft)
        }
        timeLeft--
        // Можно добавить сигналы для отдыха, если потребуется
    }
}

suspend fun doPauseInRest(isPaused: () -> Boolean, timeLeft: Int): Int {
    var currentTimeLeft = timeLeft
    while (isPaused()) {
        delay(100)
    }
    return currentTimeLeft
}

@Composable
fun ExerciseScreen(
    secondsPerRep: Int,
    reps: Int,
    restSeconds: Int,
    sets: Int,
    prepTime: Int
) {
    var isPreparation by remember { mutableStateOf(true) }
    var prepTimeLeft by remember { mutableStateOf(prepTime) }
    var currentSet by remember { mutableStateOf(1) }
    var currentRep by remember { mutableStateOf(1) }
    var isRest by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(secondsPerRep) }
    var restTimeLeft by remember { mutableStateOf(restSeconds) }
    var finished by remember { mutableStateOf(false) }
    val phaseColor = when {
        isPreparation -> Color(0xFFFFF176) // жёлтый
        isRest -> Color(0xFF90CAF9)
        else -> Color(0xFFA5D6A7)
    }
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(currentSet, isRest, isPaused, finished, isPreparation) {
        if (!finished) {
            if (isPreparation) {
                doRest(
                    prepTimeLeft,
                    toneGen,
                    isPaused = { isPaused },
                    onTimeLeftChange = { left -> prepTimeLeft = left }
                )
                isPreparation = false
            } else if (!isRest) {
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
                            restTimeLeft = restSeconds
                        } else {
                            finished = true
                        }
                    },
                    isPaused = { isPaused },
                    onTimeLeftChange = { timeLeft = it }
                )
            } else {
                doRest(
                    restTimeLeft,
                    toneGen,
                    isPaused = { isPaused },
                    onTimeLeftChange = { left -> restTimeLeft = left; timeLeft = left }
                )
                isRest = false
                timeLeft = secondsPerRep
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(phaseColor)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val bigText = TextStyle(fontSize = 36.sp)
            val mediumText = TextStyle(fontSize = 28.sp)
            val hugeText = TextStyle(fontSize = 96.sp)
            if (finished) {
                Text(
                    text = "Упражнение завершено", color = Color.Red,
                    style = bigText,
                    modifier = Modifier.wrapContentSize(Alignment.Center)
                )
            } else if (isPreparation) {
                Text(text = "Подготовка", style = bigText, color = Color(0xFFFBC02D))
                Text(text = prepTimeLeft.toString(), style = hugeText)
                Spacer(modifier = Modifier.height(32.dp))
            } else {
                Text(text = "Подход: $currentSet / $sets", style = bigText)
                Text(text = "Повторение: $currentRep / $reps", style = bigText)
                Text(text = if (isRest) "Отдых" else "Выполнение", style = mediumText)
                Text(text = timeLeft.toString(), style = hugeText)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        if (!finished) {
            Button(
                onClick = { isPaused = !isPaused },
                shape = CircleShape,
                modifier = Modifier
                    .height(120.dp)
                    .width(120.dp)
                    .padding(16.dp)
                    .align(Alignment.BottomEnd)
//                    .padding(end = 0.dp, bottom = 24.dp)
                    .navigationBarsPadding(),
                interactionSource = interactionSource
            ) {
                if (!isPaused) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_media_pause),
                        contentDescription = "Пауза",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_media_play),
                        contentDescription = "Продолжить",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}
