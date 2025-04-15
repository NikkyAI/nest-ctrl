package nestdrop.deck

import DeckConfig
import QUEUES
import imgSpritesMap
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import nestdrop.Preset
import nestdrop.Queue
import nestdrop.QueueType
import tags.nestdropQueueSearches
import tags.queueTagsInitialized
import ui.screens.customSearches
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger { }

suspend fun Deck.applyConfig(deckConfig: DeckConfig) = measureTimedValue {
    deckConfig.apply {
        this@applyConfig.ndTime.transitionTime.value = transitionTime
        this@applyConfig.ndStrobe.enabled.value = strobe.enabled

        this@applyConfig.presetSwitching.triggerTime.value = triggerTime
        this@applyConfig.presetSwitching.transitTimeSync.value = transitionTimeBeatSync
        this@applyConfig.presetSwitching.transitTimeBeats.value = transitTimeBeats

        run {
//            this@applyConfig.presetQueue.autoChange.value = presetQueue.autoChange
//            logger.infoF { "presetQueues toggleNames: ${presetQueue.toggles}" }
//            logger.infoF { "presetQueues: ${presetQueuesV.map { it.name }}" }
//            val presetQueuesToggleIndices = presetQueue.toggles.map { queue ->
//                presetQueuesV.indexOfFirst { it.name == queue }
//            }.filterNot { it == -1 }.toSet()
//            logger.info { "presetQueues toggleIndices: $presetQueuesToggleIndices" }
//            this@applyConfig.presetQueue.toggles.forEachIndexed { index, toggle ->
//                toggle.value = index in presetQueuesToggleIndices
//            }
        }
        run {
//            this@applyConfig.preset.autoChange.value = preset.autoChange
//            this@applyConfig.presetQueue.index.value = presetQueuesV
//                .indexOfFirst { it.name == deckConfig.presetQueue.name }
//                .takeUnless { it == -1 }
//                ?: deckConfig.presetQueue.index
        }

        run {
//            //TODO: find a way to load queue by name without blocking here ?
            logger.debug { "loading spoutQueue on $deckName" }
            val spoutSpriteQueuesValue = measureTimedValue {
                withTimeoutOrNull(10.seconds) {
                    QUEUES.spoutQueues().map { it.filter { it.deck == id } }
                    .first {
                        it
                            .also { logger.debug { "spoutSpritesQueue: $it" } }
                            .isNotEmpty()
                    }
//                    spoutSpriteQueues.first {
//                        it
//                            .also { logger.debug { "spoutSpritesQueue: $it" } }
//                            .isNotEmpty()
//                    }
                } ?: run {
                    logger.error { "failed to load spoutSpriteQueues on $deckName" }
                    emptyList()
                }
            }.apply {
                logger.info { "loaded spoutSpriteQueues on $deckName in $duration" }
            }.value
//            val spriteQueueValue = spriteQueuesValue.firstOrNull() { it.name == spriteQueue.name }

            run {
                this@applyConfig.imgSprite.autoChange.value = sprite.autoChange
                this@applyConfig.imgSprite.toggles.value = sprite.toggles
                sprite.name?.let {
                    imgSpritesMap.value[it]
                }?.let {
                    this@applyConfig.imgSprite.spriteImgLocation.value = it
                }
            }
            run {
                logger.debug { "loading spout queue ${deckConfig.spoutQueue.name} from $spoutSpriteQueuesValue" }
                val spoutQueueValue = spoutSpriteQueuesValue.firstOrNull() { it.name == deckConfig.spoutQueue.name }
                    ?: spoutSpriteQueuesValue.firstOrNull { it.deck == this@applyConfig.id && it.name.contains("spout") }
                this@applyConfig.spoutQueue.index.value = spoutSpriteQueuesValue.indexOf(spoutQueueValue)
                    .takeUnless { it == -1 }  // ?: deckConfig.spoutQueue.index.takeUnless { it == -1 }
                    ?: run {
                        logger.error { "$deckName failed to find matching spout queue" }
                        -1
                        //   ?: spriteQueuesValue.indexOfFirst { it.deck == this@applyConfig.N && it.name.contains("spout") }
                    }
//                val spoutSprites = withTimeoutOrNull(500.milliseconds) {
//                    logger.info { "loading spout sprites from queue for $deckName" }
//                    spoutQueueValue?.presets.orEmpty()
//                }.orEmpty()
//                this@applyConfig.spout.index.value = spoutSprites.indexOfFirst { it.encoded == spout.label }
//                    .takeUnless { it == -1 } ?: spout.index
            }
        }
        run {
            this@applyConfig.imgSpriteFx.autoChange.value = spriteFX.autoChange
            this@applyConfig.imgSpriteFx.blendMode.value = spriteFX.blendMode
            this@applyConfig.imgSpriteFx.toggles.forEachIndexed { index, toggle ->
                toggle.value = index in spriteFX.toggles
            }
        }
        run {
            val customSearches = withTimeoutOrNull(5.seconds) {
                measureTimedValue {
                    while (customSearches.value.isEmpty()) {
                        delay(100)
                    }
                    customSearches.value
                }.apply {
                    logger.info { "loaded customSearches on $deckName in $duration" }
                }.value
            } ?: run {
                logger.error { "failed to load customSearches on $deckName" }
                emptyList()
            }

            val nestdropQueueSearches = withTimeoutOrNull(5.seconds) {
                measureTimedValue {
                    while (!queueTagsInitialized.value) {
                        delay(100)
                    }
                    nestdropQueueSearches.value
                }.apply {
                    logger.info { "loaded nestdropQueueSearches on $deckName in $duration" }
                }.value
            } ?: run {
                logger.error { "failed to load nestdropQueueSearches on $deckName" }
                emptyList()
            }

            val combinedSearches = customSearches + nestdropQueueSearches

            this@applyConfig.search.autoChange.value = search.autoChange
            this@applyConfig.search.value = combinedSearches.firstOrNull { it.label == search.name }
        }
//        run {
//            this@applyConfig.bpmSyncEnabled.value = bpmSync.enabled
//            this@applyConfig.bpmSyncMultiplier.value = bpmSync.multiplier
//        }
    }
}.run {
    logger.debug { "applied config on $deckName in $duration" }
}

