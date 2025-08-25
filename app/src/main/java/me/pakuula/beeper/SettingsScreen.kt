package me.pakuula.beeper

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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