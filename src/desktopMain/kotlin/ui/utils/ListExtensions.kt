package ui.utils

import androidx.compose.runtime.snapshots.SnapshotStateList

fun <T> SnapshotStateList<T>.replace(items: List<T>) {
    val oldSize = size
    addAll(0, items)
    if(oldSize > 1)
    removeRange(items.size, items.size+oldSize)
}