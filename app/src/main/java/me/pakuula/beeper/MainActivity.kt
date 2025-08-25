@file:OptIn(ExperimentalMaterial3Api::class)

package me.pakuula.beeper

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
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
            MainView()
        }
    }

    @Composable
    private fun MainView() {
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
                        HomeScreen(timers, navController)
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
                        AddTimer(timers, navController)
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

    @Composable
    private fun HomeScreen(
        timers: SnapshotStateList<TimerPreset>,
        navController: NavHostController
    ) {
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

    @Composable
    private fun AddTimer(
        timers: SnapshotStateList<TimerPreset>,
        navController: NavHostController
    ) {
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