//private val Deck.presetQueueFlow
//    get() = combine(
//        presetQueue.name,
//        presetQueue.index,
//        presetQueue.autoChange,
//        combine(presetQueue.toggles) {
//            it.mapIndexed { i, b ->
//                i to b
//            }.toMap()
//        }.combine(presetQueues) { toggleStates, presetQueues ->
//            toggleStates
//                .filterValues { it }
//                .mapKeys { (index, _) ->
//                    presetQueues.getOrNull(index)?.name ?: ""
//                }
//                .filterKeys { it.isNotBlank() }
//                .keys
//        },
//    ) { presetQueueName, presetQueueIndex, autoChange, toggles ->
//        DeckConfig.PresetQueue(
//            index = presetQueueIndex,
//            name = presetQueueName,
//            autoChange = autoChange,
//            toggles = toggles
//        )
//    }


private val Deck.spriteFlow
    get() = combine(
        imgSprite.name,
//        imgSprite.index,
        imgSprite.autoChange,
        imgSprite.toggles,
    ) { spriteName, autoChange, toggles ->
        DeckConfig.Sprite(
            name = spriteName,
//            index = spriteIndex,
            autoChange = autoChange,
            toggles = toggles,
        )
    }

private val Deck.spriteFxFlow
    get() = combine(
        imgSpriteFx.index,
        imgSpriteFx.autoChange,
        imgSpriteFx.blendMode,
        combine(imgSpriteFx.toggles) {
            it.mapIndexed { i, b ->
                i to b
            }.toMap()
        }
    ) { spriteFXIndex, autoChange, blendMode, toggleStates ->
        val spriteFXToggles = toggleStates
            .filterValues { it }
            .keys

        DeckConfig.SpriteFX(
            autoChange = autoChange,
            blendMode = blendMode,
            index = spriteFXIndex,
            toggles = spriteFXToggles
        )
    }

val Deck.configFlow: Flow<DeckConfig>
    get() {
        return flowOf(
            DeckConfig()
        )
            .let { configFlow ->
                combine(
                    configFlow,
                    presetSwitching.triggerTime,
                    presetSwitching.transitionTime,
                    presetSwitching.transitTimeSync,
                    presetSwitching.transitTimeBeats
                ) { config, triggerTime, transitionTime, transitTimeSync, transitTimeBeats ->
                    config.copy(
                        triggerTime = triggerTime,
                        transitionTime = transitionTime,
                        transitionTimeBeatSync = transitTimeSync,
                        transitTimeBeats = transitTimeBeats,
                    )
                }
            }
//            .combine(presetQueueFlow) { config, presetQueue ->
//                config.copy(
//                    presetQueue = presetQueue,
//                )
//            }
//            .combine(
//                preset.autoChange.map { autoChange ->
//                    DeckConfig.Preset(autoChange = autoChange)
//                }
//            ) { config, preset ->
//                config.copy(preset = preset)
//            }
//            .combine(
//                combine(
//                    spriteQueue.name,
//                    spriteQueue.index,
//                ) { spriteQueueName, spriteQueueIndex ->
//                    DeckConfig.SpriteQueue(
//                        index = spriteQueueIndex,
//                        name = spriteQueueName,
//                    )
//                }
//            ) { config, spriteQueue ->
//                config.copy(
//                    spriteQueue = spriteQueue
//                )
//            }
            .combine(spriteFlow) { config, sprite ->
                config.copy(
                    sprite = sprite
                )
            }
            .combine(spriteFxFlow) { config, spriteFx ->
                config.copy(
                    spriteFX = spriteFx
                )
            }
            .combine(
                combine(
                    spoutQueue.index, //TODO: remove ?
                    spoutQueue.name,
                ) { index, name ->
                    DeckConfig.SpoutQueue(
                        index = index,
                        name = name,
                    )
                }
            ) { config, spoutQueue ->
                config.copy(
                    spoutQueue = spoutQueue
                )
            }
            .combine(
                combine(
                    spout,
                    spout.index,
                ) { spoutSprite, index ->
                    DeckConfig.Spout(
                        index = index,
                        label = spoutSprite?.encoded,
                    )
                }
            ) { config, spout ->
                config.copy(
                    spout = spout
                )
            }
//            .combine(
//                combine(
//                    bpmSyncEnabled,
//                    bpmSyncMultiplier
//                ) { syncBpm, multiplier ->
//                    DeckConfig.BpmSync(
//                        enabled = syncBpm,
//                        multiplier = multiplier,
//                    )
//                }
//            ) { config, bpmSync ->
//                config.copy(
//                    bpmSync = bpmSync
//                )
//            }
            .combine(
                ndStrobe.enabled,
            ) { config, strobeEnabled ->
                config.copy(
                    strobe = DeckConfig.Strobe(strobeEnabled)
                )
            }
            .combine(
                search.combine(
                    search.autoChange
                ) { search, autoChange ->
                    DeckConfig.SearchConfig(
                        autoChange = autoChange,
                        name = search?.label
                    )
                }
            ) { config, searchConfig ->
                config.copy(
                    search = searchConfig
                )
            }
    }
