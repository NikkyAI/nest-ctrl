import io.github.xn32.json5k.Json5
import io.klogging.logger
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.plus
import kotlinx.serialization.Serializable
import logging.infoF
import nestdrop.deck.Deck
import nestdrop.deck.applyConfig
import nestdrop.deck.configFlow
import java.io.File
import kotlin.time.Duration.Companion.seconds

private val logger = logger("Config")

val configScope = CoroutineScope(
    Dispatchers.IO
        .limitedParallelism(32)
) + CoroutineName("flows")

@Serializable
data class Config(
    val beats: Int = 32,
    val deck1: DeckConfig = DeckConfig(
        triggerTime = 0.75f
    ),
    val deck2: DeckConfig = DeckConfig(
        triggerTime = 0.25f
    )
)

@Serializable
data class DeckConfig(
    val triggerTime: Float = 1.0f,
    val transitionTime: Float = 5.0f,
//    val presetQueues: PresetQueues = PresetQueues(),
    val presetQueue: PresetQueue = PresetQueue(),
    val preset: Preset = Preset(),
    val sprite: Sprite = Sprite(),
    val spriteQueue: SpriteQueue = SpriteQueue(),
    val spoutQueue: SpoutQueue = SpoutQueue(),
    val spout: Spout = Spout(),
    val spriteFX: SpriteFX = SpriteFX(),
    val bpmSync: BpmSync = BpmSync(),
    val strobe: Strobe = Strobe()
) {
    @Serializable
    data class PresetQueue(
        val index: Int = -1,
        val name: String? = null,
        val autoChange: Boolean = false,
        val toggles: Set<String> = emptySet(),
//    val toggles: Set<String> = emptySet(),
    )

    @Serializable
    data class BpmSync(
        val enabled: Boolean = false,
        val multiplier: Int = 4
    )

    @Serializable
    data class Strobe(
        val enabled: Boolean = false,
    )

    @Serializable
    data class Preset(
        val autoChange: Boolean = false,
    )

    @Serializable
    data class SpriteQueue(
//    val autoChange: Boolean = false,
        val index: Int = -1,
        val name: String? = null,
    )

    @Serializable
    data class Sprite(
        val autoChange: Boolean = false,
        val index: Int = -1,
        val name: String? = null,
        val toggles: Set<String> = emptySet(),
    )

    @Serializable
    data class SpoutQueue(
        val index: Int = -1,
        val name: String? = null,
    )

    @Serializable
    data class Spout(
//    val autoChange: Boolean = false,
        val index: Int = -1,
        val label: String? = null,
//    val toggles: Set<String> = emptySet(),
    )

    @Serializable
    data class SpriteFX(
        val autoChange: Boolean = false,
        val blendMode: Boolean = false,
        val index: Int = -1,
        val toggles: Set<Int> = emptySet(),
    )
}

val config = MutableStateFlow(Config())

val configFile = File("config.json5")

val json5 = Json5 {
    prettyPrint = true
    encodeDefaults = true
}

suspend fun updateConfig(block: suspend Config.() -> Config) {
    config.value = config.value.block()
}

suspend fun loadConfig(
    deck1: Deck,
    deck2: Deck,
) {
    if (configFile.exists()) {
        configFile.readText()
        config.value = json5.decodeFromString(
            Config.serializer(),
            configFile.readText(),
        )
    }

    config
        .sample(1.seconds)
//        .dropWhile { it == Config() }
        .onEach { config ->
            logger.infoF { "saving config $config" }
            saveConfig(config)
        }
//        .runningHistory(config.value)
//        .onEach { (new, old) ->
//            logger.infoF { "old: $old" }
//            logger.infoF { "new: $new" }
//        }
        .launchIn(configScope)


    deck1.configFlow
        .onEach { deckConfig ->
            updateConfig {
                copy(deck1 = deckConfig)
            }
        }
        .launchIn(configScope)
    deck2.configFlow
        .onEach { deckConfig ->
            updateConfig {
                copy(deck2 = deckConfig)
            }
        }
        .launchIn(configScope)

    config.value.also { config ->
        logger.infoF { "loaded $config" }
        beatFrame.value = config.beats
        deck1.applyConfig(config.deck1)
        deck2.applyConfig(config.deck2)
    }
}


suspend fun saveConfig(config: Config) {
    configFile.writeText(
        json5.encodeToString(Config.serializer(), config)
    )
}