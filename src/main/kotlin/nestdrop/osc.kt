package nestdrop

import osc.OSCMessage
import osc.nestdropPortSend

suspend fun nestdropSetPreset(id: Int, deck: Int, hardcut: Boolean = false) {
    nestdropPortSend(
        OSCMessage("/PresetID/$id/Deck$deck", if (hardcut) 0 else 1)
    )
}

enum class OverlayMode {
    Overlay,
    Nested
}

suspend fun nestdropSetSprite(id: Int, deck: Int, overlayMode: OverlayMode = OverlayMode.Nested, single: Boolean = true) {
    val parameter = when(overlayMode) {
        OverlayMode.Overlay -> 0
        OverlayMode.Nested -> 1
    }

    nestdropPortSend(
        OSCMessage("/PresetID/$id/Deck$deck", if (single) parameter else 10 + parameter)
    )
}