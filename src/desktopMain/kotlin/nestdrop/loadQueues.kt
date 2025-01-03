package nestdrop

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import nestdrop.deck.Deck
import nestdrop.deck.PresetQueues
import nestdropConfig
import utils.xml

private val logger = KotlinLogging.logger { }


//fun loadNumberOfDecks(): Int {
//    return runCommandCaptureOutput(
//        "xq", "-x", "/NestDropSettings/MainWindow/Settings_General/@NbDecks",
//        workingDir = File("."),
//        input = nestdropConfig
//    ).trim().toInt()
//}

private suspend fun parseNestdropXml(
    retries: Int = 0
): NestdropSettings {
    val nestdropSettings: NestdropSettings = try {
        xml.decodeFromString(
            NestdropSettings.serializer(), nestdropConfig.readText()
                .substringAfter(
                    """<?xml version="1.0" encoding="utf-8"?>"""
                )
//            .lines().drop(1).joinToString("/n")
        )
    } catch (e: nl.adaptivity.xmlutil.XmlException) {
        logger.error(e) { "failed to parse XML" }
        delay(100)
        if (retries < 5) {
            return parseNestdropXml( retries + 1)
        } else {
            throw e
        }
    }
    return nestdropSettings
}

suspend fun loadNestdropConfig(
    presetQueues: PresetQueues,
    decks: List<Deck>
) {
    val nestdropSettings = parseNestdropXml()
    logger.info { "loaded xml from $nestdropConfig" }

//    val numberOfDecks = loadNumberOfDecks()
    val numberOfDecks = nestdropSettings.mainWindow.settingsGeneral.nbDecks
    Deck.enabled.value = numberOfDecks

    logger.info { "loading queues from $nestdropConfig" }
    try {
        val queues = nestdropSettings.queueWindows.queues
            .filter { it.deck != null }
            .map { queue ->
                Queue(
                    index = queue.index,
                    name = queue.name,
                    type = QueueType.entries[queue.type],
                    open = queue.open,
                    deck = queue.deck!!,
                    presets = queue.presets.mapIndexed() { i, p ->
                        Preset(
                            index = i,
                            name = p.name,
                            id = p.id,
                            effects = p.effect ?: 0,
                            overlay = p.overlay,
                        )
                    }
                )
            }
        logger.info { "loaded ${queues.size} queues from xml" }

        presetQueues.allQueues.value = queues.filter { /*it.open &&*/ it.type == QueueType.Preset }
        presetQueues.queues.value = queues.filter { it.open && it.type == QueueType.Preset }
        presetQueues.isInitialized.value = true
        decks.forEach { deck ->
            deck.spriteQueues.value = queues.filter { it.open && it.deck == deck.id && it.type == QueueType.Sprite }
        }
    } catch (e: Exception) {
        logger.error(e) { "failed to load queues" }
    }
}

