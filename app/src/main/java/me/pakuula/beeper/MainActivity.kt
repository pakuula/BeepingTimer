package me.pakuula.beeper

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument // Исправленный импорт
import me.pakuula.beeper.theme.BeeperTheme

@Composable
fun TimerList(
    timers: List<TimerPreset>,
    onPresetClick: (TimerPreset) -> Unit,
    onEdit: (TimerPreset) -> Unit
) {
    Log.i("EditLog", "TimerList called with ${timers.size} presets")
    timers.forEach { Log.i("EditLog", "- ${it.title}") }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(16.dp)
    ) {
        items(timers, key = { it.title }) { preset ->
            TimerPresetWidget(
                preset = preset,
                onStart = { onPresetClick(it) },
                onEdit = onEdit
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.i("EditLog", "onCreate called")
        // Инициализация таймеров
        if (TimerStorage.isFirstLaunch(this)) {
            val defaultTimers = listOf(
                TimerPreset("6 сек/8 повторов/50 сек/4 подхода", 6, 8, 50, 4),
                TimerPreset("8 сек/6 повторов/50 сек/4 подхода", 8, 6, 50, 4),
                TimerPreset("6 сек/8 повторов/8 сек/8 подходов", 6, 8, 8, 8),
                TimerPreset("8 сек/6 повторов/8 сек/8 подходов", 8, 6, 8, 8)
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
                                        navController.navigate("edit/${Uri.encode(preset.title)}")
                                    }
                                )
                                FloatingActionButton(
                                    onClick = { navController.navigate("add") },
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                                    containerColor = Color(0xFF2196F3)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Добавить таймер", tint = Color.White)
                                }
                            }
                        }
                        composable(
                            "edit/{timerTitle}",
                            arguments = listOf(navArgument("timerTitle") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val timerTitle = backStackEntry.arguments?.getString("timerTitle") ?: ""
                            val decodedTitle = Uri.decode(timerTitle)
                            val preset = timers.find { it.title == decodedTitle }
                            if (preset != null) {
                                TimerEditScreen(
                                    preset = preset,
                                    onSave = { edited ->
                                        val idx = timers.indexOfFirst { it.title == preset.title }
                                        if (idx >= 0) timers[idx] = edited
                                        else timers.add(edited)
                                        TimerStorage.saveTimers(this@MainActivity, timers)
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    },
                                    onDelete = { deleted ->
                                        timers.removeAll { it.title == deleted.title }
                                        TimerStorage.saveTimers(this@MainActivity, timers)
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    },
                                    onCancel = {
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    },
                                    isNameUnique = { name -> timers.none { it.title == name || (preset.title == name) } }
                                )
                            } else {
                                // Если таймер не найден, просто возвращаемся назад
                                LaunchedEffect(Unit) { navController.popBackStack() }
                            }
                        }
                        composable("add") {
                            val defaultName = "Таймер ${timers.size + 1}"
                            val defaultPreset = TimerPreset(
                                defaultName,
                                8, // secondsPerRep
                                6, // reps
                                50, // restSeconds
                                4 // sets
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
                                onCancel = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                isNameUnique = { name -> timers.none { it.title == name } }
                            )
                        }
                    }
                }
            }
        }
    }
}