@file:OptIn(ExperimentalMaterial3Api::class)

package me.pakuula.beeper

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import me.pakuula.beeper.theme.BeeperTheme
import java.util.UUID

class MainActivity : ComponentActivity() {
    fun TimerPreset.getTitle(): String {
        if (this.title.isNotBlank()) return this.title

        val secString = getString(R.string.sec_short)

        return "${this.sets} x ${this.reps} x ${this.secondsPerRep} $secString / ${this.restSeconds} $secString"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Инициализация таймеров
        if (TimerStorage.isFirstLaunch(this)) {
            val defaultTimers = listOf(
                TimerPreset(
                    id = UUID.randomUUID().toString(),
                    title = "",
                    secondsPerRep = 6,
                    reps = 8,
                    restSeconds = 40,
                    sets = 4
                ),
                TimerPreset(
                    id = UUID.randomUUID().toString(),
                    title = "",
                    secondsPerRep = 8,
                    reps = 6,
                    restSeconds = 40,
                    sets = 4
                ),
                TimerPreset(
                    id = UUID.randomUUID().toString(),
                    title = "",
                    secondsPerRep = 6,
                    reps = 8,
                    restSeconds = 8,
                    sets = 8
                ),
                TimerPreset(
                    id = UUID.randomUUID().toString(),
                    title = "",
                    secondsPerRep = 8,
                    reps = 6,
                    restSeconds = 8,
                    sets = 8
                )
            )
            TimerStorage.saveTimers(this, defaultTimers)
        }
        setContent {
            BeeperTheme {
                val timers = remember { mutableStateListOf<TimerPreset>() }
                val navController = rememberNavController()
                // Загрузка таймеров из конфигурации
                LaunchedEffect(Unit) {
                    timers.clear()
                    timers.addAll(TimerStorage.loadTimers(this@MainActivity))
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            Box(modifier = Modifier.fillMaxSize()) {
                                TimerList(
                                    timers = timers,
                                    onPresetClick = { preset ->
                                        val intent = Intent(
                                            this@MainActivity,
                                            ExerciseActivity::class.java
                                        ).apply {
                                            putExtra(
                                                ExerciseActivity.SECONDS_PER_REP_KEY,
                                                preset.secondsPerRep
                                            )
                                            putExtra(ExerciseActivity.REPS_KEY, preset.reps)
                                            putExtra(
                                                ExerciseActivity.REST_SECONDS_KEY,
                                                preset.restSeconds
                                            )
                                            putExtra(ExerciseActivity.SETS_KEY, preset.sets)
                                        }
                                        startActivity(intent)
                                    },
                                    onEdit = { preset ->
                                        navController.navigate("edit/${preset.id}")
                                    }
                                )
                                // Меню и FAB в одном контейнере для правильного позиционирования
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp)
                                        .wrapContentSize(Alignment.BottomEnd)
                                        .navigationBarsPadding()
                                ) {
                                    var menuExpanded by remember { mutableStateOf(false) }
                                    FloatingActionButton(
                                        onClick = { menuExpanded = true },
                                        containerColor = Color(0xFF2196F3)
                                    ) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.menu),
                                            tint = Color.White
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier.width(48.dp) // ширина меню равна размеру иконки
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentAlignment = Alignment.CenterEnd
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = stringResource(R.string.add_timer),
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                navController.navigate("add")
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentAlignment = Alignment.CenterEnd
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Settings,
                                                        contentDescription = stringResource(R.string.settings),
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                navController.navigate("settings")
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // edit существующего таймера
                        composable(
                            "edit/{timerId}",
                            arguments = listOf(navArgument("timerId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val timerId = backStackEntry.arguments?.getString("timerId") ?: ""
                            val preset = timers.find { it.id == timerId }
                            if (preset != null) {
                                TimerEditScreen(
                                    preset = preset,
                                    onSave = { edited ->
                                        val idx = timers.indexOfFirst { it.id == preset.id }
                                        if (idx >= 0) timers[idx] = edited
                                        else timers.add(edited)
                                        TimerStorage.saveTimers(this@MainActivity, timers)
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    },
                                    onDelete = { deleted ->
                                        timers.removeAll { it.id == deleted.id }
                                        TimerStorage.saveTimers(this@MainActivity, timers)
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    },
                                    isNameUnique = { name -> timers.none { it.title == name && it.id != preset.id } },
                                    isNew = false // таймер не новый
                                )
                            } else {
                                LaunchedEffect(Unit) { navController.popBackStack() }
                            }
                        }
                        // add новый таймер
                        composable("add") {
                            val timerText = stringResource(R.string.timer)
                            val defaultName = "$timerText ${timers.size + 1}"
                            val defaultPreset = TimerPreset(
                                id = UUID.randomUUID().toString(),
                                title = defaultName,
                                secondsPerRep = Defaults.SECONDS_PER_REP,
                                reps = Defaults.REPS,
                                restSeconds = Defaults.REST_SECONDS,
                                sets = Defaults.SETS,
                            )
                            TimerEditScreen(
                                preset = defaultPreset,
                                onSave = { newPreset ->
                                    if (timers.none { it.title == newPreset.title }) {
                                        timers.add(newPreset)
                                        TimerStorage.saveTimers(this@MainActivity, timers)
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                },
                                onDelete = {}, // не показывать кнопку удаления
                                isNameUnique = { name -> timers.none { it.title == name } },
                                isNew = true // таймер новый
                            )
                        }
                        composable("settings") {
                            val settings = remember { SettingsStorage.load(this@MainActivity) }
                            SettingsScreen(
                                settings = settings,
                                onSave = { newSettings ->
                                    SettingsStorage.save(this@MainActivity, newSettings)
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TimerList(
        timers: List<TimerPreset>,
        onPresetClick: (TimerPreset) -> Unit,
        onEdit: (TimerPreset) -> Unit
    ) {
        if (timers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(R.string.no_timers),
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 20.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(16.dp)
            ) {
                items(timers, key = { it.id }) { preset ->
                    TimerPresetWidget(
                        preset = preset,
                        title = preset.getTitle(),
                        onStart = { onPresetClick(it) },
                        onEdit = onEdit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onSave: (Settings) -> Unit
) {
    var volume by remember { mutableFloatStateOf(settings.volume.toFloat()) }
    var prepTime by remember { mutableIntStateOf(settings.prepTime) }
    var beepsBeforeStart by remember { mutableIntStateOf(settings.beepsBeforeStart) }
    var beepsBeforeSet by remember { mutableIntStateOf(settings.beepsBeforeSet) }
    var languageExpanded by remember { mutableStateOf(false) }
    val languages = listOf("ru", "en")
    var selectedLanguage by remember { mutableStateOf(settings.language) }
    var voiceExpanded by remember { mutableStateOf(false) }
    val voices = listOf("default", "male", "female")
    var selectedVoice by remember { mutableStateOf(settings.voice) }
    var reverseRepCount by remember { mutableStateOf(settings.reverseRepCount) }
    var mute by remember { mutableStateOf(settings.mute) }
    var timerSettingsExpanded by remember { mutableStateOf(false) }
    var ttsSettingsExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(16.dp)
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(stringResource(R.string.settings_volume), fontSize = 16.sp)
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0f..100f
                )
                Text("${volume.toInt()}%", fontSize = 14.sp)
            }
            item {
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .animateContentSize(),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.settings_timer_settings),
                                fontSize = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                timerSettingsExpanded = !timerSettingsExpanded
                            }) {
                                Icon(
                                    imageVector = if (timerSettingsExpanded) Icons.Default.ArrowDropDown else Icons.Default.Add,
                                    contentDescription =
                                        if (timerSettingsExpanded) stringResource(R.string.settings_collapse)
                                        else stringResource(R.string.settings_expand)
                                )
                            }
                        }
                        if (timerSettingsExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.settings_preparation_time),
                                fontSize = 16.sp
                            )
                            Slider(
                                value = prepTime.toFloat(),
                                onValueChange = { prepTime = it.toInt() },
                                valueRange = 0f..60f
                            )
                            val sec = stringResource(R.string.sec_short)
                            Text("$prepTime $sec", fontSize = 14.sp)
                            Text(
                                stringResource(R.string.settings_beeps_before_start),
                                fontSize = 16.sp
                            )
                            Slider(
                                value = beepsBeforeStart.toFloat(),
                                onValueChange = { beepsBeforeStart = it.toInt() },
                                valueRange = 0f..10f
                            )
                            Text("$beepsBeforeStart", fontSize = 14.sp)
                            Text(
                                stringResource(R.string.settings_beeps_before_set),
                                fontSize = 16.sp
                            )
                            Slider(
                                value = beepsBeforeSet.toFloat(),
                                onValueChange = { beepsBeforeSet = it.toInt() },
                                valueRange = 0f..10f
                            )
                            Text("$beepsBeforeSet", fontSize = 14.sp)
                        }
                    }
                }
            }
            item {
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .animateContentSize(),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.settings_text_to_speech_settings),
                                fontSize = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { ttsSettingsExpanded = !ttsSettingsExpanded }) {
                                Icon(
                                    imageVector = if (ttsSettingsExpanded) Icons.Default.ArrowDropDown else Icons.Default.Add,
                                    contentDescription =
                                        if (ttsSettingsExpanded) stringResource(R.string.settings_collapse)
                                        else stringResource(R.string.settings_expand)
                                )
                            }
                        }
                        if (ttsSettingsExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.settings_tts_enable), fontSize = 16.sp)
                            androidx.compose.material3.Switch(
                                checked = !mute,
                                onCheckedChange = { mute = !it }
                            )
                            Text(
                                text = if (mute) stringResource(R.string.settings_off)
                                else stringResource(R.string.settings_on),
                                fontSize = 14.sp
                            )
                            Text(stringResource(R.string.settings_language), fontSize = 16.sp)
                            ExposedDropdownMenuBox(
                                expanded = languageExpanded && !mute,
                                onExpandedChange = {
                                    languageExpanded = if (mute) false else !languageExpanded
                                }
                            ) {
                                OutlinedTextField(
                                    value = selectedLanguage,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.settings_language_label)) },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = languageExpanded
                                        )
                                    },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                DropdownMenu(
                                    expanded = languageExpanded,
                                    onDismissRequest = { languageExpanded = false }
                                ) {
                                    languages.forEach { lang ->
                                        DropdownMenuItem(
                                            text = { Text(lang) },
                                            onClick = {
                                                selectedLanguage = lang
                                                languageExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Text(stringResource(R.string.settings_tts_language), fontSize = 16.sp)
                            ExposedDropdownMenuBox(
                                expanded = voiceExpanded && !mute,
                                onExpandedChange = {
                                    voiceExpanded = if (mute) false else !voiceExpanded
                                }
                            ) {
                                OutlinedTextField(
                                    value = selectedVoice,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.settings_tts_language_label)) },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = voiceExpanded
                                        )
                                    },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                DropdownMenu(
                                    expanded = voiceExpanded,
                                    onDismissRequest = { voiceExpanded = false }
                                ) {
                                    voices.forEach { v ->
                                        DropdownMenuItem(
                                            text = { Text(v) },
                                            onClick = {
                                                selectedVoice = v
                                                voiceExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Text(stringResource(R.string.settings_reps_in_reverse_order), fontSize = 16.sp)
                            androidx.compose.material3.Switch(
                                checked = reverseRepCount,
                                enabled = !mute,
                                onCheckedChange = { reverseRepCount = it }
                            )
                            Text(
                                text = if (reverseRepCount) stringResource(R.string.settings_on)
                                else stringResource(R.string.settings_off),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                androidx.compose.material3.Button(onClick = {
                    onSave(
                        settings.copy(
                            volume = volume.toInt(),
                            prepTime = prepTime,
                            beepsBeforeStart = beepsBeforeStart,
                            beepsBeforeSet = beepsBeforeSet,
                            language = selectedLanguage,
                            voice = selectedVoice,
                            reverseRepCount = reverseRepCount,
                            mute = mute
                        )
                    )
                }) {
                    Text(stringResource(R.string.save), fontSize = 18.sp)
                }
            }
        }
    }
}