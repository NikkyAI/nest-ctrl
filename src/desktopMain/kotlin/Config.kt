import androidx.compose.runtime.Immutable
import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.annotations.TomlMultiline
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.xn32.json5k.Json5
import io.github.xn32.json5k.SerialComment
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.plus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import nestdrop.deck.Deck
import nestdrop.deck.applyConfig
import nestdrop.deck.configFlow
import utils.LOOM
import tags.PresetPlaylist
import tags.Term
import tags.TermDouble
import ui.screens.customSearches
import utils.prettyPrint
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger { }

@OptIn(ExperimentalCoroutinesApi::class)
val configScope = CoroutineScope(
    Dispatchers.LOOM
        .limitedParallelism(16)
) + CoroutineName("flows")

@Immutable
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
    @TomlMultiline
    @SerialName("presetPlaylists")
    @Deprecated("to be removed") val presetPlaylistsOld: List<PresetPlaylistDouble> = emptyList(),
    val playlists: Map<String, List<Term>> = emptyMap()
) {
    val presetPlaylists by lazy {
        playlists.entries.map {
            PresetPlaylist(it.key, it.value)
        }
    }

    @Serializable
    data class PresetPlaylistDouble(
        val label: String,
        val terms: List<TermDouble>
    )
}

@Immutable
@Serializable
data class DeckConfig(
    val triggerTime: Float = 1.0f,
    val transitionTime: Float = 5.0f,
    val transitionTimeBeatSync: Boolean = true,
//    val transitionTimeSyncFrameFraction: Float = 0.125f,
    val transitTimeBeats: Int = 8,
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
    @Immutable
    @Serializable
    data class SearchConfig @OptIn(ExperimentalSerializationApi::class) constructor(
        val autoChange: Boolean = false,
        @JsonNames("name", "label")
        val label: String? = null,
//        val enabledFragments: Set<String> = emptySet(),
    )

    @Immutable
    @Serializable
    data class PresetQueue(
        val index: Int = -1,
        val name: String? = null,
        val autoChange: Boolean = false,
        val toggles: Set<String> = emptySet(),
//    val toggles: Set<String> = emptySet(),
    )

    @Immutable
    @Serializable
    data class BpmSync(
        val enabled: Boolean = false,
        val multiplier: Int = 4
    )

    @Immutable
    @Serializable
    data class Strobe(
        val enabled: Boolean = false,
    )

    @Immutable
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

    @Immutable
    @Serializable
    data class Sprite(
        val autoChange: Boolean = false,
        val index: Int = -1,
        val name: String? = null,
        @TomlMultiline
        val toggles: Set<String> = emptySet(),
    )

    @Immutable
    @Serializable
    data class SpoutQueue(
        @SerialComment("deprecated, will be removed")
        val index: Int = -1,
        val name: String? = null,
    )

    @Immutable
    @Serializable
    data class Spout(
        val index: Int = -1,
        val label: String? = null,
    )

    @Immutable
    @Serializable
    data class SpriteFX(
        val autoChange: Boolean = false,
        val blendMode: Boolean = false,
        val index: Int = -1,
        @TomlMultiline
        val toggles: Set<Int> = emptySet(),
    )
}

val config = MutableStateFlow(Config())

private val json = Json {
    allowComments = true
    prettyPrint = true
    allowTrailingComma = true
    encodeDefaults = true
    useAlternativeNames = true
    isLenient = true
//    this.explicitNulls = false
//    namingStrategy = JsonNamingStrategy.SnakeCase
}
val toml = Toml(
    inputConfig = TomlInputConfig(
        allowEmptyToml = false
    )
)
val json5 = Json5 {
    prettyPrint = true
    encodeDefaults = true
}

suspend fun updateConfig(block: suspend Config.() -> Config) {
    config.value = config.value.block()
}

suspend fun Deck.updateConfig(deckConfig: DeckConfig) {
    updateConfig {
        when (id) {
            1 -> copy(deck1 = deckConfig)
            2 -> copy(deck2 = deckConfig)
            3 -> copy(deck3 = deckConfig)
            4 -> copy(deck4 = deckConfig)
            else -> copy()
        }
    }
}

@OptIn(FlowPreview::class)
suspend fun loadConfig() {
    logger.info { "load config" }
    val newConfigValue = if (configFile.exists()) {
        logger.info { "loading: $configFile" }
//        configFile.readText()
        json.decodeFromString(
            Config.serializer(),
            configFile.readText(),
        )
    } else if (configFileJson5.exists()) {
        logger.info { "loading: $configFileJson5" }
//        configFile.readText()
        json.decodeFromString(
            Config.serializer(),
            configFileJson5.readText(),
        )
    } else {
        logger.info { "does not exist: $configFile $configFileJson5" }
        null
    }

    if (newConfigValue != null) {
        config.value = newConfigValue.let { c ->
            if (c.playlists.isEmpty()) {
                c.copy(
                    playlists = c.presetPlaylistsOld.associate { it.label to it.terms.map { it.toTerm() } },
                    presetPlaylistsOld = emptyList()
                )
            } else {
                c
            }
        }
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
        logger.info { "loaded config" }
        logger.debug { config.prettyPrint() }
//        beatFrame.value = config.beats
        customSearches.value = config.presetPlaylists
        decks.forEach { deck ->
            when (deck.id) {
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
            copy(playlists = searches.associate { it.label to it.terms })
        }
    }.launchIn(configScope)
}

fun saveConfig(config: Config) {
    configFile.parentFile.mkdirs()
    configFile.writeText(
        json.encodeToString(Config.serializer(), config)
    )
    configFileToml.writeText(
        toml.encodeToString(Config.serializer(), config)
    )
}