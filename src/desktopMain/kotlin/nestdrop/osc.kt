package nestdrop

import io.github.oshai.kotlinlogging.KotlinLogging
import osc.OSCMessage
import osc.nestdropPortSend

suspend fun nestdropSetPreset(id: Int, deck: Int, hardcut: Boolean = false) {
    nestdropPortSend(
        OSCMessage("/PresetID/$id/Deck$deck", if (hardcut) 0 else 1)
    )
}

enum class ImgMode(val code: Int) {
    Overlay(0),
    Nested(1),
}

private val logger = KotlinLogging.logger {}
suspend fun nestdropSetSprite(id: Int, deck: Int, overlayMode: ImgMode = ImgMode.Nested, single: Boolean = true) {
    val parameter = if (single) {
        overlayMode.code
    } else {
        10 + overlayMode.code
    }

    logger.info { "NESTDROP OUT: /PresetID/$id/Deck$deck $parameter" }
    nestdropPortSend(
        OSCMessage("/PresetID/$id/Deck$deck", parameter)
    )
}