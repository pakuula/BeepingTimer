package me.pakuula.beeper

//noinspection SuspiciousImport

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
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

    private var SecondsPerRep = 6
    private var RepNumber = 8
    private var RestSeconds = 50
    private var SetNumber = 7
    private var PreparationSeconds = 7
    private lateinit var AppSettings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        SecondsPerRep = intent.getIntExtra("secondsPerRep", 6)
        RepNumber = intent.getIntExtra("reps", 8)
        RestSeconds = intent.getIntExtra("restSeconds", 50)
        SetNumber = intent.getIntExtra("sets", 4)
        AppSettings = SettingsStorage.load(this)
        PreparationSeconds = AppSettings.prepTime
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.getDefault()
            }
        }
        toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, AppSettings.volume)
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
                    isPreparation = PreparationSeconds > 0,
                    maxRep = RepNumber,
                    maxSet = SetNumber,
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
                    workInfo.isPreparation -> PreparationSeconds
                    workInfo.isRest -> RestSeconds
                    workInfo.isFinished -> 0
                    else -> SecondsPerRep
                }
            )
        }
        var muteIconRequested by remember { mutableIntStateOf(0) }
        var showMuteIcon by remember { mutableStateOf(false) }

        LaunchedEffect(muteIconRequested) {
            if (muteIconRequested == 0) return@LaunchedEffect
            // Показываем иконку, если пользователь кликнул по экрану
            showMuteIcon = true
            // Скрываем иконку через 5 секунд
            delay(5000)
            showMuteIcon = false
        }

        val phaseColor = when {
            workInfo.isPreparation -> Color(0xFFFFF176) // жёлтый
            workInfo.isRest -> Color(0xFF90CAF9)
            workInfo.isFinished -> Color(0xFF81C784) // зелёный
            // Выполнение упражнения
            else -> Color(0xFFA5D6A7)
        }

        val skipForwardBackward = {
            forward: Boolean ->
            workInfo = if (forward) {
                workInfo.next()
            } else {
                workInfo.prev()
            }
            when {
                workInfo.isWorking -> timeLeft = SecondsPerRep
                workInfo.isRest -> timeLeft = RestSeconds
                workInfo.isPreparation -> timeLeft = PreparationSeconds
            }
        }

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


        val swipeThreshold = 100.dp
        var totalDragAmount = 0.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(phaseColor)
                .systemBarsPadding()
                .clickable {
                    muteIconRequested++
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            totalDragAmount = 0.dp // Сбрасываем накопленное значение при начале перетаскивания
                        },
                        onDragEnd = {
                            // Проверяем, достаточно ли было перетянуто для переключения
                            if (totalDragAmount > swipeThreshold) {
                                skipForwardBackward(true) // Перемотка вперёд
                            } else if (totalDragAmount < -swipeThreshold) {
                                skipForwardBackward(false) // Перемотка назад
                            }
                            totalDragAmount = 0.dp // Сбрасываем после завершения перетаскивания
                        },
                    ) {
                        change, dragAmount ->
                        change.consume()
                        totalDragAmount += dragAmount.dp
                    }
                }
        ) {
            if (!workInfo.isFinished && showMuteIcon) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(all = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            AppSettings = AppSettings.copy(
                                mute = !AppSettings.mute
                            )
                            SettingsStorage.save(this@ExerciseActivity, AppSettings)
                        },
                    ) {
                        Icon(
                            imageVector = if (AppSettings.mute) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                            contentDescription = if (AppSettings.mute) "Включить звук" else "Выключить звук",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
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
                    Text(text = "Подход: ${workInfo.currentSet} / $SetNumber", style = bigText)
                    Text(text = "Повторение: ${workInfo.currentRep} / $RepNumber", style = bigText)
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
                // Определяем размеры и отступы для кнопок
                val iconWidth = 64.dp
                val pauseWidth = 96.dp
                val totalIconsWidth = iconWidth * 2 + pauseWidth
                val spaceL = (screenWidth - totalIconsWidth) / 3f
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(spaceL / 2))
                    // Назад
                    IconButton(
                        onClick = {
                            skipForwardBackward(false)
                        },
                        modifier = Modifier.size(iconWidth)
                    ) {
                        Icon(
                            // painter = painterResource(R.drawable.ic_media_previous),
                            imageVector = Icons.Outlined.SkipPrevious,
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
                                timeLeft = SecondsPerRep
                            }
                        },
                        modifier = Modifier.size(pauseWidth)
                    ) {
                        if (isPaused) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = "Воспроизведение",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(pauseWidth)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Pause,
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
                            skipForwardBackward(true)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SkipNext,
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
        remainingSeconds: Int,
        onTimeLeftChange: (Int) -> Unit,
        isPreparation: Boolean = false,
    ) {
        if (!isPreparation && remainingSeconds == RestSeconds) {
            // Если это не подготовка, то говорим о начале отдыха
            speak("Отдых $RestSeconds секунд")
        }
        var timeLeft = remainingSeconds
        while (timeLeft > 0) {
            if (timeLeft <= AppSettings.beepsBeforeStart) {
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
                    nextWork.isRest -> RestSeconds
                    else -> SecondsPerRep
                }
            )
            nextWork
        }
        if (workInfo.isFinished) {
            // Упражнение завершено, ничего не делаем
            return workInfo
        }
        if (workInfo.isPreparation || workInfo.isRest) {
            // Подготовка или отдых: отсчитываем время до завершения
            doRest(
                remainingSeconds = timeLeft,
                onTimeLeftChange = onTimeLeftChange,
            )
            // Возвращаем управление в главную composable процедуру
            return updateWorkAndTimeLeft()
        }
        // else: подход к выполнению упражнения
        var timeLeft = timeLeft
        while (timeLeft > 0) {
            when (timeLeft) {
                SecondsPerRep -> {
                    if (!AppSettings.mute) {
                        val repToSpeak = if (AppSettings.reverseRepCount) {
                            workInfo.maxRep - workInfo.currentRep + 1
                        } else {
                            workInfo.currentRep
                        }
                        val text = repToSpeak.toString()
                        speak(text)
                    } else {
                        // Начало подхода: длинный громкий сигнал
                        toneGen.startTone(TONE_CDMA_ALERT_CALL_GUARD, 200)
                    }
                }

                SecondsPerRep / 2 -> {
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
                speak("Упражнение завершено")
            }
        }
        return updateWorkAndTimeLeft()
    }

    private fun speak(text: String) {
        if (AppSettings.mute) return
        textToSpeech.speak(
            text,
            QUEUE_FLUSH,
            null,
            null
        )
    }
}
