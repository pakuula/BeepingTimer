package me.pakuula.beeper

//noinspection SuspiciousImport
import android.R
import android.annotation.SuppressLint
import android.media.AudioManager
import android.media.ToneGenerator
import android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
import android.media.ToneGenerator.TONE_PROP_BEEP
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.QUEUE_FLUSH
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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import me.pakuula.beeper.theme.BeeperTheme
import me.pakuula.beeper.util.Work
import java.util.Locale

class ExerciseActivity : ComponentActivity() {
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var toneGen: ToneGenerator

    private var secondsPerRep = 6
    private var reps = 8
    private var restSeconds = 50
    private var sets = 7
    private var prepTime = 7
    private var settings: Settings = Settings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        secondsPerRep = intent.getIntExtra("secondsPerRep", 6)
        reps = intent.getIntExtra("reps", 8)
        restSeconds = intent.getIntExtra("restSeconds", 50)
        sets = intent.getIntExtra("sets", 4)
        settings = SettingsStorage.load(this)
        prepTime = settings.prepTime
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.getDefault()
            }
        }
        toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, settings.volume)
        setContent {
            BeeperTheme {
                ExerciseScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.shutdown()
        toneGen.release()
    }


    @SuppressLint("ConfigurationScreenWidthHeight")
    @Composable
    fun ExerciseScreen() {
        var workInfo by remember {
            mutableStateOf(
                Work(
                    isPreparation = prepTime > 0,
                    maxRep = reps,
                    maxSet = sets,
                    currentRep = 1,
                    currentSet = 1,
                    isRest = false,
                    isFinished = false
                )
            )
        }
        var isPaused by remember { mutableStateOf(false) }
        var timeLeft by remember {
            mutableIntStateOf(
                when {
                    workInfo.isPreparation -> prepTime
                    workInfo.isRest -> restSeconds
                    workInfo.isFinished -> 0
                    else -> secondsPerRep
                }
            )
        }
        val phaseColor = when {
            workInfo.isPreparation -> Color(0xFFFFF176) // жёлтый
            workInfo.isRest -> Color(0xFF90CAF9)
            workInfo.isFinished -> Color(0xFF81C784) // зелёный
            // Выполнение упражнения
            else -> Color(0xFFA5D6A7)
        }
//        val toneGen = remember {
//            mutableStateOf(
//                ToneGenerator(AudioManager.STREAM_MUSIC, settings.volume)
//            )
//        }


        LaunchedEffect(workInfo, isPaused) {
            if (!isPaused) {
                workInfo = runExercise(
                    workInfo = workInfo,
                    timeLeft = timeLeft,
                ) { newTimeLeft ->
                    timeLeft = newTimeLeft
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
                if (workInfo.isFinished) {
                    Text(
                        text = "Упражнение завершено", color = Color.Red,
                        style = bigText,
                        modifier = Modifier.wrapContentSize(Alignment.Center)
                    )
                } else if (workInfo.isPreparation) {
                    Text(text = "Подготовка", style = bigText, color = Color(0xFFFBC02D))
                    Text(text = timeLeft.toString(), style = hugeText)
                    Spacer(modifier = Modifier.height(32.dp))
                } else {
                    Text(text = "Подход: ${workInfo.currentSet} / $sets", style = bigText)
                    Text(text = "Повторение: ${workInfo.currentRep} / $reps", style = bigText)
                    Text(text = if (workInfo.isRest) "Отдых" else "Выполнение", style = mediumText)
                    Text(text = timeLeft.toString(), style = hugeText)
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            // Нижняя панель управления: перемотка назад, пауза/воспроизведение, перемотка вперед
            if (!workInfo.isFinished) {
                // Получаем ширину экрана
                val screenWidth =
                    androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
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
                    // Назад
                    IconButton(
                        onClick = {
                            workInfo = workInfo.prev()
                            when {
                                workInfo.isWorking -> timeLeft = secondsPerRep
                                workInfo.isRest -> timeLeft = restSeconds
                                workInfo.isPreparation -> timeLeft = prepTime
                            }
                        },
                        modifier = Modifier.size(iconWidth)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_media_previous),
                            contentDescription = "Назад",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(iconWidth)
                        )
                    }
                    Spacer(modifier = Modifier.width(spaceL))
                    // Пауза/воспроизведение
                    IconButton(
                        onClick = {
                            isPaused = !isPaused
                            if (!isPaused && workInfo.isWorking) {
                                timeLeft = secondsPerRep
                            }
                        },
                        modifier = Modifier.size(pauseWidth)
                    ) {
                        if (isPaused) {
                            Icon(
                                painter = painterResource(R.drawable.ic_media_play),
                                contentDescription = "Воспроизведение",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(pauseWidth)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_media_pause),
                                contentDescription = "Пауза",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(pauseWidth)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(spaceL))
                    // Вперёд
                    IconButton(
                        modifier = Modifier.size(iconWidth),
                        onClick = {
                            workInfo = workInfo.next()
                            when {
                                workInfo.isWorking -> timeLeft = secondsPerRep
                                workInfo.isRest -> timeLeft = restSeconds
                                workInfo.isPreparation -> timeLeft = prepTime
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_media_next),
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

    suspend fun doRest(
        restSeconds: Int,
        onTimeLeftChange: (Int) -> Unit,
    ) {
        var timeLeft = restSeconds
        while (timeLeft > 0) {
            if (timeLeft <= settings.beepsBeforeStart) {
                toneGen.startTone(TONE_PROP_BEEP, 100)
            }
            onTimeLeftChange(timeLeft)
            delay(1000)
            timeLeft--
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun runExercise(
        workInfo: Work,
        timeLeft: Int,
        onTimeLeftChange: (Int) -> Unit,
    ): Work {
        val updateWorkAndTimeLeft = {
            val nextWork = workInfo.next()
            onTimeLeftChange(
                when {
                    nextWork.isRest -> restSeconds
                    else -> secondsPerRep
                }
            )
            nextWork
        }
        if (workInfo.isFinished) return workInfo
        if (workInfo.isPreparation || workInfo.isRest) {
            doRest(
                restSeconds = timeLeft,
                onTimeLeftChange = onTimeLeftChange,
            )
            return updateWorkAndTimeLeft()
        }
        if (workInfo.currentRep == 1 && timeLeft == secondsPerRep) {
            // Начало подхода: длинный громкий сигнал
            // toneGen.startTone(TONE_CDMA_ALERT_CALL_GUARD, 500)
            textToSpeech.speak(
                "Поехали!",
                QUEUE_FLUSH,
                null,
                null
            )
        }
        var timeLeft = timeLeft
        while (timeLeft > 0) {
            when (timeLeft) {
                secondsPerRep -> {
                    val repToSpeak = if (settings.reverseRepCount) {
                        workInfo.maxRep - workInfo.currentRep + 1
                    } else {
                        workInfo.currentRep
                    }
                    textToSpeech.speak(
                        repToSpeak.toString(),
                        QUEUE_FLUSH,
                        null,
                        null
                    )
                }

                secondsPerRep / 2 -> {
                    // Двойной бип в середине повторения
                    toneGen.startTone(TONE_CDMA_ALERT_CALL_GUARD, 100)
                }

                else -> {
                    // Обычный бип каждую секунду
                    toneGen.startTone(TONE_PROP_BEEP, 100)
                }
            }
            onTimeLeftChange(timeLeft)
            delay(1000)

            timeLeft--
        }


        // Конец подхода: длинный бип
        if (workInfo.isLastRep()) {
            toneGen.startTone(TONE_CDMA_ALERT_CALL_GUARD, 500)
            if (workInfo.isVeryLastRep()) {
                textToSpeech.speak(
                    "Упражнение завершено",
                    QUEUE_FLUSH,
                    null,
                    null
                )
            } else {
                textToSpeech.speak(
                    "Отдых $restSeconds секунд",
                    QUEUE_FLUSH,
                    null,
                    null
                )
            }
        }
        return updateWorkAndTimeLeft()
    }
}

