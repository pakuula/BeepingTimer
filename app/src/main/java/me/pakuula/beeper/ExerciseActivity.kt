package me.pakuula.beeper

//noinspection SuspiciousImport
import android.annotation.SuppressLint
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import me.pakuula.beeper.ExerciseActivity.Companion.REPS_KEY
import me.pakuula.beeper.ExerciseActivity.Companion.REST_SECONDS_KEY
import me.pakuula.beeper.ExerciseActivity.Companion.SECONDS_PER_REP_KEY
import me.pakuula.beeper.ExerciseActivity.Companion.SETS_KEY
import me.pakuula.beeper.theme.BeeperTheme
import me.pakuula.beeper.util.Work
import java.util.Locale

/**
 * ExerciseActivity - основная активность приложения, которая управляет выполнением упражнений.
 *
 * Она инициализирует необходимые компоненты, такие как TextToSpeech и ToneGenerator,
 * а также управляет состоянием выполнения упражнений через ViewModel.
 *
 * @property SECONDS_PER_REP_KEY ключ для передачи количества секунд на повторение.
 * @property REPS_KEY ключ для передачи количества повторений.
 * @property REST_SECONDS_KEY ключ для передачи количества секунд отдыха между подходами.
 * @property SETS_KEY ключ для передачи количества подходов.
 */
class ExerciseActivity : ComponentActivity() {

    val digitalFont = FontFamily(
        Font(R.font.ds_digi, FontWeight.Normal),

        )


    private lateinit var viewModel: ExerciseViewModel
    val isPreparation: Boolean get() = viewModel.workInfo.value.isPreparation
    val timeLeft: Int get() = viewModel.timeLeft.value
    val workInfo: Work get() = viewModel.workInfo.value

    @Suppress("unused")
    val showMuteIcon: Boolean get() = viewModel.showMuteIcon.value

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var toneGen: ToneGenerator
    val bigText = TextStyle(fontSize = 36.sp)
    val phaseColor
        get() = when {
            workInfo.isPreparation -> Color(0xFFFFF176) // жёлтый
            workInfo.isRest -> Color(0xFF90CAF9)
            workInfo.isFinished -> Color(0xFF81C784) // зелёный
            // Выполнение упражнения
            else -> Color(0xFFA5D6A7)
        }
    private var paramSecondsPerRep = Defaults.SECONDS_PER_REP
    private var paramRepNumber = Defaults.REPS
    private var paramRestSeconds = Defaults.REST_SECONDS
    private var paramSetNumber = Defaults.SETS
    private var paramPreparationSeconds = Defaults.PREPARATION_SECONDS
    private lateinit var appSettings: Settings

    companion object {
        const val SECONDS_PER_REP_KEY = "secondsPerRep"
        const val REPS_KEY = "reps"
        const val REST_SECONDS_KEY = "restSeconds"
        const val SETS_KEY = "sets"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Получаем параметры из Intent или используем значения по умолчанию
        paramSecondsPerRep = intent.getIntExtra(SECONDS_PER_REP_KEY, Defaults.SECONDS_PER_REP)
        paramRepNumber = intent.getIntExtra(REPS_KEY, Defaults.REPS)
        paramRestSeconds = intent.getIntExtra(REST_SECONDS_KEY, Defaults.REST_SECONDS)
        paramSetNumber = intent.getIntExtra(SETS_KEY, Defaults.SETS)

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
        // Инициализация workInfo в viewModel только есл�� оно ещё не восстановлено
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

        LaunchedEffect(muteIconRequested) {
            if (muteIconRequested == 0) return@LaunchedEffect
            viewModel.setShowMuteIcon(true)
            delay(5000)
            viewModel.setShowMuteIcon(false)
            viewModel.setMuteIconRequested(0)
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
        var boxHeightPx by remember { mutableIntStateOf(0) }

        val boxModifierFactory = {
            Modifier
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
                            val direction = if (appSettings.swipeRightToLeft) -1 else 1
                            val dragAmount = totalDragAmount * direction
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
                    boxHeightPx = coordinates.size.height
                }
        }
        // Определяем, какую компоновку использовать: горизонтальную или вертикальную
        val isLandscape =
            resources.configuration.orientation == ORIENTATION_LANDSCAPE
        if (isLandscape) {
            HorizontalLayout(boxModifierFactory) { boxHeightPx }
        } else {
            VerticalLayout(boxModifierFactory) { boxWidthPx }
        }
    }

