import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.xn32.json5k.Json5
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.plus
import kotlinx.serialization.Serializable
import nestdrop.deck.Deck
import nestdrop.deck.applyConfig
import nestdrop.deck.configFlow
import tags.TagScoreEval
import ui.screens.customSearches
import java.io.File
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger { }

val configScope = CoroutineScope(
    Dispatchers.IO
        .limitedParallelism(32)
) + CoroutineName("flows")

@Serializable
data class Config(
    val beats: Int = 32,
    val deck1: DeckConfig = DeckConfig(
        triggerTime = 0.5f + 0.125f
    ),
    val deck2: DeckConfig = DeckConfig(
        triggerTime = 0.125f
    ),
    val deck3: DeckConfig = DeckConfig(
        triggerTime = 0.75f + 0.125f
    ),
    val deck4: DeckConfig = DeckConfig(
        triggerTime = 0.25f + 0.125f
    ),
    val searches: List<TagScoreEval> = emptyList()
)

@Serializable
data class DeckConfig(
    val triggerTime: Float = 1.0f,
    val transitionTime: Float = 5.0f,
//    val presetQueues: PresetQueues = PresetQueues(),
    val search: SearchConfig = SearchConfig(),
    val presetQueue: PresetQueue = PresetQueue(),
    val preset: Preset = Preset(),
    val sprite: Sprite = Sprite(),
//    val spriteQueue: SpriteQueue = SpriteQueue(),
    val spoutQueue: SpoutQueue = SpoutQueue(),
    val spout: Spout = Spout(),
    val spriteFX: SpriteFX = SpriteFX(),
    val bpmSync: BpmSync = BpmSync(),
    val strobe: Strobe = Strobe()
) {
    @Serializable
    data class SearchConfig(
        val autoChange: Boolean = false,
        val name: String? = null,
    )
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

//    @Serializable
//    data class SpriteQueue(
////    val autoChange: Boolean = false,
//        val index: Int = -1,
//        val name: String? = null,
//    )

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
suspend fun Deck.updateConfig(deckConfig: DeckConfig) {
    updateConfig {
        when (N) {
            1 -> {
                copy(deck1 = deckConfig)
            }
            2 -> {
                copy(deck2 = deckConfig)
            }
            3 -> {
                copy(deck3 = deckConfig)
            }
            4 -> {
                copy(deck4 = deckConfig)
            }
            else -> {
                copy()
            }
        }
    }
}

suspend fun loadConfig() {
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
            logger.info { "saving config" }
            logger.trace { config }
            saveConfig(config)
        }
//        .runningHistory(config.value)
//        .onEach { (new, old) ->
//            logger.infoF { "old: $old" }
//            logger.infoF { "new: $new" }
//        }
        .launchIn(configScope)

    config.value.also { config ->
        logger.info { "loaded $config" }
        beatFrame.value = config.beats
        customSearches.value = config.searches
        decks.forEach { deck ->
            when (deck.N) {
                1 -> deck.applyConfig(config.deck1)
                2 -> deck.applyConfig(config.deck2)
                3 -> deck.applyConfig(config.deck3)
                4 -> deck.applyConfig(config.deck4)
            }
        }

        delay(250)
    }

    decks.forEach { deck ->
        deck.configFlow
            .onEach { deckConfig ->
                deck.updateConfig(deckConfig)
            }
            .launchIn(configScope)
    }

    customSearches.onEach { searches ->
        updateConfig {
            copy(searches = searches)
        }
    }.launchIn(configScope)
}

suspend fun saveConfig(config: Config) {
    configFile.writeText(
        json5.encodeToString(Config.serializer(), config)
    )
}