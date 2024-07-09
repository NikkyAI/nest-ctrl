package nestdrop.deck

import io.github.oshai.kotlinlogging.KotlinLogging
import nestdrop.XmlDataClasses
import nestdropConfig
import utils.runCommandCaptureOutput
import utils.xml
import java.io.File

private val logger = KotlinLogging.logger { }

suspend fun loadDeckSettings(decks: List<Deck>) {
    decks.forEach { deck ->
        logger.info { "loading settings from ${deck.deckName}" }
        val deckSettings = parseDeckConfig(deck.N)

        deck.ndTime.transitionTime.value = deckSettings.transitTime.value
        deck.ndTime.animationSpeed.value = deckSettings.animationSpeed.value
        deck.ndTime.zoomSpeed.value = deckSettings.zoomSpeed.value
        deck.ndTime.rotationSpeed.value = deckSettings.rotationSpeed.value
        deck.ndTime.wrapSpeed.value = deckSettings.wrapSpeed.value
        deck.ndTime.horizontalMotion.value = deckSettings.horizontalMotion.value
        deck.ndTime.verticalMotion.value = deckSettings.verticalMotion.value
        deck.ndTime.stretchSpeed.value = deckSettings.stretchSpeed.value
        deck.ndTime.waveMode.value = deckSettings.waveMode.value

        deck.ndColor.negative.value = deckSettings.negative.value
        deck.ndColor.red.value = deckSettings.red.value
        deck.ndColor.green.value = deckSettings.green.value
        deck.ndColor.blue.value = deckSettings.blue.value
        deck.ndColor.brightness.value = deckSettings.brightness.value
        deck.ndColor.contrast.value = deckSettings.contrast.value
        deck.ndColor.gamma.value = deckSettings.gamma.value
        deck.ndColor.hueShift.value = deckSettings.hue.value
        deck.ndColor.saturation.value = deckSettings.saturation.value
        deck.ndColor.lumaKey.minState.value = deckSettings.lumaKey.min
        deck.ndColor.lumaKey.maxState.value = deckSettings.lumaKey.max
        deck.ndColor.alpha.value = deckSettings.alpha.value

        deck.ndStrobe.effect.value = Effect.entries[deckSettings.strobe.effectIndex]
        deck.ndStrobe.effectSpan.minState.value = deckSettings.strobe.effectSpanMin
        deck.ndStrobe.effectSpan.maxState.value = deckSettings.strobe.effectSpanMax
        deck.ndStrobe.trigger.value = Trigger.entries[deckSettings.strobe.triggerIndex]
        deck.ndStrobe.effectSpeed.value = deckSettings.strobe.speed
        deck.ndStrobe.pulseWidth.value = deckSettings.strobe.pulseWidth
        deck.ndStrobe.waveForm.value = Waveform.entries[deckSettings.strobe.waveFormIndex]
        deck.ndStrobe.enabled.value = false

        deck.ndAudio.bass.value = deckSettings.audio.bass
        deck.ndAudio.mid.value = deckSettings.audio.bass
        deck.ndAudio.treble.value = deckSettings.audio.bass

        deck.ndOutput.ndDeckPinToTop.value = deckSettings.videoDeck.topMost
    }
}

private suspend fun parseDeckConfig(deck: Int): XmlDataClasses.DeckSettings {
    logger.debug { "querying /NestDropSettings/MainWindow/Settings_Deck${deck}" }
    val deckConfigXml = runCommandCaptureOutput(
        "xq", "-n", "-x", "/NestDropSettings/MainWindow/Settings_Deck${deck}",
        workingDir = File("."),
        input = nestdropConfig
    ).trim()

    logger.debug { "parsing" }
//    logger.debugF { "$deckConfigXml" }

    return xml.decodeFromString(
        XmlDataClasses.DeckSettings.serializer(), deckConfigXml
    ).also {
        logger.info { it }
    }
}