package me.pakuula.beeper

//noinspection SuspiciousImport
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.delay
import me.pakuula.beeper.theme.BeeperTheme

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
            BeeperTheme {
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
    toneGen: ToneGenerator,
    onRepEnd: () -> Boolean, // возвращает true если продолжаем, false если ручной переход
    onSetEnd: () -> Boolean, // аналогично для подхода
    isPaused: () -> Boolean,
    onTimeLeftChange: (Int) -> Unit
) {
    // Начало подхода: длинный громкий сигнал
    toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
    while (true) {
        var timeLeft = secondsPerRep
        while (timeLeft > 0) {
            // Двойной бип в начале повторения
            when (timeLeft) {
                secondsPerRep -> {
                    repeat(2) {
                        toneGen.startTone(
                            ToneGenerator.TONE_PROP_BEEP,
                            100
                        ); delay(100)
                    }
                }
                secondsPerRep / 2 -> {
                    // Двойной бип в середине повторения
                    repeat(2) {
                        toneGen.startTone(
                            ToneGenerator.TONE_PROP_BEEP,
                            100
                        ); delay(100)
                    }
                }
                else -> {
                    // Обычный бип каждую секунду
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                }
            }
            onTimeLeftChange(timeLeft)
            delay(1000)
            if (isPaused()) {
                doPauseInExercise(isPaused, timeLeft, onTimeLeftChange)
            }
            timeLeft--
        }
        // Если onRepEnd() == false, цикл завершается
        if (!onRepEnd()) break
    }
    // Конец подхода: длинный бип
    toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
    // Если onSetEnd() == false, не продолжаем дальше
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
    val currentTimeLeft = timeLeft
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
    var prepTimeLeft by remember { mutableIntStateOf(prepTime) }
    var currentSet by remember { mutableIntStateOf(1) }
    var currentRep by remember { mutableIntStateOf(1) }
    var isRest by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(secondsPerRep) }
    var restTimeLeft by remember { mutableIntStateOf(restSeconds) }
    var finished by remember { mutableStateOf(false) }
    val phaseColor = when {
        isPreparation -> Color(0xFFFFF176) // жёлтый
        isRest -> Color(0xFF90CAF9)
        else -> Color(0xFFA5D6A7)
    }
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

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
                    toneGen,
                    onRepEnd = {
                        currentRep = (currentRep % reps) + 1
                        currentRep != 1
                    },
                    onSetEnd = {
                        if (currentSet < sets) {
                            currentSet++
                            isRest = true
                            currentRep = 1
                            restTimeLeft = restSeconds
                            true
                        } else {
                            finished = true
                            false
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
        // Нижняя панель управления: перемотка назад, пауза/воспроизведение, перемотка вперед
        if (!finished) {
            // Получаем ширину экрана
            val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
            val iconWidth = 64.dp
            val pauseWidth = 96.dp
            val totalIconsWidth = iconWidth * 2 + pauseWidth
            val spaceL = (screenWidth - totalIconsWidth) / 3f
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(spaceL / 2))
                IconButton(
                    onClick = {
                        if (isPreparation) return@IconButton
                        if (isRest) {
                            if (currentSet > 1) {
                                currentSet--
                                currentRep = reps
                                isRest = false
                                timeLeft = secondsPerRep
                            }
                        } else {
                            if (currentRep > 1) {
                                currentRep--
                                timeLeft = secondsPerRep
                            } else if (currentSet > 1) {
                                // currentSet--
                                // currentRep = reps
                                currentRep = 1
                                isRest = true
                                restTimeLeft = restSeconds
                            }
                        }
                    },
                    modifier = Modifier.size(iconWidth)
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_media_previous),
                        contentDescription = "Назад",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(iconWidth)
                    )
                }
                Spacer(modifier = Modifier.width(spaceL))
                IconButton(
                    onClick = { isPaused = !isPaused },
                    modifier = Modifier.size(pauseWidth)
                ) {
                    if (isPaused) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_media_play),
                            contentDescription = "Воспроизведение",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(pauseWidth)
                        )
                    } else {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_media_pause),
                            contentDescription = "Пауза",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(pauseWidth)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(spaceL))
                IconButton(
                    onClick = {
                        if (isPreparation) {
                            isPreparation = false
                            return@IconButton
                        }
                        // Исправленная логика для "вперёд": если последнее повторение последнего подхода, завершаем упражнение
                        if (isRest) {
                            if (currentSet < sets) {
                                currentRep = 1
                                isRest = false
                                timeLeft = secondsPerRep
                            } else if (currentSet == sets) {
                                // Если это отдых перед последним подходом, сразу переходим к первому повтору последнего подхода
                                currentRep = 1
                                isRest = false
                                timeLeft = secondsPerRep
                            }
                        } else {
                            if (currentSet == sets && currentRep == reps) {
                                // Последнее повторение последнего подхода — завершаем упражнение
                                finished = true
                                return@IconButton
                            }
                            if (currentRep < reps) {
                                currentRep++
                                timeLeft = secondsPerRep
                            } else if (currentSet < sets) {
                                currentSet++
                                currentRep = 1
                                isRest = true
                                restTimeLeft = restSeconds
                            }
                        }
                    },
                    modifier = Modifier.size(iconWidth)
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_media_next),
                        contentDescription = "Вперёд",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(iconWidth)
                    )
                }
                Spacer(modifier = Modifier.width(spaceL / 2))
            }
        }
    }
}
