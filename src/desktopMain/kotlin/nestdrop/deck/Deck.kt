package nestdrop.deck

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import beatFrame
import configFolder
import decks
import flowScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import nestdrop.NestdropControl
import nestdrop.NestdropSpriteQueue
import nestdrop.PerformanceLogRow
import nestdrop.PresetIdState
import nestdrop.PresetLocation
import nestdrop.Queue
import nestdrop.imgFxMap
import nestdrop.nestdropSetPreset
import nestdrop.nestdropSetSprite
import osc.OSCMessage
import osc.nestdropSendChannel
import tags.TagScoreEval
import tags.pickItemToGenerate
import tags.presetTagsMapping
import ui.screens.presetsMap
import ui.screens.imgSpritesMap
import utils.HistoryNotNull
import utils.prettyPrint
import utils.runningHistory
import java.io.File
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

class Deck(
    val id: Int,
    val hexColor: Long,
) {
    companion object {
        val enabled = MutableStateFlow(1)
        val enabledDecks = MutableStateFlow(emptyList<Deck>())

        init {
            enabled
                .map { enabledId ->
                    decks.filter { it.id <= enabledId }
                }
                .onEach {
                    enabledDecks.value = it
                }
                .launchIn(flowScope)
        }

        //        val enabledDecks = mutableStateMapOf<Int, Boolean>()
        private val logger = KotlinLogging.logger { }
    }

    val color = Color(hexColor)
    val dimmedColor = color.copy(alpha = 0.5f).compositeOver(Color.Black)

    @Immutable
    inner class NdTime {

        // time
        val transitionTime =
            NestdropControl.SliderWithResetButton(id, "TransitTime", 0.0f..30.0f, 5.0f, sendResetMessage = false)
        val animationSpeed = NestdropControl.SliderWithResetButton(id, "AnimSpeed", 0f..2f, 1.0f)
        val zoomSpeed = NestdropControl.SliderWithResetButton(id, "ZoomSpeed", 0.5f..1.5f, 1.0f)
        val rotationSpeed = NestdropControl.SliderWithResetButton(id, "RotationSpeed", 0f..2f, 1.0f)
        val wrapSpeed = NestdropControl.SliderWithResetButton(id, "WrapSpeed", 0f..2f, 1.0f)
        val horizontalMotion = NestdropControl.SliderWithResetButton(id, "HorizonMotion", -0.5f..0.5f, 0.0f)
        val verticalMotion = NestdropControl.SliderWithResetButton(id, "VerticalMotion", -0.5f..0.5f, 0.0f)
        val stretchSpeed = NestdropControl.SliderWithResetButton(id, "StretchSpeed", 0.5f..1.5f, 1.0f)
        val waveMode = NestdropControl.SliderWithResetButton(id, "WaveMode", 0.5f..1.5f, 1.0f)

        suspend fun startFlows() {
            transitionTime.startFlows()
            animationSpeed.startFlows()
            zoomSpeed.startFlows()
            rotationSpeed.startFlows()
            wrapSpeed.startFlows()
            horizontalMotion.startFlows()
            verticalMotion.startFlows()
            stretchSpeed.startFlows()
            waveMode.startFlows()
        }
    }

    val ndTime = NdTime()

    // color
    @Immutable
    inner class NdColor {
        val negative = NestdropControl.Slider(id, "Negative", 0f..1f, 0f)

        //        val brightness = NestdropControl.SliderWithResetButton(N, "Brightness", 0.5f..1.5f, 1.0f)
        val brightness = NestdropControl.SliderWithResetButton(id, "Brightness", 0.5f..1.05f, 1.0f)
        val contrast = NestdropControl.SliderWithResetButton(id, "Contrast", 0.5f..1.5f, 1.0f)
        val gamma = NestdropControl.SliderWithResetButton(id, "Gamma", 0.5f..1.5f, 1.0f)
        val hueShift = NestdropControl.SliderWithResetButton(id, "Hue", 0f..2f, 0f)
        val saturation = NestdropControl.SliderWithResetButton(id, "Saturation", 0f..1f, 1f)
        val lumaKey = NestdropControl.RangeSliderWithResetButton(id, "LumaKey", 0f..1f, 0f, 1f)
        val red = NestdropControl.SliderWithResetButton(id, "R", 0f..1f, 1f)
        val green = NestdropControl.SliderWithResetButton(id, "G", 0f..1f, 1f)
        val blue = NestdropControl.SliderWithResetButton(id, "B", 0f..1f, 1f)
        val alpha = NestdropControl.SliderWithResetButton(id, "Alpha", 0f..1f, 1f)

        suspend fun startFlows() {
            negative.startFlows()
            brightness.startFlows()
            contrast.startFlows()
            gamma.startFlows()
            hueShift.startFlows()
            saturation.startFlows()
            lumaKey.startFlows()
            red.startFlows()
            green.startFlows()
            blue.startFlows()
            alpha.startFlows()
        }
    }

    val ndColor = NdColor()

    //strobe / lfo
    @Immutable
    inner class NdStrobe {

        val effect = NestdropControl.Dropdown(
            id,
            "StrobeEffect",
            Effect.entries,
            { Effect.entries.indexOf(it) },
            Effect.AnimationSpeed
        )
        val effectSpan = NestdropControl.RangeSliderWithResetButton(id, "StrobeEffectSpan", 0f..1f, 0f, 1f)
        val trigger = NestdropControl.Dropdown(
            id,
            "StrobeTrigger",
            Trigger.entries,
            { Trigger.entries.indexOf(it) },
            Trigger.TimesPerSecond
        )
        val effectSpeed = NestdropControl.Slider(id, "StrobeSpeed", 0.01f..30f, 3f)
        val pulseWidth = NestdropControl.SliderWithResetButton(id, "StrobePulseWidth", 0f..1f, 1f)
        val waveForm = NestdropControl.Dropdown(
            id,
            "StrobeRamp",
            Waveform.entries,
            { Waveform.entries.indexOf(it) },
            Waveform.Square
        )
        val enabled = NestdropControl.ToggleButton(id, "StrobeOnOff", false)

        suspend fun startFlows() {
            effect.startFlows()
            effectSpan.startFlows()
            trigger.startFlows()
            effectSpeed.startFlows()
            pulseWidth.startFlows()
            waveForm.startFlows()
            enabled.startFlows()

//            Link.bpm
//                .combine(bpmSyncMultiplier) { bpm, multiplier ->
//                    val beatsPerSecond = bpm / 60.0f
//                    beatsPerSecond / multiplier
//                }
//                .map {
//                    (it * 100).roundToInt() / 100f
//                }
//                .combine(bpmSyncEnabled) { a, b -> a to b }
//                .distinctUntilChanged()
//                .onEach { (value, toggle) ->
//                    if (toggle) {
//                        effectSpeed.value = value
//                    }
//                }
//                .launchIn(flowScope)
        }
    }

    val ndStrobe = NdStrobe()

    // audio
    @Immutable
    inner class NdAudio {

        val bass = NestdropControl.Slider(id, "Bass", 0f..1f, 1f)
        val mid = NestdropControl.Slider(id, "Mid", 0f..1f, 1f)
        val treble = NestdropControl.Slider(id, "Treble", 0f..1f, 1f)
        suspend fun startFlows() {
            bass.startFlows()
            mid.startFlows()
            treble.startFlows()
        }
    }

    val ndAudio = NdAudio()

    // output
    @Immutable
    inner class NdOutput {
        val pinToTop = NestdropControl.ToggleButton(id, "TopMost", false)
        val spoutPreview = NestdropControl.Dropdown(
            id,
            "SpoutPreview",
            SpoutPreviewSize.entries,
            { SpoutPreviewSize.entries.indexOf(it) },
            SpoutPreviewSize.`1_4`
        )

        suspend fun startFlows() {
            pinToTop.startFlows()
            spoutPreview.startFlows()
        }
    }

    val ndOutput = NdOutput()

    suspend fun appendWarn(message: String) {
        configFolder.resolve("$deckName.warn.log").appendText(
            "\n$message"
        )
        configFolder.resolve("warn.log").appendText(
            "\n$message"
        )
    }

    @Immutable
    inner class PresetSwitching() {
        val transitionTime =
            MutableStateFlow(1f) // OscSynced.Value("/deck$N/transitionTime", 1.0f, target = Target.TouchOSC)
        val triggerTime =
            MutableStateFlow(0.75f) // OscSynced.Value("/deck$N/triggerTime", 0.75f, target = Target.TouchOSC)
        val currentPreset = MutableStateFlow<PerformanceLogRow?>(null)
        private val hasSwitched = MutableStateFlow(false)


        suspend fun resetLatch() {
            hasSwitched.value = false
        }

        suspend fun beatFlow(
            flow: Flow<HistoryNotNull<Double>>
        ) = flow
            .combine(
                triggerTime.combine(beatFrame) { triggerTime, b ->
                    triggerTime * b
                }
            ) { (currentBeat, lastBeat), triggerAt ->
                Triple(currentBeat, lastBeat, triggerAt)
            }.onEach { (currentBeat, lastBeat, triggerAt) ->
                if (!hasSwitched.value && lastBeat < triggerAt && currentBeat >= triggerAt) {
                    logger.info { "triggered at ${(currentBeat * 1000).roundToInt() / 1000f} ${(triggerAt * 1000).roundToInt() / 1000f}" }
                    hasSwitched.value = true
                    doSwitch()
                }
            }

        private suspend fun doSwitch() {
            // change preset queue
//            if (presetQueue.autoChange.value) {
//                presetQueue.next()
//            }
            // change preset
//            if (preset.autoChange.value) {
//                preset.next()
//            }
            // change sprite
            if (imgSprite.autoChange.value) {
                imgSprite.next()
            }

            if (imgSpriteFx.autoChange.value) {
                imgSpriteFx.next()
            }
            if (search.autoChange.value) {
                search.next()
            }
        }

        suspend fun warn() {
            val otherDecks = enabledDecks.value
                .mapNotNull { it.presetSwitching.currentPreset.value?.let { preset -> it.id to preset.preset } }
                .joinToString(", ", "{", "}") { (k, v) -> "$k: \"$v\"" }
            appendWarn("SKIPPED $deckName preset: \"${currentPreset.value?.preset}\" all decks: $otherDecks")
        }
    }

    val presetSwitching = PresetSwitching()


    suspend fun startFlows() {
        logger.info { "starting coroutines for $deckName" }

        ndTime.startFlows()
        ndColor.startFlows()
        ndStrobe.startFlows()
        ndAudio.startFlows()
        ndOutput.startFlows()

//        transitionTime
//            //TODO: combine with trigger flow
//            .combine(emitTransitionTime) { a, _ -> a }
//            .onEach {
//                logger.infoF { "$deckName set transition time" }
//                nestdropPortSend(
//                    OSCMessage(nestdropAddress("sTransitTime"), it)
//                )
//            }.launchIn(flowScope)

        presetSwitching.currentPreset
            .onEach {
                preset.name.value = it?.preset ?: "unset"
            }
            .launchIn(flowScope)

//        presetQueues.startFlows()
//        presetQueue.startFlows()
        preset.startFlows()
        spriteQueues.startFlows()
//        spriteQueue.startFlows()
        imgSprite.startFlows()
        imgSpriteFx.startFlows()
        spoutQueue.startFlows()
        spout.startFlows()
    }

    @Immutable
    inner class Search : MutableStateFlow<TagScoreEval?> by MutableStateFlow(null) {

        val autoChange = MutableStateFlow(false)

        suspend fun next() {
            search.value?.let { search ->

                val presets = presetsMap.value
                val presetTags = presetTagsMapping.value

//            val sortedKeys = presets.keys.sortedByDescending { key ->
//                val tags = presetTags[key].orEmpty()
//
//                search.score(tags)
//            }

                val filtered = presets.mapNotNull { (key, preset) ->
                    val tags = presetTags[key].orEmpty()

                    val score = search.score(tags)
//                val preset = presets[key]
                    if (score > 0.0) {
                        preset to (score)
                    } else {
                        null
                    }
                }.toMap()

                val id = pickItemToGenerate(filtered)
                nestdropSetPreset(id.id, deck = this@Deck.id)
            }
        }
//        val search = MutableStateFlow<TagScoreEval?>(null)
    }

    val search = Search()

    @Immutable
    inner class Preset {
        //        private val trigger = MutableStateFlow(0)
//        val autoChange = MutableStateFlow(false)
        val name = MutableStateFlow("uninitialized")

//        @Deprecated("use search.next()")
//        suspend fun next() {
//            logger.info { "$deckName.preset.next()" }
//            // TODO: trigger this by emitting a history event (queue, deck, index or such)
//            presetQueue.value?.also { queue ->
//                val index = Random.nextInt(queue.presets.size)
//                val preset1 = queue.presets.getOrNull(index) // ?: return
//                logger.info { "switching ${queue.name} to $index '$preset1'" }
//                nestdropPortSend(
//                    OSCMessage(
//                        "/PresetID/${queue.name}/$index",
//                        listOf(
//                            if (false) 0 else 1
//                        )
//                    )
//                )
//            }
//        }


        suspend fun startFlows() {
//            logger.info { "starting flows for $deckName-preset" }

//            trigger
//                .drop(1)
//                .onEach {
//                    next()
//                }
//                .launchIn(flowScope)
        }
    }

    val preset = Preset()

    @Immutable
    inner class SpriteQueues : MutableStateFlow<List<Queue>> by MutableStateFlow(emptyList()) {
        suspend fun startFlows() {
//            logger.info { "initializing $deckName sprite queues" }
        }
    }

    val spriteQueues = SpriteQueues()

    @Immutable
    inner class ImgSprite {
        //TODO: replace with a map ?
        val toggles = MutableStateFlow<Set<String>>(emptySet())

        //        private val trigger = MutableStateFlow(0)
        val autoChange = MutableStateFlow(false)

        val spriteImgLocation = MutableStateFlow<PresetLocation.Img?>(null)

        //        @Deprecated("switch to using scanned sprite img location")
//        val index = MutableStateFlow(-2) // OscSynced.ExclusiveSwitch("/deck$N/sprite/index", 40, -2)
        val name = MutableStateFlow("uninitialized")
        // OscSynced.Value("/deck$N/sprite/name", "uninitialized", receive = false)

//        private val nestdropSprite = NestdropSpriteQueue(
//            nestdropSendChannel
//        ) { spriteIndex ->
//            if (spriteIndex != -1) {
////                logger.debugF { "syncing spout after sprite change" }
////                spout.sync.trigger()
//                logger.debugF { "setting spritefx after sprite change" }
//                imgSpriteFx.setSpriteFx(
//                    imgSpriteFx.index.value,
//                    !imgSpriteFx.blendMode.value,
//                )
//                imgSpriteFx.setSpriteFx(
//                    imgSpriteFx.index.value,
//                    imgSpriteFx.blendMode.value,
//                )
////                spriteFX.sync.trigger()
////                spout.resend()
//            }
//        }

        suspend fun next() {
            logger.info { "$deckName.sprite.next()" }

            //TODO: load all available sprite locations

            // TODO: filter enabled sprites in deck?

            val enabledKeys = this.toggles.value
            val nextRandom = imgSpritesMap.value.filterKeys { it in enabledKeys }.values.randomOrNull()

            if (nextRandom != null) {
                spriteImgLocation.value = nextRandom
            }

//            spriteQueue.value?.also { spriteQueue ->
//                val enabledSprites = spriteQueue.presets.filterIndexed() { index, it ->
////                    logger.debugF { spriteQueue.name + " " +spriteQueue.type + " " + it }
//                    toggles[index].value
//                }
//                logger.debugF { "enabled sprites: $enabledSprites" }
//                if (enabledSprites.isNotEmpty()) {
//                    val next = enabledSprites.random()
//                    index.value = spriteQueue.presets.indexOf(next)
////                    withTimeout(100.milliseconds) {
////                        // block until new queue is loaded
////                        name.first { it == next }
////                    }
//                }
//            }
        }

        suspend fun startFlows() {
            logger.info { "starting coroutines on $deckName img-sprite" }

//            nestdropSprite.startFlows()

//            trigger
//                .drop(1)
//                .onEach {
//                    next()
//                    //spriteFX.sync.trigger()
//                }
//                .launchIn(flowScope)

            spriteImgLocation
                .runningHistory()
                .sample(50.milliseconds)
                .onEach { (next, previous) ->
                    name.value = next?.name ?: "-"
                    if (next == null && previous != null) {
                        nestdropSetSprite(previous.id, id)
                    } else if (next != null) {
                        nestdropSetSprite(next.id, id)
                    }
//                    nestdropSprite.send(
//                        when {
//                            presetIdState is PresetIdState.Data && presetIdState.index != -1 -> presetIdState
////                                .also {
////                                    logger.warnF { "sending $presetIdState" }
////                                }
//                            else -> PresetIdState.Unset
//                        }
//                    )
//                    spriteFX.sync.trigger()
                }
                .launchIn(flowScope)

//            index
////                .combine(resyncToTouchOSC) { a, _ -> a }
//                .combine(spriteQueue) { index, queue ->
//                    queue?.presets?.getOrNull(index)
//                }.onEach { spritePreset ->
////                    logger.infoF { "setting name of sprite $spritePreset" }
//                    name.value = spritePreset?.name ?: "-"
//                }
//                .launchIn(flowScope)

//            // do sprite change
//            index
//                // set current image sprite again if spout was enabled or changed
////                .combine(spout.index.filter { it != -1 }) { a, _ -> a }
//                .combine(spriteQueue) { index, queue ->
//                    when {
//                        queue != null ->
//                            PresetIdState.Data(
//                                index = index,
//                                queue = queue,
//                            )
//
//                        else -> PresetIdState.Unset
//
//                    }
//                }
//                //maybe debounce ? does this break history ?
//                .sample(50.milliseconds)
//                .onEach { presetIdState ->
//                    nestdropSprite.send(
//                        when {
//                            presetIdState is PresetIdState.Data && presetIdState.index != -1 -> presetIdState
////                                .also {
////                                    logger.warnF { "sending $presetIdState" }
////                                }
//                            else -> PresetIdState.Unset
//                        }
//                    )
////                    spriteFX.sync.trigger()
//                }
//                .launchIn(flowScope)
        }
    }

    val imgSprite = ImgSprite()

    @Immutable
    inner class ImgSpriteFX {
        val autoChange = MutableStateFlow(true)
        val blendMode = MutableStateFlow(false)
        val index = MutableStateFlow(0)
        val name = MutableStateFlow("uninitialized")
        val shortLabel = MutableStateFlow("uninitialized")
        val toggles = List(50) {
            MutableStateFlow(false)
//            OscSynced.Value("/deck$N/sprite_FX/toggle/${it}", false).apply {
//                logSending = false
//            }
        }
        val sync = MutableStateFlow(0)
        private val trigger = MutableStateFlow(0)

        suspend fun next() {
            logger.info { "$deckName.spriteFX.next()" }
            val enabledFx = imgFxMap.value.filterKeys { key ->
                toggles.getOrNull(key)?.value ?: false
            }.toList()
            if (enabledFx.isNotEmpty()) {
                val next = enabledFx.random()
                index.value = next.first
            }
        }

        suspend fun setSpriteFx(index: Int, blendMode: Boolean) {
            val fx = if (blendMode) index + 50 else index
            logger.info { "setting $deckName FX to $fx" }
            val arg = fx / 99.0f
            nestdropSendChannel.send(OSCMessage(nestdropDeckAddress("sSpriteFx"), arg))
        }

        suspend fun startFlows() {
            logger.info { "starting coroutines for $deckName img-sprite-fx" }

            trigger
                .drop(1)
                .onEach {
                    next()
                }
                .launchIn(flowScope)

            combine(index, blendMode, imgFxMap) { index, blendMode, spriteFXMap ->
                val fx = if (blendMode) index + 50 else index
                name.value = spriteFXMap[fx]
                    ?.let { "FX $fx: $it" }
                    ?: if (fx == -1) "uninitialized" else "unknown FX $fx"
                shortLabel.value = fx.takeIf { spriteFXMap.containsKey(fx) }?.let {
                    if (blendMode) {
                        "FX: $fx ($index + 50)"
                    } else {
                        "FX: $fx"
                    }
                } ?: "error"
            }.launchIn(flowScope)

            index
                .combine(sync) { a, _ -> a }
                .combine(blendMode) { index, blendMode ->
                    setSpriteFx(index, blendMode)
                }
//                .filter { fx -> fx in (0..99) }
//                .onEach { fx ->
//                    logger.infoF { "setting $deckName FX to $fx" }
//                    nestdropPortSend(nestdropAddress("sSpriteFx"), fx / 99.0f)
//                }
                .launchIn(flowScope)
        }
    }

    val imgSpriteFx = ImgSpriteFX()

    @Immutable
    inner class SpoutQueue : MutableStateFlow<Queue?> by MutableStateFlow(null) {
        val index = MutableStateFlow(-1)
        val name = MutableStateFlow("uninitialized")

        suspend fun startFlows() {
            logger.info { "starting coroutines on $deckName spout-queue" }
            index
//                .combine(resyncToTouchOSC) { a, _ -> a }
                .combine(spriteQueues) { index, queues ->
                    queues.getOrNull(index)
                }
                .onEach {
                    spoutQueue.value = it
                }.launchIn(flowScope)

            onEach {
                name.value = it?.name ?: "unset"
            }.launchIn(flowScope)
        }
    }

    val spoutQueue = SpoutQueue()

    @Immutable
    inner class Spout : MutableStateFlow<nestdrop.Preset?> by MutableStateFlow(null) {
//        val toggles = List(20) {
//            MutableStateFlow(false)
//        }

        val sync = MutableStateFlow(0)

        // val autoChange = MutableStateFlow(false)
        val index = MutableStateFlow(-1)
        val name = MutableStateFlow("uninitialized")
        val fx = MutableStateFlow(0)

        private val nestdropSpout = NestdropSpriteQueue(
            nestdropSendChannel
        )

        suspend fun startFlows() {
            logger.info { "starting coroutines on $deckName spout" }
            nestdropSpout.startFlows()

            index
//                .combine(resyncToTouchOSC) { a, _ -> a }
                .combine(spoutQueue) { index, queue ->
                    queue?.presets?.getOrNull(index)
                }.onEach { spoutPreset ->
                    this.value = spoutPreset
                    logger.info { "$deckName spout name\n${spoutPreset?.prettyPrint()}" }
                    fx.value = spoutPreset?.effects ?: 0
                    name.value = spoutPreset?.label ?: "-"
                }
                .launchIn(flowScope)

            // do spout change
            index
                .onEach {
                    logger.info { "$deckName spout index changed: $it" }
                }
//                .combine(resyncToTouchOSC) { a, _ -> a }
                .combine(spoutQueue
                    .onEach {
                        logger.info { "$deckName spout queue changed: ${it?.name}" }
                    }) { index, queue ->
                    index to queue
                }
                .combine(
                    sync
//                        .drop(1)
                        .onEach {
                            logger.info { "$deckName spout sync triggered" }
                        }
                ) { a, _ -> a }
//                .runningHistory()
                .sample(100.milliseconds)
                .onEach { (index, queue) ->
                    logger.info { "$deckName spout change: $index in ${queue?.name}" }
                    nestdropSpout.send(
                        when {
                            queue != null && index != -1 -> PresetIdState.Data(index, queue, true)
                            else -> PresetIdState.Unset
                        }
                    )
                }
                //maybe debounce ? does this break history ?
//                .onEach { (current, last) ->
//                    val (index, queue) = current
//                    if (index == -1) {
//                        if (last != null) {
//                            val (lastIndex, lastQueue) = last
//                            if (lastIndex != -1) {
//                                // unset send last index again
//                                presetId(lastQueue, lastIndex, hardcut = true)
//                            }
//                        }
//                    } else {
//                        if (index != last?.first) {
//                            presetId(queue, index)
//                        }
//                    }
//                }
                .launchIn(flowScope)
        }

//        suspend fun resend() {
//            delay(50)
//            val currentIndex = index.value
//            if (currentIndex >= 0) {
//                index.value -= 1
//                index.value += 1
//            }
//
//        }
    }

    val spout = Spout()


//    val skipHistory = MutableStateFlow(false)


    @Serializable
    data class DeckState(
        val deck: Int,
        val timestamp: String,
        val preset: String,
//        val presetQueue: String,
        val imgSprite: String,
        val imgFx: Int,
        val spoutSprite: String,
        val spoutFx: Int,
    ) {
        val timestampInstant get() = Instant.parse(timestamp)
    }

    val currentState: Flow<DeckState>
        get() = combine(
            preset.name,
            combine(imgSprite.name, imgSpriteFx.index) { a, b -> a to b },
            combine(spout.name, spout.fx) { a, b -> a to b }
        ) { preset, (imgSprite, imgFx), (spoutSprite, spoutFx) ->
            DeckState(
                deck = id,
                timestamp = Clock.System.now().let {
//                    var nanoseconds = (it.nanosecondsOfSecond / 1000_0000.0).roundToInt() * 1_000_0000
//                    if(nanoseconds==0) {
//                        nanoseconds += 1_000_0000
//                    }
//
//
//                    Instant.fromEpochSeconds(it.epochSeconds, nanoseconds).toString()

                    var milliseconds = it.toEpochMilliseconds()
                    if (milliseconds % 1_000L == 0L) {
                        milliseconds++
                    }
                    Instant.fromEpochMilliseconds(milliseconds).toString()
                },
                preset = preset,
//                presetQueue = presetQueue.name.value,
                imgSprite = imgSprite,
                imgFx = imgFx,
                spoutSprite = spoutSprite,
                spoutFx = spoutFx
            )
        }
//            .filter { !skipHistory.value }
//            .combine(skipHistory) { state, skipHistory ->
//                state.takeUnless { skipHistory }
//            }.filterNotNull()

    fun nestdropDeckAddress(address: String) = "/Controls/Deck$id/$address"


    val deckName: String = "Deck $id"
}

