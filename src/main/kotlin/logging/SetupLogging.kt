package logging

import io.klogging.Level
import io.klogging.config.LoggingConfig
import io.klogging.config.loggingConfiguration
import io.klogging.sending.STDOUT
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun setupLogging() {
    val latestFile = LogFile(File("logs/latest.log"))
    val latestTrace = LogFile(File("logs/latest-trace.log"))
    val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Date())
    val timestamped = LogFile(File("logs/log-$timestamp.log"))

    loggingConfiguration {
        val dockerLogging = false
        if (dockerLogging) {
            sink("stdout", DOCKER_RENDERER, STDOUT)
        } else {
            sink("stdout", CUSTOM_RENDERER_ANSI, STDOUT)
        }
        sink("file_latest", CUSTOM_RENDERER, latestFile)
        sink("file_latest_trace", CUSTOM_RENDERER, latestTrace)
        sink("file", CUSTOM_RENDERER, timestamped)
//        val seqServer = null // envOrNull("SEQ_SERVER") //?: "http://localhost:5341"
//        if (seqServer != null) {
//            sink("seq", seq(seqServer))
//        }
        fun LoggingConfig.applyFromMinLevel(level: Level) {
            fromMinLevel(level) {
                toSink("stdout")
//                if (seqServer != null) {
//                    toSink("seq")
//                }
                toSink("file_latest")
                toSink("file")
            }
        }
        logging {
            fromLoggerBase("moe.nikky", stopOnMatch = true)
            applyFromMinLevel(Level.DEBUG)
            fromMinLevel(Level.TRACE) {
                toSink("file_latest_trace")
            }
        }
        logging {
            //TODO: fix logger matcher
            exactLogger("\\Q[R]:[KTOR]:[ExclusionRequestRateLimiter]\\E", stopOnMatch = true)
            applyFromMinLevel(Level.INFO)
        }
        logging {
            applyFromMinLevel(Level.INFO)
        }
    }
}