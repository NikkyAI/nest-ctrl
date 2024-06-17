package nestdrop

import kotlinx.coroutines.flow.MutableStateFlow
import com.github.doyaaaaaken.kotlincsv.dsl.context.ExcessFieldsRowBehaviour
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.util.CSVParseFormatException
import io.klogging.logger
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import logging.errorF
import java.io.File

val performanceLogsMap = MutableStateFlow<Map<String, List<PerformanceLogRow>>>(emptyMap())
val performanceLogs = MutableStateFlow<List<PerformanceLogRow>>(emptyList())

private val logger = logger("nestcontrol.performanceLogKt")

val csvReader = csvReader {
    excessFieldsRowBehaviour = ExcessFieldsRowBehaviour.TRIM
}
suspend fun parsePerformanceLog(file: File): List<PerformanceLogRow>? {
    val dateTime = try {
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
        logger.errorF(e) { "failed to parse '${file.nameWithoutExtension}'" }
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
                        val time = row["Time"] ?: error("no key 'Time' in $row")

                        PerformanceLogRow(
                            try {
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
                                logger.errorF(e) { "failed to parse $time" }
                                null
                            } ?: error("failed to parse datetime"),
                            row["Preset"] ?: error("no key 'Preset' in $row"),
                            deck = when (val deck = row["Deck"] ?: error("no key 'Deck' in $row")) {
                                "Deck1" -> 1
                                "Deck2" -> 2
                                "Deck3" -> 3
                                "Deck4" -> 4

                                else -> error("unknown deck '$deck'")
                            }
                        )
                    } catch (e: Exception) {
                        logger.errorF(e) { "failed to parse history row '$row'" }
                        throw e
                    }

                    //Do something
//                logger.warnF { row }

                }.catch { e ->
                    logger.errorF(e) { "failed to parse" }
                    throw e
                }.onEach {
//                    logger.infoF { it }
                }
                .toList()
        }
    } catch (e: CSVParseFormatException) {
        logger.errorF(e) { "failed to parse file $file" }
        return null
    }
    return rows
}