package logging

import io.klogging.events.LogEvent
import io.klogging.rendering.RenderString
import io.klogging.rendering.colour5
import io.klogging.rendering.evalTemplate
import io.klogging.rendering.localString
import io.klogging.sending.SendString
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File

val DOCKER_RENDERER: RenderString = object: RenderString {
    override fun invoke(event: LogEvent): String {
        val loggerOrFile = event.items["file"] ?: event.logger
        val message = "${event.level.colour5} $loggerOrFile : ${event.evalTemplate()}"
        val cleanedItems = event.items - "file"
        val maybeItems = if (cleanedItems.isNotEmpty()) " : $cleanedItems" else ""
        val maybeStackTrace = if (event.stackTrace != null) "\n${event.stackTrace}" else ""
        return message + maybeItems + maybeStackTrace
    }
}

val CUSTOM_RENDERER: RenderString = object: RenderString {
    override fun invoke(event: LogEvent): String {
        val loggerOrFile = event.items["file"]?.let { ".($it)" } ?: event.logger
        val time = event.timestamp.localString.substring(0..22)
        val message = "$time ${event.level} $loggerOrFile : ${event.evalTemplate()}"
        val cleanedItems = event.items - "file"
        val maybeItems = if (cleanedItems.isNotEmpty()) " : $cleanedItems" else ""
        val maybeStackTrace = if (event.stackTrace != null) "\n${event.stackTrace}" else ""
        return message + maybeItems + maybeStackTrace
    }
}
val CUSTOM_RENDERER_ANSI: RenderString = object: RenderString {
    override fun invoke(event: LogEvent): String {
        val loggerOrFile = event.items["file"]?.let { ".($it)" } ?: event.logger
        val time = event.timestamp.localString.substring(0..22)
        val message = "$time ${event.level.colour5} $loggerOrFile : ${event.evalTemplate()}"
        val cleanedItems = event.items - "file"
        val maybeItems = if (cleanedItems.isNotEmpty()) " : $cleanedItems" else ""
        val maybeStackTrace = if (event.stackTrace != null) "\n${event.stackTrace}" else ""
        return message + maybeItems + maybeStackTrace
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
val logScope = CoroutineScope(
    Dispatchers.IO.limitedParallelism(10) + CoroutineName("log")
)

class LogFile(
    file: File,
    append: Boolean = false
): SendString {
    private val channel = Channel<String>(capacity = Channel.BUFFERED)
    init {
        runBlocking {
            file.parentFile.mkdirs()
            if(!append && file.exists()) {
                file.delete()
                withContext(Dispatchers.IO) {
                    file.createNewFile()
                }
            } else if(!file.exists()) {
                withContext(Dispatchers.IO) {
                    file.createNewFile()
                }
            }
            logScope.launch {
                file.writeChannel().use {
                    for (line in channel) {
                        writeStringUtf8(line + "\n")
                    }
                }
            }
        }
    }

    override fun invoke(eventString: String) {
        logScope.launch {
            channel.send(eventString)
        }
    }
}

suspend fun logFile(file: File, append: Boolean = false): SendString {
    file.parentFile.mkdirs()
    if(!append && file.exists()) {
        file.delete()
        withContext(Dispatchers.IO) {
            file.createNewFile()
        }
    } else if(!file.exists()) {
        withContext(Dispatchers.IO) {
            file.createNewFile()
        }
    }

    val channel = Channel<String>()
    logScope.launch {
        file.writeChannel().use {
             for (line in channel) {
                 writeStringUtf8(line + "\n")
             }
        }
    }

    return object : SendString {
        override fun invoke(eventString: String) {
            logScope.launch {
                channel.send(eventString)
            }
        }
    }
}