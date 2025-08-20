package me.pakuula.beeper.util

import android.os.Parcel
import android.os.Parcelable

data class Work(
    val isPreparation: Boolean = true,
    val currentRep: Int = 1,
    val currentSet: Int = 1,
    val isRest: Boolean = false,
    val isFinished: Boolean = false,
    val maxRep: Int,
    val maxSet: Int
) : Parcelable {
    val isWorking = !isFinished && !isPreparation && !isRest

    init {
        checkInvariant()
    }

    fun checkInvariant() {
        assert(maxRep > 0) { "maxRep must be positive" }
        assert(maxSet > 0) { "maxSet must be positive" }
        // Add other consistency checks as needed
        assert(currentRep in 1..maxRep) { "currentRep must be between 1 and maxRep" }
        assert(currentSet in 1..maxSet) { "currentSet must be between 1 and maxSet" }

        assert(!isFinished || isVeryLastRep()) {
            "isVeryLastRep must be true if it is finished"
        }
        assert(!(isFinished && isPreparation)) {
            "isFinished cannot be true if it is in preparation phase"
        }
        assert(!(isFinished && isRest)) {
            "isFinished cannot be true if it is in rest phase"
        }
        assert(!(isPreparation && isRest)) {
            "isPreparation cannot be true if it is in rest phase"
        }
        assert(!isPreparation || (currentRep == 1 && currentSet == 1)) {
            "currentRep and currentSet must be 1 if it is in preparation phase"
        }
    }

    fun isLastRep() = currentRep == maxRep
    fun isLastSet() = currentSet == maxSet
    fun isVeryLastRep() = isLastSet() && isLastRep()

    fun setIsFinished(finished: Boolean): Work {
        return this.copy(isFinished = finished)
    }

    fun setIsPreparation(preparation: Boolean): Work {
        return this.copy(isPreparation = preparation)
    }

    fun setIsRest(rest: Boolean): Work {
        return this.copy(isRest = rest)
    }

    fun next(): Work {
        if (isFinished) {
            return this
        } else if (isPreparation) {
            // Если мы в подготовительном режиме, то просто переходим к первому повтору
            // Опираемся на выполнение инвариантов из init
            return setIsPreparation(false)
        } else if (isVeryLastRep()) {
            return setIsFinished(true)
        } else if (isRest) {
            // Если мы в режиме отдыха, то просто переходим к первому повтору следующего подхода
            // Опираемся на выполнение инвариантов из init
            return setIsRest(false)
        } else if (isLastRep()) {
            return this.copy(currentRep = 1, currentSet = currentSet + 1, isRest = true)
        } else {
            return this.copy(currentRep = currentRep + 1)
        }
    }

    fun prev(): Work {
        if (isPreparation) {
            return this
        }
        if (isFinished) {
            // Возвращаемся к последнему подходу, последнему повтору
            return this.copy(
                isFinished = false,
                currentSet = maxSet,
                currentRep = maxRep,
                isRest = false,
                isPreparation = false,
            )
        }
        if (isRest) {
            // Предполагается, что при isRest = true счетчик повторов == 1, а счетчик подходов > 1
            return this.copy(
                isRest = false,
                currentRep = maxRep,
                currentSet = if (currentSet > 1) currentSet - 1 else 1,
            )
        }

        if (currentRep > 1) {
            return this.copy(currentRep = currentRep - 1)
        }
        // Переходим в паузу, если мы на первом повторе
        if (currentSet > 1) {
            return setIsRest(true)
        }
        // currentSet == 1 && currentRep == 1
        // уже на первом повторе первого подхода
        // Возврата к подготовительному этапу не предусмотрено
        return this
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (isPreparation) 1 else 0)
        parcel.writeInt(currentRep)
        parcel.writeInt(currentSet)
        parcel.writeByte(if (isRest) 1 else 0)
        parcel.writeByte(if (isFinished) 1 else 0)
        parcel.writeInt(maxRep)
        parcel.writeInt(maxSet)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Work> {
        override fun createFromParcel(parcel: Parcel): Work {
            return Work(
                isPreparation = parcel.readByte().toInt() != 0,
                currentRep = parcel.readInt(),
                currentSet = parcel.readInt(),
                isRest = parcel.readByte().toInt() != 0,
                isFinished = parcel.readByte().toInt() != 0,
                maxRep = parcel.readInt(),
                maxSet = parcel.readInt()
            )
        }

        override fun newArray(size: Int): Array<Work?> = arrayOfNulls(size)

    }
}