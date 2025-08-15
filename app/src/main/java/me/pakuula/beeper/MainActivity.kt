@file:OptIn(ExperimentalMaterial3Api::class)

package me.pakuula.beeper

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

@Composable
fun TimerList(
    timers: List<TimerPreset>,
    onPresetClick: (TimerPreset) -> Unit,
    onEdit: (TimerPreset) -> Unit
) {
    if (timers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Нет таймеров",
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
                    onStart = { onPresetClick(it) },
                    onEdit = onEdit
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        Log.i("EditLog", "onCreate called")
        // Инициализация таймеров
        if (TimerStorage.isFirstLaunch(this)) {
            val defaultTimers = listOf(
                TimerPreset(UUID.randomUUID().toString(), "6 сек/8 повторов/50 сек/4 подхода", 6, 8, 50, 4),
                TimerPreset(UUID.randomUUID().toString(), "8 сек/6 повторов/50 сек/4 подхода", 8, 6, 50, 4),
                TimerPreset(UUID.randomUUID().toString(), "6 сек/8 повторов/8 сек/8 подходов", 6, 8, 8, 8),
                TimerPreset(UUID.randomUUID().toString(), "8 сек/6 повторов/8 сек/8 подходов", 8, 6, 8, 8)
            )
            TimerStorage.saveTimers(this, defaultTimers)
        }
        setContent {
            BeeperTheme {
                Log.i("EditLog", "setContent called")
                val timers = remember { mutableStateListOf<TimerPreset>() }
                val navController = rememberNavController()
                // Загрузка таймеров из конфигурации
                LaunchedEffect(Unit) {
                    Log.i("EditLog", "Loading timers")
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
                                        val intent = Intent(this@MainActivity, ExerciseActivity::class.java).apply {
                                            putExtra("secondsPerRep", preset.secondsPerRep)
                                            putExtra("reps", preset.reps)
                                            putExtra("restSeconds", preset.restSeconds)
                                            putExtra("sets", preset.sets)
                                            putExtra("prepTime", preset.prepTime)
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
                                        Icon(Icons.Default.MoreVert, contentDescription = "Меню", tint = Color.White)
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Добавить таймер") },
                                            onClick = {
                                                menuExpanded = false
                                                navController.navigate("add")
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Настройки") },
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
                            val settings = remember { SettingsStorage.load(this@MainActivity) }
                            val defaultName = "Таймер ${timers.size + 1}"
                            val defaultPreset = TimerPreset(
                                UUID.randomUUID().toString(),
                                defaultName,
                                8, // secondsPerRep
                                6, // reps
                                50, // restSeconds
                                4, // sets
                                settings.prepTime // использовать настройку
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
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(WindowInsets.statusBars.asPaddingValues())
        .padding(WindowInsets.navigationBars.asPaddingValues())
        .padding(16.dp)
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text("Громкость звуковых сигналов", fontSize = 16.sp)
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0f..100f
                )
                Text("${volume.toInt()}%", fontSize = 14.sp)
            }
            item {
                Text("Язык приложения", fontSize = 16.sp)
                ExposedDropdownMenuBox(
                    expanded = languageExpanded,
                    onExpandedChange = { languageExpanded = !languageExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedLanguage,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Язык") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                        modifier = Modifier.menuAnchor()
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
            }
            item {
                Text("Голос синтезатора речи", fontSize = 16.sp)
                ExposedDropdownMenuBox(
                    expanded = voiceExpanded,
                    onExpandedChange = { voiceExpanded = !voiceExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedVoice,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Голос") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceExpanded) },
                        modifier = Modifier.menuAnchor()
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
            }
            item {
                Text("Время на подготовку к тренировке", fontSize = 16.sp)
                Slider(
                    value = prepTime.toFloat(),
                    onValueChange = { prepTime = it.toInt() },
                    valueRange = 0f..60f
                )
                Text("$prepTime сек", fontSize = 14.sp)
            }
            item {
                Text("Количество бипов перед началом тренировки", fontSize = 16.sp)
                Slider(
                    value = beepsBeforeStart.toFloat(),
                    onValueChange = { beepsBeforeStart = it.toInt() },
                    valueRange = 0f..10f
                )
                Text("$beepsBeforeStart", fontSize = 14.sp)
            }
            item {
                Text("Количество бипов перед началом подхода", fontSize = 16.sp)
                Slider(
                    value = beepsBeforeSet.toFloat(),
                    onValueChange = { beepsBeforeSet = it.toInt() },
                    valueRange = 0f..10f
                )
                Text("$beepsBeforeSet", fontSize = 14.sp)
            }
        }
        // Кнопка "Сохранить"
        IconButton(
            onClick = {
                val newSettings = Settings(
                    volume = volume.toInt(),
                    language = selectedLanguage,
                    voice = selectedVoice,
                    prepTime = prepTime,
                    beepsBeforeStart = beepsBeforeStart,
                    beepsBeforeSet = beepsBeforeSet
                )
                onSave(newSettings)
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = "Сохранить")
        }
    }
}