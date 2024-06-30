package nestdrop

import io.klogging.logger
import logging.errorF
import logging.infoF
import nestdrop.deck.Deck
import nestdrop.deck.PresetQueues
import nestdropConfig
import utils.runCommandCaptureOutput
import utils.xml
import java.io.File

private val logger = logger("nestdrop.loadQueuesKt")



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

    logger.infoF { "loading queues from $nestdropConfig" }
    try {
        val queueCount = runCommandCaptureOutput(
            "xq", "-x", "count(/NestDropSettings/QueueWindows/*)",
            workingDir = File("."),
            input = nestdropConfig
        ).trim().toInt()

        val queues = (0 until queueCount).mapNotNull {
            loadQueue(it)
        }

        presetQueues.allQueues.value = queues.filter { /*it.open &&*/ it.type == QueueType.Preset }
        presetQueues.queues.value = queues.filter { it.open && it.type == QueueType.Preset }
        decks.forEach { deck ->
            deck.spriteQueues.value = queues.filter { it.open && it.deck == deck.N && it.type == QueueType.Sprite }
        }
    } catch (e: Exception) {
        logger.errorF(e) { "failed to load queues" }
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
        logger.errorF(e) { "failed to parse queue $index" }
        return null
    } catch (e: Exception) {
        logger.errorF(e) { "failed to parse queue $index" }
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
