package me.pakuula.beeper

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Модель пресета
data class TimerPreset(
    val id: String, // Уникальный идентификатор таймера
    val title: String, // Название пресета
    // Параметры таймера
    val secondsPerRep: Int, // Время на одно повторение в секундах
    val reps: Int, // Количество повторений
    val restSeconds: Int, // Время отдыха между повторениями в секундах
    val sets: Int, // Количество подходов
    val prepTime: Int = 7 // Время подготовки по умолчанию
)

@Composable
fun TimerPresetWidget(
    preset: TimerPreset,
    onStart: (TimerPreset) -> Unit,
    onEdit: (TimerPreset) -> Unit = {}
) {
    Log.i("EditLog", "TimerPresetWidget called for ${preset.title}")
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = preset.title,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onStart(preset) }
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropDown else Icons.Default.Add,
                        contentDescription = if (expanded) "Свернуть" else "Развернуть"
                    )
                }
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Одно повторение: ${preset.secondsPerRep} сек")
                Text("Число повторений: ${preset.reps}")
                Text("Число подходов: ${preset.sets}")
                Text("Подготовка: ${preset.prepTime} сек")
                Text("Отдых: ${preset.restSeconds} сек")
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { onEdit(preset) },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать"
                        )
                    }
                    IconButton(
                        onClick = { onStart(preset) },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Запустить"
                        )
                    }
                }
            }
        }
    }
}
