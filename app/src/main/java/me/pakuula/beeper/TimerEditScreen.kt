package me.pakuula.beeper

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimerEditScreen(
    preset: TimerPreset,
    onSave: (TimerPreset) -> Unit,
    onDelete: (TimerPreset) -> Unit,
    onCancel: () -> Unit,
    isNameUnique: (String) -> Boolean
) {
    var title by remember { mutableStateOf(TextFieldValue(preset.title)) }
    var secondsPerRep by remember { mutableIntStateOf(preset.secondsPerRep) }
    var reps by remember { mutableIntStateOf(preset.reps) }
    var restSeconds by remember { mutableIntStateOf(preset.restSeconds) }
    var sets by remember { mutableIntStateOf(preset.sets) }
    var prepTime by remember { mutableIntStateOf(preset.prepTime) }
    var showNameError by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Редактирование таймера", fontSize = 22.sp)
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    showNameError = !isNameUnique(it.text)
                },
                label = { Text("Название таймера") },
                isError = showNameError
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Длительность повторения:", modifier = Modifier.weight(1f))
                DropdownMenuBox(
                    selected = secondsPerRep,
                    options = listOf(6, 8, 10),
                    onSelect = { secondsPerRep = it }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Количество повторений:", modifier = Modifier.weight(1f))
                NumberPicker(value = reps, onValueChange = { reps = it }, range = 1..20)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Длительность отдыха:", modifier = Modifier.weight(1f))
                NumberPicker(value = restSeconds, onValueChange = { restSeconds = it }, range = 1..300)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Количество подходов:", modifier = Modifier.weight(1f))
                NumberPicker(value = sets, onValueChange = { sets = it }, range = 1..20)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Время подготовки:", modifier = Modifier.weight(1f))
                NumberPicker(value = prepTime, onValueChange = { prepTime = it }, range = 0..60)
            }
            if (showNameError) {
                Text("Имя таймера должно быть уникальным!", color = MaterialTheme.colorScheme.error)
            }
        }
        // Добавлен вертикальный интервал между последней строкой и рядом кнопок
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
            }
            IconButton(onClick = {
                if (!showNameError && title.text.isNotBlank()) {
                    Log.i("EditLog", "Saving timer: ${title.text}")
                    onSave(
                        TimerPreset(
                            preset.id, // сохраняем исходный id таймера
                            title.text,
                            secondsPerRep,
                            reps,
                            restSeconds,
                            sets,
                            prepTime
                        )
                    )
                }
            }) {
                Icon(Icons.Default.Check, contentDescription = "Сохранить")
            }
        }
        IconButton(
            onClick = { onCancel() },
            modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
        }
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Удалить таймер?") },
                text = { Text("Вы уверены, что хотите удалить таймер \"${title.text}\"?") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        onDelete(
                            TimerPreset(
                                preset.id, // сохраняем исходный id таймера
                                title.text,
                                secondsPerRep,
                                reps,
                                restSeconds,
                                sets,
                                prepTime
                            )
                        )
                    }) { Text("Удалить") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") }
                }
            )
        }
    }
}

@Composable
fun DropdownMenuBox(selected: Int, options: List<Int>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) {
            Text("$selected сек")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text("$it сек") }, onClick = {
                    onSelect(it)
                    expanded = false
                })
            }
        }
    }
}

@Composable
fun NumberPicker(value: Int, onValueChange: (Int) -> Unit, range: IntRange) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { if (value > range.first) onValueChange(value - 1) }) {
            Text("-")
        }
        Text("$value", fontSize = 18.sp, modifier = Modifier.width(32.dp))
        IconButton(onClick = { if (value < range.last) onValueChange(value + 1) }) {
            Text("+")
        }
    }
}
