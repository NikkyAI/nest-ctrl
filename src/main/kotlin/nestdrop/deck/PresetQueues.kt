package nestdrop.deck

import flowScope
import io.klogging.logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logging.infoF
import nestdrop.Queue
import osc.OSCMessage
import osc.nestdropSendChannel


class PresetQueues(
    private val mutableQueues: MutableStateFlow<List<Queue>> = MutableStateFlow(emptyList())
) : StateFlow<List<Queue>> by mutableQueues {
    val allQueues = MutableStateFlow<List<Queue>>(emptyList())
    val queues = MutableStateFlow<List<Queue>>(emptyList())
    private val logger = logger(PresetQueues::class.qualifiedName!!)

    val deckSwitches = List(20) { MutableStateFlow( 0 ) }

    suspend fun startFlows() {
        logger.infoF { "initializing preset queues" }

        queues
            .combine(
                flow = deckSwitches.foldIndexed<MutableStateFlow<Int>, Flow<Map<Int, Int>>>(
                    flowOf(mapOf())
                ) { queueIndex, mapFlow, deckNumberFlow ->
                    //            logger.infoF { "foldIndexed $queueIndex" }
                    mapFlow.combine(deckNumberFlow) { map, deckNumber ->
                        //                logger.infoF { "foldIndexed combine $queueIndex" }
                        map + (queueIndex to deckNumber)
                    }
                }
            ) { queues, switchSates ->
                logger.infoF { "combining queues with deck switches" }
                queues.mapIndexed { i, queue ->
                    val deckOverride = switchSates[i]?.takeUnless { it < 0 }
                    if (deckOverride != null) {
                        queue.copy(deck = deckOverride)
                    } else {
                        queue
                    }
                }
            }
            .onEach {
                mutableQueues.value = it
                logger.infoF { "preset queues updated: ${it.joinToString { 
                    "name: ${it.name} deck: ${it.deck} presets: ${it.presets.size}"
                }}" }
            }
            .launchIn(flowScope)

        this
            .onEach { queues ->
                deckSwitches.forEachIndexed { index, stateFlow ->
                    stateFlow.value = queues.getOrNull(index)
                        ?.deck
                        ?: -1
                }
            }
            .launchIn(flowScope)

        //TODO: listen to deck switches and trigger nestdrop message `/Queue/$name/Deck ${index + 1}`
        deckSwitches.forEachIndexed { queueIndex, stateFlow ->
            stateFlow.filter { it > 0 }
                .onEach { deck ->
                    val queue = this.queues.value.getOrNull(queueIndex)
                    if (queue != null) {
                        nestdropSendChannel.send(OSCMessage("/Queue/${queue.name}/Deck", deck))
                    }
                }
                .launchIn(flowScope)
        }
    }
}

/***
 * each preset queue is independent of decks
 * decks can change the deck of a preset queue
 *
 */
