package nestdrop

import osc.OSCMessage
import osc.nestdropPortSend

suspend fun nestdropSetPreset(id: Int, deck: Int, hardcut: Boolean = false) {
    nestdropPortSend(
        OSCMessage("/PresetID/$id/Deck$deck", if (hardcut) 0 else 1)
    )
}