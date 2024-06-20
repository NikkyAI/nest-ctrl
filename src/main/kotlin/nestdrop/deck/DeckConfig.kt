package nestdrop.deck

import DeckConfig
import io.klogging.logger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull
import logging.debugF
import logging.infoF
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val logger = logger("nestcontrol.deck.DeckConfigKt")

suspend fun Deck.applyConfig(deckConfig: DeckConfig) {
    deckConfig.apply {
        this@applyConfig.ndTime.transitionTime.value = transitionTime
        this@applyConfig.ndStrobe.enabled.value = strobe.enabled

        this@applyConfig.triggerTime.value = triggerTime
        val presetQueuesV = this@applyConfig.presetQueues.queues.value

        run {
            this@applyConfig.presetQueue.autoChange.value = presetQueue.autoChange
//            logger.infoF { "presetQueues toggleNames: ${presetQueue.toggles}" }
//            logger.infoF { "presetQueues: ${presetQueuesV.map { it.name }}" }
            val presetQueuesToggleIndices = presetQueue.toggles.map { queue ->
                presetQueuesV.indexOfFirst { it.name == queue }
            }.filterNot { it == -1 }.toSet()
            logger.infoF { "presetQueues toggleIndices: $presetQueuesToggleIndices" }
            this@applyConfig.presetQueue.toggles.forEachIndexed { index, toggle ->
                toggle.value = index in presetQueuesToggleIndices
            }
        }
        run {
            this@applyConfig.preset.autoChange.value = preset.autoChange
            this@applyConfig.presetQueue.index.value = presetQueuesV
                .indexOfFirst { it.name == deckConfig.presetQueue.name }
                .takeUnless { it == -1 }
                ?: deckConfig.presetQueue.index
        }

        run {
//            //TODO: find a way to load queue by name without blocking here
            val spriteQueuesValue = withTimeoutOrNull(5.seconds) {
                spriteQueues.first {
                    it
                        .also { logger.debugF { it } }
                        .isNotEmpty()
                }
            } ?: error("failed to load sprite queues on $deckName")
            val spriteQueueValue = spriteQueuesValue.firstOrNull() { it.name == spriteQueue.name }

            run {
                this@applyConfig.spriteQueue.index.value = spriteQueuesValue
                    .indexOf(spriteQueueValue)
                    .takeUnless { it == -1 }
                    ?: deckConfig.spriteQueue.index
                        .takeUnless { it == -1 }
                            ?: spriteQueuesValue.indexOfFirst { it.deck == this@applyConfig.N && it.name.contains("sprite") }

                val sprites = withTimeoutOrNull(500.milliseconds) {
                    logger.infoF { "loading presets from queue" }
                    spriteQueueValue?.presets.orEmpty()
                }.orEmpty()
                logger.infoF { "loaded $deckName $sprites" }
                this@applyConfig.imgSprite.autoChange.value = sprite.autoChange
                val spriteToggleIndices = sprite.toggles.map { name ->
                    sprites.indexOfFirst { it.name == name }
                }.toSet()
                this@applyConfig.imgSprite.toggles.forEachIndexed { index, toggle ->
                    toggle.value = index in spriteToggleIndices
                }
                this@applyConfig.imgSprite.index.value = sprites.indexOfFirst { it.name == sprite.name }
                    .takeUnless { it == -1 } ?: sprite.index
            }
            run {
                logger.debugF { "loading spout queue ${deckConfig.spoutQueue.name} from $spriteQueuesValue" }
                val spoutQueueValue = spriteQueuesValue.firstOrNull() { it.name == deckConfig.spoutQueue.name }
                this@applyConfig.spoutQueue.index.value = spriteQueuesValue.indexOf(spoutQueueValue)
                    .takeUnless { it == -1 } ?: deckConfig.spoutQueue.index.takeUnless { it == -1 } ?: spriteQueuesValue.indexOfFirst { it.deck == this@applyConfig.N && it.name.contains("spout") }
                val spouts = withTimeoutOrNull(500.milliseconds) {
                    logger.infoF { "loading presets from queue" }
                    spoutQueueValue?.presets.orEmpty()
                }.orEmpty()
                this@applyConfig.spout.index.value = spouts.indexOfFirst { it.label == spout.label }
                    .takeUnless { it == -1 } ?: spout.index
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
            this@applyConfig.bpmSyncEnabled.value = bpmSync.enabled
            this@applyConfig.bpmSyncMultiplier.value = bpmSync.multiplier
        }
    }
}

private val Deck.presetQueueFlow
    get() = combine(
        presetQueue.name,
        presetQueue.index,
        presetQueue.autoChange,
        combine(presetQueue.toggles) {
            it.mapIndexed { i, b ->
                i to b
            }.toMap()
        }.combine(presetQueues) { toggleStates, presetQueues ->
            toggleStates
                .filterValues { it }
                .mapKeys { (index, _) ->
                    presetQueues.getOrNull(index)?.name ?: ""
                }
                .filterKeys { it.isNotBlank() }
                .keys
        },
    ) { presetQueueName, presetQueueIndex, autoChange, toggles ->
        DeckConfig.PresetQueue(
            index = presetQueueIndex,
            name = presetQueueName,
            autoChange = autoChange,
            toggles = toggles
        )
    }


private val Deck.spriteFlow
    get() = combine(
        imgSprite.name,
        imgSprite.index,
        imgSprite.autoChange,
        combine(imgSprite.toggles) {
            it.mapIndexed { i, b ->
                i to b
            }.toMap()
        }.combine(spriteQueue.filterNotNull()) { toggleStates, spriteQueue ->
            toggleStates
                .filterValues { it }
                .mapKeys { (index, _) ->
                    spriteQueue.presets.getOrNull(index)?.name ?: ""
                }
                .filterKeys { it.isNotBlank() }
                .keys
        }
    ) { spriteName, spriteIndex, autoChange, toggles ->
        DeckConfig.Sprite(
            name = spriteName,
            index = spriteIndex,
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
                    triggerTime,
                    transitionTime,
                ) { config, triggerTime, transitionTime ->
                    config.copy(
                        triggerTime = triggerTime,
                        transitionTime = transitionTime,
                    )
                }
            }
            .combine(presetQueueFlow) { config, presetQueue ->
                config.copy(
                    presetQueue = presetQueue,
                )
            }
            .combine(
                preset.autoChange.map { autoChange ->
                    DeckConfig.Preset(autoChange = autoChange)
                }
            ) { config, preset ->
                config.copy(preset = preset)
            }
            .combine(
                combine(
                    spriteQueue.name,
                    spriteQueue.index,
                ) { spriteQueueName, spriteQueueIndex ->
                    DeckConfig.SpriteQueue(
                        index = spriteQueueIndex,
                        name = spriteQueueName,
                    )
                }
            ) { config, spriteQueue ->
                config.copy(
                    spriteQueue = spriteQueue
                )
            }
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
                    spoutQueue.index,
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
                    spout.index,
                    spout.name,
                ) { index, name ->
                    DeckConfig.Spout(
                        index = index,
                        label = name,
                    )
                }
            ) { config, spout ->
                config.copy(
                    spout = spout
                )
            }
            .combine(
                combine(
                    bpmSyncEnabled,
                    bpmSyncMultiplier
                ) { syncBpm, multiplier ->
                    DeckConfig.BpmSync(
                        enabled = syncBpm,
                        multiplier = multiplier,
                    )
                }
            ) { config, bpmSync ->
                config.copy(
                    bpmSync = bpmSync
                )
            }
            .combine(
                ndStrobe.enabled,
            ) { config, strobeEnabled ->
                config.copy(
                    strobe = DeckConfig.Strobe(strobeEnabled)
                )
            }
    }