package nestdrop.deck

import flowScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.sample
import nestdrop.Preset
import nestdrop.Queue
import nestdrop.QueueType
import osc.OSCMessage
import osc.nestdropSendChannel
import scanFileSystemQueueForImgSprites
import scanFileSystemQueueForMilk
import kotlin.time.Duration.Companion.milliseconds


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

sealed interface OSCQueueUpdate {
    abstract val name: String

    data class UpdateQueue(
        override val name: String,
        val active: Boolean,
        val type: QueueType,
        val beatOffset: Float,
        val beatMultiplier: Float,
        val deckNumber: Int,
        val presetCount: Int,
    ) : OSCQueueUpdate

    data class Deck(
        override val name: String,
        val deckNumber: Int,
    ) : OSCQueueUpdate

    data class BeatOffset(
        override val name: String,
        val beatOffset: Float,
    ) : OSCQueueUpdate

    data class BeatMultiplier(
        override val name: String,
        val beatMultiplier: Float,
    ) : OSCQueueUpdate
}


class Queues {
//    private val presetQueuesInternal = MutableStateFlow<Map<String, Queue<Preset.Milkdrop>>>(emptyMap())
//    val presetQueues = presetQueuesInternal.asStateFlow()
//    private val spriteQueuesInternal = MutableStateFlow<Map<String, Queue<Preset.ImageSprite>>>(emptyMap())
//    val spriteQueues = spriteQueuesInternal.asStateFlow()
//    private val spoutQueuesInternal = MutableStateFlow<Map<String, Queue<Preset.SpoutSprite>>>(emptyMap())
//    val spoutQueues = spoutQueuesInternal.asStateFlow()

//    val allQueues = combine(
//        presetQueues,
//        spriteQueues,
//        spoutQueues
//    ) { a, b, c ->
//        a + b + c
//    }.stateIn(
//        scope = flowScope,
//        started = SharingStarted.WhileSubscribed(5000),
//        initialValue = emptyMap(),
//    )

    //    val allQueues = MutableStateFlow<List<Queue<out Preset>>>(emptyList())
//    val allQueues = mutableStateMapOf<String, Queue<out Preset>>()
    private val allQueuesInternal = MutableStateFlow<Map<String, Queue<out Preset>>>(emptyMap())
    val allQueues = allQueuesInternal.asStateFlow()

    val isInitialized = MutableStateFlow(false)
    private val logger = KotlinLogging.logger { }

//    private val deckSwitches = List(20) { MutableStateFlow(0) }

    //    private val presetQueuesFromConfig = MutableStateFlow<Map<String, Queue<Preset.Milkdrop>>>(emptyMap())
//    private val imgSpriteQueuesFromConfig = MutableStateFlow<Map<String, Queue<Preset.ImageSprite>>>(emptyMap())
//    private val spoutSpriteQueuesFromConfig = MutableStateFlow<Map<String, Queue<Preset.SpoutSprite>>>(emptyMap())
    private val queuesFromConfig = MutableStateFlow<Map<String, Queue<out Preset>>>(emptyMap())

    fun updateQueues(
        presetQueues: List<Queue<Preset.Milkdrop>>,
        imgSpriteQueues: List<Queue<Preset.ImageSprite>>,
        spoutSpriteQueues: List<Queue<Preset.SpoutSprite>>,
    ) {
        imgSpriteQueues.forEach {
            logger.info { "update IMG Queue from XML: $it" }
        }
        spoutSpriteQueues.forEach {
            logger.info { "update SPT Queue from XML: $it" }
        }
        queuesFromConfig.value =
            (presetQueues + imgSpriteQueues + spoutSpriteQueues).associateBy { it.name.replace(" ", "_") }
//        presetQueuesFromConfig.value = presetQueues.associateBy { it.name }
//        imgSpriteQueuesFromConfig.value = imgSpriteQueues.associateBy { it.name }
//        spoutSpriteQueuesFromConfig.value = spoutSpriteQueues.associateBy { it.name }
    }

    val updateQueueMessages = Channel<OSCQueueUpdate>()

