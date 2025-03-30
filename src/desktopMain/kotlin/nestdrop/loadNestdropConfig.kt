package nestdrop

import androidx.compose.ui.graphics.Color
import flowScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import nestdrop.deck.Deck
import nestdrop.deck.Queues
import nestdropConfigFile
import utils.KWatchChannel
import utils.KWatchEvent
import utils.asWatchChannel
import utils.xml
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import nestdrop.deck.Effect
import nestdrop.deck.Trigger
import nestdrop.deck.Waveform
import scanFileSystemQueueForImgSprites
import scanFileSystemQueueForMilk
import imgSpritesMap
import presetsMap

private val logger = KotlinLogging.logger { }


suspend fun watchNestdropConfig(): StateFlow<NestdropSettings> {
    val initial = parseNestdropXml()
    val stateflow = MutableStateFlow(initial)

    nestdropConfigFile
        .asWatchChannel(KWatchChannel.Mode.SingleFile)
        .consumeAsFlow()
        .mapNotNull {
            if (it.kind == KWatchEvent.Kind.Modified) {
                try {
                    parseNestdropXml()
                } catch (e: Exception) {
                    logger.error(e) { "failed to load XML after file modification was detected" }
                    null
                }
            } else {
                null
            }
        }
        .distinctUntilChanged()
        .onEach {
            logger.info { "parsed (changed) xml from $nestdropConfigFile" }
//            loadNestdropConfig(it, presetQueues, decks)
            stateflow.value = it
        }
        .launchIn(flowScope)

    return stateflow.asStateFlow()
}

//fun loadNumberOfDecks(): Int {
//    return runCommandCaptureOutput(
//        "xq", "-x", "/NestDropSettings/MainWindow/Settings_General/@NbDecks",
//        workingDir = File("."),
//        input = nestdropConfig
//    ).trim().toInt()
//}

suspend fun parseNestdropXml(
    retries: Int = 0
): NestdropSettings {
    val nestdropSettings: NestdropSettings = try {
        xml.decodeFromString(
            NestdropSettings.serializer(), nestdropConfigFile.readText().also {
                logger.trace { "parsing xml: $it" }
            }
                .substringAfter(
                    """<?xml version="1.0" encoding="utf-8"?>"""
                )
//            .lines().drop(1).joinToString("/n")
        )
    } catch (e: nl.adaptivity.xmlutil.XmlException) {
        if (retries < 5) {
            logger.warn { "failed to parse XML: ${e.message}" }
            delay(100)
            return parseNestdropXml(retries + 1)
        } else {
            logger.error(e) { "failed to parse XML" }
            throw e
        }
    }
    return nestdropSettings
}

