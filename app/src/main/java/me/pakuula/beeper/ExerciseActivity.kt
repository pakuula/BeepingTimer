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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

    private lateinit var viewModel: ExerciseViewModel
    val isPreparation: Boolean get() = viewModel.workInfo.value.isPreparation
    val timeLeft: Int get() = viewModel.timeLeft.value
    val workInfo: Work get() = viewModel.workInfo.value

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var toneGen: ToneGenerator

    private var paramSecondsPerRep = 6
    private var paramRepNumber = 8
    private var paramRestSeconds = 50
    private var paramSetNumber = 7
    private var paramPreparationSeconds = 7
    private lateinit var appSettings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        paramSecondsPerRep = intent.getIntExtra("secondsPerRep", 6)
        paramRepNumber = intent.getIntExtra("reps", 8)
        paramRestSeconds = intent.getIntExtra("restSeconds", 50)
        paramSetNumber = intent.getIntExtra("sets", 4)
        appSettings = SettingsStorage.load(this)
        paramPreparationSeconds = appSettings.prepTime
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.getDefault()
            }
        }
        toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, appSettings.volume)
        // Инициализация viewModel с актуальными параметрами
        viewModel = androidx.lifecycle.ViewModelProvider(
            this, ExerciseViewModelFactory(
                paramPreparationSeconds, paramRestSeconds, paramSecondsPerRep
            )
        )[ExerciseViewModel::class.java]
        // Инициализация workInfo в viewModel только если оно ещё не восстановлено
        if (!viewModel.isWorkInfoInitialized()) {
            viewModel.initWorkInfo(
                isPreparation = paramPreparationSeconds > 0,
                maxRep = paramRepNumber,
                maxSet = paramSetNumber
            )
            viewModel.resetTimeLeft()
        }

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

    // skipForwardBackward теперь работает через viewModel
    fun skipForwardBackward(forward: Boolean) {
        if (forward) {
            viewModel.nextWork()
        } else {
            viewModel.prevWork()
        }
    }

    @SuppressLint("ConfigurationScreenWidthHeight")
    @Composable
    fun ExerciseScreen() {
        // Получаем состояние паузы из viewModel
        val isPaused by viewModel.isPaused.collectAsState()
        // Получаем workInfo из viewModel
        val workInfo by viewModel.workInfo.collectAsState()
        // Получаем оставшееся время из viewModel
        val timeLeft by viewModel.timeLeft.collectAsState()

        // Получаем muteIconRequested и showMuteIcon из viewModel
        val muteIconRequested by viewModel.muteIconRequested.collectAsState()
        val showMuteIcon by viewModel.showMuteIcon.collectAsState()

        LaunchedEffect(muteIconRequested) {
            if (muteIconRequested == 0) return@LaunchedEffect
            viewModel.setShowMuteIcon(true)
            delay(5000)
            viewModel.setShowMuteIcon(false)
            viewModel.setMuteIconRequested(0)
        }

        val phaseColor = when {
            workInfo.isPreparation -> Color(0xFFFFF176) // жёлтый
            workInfo.isRest -> Color(0xFF90CAF9)
            workInfo.isFinished -> Color(0xFF81C784) // зелёный
            // Выполнение упражнения
            else -> Color(0xFFA5D6A7)
        }

        LaunchedEffect(workInfo, isPaused) {
            if (!isPaused) {
                viewModel.setWorkInfo(
                    runExercise(
                        workInfo = workInfo,
                        timeLeft = timeLeft,
                    ) { newTimeLeft ->
                        viewModel.setTimeLeft(newTimeLeft)
                    })
            }
        }


        val swipeThreshold = 100.dp
        var totalDragAmount = 0.dp

        var boxWidthPx by remember { mutableIntStateOf(0) }
        val density = androidx.compose.ui.platform.LocalDensity.current
        val boxModifier = Modifier
            .fillMaxSize()
            .background(phaseColor)
            .systemBarsPadding()
            .clickable {
                viewModel.increaseMuteIconRequested()
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDragAmount =
                            0.dp // Сбрасываем накопленное значение при начале перетаскивания
                    },
                    onDragEnd = {
                        // Проверяем, достаточно ли было перетянуто для переключения
                        val coef = if (appSettings.swipeRightToLeft) -1 else 1
                        val dragAmount = totalDragAmount * coef
                        if (dragAmount > swipeThreshold) {
                            skipForwardBackward(true) // Перемотка вперёд
                        } else if (dragAmount < -swipeThreshold) {
                            skipForwardBackward(false) // Перемотка назад
                        }
                        totalDragAmount = 0.dp // Сбрасываем после завершения перетаскивания
                    },
                ) { change, dragAmount ->
                    change.consume()
                    totalDragAmount += dragAmount.dp
                }
            }
            .onGloballyPositioned { coordinates ->
                boxWidthPx = coordinates.size.width
            }
        Box(modifier = boxModifier) {
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
                            appSettings = appSettings.copy(
                                mute = !appSettings.mute
                            )
                            SettingsStorage.save(this@ExerciseActivity, appSettings)
                        },
                    ) {
                        Icon(
                            imageVector = if (appSettings.mute) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                            contentDescription = if (appSettings.mute) "Включить звук" else "Выключить звук",
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
                        text = "Упражнение завершено",
                        color = Color.Red,
                        style = bigText,
                        modifier = Modifier.wrapContentSize(Alignment.Center)
                    )
                } else if (workInfo.isPreparation) {
                    Text(text = "Подготовка", style = bigText, color = Color(0xFFFBC02D))
                    Text(text = timeLeft.toString(), style = hugeText)
                    Spacer(modifier = Modifier.height(32.dp))
                } else {
                    Text(text = "Подход: ${workInfo.currentSet} / $paramSetNumber", style = bigText)
                    Text(
                        text = "Повторение: ${workInfo.currentRep} / $paramRepNumber",
                        style = bigText
                    )
                    Text(text = if (workInfo.isRest) "Отдых" else "Выполнение", style = mediumText)
                    Text(text = timeLeft.toString(), style = hugeText)
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            // Нижняя панель управления: перемотка назад, пауза/воспроизведение, перемотка вперед
            if (!workInfo.isFinished) {
                val boxWidthDp = with(density) { boxWidthPx.toDp() }
                // Получаем ширину экрана
                androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
                // Определяем размеры и отступы для кнопок
                val iconWidth = 64.dp
                val pauseWidth = 96.dp
                val totalIconsWidth = iconWidth * 2 + pauseWidth
                val spaceL = (boxWidthDp - totalIconsWidth) / 3f
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .width(boxWidthDp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(spaceL / 2))
                    // Назад
                    IconButton(
                        onClick = {
                            skipForwardBackward(false)
                        }, modifier = Modifier.size(iconWidth)
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
                            viewModel.togglePaused()
                            if (!isPaused && workInfo.isWorking) {
                                viewModel.setTimeLeft(paramSecondsPerRep)
                            }
                        }, modifier = Modifier.size(pauseWidth)
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


    suspend fun doRest() {
        if (!isPreparation && timeLeft == paramRestSeconds) {
            // Если это не подготовка, то говорим о начале отдыха
            speak("Отдых $paramRestSeconds секунд")
        }
        // var timeLeft = viewModel.timeLeft
        while (timeLeft > 0) {
            if (timeLeft <= appSettings.beepsBeforeStart) {
                toneGen.startTone(TONE_PROP_BEEP, 100)
            }
            // onTimeLeftChange(timeLeft)
            delay(1000)
            viewModel.decreaseTimeLeft()
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
                    nextWork.isRest -> paramRestSeconds
                    else -> paramSecondsPerRep
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
            )
            // Возвращаем управление в главную composable процедуру
            return updateWorkAndTimeLeft()
        }
        // else: подход к выполнению упражнения
        var timeLeft = timeLeft
        while (timeLeft > 0) {
            when (timeLeft) {
                paramSecondsPerRep -> {
                    if (!appSettings.mute) {
                        val repToSpeak = if (appSettings.reverseRepCount) {
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

                paramSecondsPerRep / 2 -> {
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
        if (appSettings.mute) return
        textToSpeech.speak(
            text, QUEUE_FLUSH, null, null
        )
    }
}