    private fun <PRESET : Preset> Queue<PRESET>.updateQueue(update: OSCQueueUpdate): Queue<PRESET> {
//        logger.debug { "osc queue update: $update" }
        return when (update) {
            is OSCQueueUpdate.UpdateQueue -> {
                //TODO: when isFileExplorerQueue &&
                if (!isFileExplorer) {
                    copy(
                        type = update.type,
                        active = update.active,
                        beatOffset = update.beatOffset,
                        beatMultiplier = update.beatMultiplier,
                        deck = update.deckNumber,
                        presetCount = update.presetCount,
                    )
                } else {
                    when (type) {
                        QueueType.PRESET -> {
                            copy(
                                type = update.type,
                                active = update.active,
                                beatOffset = update.beatOffset,
                                beatMultiplier = update.beatMultiplier,
                                deck = update.deckNumber,
                                presets = //if(update.presetCount != presetCount) {
                                    scanFileSystemQueueForMilk(fileExplorerPath) as List<PRESET>
//                                } else {
//                                    presets
//                                }
                                ,
                                presetCount = update.presetCount,
                            )
                        }

                        QueueType.SPRITE -> {
                            copy(
                                type = update.type,
                                active = update.active,
                                beatOffset = update.beatOffset,
                                beatMultiplier = update.beatMultiplier,
                                deck = update.deckNumber,
                                presets = scanFileSystemQueueForImgSprites(fileExplorerPath) as List<PRESET>,
                                presetCount = update.presetCount,
                            )
                        }

                        else -> error("unknown queue type has IsFileExplorer=true $type")
                    }
                }
            }

            is OSCQueueUpdate.Deck -> copy(
                deck = update.deckNumber,
            )

            is OSCQueueUpdate.BeatOffset -> copy(
                beatOffset = update.beatOffset,
            )

            is OSCQueueUpdate.BeatMultiplier -> copy(
                beatMultiplier = update.beatMultiplier,
            )
        }
    }

    val combinedUpdates = MutableStateFlow<Map<String, OSCQueueUpdate.UpdateQueue>>(emptyMap())

    @OptIn(FlowPreview::class)
    suspend fun startFlows() {
        logger.info { "starting coroutines for preset-queues" }

        val updatesFlow = updateQueueMessages
            .consumeAsFlow()
            .onEach {
                logger.info { "queue update: $it" }
            }
            .runningFold(
                emptyMap<String, OSCQueueUpdate.UpdateQueue>()
            ) { cache, update ->
                val nextState = when (update) {
                    is OSCQueueUpdate.UpdateQueue -> update
                    else -> {
                        val currentState = cache[update.name] ?: run {
                            val queue = allQueues.value[update.name] ?: error("could not load queue ${update.name}")
                            OSCQueueUpdate.UpdateQueue(
                                update.name,
                                active = queue.active,
                                type = queue.type,
                                beatOffset = queue.beatOffset,
                                beatMultiplier = queue.beatMultiplier,
                                deckNumber = queue.deck,
                                presetCount = queue.presetCount
                            )
                        }
                        when(update) {
                            is OSCQueueUpdate.Deck -> currentState.copy(
                                deckNumber = update.deckNumber
                            )
                            is OSCQueueUpdate.BeatMultiplier -> currentState.copy(
                                beatMultiplier = update.beatMultiplier
                            )
                            is OSCQueueUpdate.BeatOffset -> currentState.copy(
                                beatOffset = update.beatOffset
                            )
                            else -> error("unreachable")
                        }
                    }
                }

                cache + (update.name to nextState)
            }

        combine(
            queuesFromConfig,
//            presetQueuesFromConfig,
//            imgSpriteQueuesFromConfig,
//            spoutSpriteQueuesFromConfig,
//            updateQueueMessages.consumeAsFlow(),
            updatesFlow.sample(100.milliseconds),
        ) { queues, /*imgSprites, spoutSprites,*/ updatesMap ->
            logger.info { "applying updates ${updatesMap.entries.joinToString(",", "{", "}") { "${it.key}: ${it.value}" }}" }
            allQueuesInternal.value = queues.mapValues { (name, queue) ->
                updatesMap[name]?.let { update ->
                    queue.updateQueue(update)
                } ?: queue
            }

//            val queueUpdate = queues[updatesMap.name]?.updateQueue(updatesMap)
//            val presetQueueUpdate = presets[update.name]?.updateQueue(update)
//            val queueImgSpriteUpdate = imgSprites[update.name]?.updateQueue(update)
//            val spoutQueueUpdate = spoutSprites[update.name]?.updateQueue(update)

//            allQueuesInternal.value = if (queueUpdate != null) {
//                queues + (updatesMap.name.replace(" ", "_") to queueUpdate)
//            } else {
//                queues
//            }
//            presetQueuesInternal.value = if (presetQueueUpdate != null) {
//                presets + (update.name to presetQueueUpdate)
//            } else {
//                presets
//            }
//            spriteQueuesInternal.value = if (queueImgSpriteUpdate != null) {
//                imgSprites + (update.name to queueImgSpriteUpdate)
//            } else {
//                imgSprites
//            }
//            spoutQueuesInternal.value = if (spoutQueueUpdate != null) {
//                spoutSprites + (update.name to spoutQueueUpdate)
//            } else {
//                spoutSprites
//            }
//            allQueuesInternal.value = presetQueuesInternal.value + spriteQueuesInternal.value + spoutQueuesInternal.value
        }
            .launchIn(flowScope)
    }
}

/***
 * each preset queue is independent of decks
 * decks can change the deck of a preset queue
 *
 */