suspend fun loadNestdropConfig(
    nestdropSettings: NestdropSettings,
    queues: Queues,
    decks: List<Deck>
) {
//    val nestdropSettings = parseNestdropXml()
//    logger.info { "loaded xml from $nestdropConfig" }

//    val numberOfDecks = loadNumberOfDecks()
    val numberOfDecks = nestdropSettings.mainWindow.settingsGeneral.nbDecks
    Deck.enabled.value = numberOfDecks

    logger.info { "loading queues from $nestdropConfigFile" }
    try {
        val presetQueues = nestdropSettings.queueWindows.queues
            .filter { it.deck != null && it.type() == QueueType.PRESET }
            .map { queue ->
                Queue(
                    index = queue.index,
                    name = queue.name,
                    type = queue.type(),
                    open = queue.open,
                    deck = queue.deck!!,
                    active = queue.active,
                    beatOffset = queue.beatOffset,
                    beatMultiplier = queue.beatMulti,
                    isFileExplorer = queue.isFileExplorer,
                    fileExplorerPath = queue.fileExplorerPath,
                    presets =  if(!queue.isFileExplorer) {
                        queue.presets.mapIndexed() { i, p ->
                            Preset.Milkdrop(
//                                index = i,
                                name = p.name,
                                id = p.id!!,
                                effects = p.effect ?: 0,
                                overlay = p.overlay,
                                comments = p.comments,
                                location = presetsMap.value[p.name] // ?: error("failed to load $${p.name}")
                            )
                        }
                    } else {
                        val presetOverrides = queue.presets
                        scanFileSystemQueueForMilk(queue.fileExplorerPath)
//                        emptyList()
                    },
                )
            }
            .also {
                logger.info { "loaded ${it.size} queues from xml" }
            }

        val imgSpriteQueues = nestdropSettings.queueWindows.queues
            .filter {
                it.deck != null
                        && it.type() == QueueType.SPRITE
                        && it.presets.all { it.type() == PresetType.ImgSprite }
            }
            .map { queue ->
                Queue(
                    index = queue.index,
                    name = queue.name,
                    type = queue.type(),
                    open = queue.open,
                    deck = queue.deck!!,
                    active = queue.active,
                    beatOffset = queue.beatOffset,
                    beatMultiplier = queue.beatMulti,
                    isFileExplorer = queue.isFileExplorer,
                    fileExplorerPath = queue.fileExplorerPath,
                    //TODO: if isFileExplorer -> scan folder for entries
                    presets = if(!queue.isFileExplorer) {
                        queue.presets.mapIndexed() { i, p ->
                            Preset.ImageSprite(
                                name = p.name,
                                id = p.id!!,
                                effects = p.effect ?: 0,
                                overlay = p.overlay,
                                comments = p.comments?.takeUnless { it.isBlank() },

                                location = imgSpritesMap.value[p.name] // ?: error("failed to load $${p.name}")
                            )
                        }
                    } else {
                        val presetOverrides = queue.presets

                        scanFileSystemQueueForImgSprites(queue.fileExplorerPath)
                    }
                )
            }
        val spoutSpriteQueues = nestdropSettings.queueWindows.queues
            .filter {
                it.deck != null
                        && it.type() == QueueType.SPRITE
                        && !it.isFileExplorer
                        && it.presets.all { it.type() == PresetType.SpoutSprite }
            }
            .map { queue ->
                Queue(
                    index = queue.index,
                    name = queue.name,
                    type = queue.type(),
                    open = queue.open,
                    deck = queue.deck!!,
                    active = queue.active,
                    beatOffset = queue.beatOffset,
                    beatMultiplier = queue.beatMulti,
//                    isFileExplorer = queue.isFileExplorer,
//                    fileExplorerPath = queue.fileExplorerPath,
                    presets = queue.presets.mapIndexed() { i, p ->
                        Preset.SpoutSprite(
                            index = i,
                            name = p.name,
                            id = p.id!!,
                            effects = p.effect ?: 0,
                            overlay = p.overlay,
                            comments = p.comments?.takeUnless { it.isBlank() }
                        )
                    }
                )
            }

        decks.forEach { deck ->
//            deck.imgSpriteQueues.value = imgSpriteQueues.filter { queue ->
//                queue.open && queue.deck == deck.id && queue.type == QueueType.SPRITE
//            }
            deck.spoutSpriteQueues.value = spoutSpriteQueues.filter { queue ->
                queue.open && queue.deck == deck.id && queue.type == QueueType.SPRITE
            }
        }
//        queues.presetQueues.value = presetQueues.associateBy { it.name }
//        queues.allQueues.update { oldList ->
//            (presetQueues + imgSpriteQueues + spoutSpriteQueues).map { newQueue ->
//                oldList.firstOrNull { it.name == newQueue.name }?.let {
//
//                }
//            }
//
//            ((presetQueues + imgSpriteQueues + spoutSpriteQueues).sortedBy { it.index })
//        }
        queues.updateQueues(
            presetQueues,
            imgSpriteQueues,
            spoutSpriteQueues,

//            (presetQueues + imgSpriteQueues + spoutSpriteQueues).sortedBy { it.index }.associateBy { it.name }
        )
//        queues.allQueues.value =
        queues.isInitialized.value = true

        nestdropSettings.queueWindows.queues
            .filter { it.deck != null && it.type() == QueueType.SETTING }
            .forEach { queue ->
                queue.presets.forEach { settingsPreset ->
                    val (time, colors, strobeLfo, audio, unknown) = settingsPreset.settingCapture .orEmpty().split(",").map { it == "1" }
                    var values = settingsPreset.settingCaptureValues.orEmpty().split(",")


                    if(time) {
                        val transitTime = values[0].toFloat()
                        val fps = values[1].toFloat()
                        val animSpeed = values[2].toFloat()
                        val zoomSpeed = values[3].toFloat()
                        val zoomExp = values[4].toFloat()
                        val rotationSpeed = values[5].toFloat()
                        val wrapSpeed = values[6].toFloat()
                        val horizontalMotion = values[7].toFloat()
                        val verticalMotion = values[8].toFloat()
                        val stretchSpeed = values[9].toFloat()
//                        val waveMode = values[10].toFloat()
                    }
                    values = values.drop(10)
                    if(colors) {
                        val negative = values[0].toFloat()
                        val red = values[1].toFloat()
                        val green = values[2].toFloat()
                        val blue = values[3].toFloat()
                        val brightness = values[4].toFloat()
                        val contrast = values[5].toFloat()
                        val gamma = values[6].toFloat()
                        val hue = values[7].toFloat()
                        val saturation = values[8].toFloat()
                        val lumaKeyMin = values[9].toFloat()
                        val lumaKeyMax = values[10].toFloat()
                        val alpha = values[11].toFloat()
                    }
                    values = values.drop(12)
                    if(audio) {
//                        println(values)
                        val unknownA = values[0].toFloat()
                        val unknownB = values[1].toFloat()
                        val bass = values[2].toFloat()
                        val mid = values[3].toFloat()
                        val treble = values[4].toFloat()
                        //TODO: figure out unknown 0 values
                    }
                    values = values.drop(5)
                    if(strobeLfo) {
//                        println(values)
                        val strobeEffect = Effect.entries[values[0].toInt()]
                        val color = Color(values[1].toInt())
                        val speed = values[2].toFloat()
                        val effectSpanMin = values[3].toFloat()
                        val effectSpanMax = values[4].toFloat()
                        val pulseWidth = values[5].toFloat()
                        val waveForm = Waveform.entries[values[6].toInt()]
                        val trigger = Trigger.entries[values[7].toInt()]
                    }
                    values = values.drop(8)
                }
            }

    } catch (e: Exception) {
        logger.error(e) { "failed to load queues" }
    }
}

