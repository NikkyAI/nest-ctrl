package nestdrop

import com.github.doyaaaaaken.kotlincsv.dsl.context.ExcessFieldsRowBehaviour
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.util.CSVParseFormatException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import utils.checkNotNullDebug
import utils.errorDebug
import java.io.File

val performanceLogsFlow = MutableSharedFlow<PerformanceLogRow>(replay = 20, extraBufferCapacity = 8)

private val logger = KotlinLogging.logger { }

val csvReader = csvReader {
    excessFieldsRowBehaviour = ExcessFieldsRowBehaviour.TRIM
}

suspend fun parsePerformanceLog(file: File): List<PerformanceLogRow>? {
    val fileDateTime = try {
        val (day, month, year) = file.nameWithoutExtension.substringBefore(' ')
            .split('-')
            .map { it.toInt() }
        val (hour, minute) = file.nameWithoutExtension.substringAfter(' ')
            .split('h')
            .map { it.toInt() }
        LocalDateTime(
            LocalDate(
                year = year,
                monthNumber = month,
                dayOfMonth = day,
            ),
            LocalTime(
                hour = hour,
                minute = minute,
            )
        )
    } catch (e: NumberFormatException) {
        logger.error(e) { "failed to parse '${file.nameWithoutExtension}'" }
        null
    } ?: return null

    val rows = try {
        csvReader.openAsync(file) {
            readAllWithHeaderAsSequence()
                .asFlow()
                .map { row ->
                    row
                        .mapKeys { (k, _) -> k.trim() }
                        .mapValues { (_, v) -> v.trim() }
                }
                .map { row ->
                    try {
                        val time = checkNotNullDebug(row["Time"]) // .checkNotNullCustom()
                        val dateTime = try {
                            val (month, day, year) = time.substringBefore(' ')
                                .split('/')
                                .map { it.toInt() }
                            val (hour, minute, second) = time.substringAfter(' ')
                                .split(':')
                                .map { it.toInt() }
                            LocalDateTime(
                                LocalDate(
                                    year = year,
                                    monthNumber = month,
                                    dayOfMonth = day,
                                ),
                                LocalTime(
                                    hour = hour,
                                    minute = minute,
                                    second = second,
                                )
                            )
                        } catch (e: NumberFormatException) {
                            logger.error(e) { "failed to parse $time" }
                            null
                        } ?: error("failed to parse datetime")

                        val deck = checkNotNullDebug(row["Deck"])
                        require(deck in (1..4).map { "Deck$it" })
                        val deckNumber = when (deck) {
                            "Deck1" -> 1
                            "Deck2" -> 2
                            "Deck3" -> 3
                            "Deck4" -> 4

                            else -> errorDebug("unknown deck '$deck'")
                        }

                        PerformanceLogRow(
                            dateTime = dateTime,
                            preset = checkNotNullDebug(row["Preset"]),
//                            spriteLinked = row["Sprite linked"]?.takeUnless { it.isEmpty() },
                            deck = deckNumber
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "failed to parse history row '$row'" }
                        throw e
                    }
                }.catch { e ->
                    logger.error(e) { "failed to parse" }
                    throw e
                }.onEach {
//                    logger.infoF { it }
                }
                .toList()
        }
    } catch (e: CSVParseFormatException) {
        logger.error(e) { "failed to parse file $file" }
        return null
    }
    return rows
}

suspend fun startWatchingFile() {

}