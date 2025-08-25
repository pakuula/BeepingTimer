package me.pakuula.beeper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimerEditScreen(
    preset: TimerPreset,
    onSave: (TimerPreset) -> Unit,
    onDelete: (TimerPreset) -> Unit,
    isNameUnique: (String) -> Boolean,
    isNew: Boolean // добавлен параметр
) {
    var title by remember { mutableStateOf(TextFieldValue(preset.title)) }
    var secondsPerRep by remember { mutableIntStateOf(preset.secondsPerRep) }
    var reps by remember { mutableIntStateOf(preset.reps) }
    var restSeconds by remember { mutableIntStateOf(preset.restSeconds) }
    var sets by remember { mutableIntStateOf(preset.sets) }
    var showNameError by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = if (isNew) stringResource(R.string.edit_timer_new) else stringResource(R.string.edit_timer_edit), fontSize = 22.sp)
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    showNameError = !isNameUnique(it.text)
                },
                label = { Text(stringResource(R.string.edit_timer_title)) },
                isError = showNameError
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.edit_timer_repetition_time), modifier = Modifier.weight(1f))
                DropdownMenuBox(
                    selected = secondsPerRep,
                    options = listOf(6, 8, 10),
                    onSelect = { secondsPerRep = it }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.edit_timer_rep_number), modifier = Modifier.weight(1f))
                NumberPicker(value = reps, onValueChange = { reps = it }, range = 1..20)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.edit_timer_rest_seconds), modifier = Modifier.weight(1f))
                NumberPicker(value = restSeconds, onValueChange = { restSeconds = it }, range = 1..300)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.edit_timer_set_number), modifier = Modifier.weight(1f))
                NumberPicker(value = sets, onValueChange = { sets = it }, range = 1..20)
            }

            if (showNameError) {
                Text(stringResource(R.string.edit_timer_unique_title_error), color = MaterialTheme.colorScheme.error)
            }
        }
        // Добавлен вертикальный интервал между последней строкой и рядом кнопок
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isNew) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
            IconButton(onClick = {
                if (!showNameError && title.text.isNotBlank()) {
                    onSave(
                        TimerPreset(
                            preset.id, // сохраняем исходный id таймера
                            title.text,
                            secondsPerRep,
                            reps,
                            restSeconds,
                            sets,
                        )
                    )
                }
            }) {
                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save))
            }
        }
        if (showDeleteConfirm && !isNew) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(stringResource(R.string.delete_timer_question)) },
                text = { Text(stringResource(R.string.delete_timer_question2, title.text)) },
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
                            )
                        )
                    }) { Text(stringResource(R.string.delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }
}

@Composable
fun DropdownMenuBox(selected: Int, options: List<Int>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        val unit = stringResource(R.string.sec_short)
        Button(onClick = { expanded = true }) {
            Text("$selected $unit")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text("$it $unit") }, onClick = {
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
