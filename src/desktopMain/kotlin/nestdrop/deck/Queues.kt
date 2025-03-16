package nestdrop.deck

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import nestdrop.Preset
import nestdrop.Queue
import osc.OSCMessage
import osc.nestdropSendChannel


suspend fun Queue<Preset>.setDeck(deck: Int) {
    //TODO: replace with nestdropControl
    nestdropSendChannel.send(
        OSCMessage("/Queue/$name/Deck", deck)
    )
}
suspend fun Queue<Preset>.setBeatoffset(bof: Float) {
    //TODO: replace with nestdropControl
    nestdropSendChannel.send(
        OSCMessage("/Queue/$name/sBof", bof)
    )
}
suspend fun Queue<Preset>.setBeatMultiplier(multiplier: Float) {
    //TODO: replace with nestdropControl
    nestdropSendChannel.send(
        OSCMessage("/Queue/$name/sBmul", multiplier)
    )
}

class Queues {
    val presetQueues = MutableStateFlow<List<Queue<Preset.Milkdrop>>>(emptyList())
    val allQueues = MutableStateFlow<List<Queue<out Preset>>>(emptyList())
//    val openQueues = MutableStateFlow<List<Queue>>(emptyList())
    val isInitialized = MutableStateFlow(false)
    private val logger = KotlinLogging.logger { }

//    private val deckSwitches = List(20) { MutableStateFlow(0) }

    suspend fun startFlows() {
        logger.info { "starting coroutines for preset-queues" }
    }
}

/***
 * each preset queue is independent of decks
 * decks can change the deck of a preset queue
 *
 */
