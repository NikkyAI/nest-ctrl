package nestdrop

import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCPacket
import flowScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import nestdrop.deck.Deck

sealed interface PresetIdState {
//    val queue: Queue<out Preset>? get() = null
    val index: Int? get() = null

    data class Data(
        override val index: Int,
        val force: Boolean = false,
//    val hardCut: Boolean = false,
    ) : PresetIdState

    data object Unset : PresetIdState
}


class NestdropSpriteQueue(
    private val nestdropSendChannel: Channel<OSCPacket>,
    private val spoutStateMap: StateFlow<Map<String, Deck.SpriteKey>>,
    private val queue: StateFlow<Queue<out Preset>?>,
    private val matchFx: Boolean = true,
) {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val channel: Channel<PresetIdState> = Channel()

    suspend fun send(
        state: PresetIdState
    ) {
        channel.send(state)
    }

    private suspend fun presetId(queue: Queue<out Preset>, index: Int?, overlay: Boolean = false) {
        if (index == null) {
            logger.warn { "failed to find sprite id" }
            return
        }
        logger.debug { "setting index $index on ${queue.name} (/Queue/${queue.name}/ActIdx/$index)" }
//        logger.debug { "setting presetId $index on ${queue.name} (\"/PresetID/${queue.name}/$id\")" }
        nestdropSendChannel.send(
            OSCMessage(
                // /PresetID/spout_1/15879 or /Queue/spout_1/ActIdx/0
                "/Queue/${queue.name}/ActIdx/$index",
//                "/PresetID/${queue.name}/$id",
//                "/PresetID/$id",
                listOf(
                    if (overlay) 0 else 1
                )
            )
        )
    }

    suspend fun startFlows() {
        channel
            .consumeAsFlow()
//            .runningHistory(PresetIdState.Unset)
//            .filterNotNull()
            .combine(
                queue.filterNotNull()
            ) { current, queue ->
                val currentActive = spoutStateMap.value.values.toSet()
//                logger.warnF { "previous: $previous" }
//                logger.warnF { "current: $current" }
//                if (previous != null) {
                when (current) {
                    is PresetIdState.Unset -> {
                        currentActive.forEach { key ->
                            logger.debug { "unsetting previous sprite $key" }
                            presetId(
                                queue = queue,
                                index = queue.presets.indexOfFirst { preset ->
                                    preset.name == key.name && if(!matchFx) true else when(preset) {
                                        is Preset.Sprite -> {
                                            preset.effects == key.fx
                                        }
                                        else -> true
                                    }

                                },
                                overlay = false
                            )
                        }
                    }

                    is PresetIdState.Data -> {
//                        require(current.queue.name == queue.name) { "queue does not match" }
                        if (current.force) {
                            logger.debug { "force setting sprite" }
                            presetId(
                                queue = queue,
                                index = (current.index + 1) % queue.presets.size,
                                overlay = false
                            )
                            delay(25)
                            presetId(queue, current.index, overlay = false)
                        } else {
                            val presetName = queue.presets.getOrNull(current.index)
                            if (currentActive.any { it.id == current.index }) {
                                logger.info { "ND: received same preset id again, doing nothing" }
//                                logger.info { "ND: received same preset id again, resetting ${queue.name} to $presetName" }
//                                presetId(
//                                    queue = queue,
//                                    index = (current.index + 1) % queue.presets.size,
////                                        current.queue.presets.first { it.id != current.id }.id,
//                                    overlay = false
//                                )
//                                delay(50)
//                                presetId(queue, current.index, overlay = false)
                            } else {
                                logger.info { "ND: switching ${queue.name} to '$presetName'" }
                                presetId(queue, current.index, overlay = false)
                            }

                        }
                    }
                }

            }
            .launchIn(flowScope)
    }

}

