package nestdrop

import kotlinx.datetime.LocalDateTime

data class PerformanceLogRow(
    val dateTime: LocalDateTime,
    val preset: String,
    val deck: Int,
)