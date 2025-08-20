package me.pakuula.beeper

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.pakuula.beeper.util.Work

class ExerciseViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val paramPreparationSeconds: Int,
    private val paramRestSeconds: Int,
    private val paramSecondsPerRep: Int
) : ViewModel() {
    // Состояние паузы
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    // Ключ для сохранения состояния workInfo
    private val workInfoKey = "workInfo"

    // Состояние работы упражнения через Parcelable
    private val _workInfo = MutableStateFlow(
        savedStateHandle.get<Work>(workInfoKey)
            ?: Work(
                isPreparation = false, // Значение по умолчанию, инициализируется позже
                maxRep = 8,
                maxSet = 4,
                currentRep = 1,
                currentSet = 1,
                isRest = false,
                isFinished = false
            )
    )
    val workInfo: StateFlow<Work> = _workInfo

    // Таймер оставшегося времени
    private val _timeLeft = MutableStateFlow(
        when {
            _workInfo.value.isPreparation -> paramPreparationSeconds
            _workInfo.value.isRest -> paramRestSeconds
            _workInfo.value.isFinished -> 0
            else -> paramSecondsPerRep
        }
    )
    val timeLeft: StateFlow<Int> = _timeLeft

    // Метод для установки значения таймера
    fun setTimeLeft(value: Int) {
        _timeLeft.value = value
    }

    fun resetTimeLeft() {
        _timeLeft.value = when {
            _workInfo.value.isPreparation -> paramPreparationSeconds
            _workInfo.value.isRest -> paramRestSeconds
            _workInfo.value.isFinished -> 0
            else -> paramSecondsPerRep
        }
    }

    fun isPositiveTimeLeft(): Boolean {
        return _timeLeft.value > 0
    }

    fun decreaseTimeLeft() {
        if (_timeLeft.value > 0) {
            _timeLeft.value -= 1
        }
    }

    // Инициализация workInfo
    fun initWorkInfo(
        isPreparation: Boolean,
        maxRep: Int,
        maxSet: Int
    ) {
        val newWork = Work(
            isPreparation = isPreparation,
            maxRep = maxRep,
            maxSet = maxSet,
            currentRep = 1,
            currentSet = 1,
            isRest = false,
            isFinished = false
        )
        _workInfo.value = newWork
        savedStateHandle[workInfoKey] = newWork
    }

    fun togglePaused() {
        _isPaused.value = !_isPaused.value
    }

    // Перемотка workInfo вперёд
    fun nextWork() {
        setWorkInfo(_workInfo.value.next())
    }

    // Перемотка workInfo назад
    fun prevWork() {
        setWorkInfo(_workInfo.value.prev())
    }

    // Прямое присваивание workInfo
    fun setWorkInfo(newWork: Work) {
        _workInfo.value = newWork
        savedStateHandle[workInfoKey] = newWork
        // Обновление таймера
        resetTimeLeft()
    }


    fun isWorkInfoInitialized(): Boolean {
        return savedStateHandle.get<Work>(workInfoKey) != null
    }
    // ...остальные переменные и методы по аналогии...

    private val _muteIconRequested: MutableStateFlow<Int> = MutableStateFlow(0)
    val muteIconRequested: StateFlow<Int> = _muteIconRequested

    fun setMuteIconRequested(request: Int) {
        _muteIconRequested.value = request
    }
    fun resetMuteIconRequested() {
        _muteIconRequested.value = 0
    }
    fun isMuteIconRequested(): Boolean {
        return _muteIconRequested.value != 0
    }
    fun increaseMuteIconRequested() {
        _muteIconRequested.value += 1
    }

    private val _showMuteIcon: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showMuteIcon: StateFlow<Boolean> = _showMuteIcon
    fun setShowMuteIcon(show: Boolean) {
        _showMuteIcon.value = show
    }
    fun resetShowMuteIcon() {
        _showMuteIcon.value = false
    }
    fun toggleShowMuteIcon() {
        _showMuteIcon.value = !_showMuteIcon.value
    }
}

class ExerciseViewModelFactory(
    private val paramPreparationSeconds: Int,
    private val paramRestSeconds: Int,
    private val paramSecondsPerRep: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ExerciseViewModel(
            savedStateHandle = SavedStateHandle(),
            paramPreparationSeconds = paramPreparationSeconds,
            paramRestSeconds = paramRestSeconds,
            paramSecondsPerRep = paramSecondsPerRep
        ) as T
    }
}