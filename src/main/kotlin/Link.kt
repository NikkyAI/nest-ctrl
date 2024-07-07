import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.deepsymmetry.libcarabiner.Message
import java.net.ConnectException
import java.net.SocketException

object Link {
    private val logger = KotlinLogging.logger { }
    val isConnected = MutableStateFlow(false)
    val bpm = MutableStateFlow(120f)
    val peers = MutableStateFlow(0)
    val beat = MutableStateFlow<Double>(0.0)
    suspend fun openConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            val selectorManager = SelectorManager(Dispatchers.IO)
            val socket = try {
                aSocket(selectorManager).tcp().connect("127.0.0.1", 17000)
            } catch (e: ConnectException) {
                logger.error(e) { "failed to connect to carabiner" }
                return@withContext true
            }
            logger.info { "connecting to ableton link..." }
            val readChannel = socket.openReadChannel()
//            val sendChannel = socket.openWriteChannel(autoFlush = false)

            try {
                isConnected.value = true
                while (true) {
                    val line = readChannel.readUTF8Line()
                    if (line != null) {
//                        logger.trace { "carabiner received: $line" }
                        if (line.startsWith("unsupported")) {
                            logger.warn { "carabiner: $line" }
                        } else {
                            try {
                                val message = Message(line)
                                logger.trace { "carabiner message: ${message.messageType} ${message.details}" }
//                                logger.debugF { "carabiner message: ${message.messageType} ${message.details}" }
                                if (message.details != null) {
//                                    message.details.forEach { (k, v) ->
//                                        logger.debugF { "$k: $v ${v::class.qualifiedName}" }
//                                    }
                                    val newBpm = message.details["bpm"] as? Double
                                    if (newBpm != null) {
                                        bpm.value = newBpm.toFloat()
                                    }
                                    val newPeers = message.details["peers"] as? Long
                                    if (newPeers != null) {
                                        peers.value = newPeers.toInt()
                                    }
                                    val newBeats = message.details["beat"] as? Double
                                    if (newBeats != null) {
                                        beat.value = newBeats
                                    }
                                }
                            } catch (e: Exception) {
                                logger.error(e) { "error decoding '$line'" }
                            }
                        }
                    } else {
                        logger.warn { "no line received, channel is closed?" }
                    }
//                    delay(10)
                }
            } catch (e: SocketException) {
                logger.error(e) { "lost connection to carabiner" }
            } finally {
                socket.close()
                selectorManager.close()
                isConnected.value = false
            }
            false
        }
    }
}