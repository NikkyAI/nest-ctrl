package obs

import io.github.oshai.kotlinlogging.KotlinLogging
import io.obswebsocket.community.client.OBSRemoteController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

val obsRemote by lazy {
    OBSRemoteController.builder() // set options, register for events, etc.
        // continue reading for more information

        .lifecycle()
        .onReady {
            onReady()
        }
        .and()
        .autoConnect(false)
        .build().also {
            logger.info { "obs remote connecting" }
            it.connect()
            logger.info { "obs remote connected" }
        }
}

private fun onReady() {
    logger.info { "obs remote is ready" }
//    obsRemote.connect()
//    obsRemote.startStream(1000L)
}

suspend fun setLumaKey(enabled: Boolean) = withContext(Dispatchers.LOOM) {
    val response = obsRemote.setSourceFilterEnabled("mpv_scene", "Luma Key", enabled, 1000L)
    response.isSuccessful
}
suspend fun startStream() = withContext(Dispatchers.LOOM) {
    obsRemote.startStream(5_000L).isSuccessful
}
suspend fun stopStream() = withContext(Dispatchers.LOOM) {
    obsRemote.stopStream(5_000L).isSuccessful
}
suspend fun startRecord() = withContext(Dispatchers.LOOM) {
    obsRemote.startRecord(5_000L).isSuccessful
}
suspend fun stopRecord() = withContext(Dispatchers.LOOM) {
    obsRemote.stopRecord(5_000L).isSuccessful
}
suspend fun openProjector() = withContext(Dispatchers.LOOM) {
    obsRemote.openSourceProjector("mpv_scene", null, null, 5_000L).isSuccessful
}

fun doThing() {
    obsRemote.connect()
}
