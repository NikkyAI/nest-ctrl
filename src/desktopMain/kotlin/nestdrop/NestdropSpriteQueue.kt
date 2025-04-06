package nestdrop

import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCPacket
import flowScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

sealed interface PresetIdState {
    val queue: Queue<out Preset>? get() = null
//    val index: Int? get() = null
    val index: Int? get() = null
    data class Data(
//        override val index: Int,
        override val index: Int,
        override val queue: Queue<out Preset>,
        val force: Boolean = false,
//    val hardCut: Boolean = false,
    ) : PresetIdState {
//        val id get() = queue.presets.getOrNull(index)?.id
    }

    data object Unset : PresetIdState
}


class NestdropSpriteQueue(
    private val nestdropSendChannel: Channel<OSCPacket>,
    private val onChange: suspend (Int) -> Unit = {}
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
        if(index == null) {
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
        var previous: PresetIdState? = null
        channel
            .consumeAsFlow()
//            .runningHistory(PresetIdState.Unset)
//            .filterNotNull()
            .onEach { current ->
//                logger.warnF { "previous: $previous" }
//                logger.warnF { "current: $current" }
//                if (previous != null) {
                    when (current) {
                        is PresetIdState.Unset -> {
                            when (val previous = previous) {
                                is PresetIdState.Data -> {
                                    if (previous.queue.type == QueueType.SPRITE) {
                                        // to unset: send last index again
                                        logger.debug { "unsetting previous sprite" }
                                        presetId(
                                            queue = previous.queue,
                                            index = previous.index,
                                            overlay = false
                                        )
                                    } else {
                                        logger.error { "previous queue was not a spout/sprite queue: previous: $previous" }
                                    }
                                }

                                is PresetIdState.Unset -> {
                                    // do nothing
                                }

                                null -> {}
                            }

                        }

                        is PresetIdState.Data -> {
                            if (current.force) {
                                logger.debug { "force setting sprite" }
                                presetId(
                                    queue = current.queue,
                                    index = current.index,
//                                    current.queue.presets.first { it.id != current.id }.id,
//                                    (current.index + 1) % current.queue.presets.size,
                                    overlay = false
                                )
                                delay(25)
                                presetId(current.queue, current.index, overlay = false)
                            } else {

                                val presetName = current.queue.presets.getOrNull(current.index)
                                val previousId = previous?.index
                                val previousPresetName = previousId?.let {
                                    previous?.queue?.presets?.getOrNull(it)
                                }
                                if (current.index == previousId) {
                                    logger.info { "ND: received same preset id again, resetting ${current.queue.name} to $presetName" }
                                    presetId(
                                        queue = current.queue,
                                        index = current.index,
//                                        current.queue.presets.first { it.id != current.id }.id,
                                        overlay = false
                                    )
//                            presetId(current.queue, current.index)
                                    delay(50)
                                    presetId(current.queue, current.index, overlay = false)
                                    onChange(current.index)
                                } else {
                                    logger.info { "ND: switching ${current.queue.name} to '$presetName' (before: $previousPresetName)" }
                                    presetId(current.queue, current.index, overlay = false)
                                    onChange(current.index)
                                }

                            }
                        }
                    }
//                } else {
//                    logger.debug { "not switching after initializing program" }
//
//                    //TODO switch to another preset and back to ensure it is set correctly
//                }
                previous = current

            }
            .launchIn(flowScope)
    }

}

