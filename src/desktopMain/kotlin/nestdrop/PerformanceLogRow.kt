package nestdrop

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class PerformanceLogRow(
    val dateTime: LocalDateTime,
    val preset: String,
//    val spriteLinked: String? = null,
    val deck: Int,
)