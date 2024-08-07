package nestdrop

import io.github.oshai.kotlinlogging.KotlinLogging
import nestdrop.deck.Deck
import nestdrop.deck.PresetQueues
import nestdropConfig
import utils.runCommandCaptureOutput
import utils.xml
import java.io.File

private val logger = KotlinLogging.logger { }


suspend fun loadNumberOfDecks(): Int {
    return runCommandCaptureOutput(
        "xq", "-x", "/NestDropSettings/MainWindow/Settings_General/@NbDecks",
        workingDir = File("."),
        input = nestdropConfig
    ).trim().toInt()
}

suspend fun loadNestdropConfig(
    presetQueues: PresetQueues,
    decks: List<Deck>,
//    deck1: Deck,
//    deck2: Deck,
) {
    val numberOfDecks = loadNumberOfDecks()
    Deck.enabled.value = numberOfDecks

    logger.info { "loading queues from $nestdropConfig" }
    try {
        val queueCount = runCommandCaptureOutput(
            "xq", "-x", "count(/NestDropSettings/QueueWindows/*)",
            workingDir = File("."),
            input = nestdropConfig
        ).trim().toInt()

        val queues = (0 until queueCount).mapNotNull {
            logger.info { "loading queue $it" }
            loadQueue(it)
        }

        logger.info { "loaded ${queues.size} queues from xml" }

        presetQueues.allQueues.value = queues.filter { /*it.open &&*/ it.type == QueueType.Preset }
        presetQueues.queues.value = queues.filter { it.open && it.type == QueueType.Preset }
        presetQueues.queuesInitialized.value = true
        decks.forEach { deck ->
            deck.spriteQueues.value = queues.filter { it.open && it.deck == deck.N && it.type == QueueType.Sprite }
        }
    } catch (e: Exception) {
        logger.error(e) { "failed to load queues" }
    }
}

suspend fun loadQueue(index: Int): Queue? {
    val queueWindowsXml = runCommandCaptureOutput(
        "xq", "-n", "-x", "/NestDropSettings/QueueWindows/Queue${index + 1}",
        workingDir = File("."),
        input = nestdropConfig
    ).trim()
    val queueWindow = try {
        xml.decodeFromString(
            XmlDataClasses.QueueWindow.serializer(), queueWindowsXml
        )
    } catch (e: NumberFormatException) {
        logger.error(e) { "failed to parse queue $index" }
        return null
    } catch (e: Exception) {
        logger.error(e) { "failed to parse queue $index" }
        return null
    }

    val type = QueueType.entries[queueWindow.type - 1]

    return Queue(
        index = index,
        name = queueWindow.name,
        type = type,
        open = queueWindow.open,
        deck = queueWindow.deck,
        presets = queueWindow.presets.mapIndexed() { i, p ->
            Preset(
                index = i,
                name = p.name,
                effects = p.effect ?: 0,
                overlay = p.overlay,
            )
        }
    )
}
