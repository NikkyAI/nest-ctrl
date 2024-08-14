package nestdrop.deck

import io.github.oshai.kotlinlogging.KotlinLogging
import nestdrop.NestdropSettings
import nestdropConfig
import utils.runCommandCaptureOutput
import utils.xml
import java.io.File

private val logger = KotlinLogging.logger { }

suspend fun loadDeckSettings(decks: List<Deck>) {
    val nestdropSettings = xml.decodeFromString(
        NestdropSettings.serializer(), nestdropConfig.readText()
            .substringAfter(
                """<?xml version="1.0" encoding="utf-8"?>"""
            )
//            .lines().drop(1).joinToString("/n")
    )
    decks.forEach { deck ->
        logger.info { "loading settings from ${deck.deckName}" }
//        val deckSettings = parseDeckConfig(deck.N)
        val deckSettings = when(deck.N) {
            1 -> nestdropSettings.mainWindow.settingsDeck1
            2 -> nestdropSettings.mainWindow.settingsDeck2
            3 -> nestdropSettings.mainWindow.settingsDeck3
            4 -> nestdropSettings.mainWindow.settingsDeck4
            else -> error("unsupported deck number")
        }

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

        deck.ndOutput.pinToTop.value = deckSettings.videoDeck.topMost
        deck.ndOutput.spoutPreview.value = deckSettings.videoDeck.preview.let {
            when(it) {
                1 -> SpoutPreviewSize.`1_1`
                2 -> SpoutPreviewSize.`1_2`
                4 -> SpoutPreviewSize.`1_4`
                8 -> SpoutPreviewSize.`1_8`
                else -> {
                    logger.error { "${deck.deckName}: cannot parse preview size $it" }
                    SpoutPreviewSize.`1_4`
                }
            }
        }
    }
}

//private suspend fun parseDeckConfig(deck: Int): NestdropSettings.MainWindow.DeckSettings {
//    logger.debug { "querying /NestDropSettings/MainWindow/Settings_Deck${deck}" }
//    val deckConfigXml = runCommandCaptureOutput(
//        "xq", "-n", "-x", "/NestDropSettings/MainWindow/Settings_Deck${deck}",
//        workingDir = File("."),
//        input = nestdropConfig
//    ).trim()
//
//    logger.debug { "parsing" }
////    logger.debugF { "$deckConfigXml" }
//
//    return xml.decodeFromString(
//        DeckSettings.serializer(), deckConfigXml
//    ).also {
////        logger.debug { it }
//    }
//}