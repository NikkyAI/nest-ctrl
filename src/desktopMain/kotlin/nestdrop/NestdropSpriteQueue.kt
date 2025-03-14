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
    val queue: Queue? get() = null
    val index: Int? get() = null
    data class Data(
        override val index: Int,
        override val queue: Queue,
        val force: Boolean = false,
//    val hardCut: Boolean = false,
    ) : PresetIdState

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

    private suspend fun presetId(queue: Queue, index: Int, overlay: Boolean = false) {
        logger.debug { "presetId $index on ${queue.name} (\"/PresetID/${queue.name}/$index\")" }
        nestdropSendChannel.send(
            OSCMessage(
                "/PresetID/${queue.name}/$index",
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
                if (previous != null) {
                    when (current) {
                        is PresetIdState.Unset -> {
                            when (val previous = previous) {
                                is PresetIdState.Data -> {
                                    if (previous.queue.type == QueueType.Sprite) {
                                        // to unset: send last index again
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
                                presetId(
                                    current.queue,
                                    (current.index + 1) % current.queue.presets.size,
                                    overlay = false
                                )
                                delay(25)
                                presetId(current.queue, current.index, overlay = false)
                            } else {

                                val presetName = current.queue.presets.getOrNull(current.index)
                                val previousIndex = previous?.index
                                val previousPresetName = previousIndex?.let {
                                    previous?.queue?.presets?.getOrNull(it)
                                }
                                if (current.index == previousIndex) {
                                    logger.info { "ND: received same preset id again, resetting ${current.queue.name} to $presetName" }
                                    presetId(
                                        current.queue,
                                        current.index + 1 % current.queue.presets.size,
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
                } else {
                    logger.debug { "not switching after initializing program" }

                    //TODO switch to another preset and back to ensure it is set correctly
                }
                previous = current

            }
            .launchIn(flowScope)
    }

}

