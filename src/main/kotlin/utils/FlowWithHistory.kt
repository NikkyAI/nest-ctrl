package utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.runningFold

data class History<T>(val current: T, val previous: T?)
data class HistoryNotNull<T>(val current: T, val previous: T)

// emits null, History(null,1), History(1,2)...
//fun <T> Flow<T>.runningHistory(): Flow<History<T>?> =
//    runningFold(
//        initial = null as (History<T>?),
//        operation = { accumulator, new -> History(new, accumulator?.current) }
//    )
// emits History(1,1), History(2, 1)...
fun <T> Flow<T>.runningHistory(initialValue: T): Flow<History<T>> =
    runningFold(
        initial = null as History<T>?,
        operation = { accumulator, new -> History(current = new, previous = accumulator?.current) }
    ).filterNotNull()

//fun <T : Any> Flow<History<T>>.onEachItem(onEach: suspend (T, T) -> Unit): Flow<History<T>> =
//
//    onEach {
//        onEach(it.current, it.previous)
//    }
fun <T> Flow<T>.runningHistoryNotNull(initialValue: T): Flow<HistoryNotNull<T>> =
    runningFold(
        initial = HistoryNotNull(initialValue, initialValue),
        operation = { accumulator, new -> HistoryNotNull(current = new, previous = accumulator.current) }
    ).drop(1)

fun <T: Any> StateFlow<T>.runningHistoryNotNull() = runningHistoryNotNull(value)