    private val skipIconSize = 64.dp
    private val pauseSize = 96.dp

    @Composable
    private fun HorizontalLayout(
        boxModifierFactory: () -> Modifier,
        getBoxHeightPx: () -> Int,
    ) {
        val density = LocalDensity.current
        val showMuteIcon by viewModel.showMuteIcon.collectAsState()
        val workInfo by viewModel.workInfo.collectAsState()
        Row(modifier = boxModifierFactory()) {
            // Левая часть: TimerView
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (!workInfo.isFinished && showMuteIcon) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(all = 16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ButtonMute()
                    }
                }
                TimerView()
            }
            // Правая часть: тулбар с кнопками
            if (!workInfo.isFinished) {
                val boxHeightDp = with(density) { getBoxHeightPx().toDp() }
                val totalIconsSize = skipIconSize * 2 + pauseSize
                val spaceL = (boxHeightDp - totalIconsSize) / 3f
                Column(
                    modifier = Modifier
                        .width(pauseSize)
                        .fillMaxHeight()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(spaceL / 2))
                    ButtonBack()
                    Spacer(modifier = Modifier.height(spaceL))
                    ButtonPlayPause()
                    Spacer(modifier = Modifier.height(spaceL))
                    ButtonForward()
                    Spacer(modifier = Modifier.height(spaceL / 2))
                }
            }
        }
    }

    @Composable
    private fun VerticalLayout(
        boxModifierFactory: () -> Modifier,
        @Suppress("unused")
        getBoxWidthPx: () -> Int,
    ) {
        val showMuteIcon by viewModel.showMuteIcon.collectAsState()
        val workInfo by viewModel.workInfo.collectAsState()
        Box(modifier = boxModifierFactory()) {
            if (!workInfo.isFinished && showMuteIcon) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(all = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ButtonMute()
                }
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Таймер и числа
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    TimerView()
                }
                // Кнопки управления
                if (!workInfo.isFinished) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp), // фиксированная высота для кнопок
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ButtonBack()
                        ButtonPlayPause()
                        ButtonForward()
                    }
                }
            }
        }
    }

    @Composable
    private fun ButtonMute() {
        var showOff: Boolean by remember {
            mutableStateOf(appSettings.mute)
        }
        IconButton(
            onClick = {
                appSettings = appSettings.copy(
                    mute = !appSettings.mute
                )
                SettingsStorage.save(this@ExerciseActivity, appSettings)
                showOff = !showOff
            },
        ) {
            val context = LocalContext.current
            Icon(
                imageVector = if (showOff) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                contentDescription = if (showOff) context.getString(R.string.sound_on) else context.getString(
                    R.string.sound_off
                ),
                tint = Color.DarkGray,
                modifier = Modifier.size(48.dp)
            )
        }
    }

    @Composable
    private fun TimerView() {
        val workInfo by viewModel.workInfo.collectAsState()
        val timeLeft by viewModel.timeLeft.collectAsState()
        val orientation = resources.configuration.orientation
        if (workInfo.isFinished) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),


                ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = getString(R.string.exercise_finished),
                        color = Color.Red,
                        style = bigText,
                        modifier = Modifier.wrapContentSize(Alignment.Center)
                    )
                }
            }
        } else if (orientation == ORIENTATION_LANDSCAPE) {
            TimerViewHorizontal(workInfo, timeLeft)
        } else {
            TimerViewVertical(workInfo, timeLeft)
        }
    }

    @Composable
    private fun TimerViewVertical(workInfo: Work, timeLeft: Int) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Верхняя часть: "подходы" и "повторения" рядом
                var upperBoxHeightPx by remember { mutableIntStateOf(0) }
                var setLabelWidthPx by remember { mutableIntStateOf(0) }
                var repLabelWidthPx by remember { mutableIntStateOf(0) }
                val context = LocalContext.current
                val setLabel = context.getString(R.string.sets_label)
                val repLabel = context.getString(R.string.reps_label)
                var labelFontSizeSp = 0f
                // Виджет с числом подходов и повторений
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            upperBoxHeightPx = coordinates.size.height
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Вычисляем размер шрифта для цифр и надписей "Подходы" и "Повторения"
                    val density = LocalDensity.current
                    val fontSizeSp =
                        with(density) { (upperBoxHeightPx * 0.5f).toSp() } // 50% высоты для цифр
                    // Получаем ширину каждой коробочки
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top,
                            modifier = Modifier
                                .weight(1f)
                                .padding(all = 8.dp)
                                .onGloballyPositioned { coordinates ->
                                    setLabelWidthPx = coordinates.size.width
                                }
                        ) {
                            // пусто, только для измерения ширины
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top,
                            modifier = Modifier
                                .weight(1f)
                                .padding(all = 8.dp)
                                .onGloballyPositioned { coordinates ->
                                    repLabelWidthPx = coordinates.size.width
                                }
                        ) {
                            // пусто, только для измерения ширины
                        }
                    }
                    // Вычисляем максимальный размер шрифта для надписей
                    val setLabelFontSizeSp = remember(setLabelWidthPx) {
                        calculateMaxFontSizeSp(setLabel, setLabelWidthPx, density)
                    }
                    val repLabelFontSizeSp = remember(repLabelWidthPx) {
                        calculateMaxFontSizeSp(repLabel, repLabelWidthPx, density)
                    }
                    labelFontSizeSp = minOf(setLabelFontSizeSp, repLabelFontSizeSp)
                    // Отрисовываем надписи и цифры
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = setLabel,
                                fontSize = labelFontSizeSp.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )
                            Text(
                                text = (paramSetNumber - workInfo.currentSet + 1).toString(),
                                fontFamily = digitalFont,
                                fontSize = fontSizeSp,
                                color = Color.DarkGray
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = repLabel,
                                fontSize = labelFontSizeSp.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )
                            Text(
                                text = (paramRepNumber - workInfo.currentRep + 1).toString(),
                                fontFamily = digitalFont,
                                fontSize = fontSizeSp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
                // Нижняя часть: таймер
                var lowerBoxHeightPx by remember { mutableIntStateOf(0) }
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            lowerBoxHeightPx = coordinates.size.height
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val density = LocalDensity.current
                    val timerFontSizeSp = with(density) { (lowerBoxHeightPx * 0.8f).toSp() }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val context = LocalContext.current
                        Text(
                            text = if (workInfo.isPreparation) context.getString(R.string.preparation)
                            else if (workInfo.isRest) context.getString(R.string.rest)
                            else context.getString(R.string.work),
                            fontSize = labelFontSizeSp.sp,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(
                            text = timeLeft.toString(),
                            fontFamily = digitalFont,
                            fontSize = timerFontSizeSp,
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }

    // Вспомогательная функция для вычисления максимального размера шрифта
    private fun calculateMaxFontSizeSp(text: String, boxWidthPx: Int, density: Density): Float {
        val factor = 0.6f
        val maxFontSizePx =
            if (text.isNotEmpty() && boxWidthPx > 0) boxWidthPx / (text.length * factor) else 12f
        return with(density) { maxFontSizePx.toSp().value }
    }

    @Composable
    private fun TimerViewHorizontal(workInfo: Work, timeLeft: Int) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // --- Три секции: подходы, повторения, таймер ---
                val context = LocalContext.current
                val labels = listOf(
                    context.getString(R.string.sets_label),
                    context.getString(R.string.reps_label),
                    if (workInfo.isPreparation) context.getString(R.string.preparation) else if (workInfo.isRest) context.getString(
                        R.string.rest
                    ) else context.getString(R.string.work)
                )
                val values = listOf(
                    (paramSetNumber - workInfo.currentSet + 1).toString(),
                    (paramRepNumber - workInfo.currentRep + 1).toString(),
                    timeLeft.toString()
                )
                val sectionWidthsPx = remember { mutableStateOf(listOf(0, 0, 0)) }
                val density = LocalDensity.current
                // Вычисляем максимальный размер шрифта для подписей
                val labelFontSizes = labels.mapIndexed { i, label ->
                    calculateMaxFontSizeSp(label, sectionWidthsPx.value.getOrNull(i) ?: 0, density)
                }
                val labelFontSizeSp = labelFontSizes.minOrNull() ?: 12f
                // Отрисовываем секции
                Row(modifier = Modifier.weight(1f)) {
                    for (i in 0..2) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(8.dp)
                                .onGloballyPositioned { coordinates ->
                                    val widths = sectionWidthsPx.value.toMutableList()
                                    widths[i] = coordinates.size.width
                                    sectionWidthsPx.value = widths
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = labels[i],
                                fontSize = labelFontSizeSp.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val valueFontSizeSp = with(density) {
                                (sectionWidthsPx.value.getOrNull(i)?.times(0.95f) ?: 48f).toSp()
                            }
                            Text(
                                text = values[i],
                                fontFamily = digitalFont,
                                fontSize = valueFontSizeSp,
                                color = if (i == 2) Color.Black else Color.DarkGray
                            )
                        }
                    }
                }
                // --- тулбар с кнопками удалён ---
            }
        }
    }

    @Composable
    private fun ButtonForward() {
        IconButton(
            modifier = Modifier.size(skipIconSize),
            onClick = {
                skipForwardBackward(true)
            },
        ) {
            val context = LocalContext.current
            Icon(
                imageVector = Icons.Outlined.SkipNext,
                contentDescription = context.getString(R.string.forward),
                tint = Color.DarkGray,
                modifier = Modifier.size(skipIconSize)
            )
        }
    }

    @Composable
    private fun ButtonPlayPause() {
        val isPaused by viewModel.isPaused.collectAsState()
        val workInfo by viewModel.workInfo.collectAsState()
        IconButton(
            onClick = {
                viewModel.togglePaused()
                if (!isPaused && workInfo.isWorking) {
                    viewModel.setTimeLeft(paramSecondsPerRep)
                }
            }, modifier = Modifier.size(pauseSize)
        ) {
            val context = LocalContext.current
            if (isPaused) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = context.getString(R.string.play),
                    tint = Color.DarkGray,
                    modifier = Modifier.size(pauseSize)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Pause,
                    contentDescription = context.getString(R.string.pause),
                    tint = Color.DarkGray,
                    modifier = Modifier.size(pauseSize)
                )
            }
        }
    }

    @Composable
    private fun ButtonBack() {
        IconButton(
            onClick = {
                skipForwardBackward(false)
            },
            modifier = Modifier.size(skipIconSize)
        ) {
            Icon(
                // painter = painterResource(R.drawable.ic_media_previous),
                imageVector = Icons.Outlined.SkipPrevious,
                contentDescription = "Назад",
                tint = Color.DarkGray,
                modifier = Modifier.size(skipIconSize)
            )
        }
    }

    // Действия: отдых и упражнение

    fun speakTimeLeft() {
//        speak("Начинаем через $timeLeft секунд")
        val text = resources.getQuantityString(
            R.plurals.start_in_seconds,
            timeLeft, timeLeft)
        speak(text)
    }

    /**
     * Выполняем отдых, если это подготовка или отдых.
     * Если это подготовка, то говорим о начале отдыха.
     *
     */
    suspend fun doRest() {
        if (!isPreparation && timeLeft == paramRestSeconds) {
            // Если это не подготовка, то говорим о начале отдыха
            val message = resources.getQuantityString(R.plurals.rest_for_seconds,
                paramRestSeconds, paramRestSeconds)
            speak(message)
//            speak("Отдых $paramRestSeconds секунд")
        }
        // var timeLeft = viewModel.timeLeft
        while (timeLeft > 0) {
            if (!(workInfo.isRest && timeLeft == paramRestSeconds) && timeLeft % 10 == 0) {
                speakTimeLeft()
            }
            if (timeLeft <= appSettings.beepsBeforeStart) {
                toneGen.startTone(TONE_PROP_BEEP, 100)
            }
            // onTimeLeftChange(timeLeft)
            delay(1000)
            viewModel.decreaseTimeLeft()
        }
    }

    /**
     * Выполняем упражнение.
     *
     * Если упражнение завершено, то ничего не делаем.
     * Если это подготовка или отдых, то отсчитываем время до завершения.
     * Если это подход к выполнению упражнения, то отсчитываем время до завершения подхода.
     *
     * @param workInfo информация о текущем состоянии таймера
     * @param timeLeft оставшееся время в секундах
     * @param onTimeLeftChange функция обратного вызова для обновления оставшегося времени
     * @return обновлённая информация о текущем состоянии таймера
     */
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
            doRest()
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
//                val context = LocalContext.current
                speak(this.getString(R.string.exercise_finished))
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
