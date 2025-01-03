package nestdrop.deck

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import beatFrame
import configFolder
import decks
import flowScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import nestdrop.ImgMode
import nestdrop.NestdropControl
import nestdrop.NestdropSpriteQueue
import nestdrop.PresetIdState
import nestdrop.PresetLocation
import nestdrop.Queue
import nestdrop.imgFxMap
import nestdrop.nestdropSetPreset
import nestdrop.nestdropSetSprite
import osc.OSCMessage
import osc.OscSynced
import osc.nestdropSendChannel
import tags.PresetPlaylist
import tags.pickItemToGenerate
import tags.presetTagsMapping
import ui.screens.presetsMap
import ui.screens.imgSpritesMap
import utils.HistoryNotNull
import utils.prettyPrint
import utils.runningHistory
import utils.runningHistoryNotNull
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class Deck(
    val id: Int,
    val color: Color,
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

    val isEnabled = enabled.map { decksEnabled ->
        id <= decksEnabled
    }.stateIn(flowScope, SharingStarted.Lazily, false)

//    val color = Color(hexColor)
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
        val waveMode = NestdropControl.SliderWithResetButton(id, "WaveMode", -15f..15f, 0.0f)

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
//        val hasSwitched = MutableStateFlow(false)
        private val hasSwitchedNew = MutableStateFlow(false)
        val hasSwitched = hasSwitchedNew.asStateFlow()

        suspend fun resetLatch() {
//            hasSwitched.value = false
        }

        //        suspend fun beatFlowOldOld(
//            flow: Flow<HistoryNotNull<Double>>
//        ) = flow
//            .combine(
//                triggerTime.combine(beatFrame) { triggerTime, b ->
//                    logger.info { "$deckName triggerTime: ${triggerTime * b} ($triggerTime * $b)" }
//                    triggerTime * b
//                }
//            ) { (currentBeat, lastBeat), triggerAt ->
//                Triple(currentBeat, lastBeat, triggerAt)
//            }.onEach { (currentBeat, lastBeat, triggerAt) ->
//
////                logger.info { "currentBeat: $currentBeat" }
////                logger.info { "triggerAt: $triggerAt" }
//                if (!hasSwitched.value && lastBeat < triggerAt && currentBeat >= triggerAt) {
//                    logger.info { "$deckName triggered at ${(currentBeat * 1000).roundToInt() / 1000f} ${(triggerAt * 1000).roundToInt() / 1000f}" }
//                    hasSwitched.value = true
//                    doSwitch()
//                }
//            }
        suspend fun beatFlowOld(
            flow: Flow<HistoryNotNull<Double>>
        ) = combine(
                flow,
                triggerTime.combine(beatFrame) { triggerTime, b ->
                    logger.info { "$deckName triggerTime: ${triggerTime * b} ($triggerTime * $b)" }
                    triggerTime * b
                },
                isEnabled
            ) { (currentBeat, lastBeat), triggerAt, isEnabled ->
                if (isEnabled && !hasSwitched.value && lastBeat < triggerAt && currentBeat >= triggerAt) {
                    logger.info { "$deckName triggered at ${(currentBeat * 1000).roundToInt() / 1000f} ${(triggerAt * 1000).roundToInt() / 1000f}" }
//                    hasSwitched.value = true
                    doSwitch()
                }
            }

        suspend fun beatFlow(
            flow: Flow<HistoryNotNull<Double>>
        ) = combine(
            flow,
            beatFrame,
            triggerTime,
            isEnabled,
        ) { (currentBeat, lastBeat), beatFrame, triggerTime, isEnabled ->
            val triggerAt = triggerTime * beatFrame
//                logger.info { "currentBeat: $currentBeat" }
//                logger.info { "triggerAt: $triggerAt" }
//                logger.info { "lastBeat: $lastBeat" }
            //TODO: reset hasSwitched after transition time has passed
            if (isEnabled && !hasSwitchedNew.value) {
                val shouldTrigger = (lastBeat < triggerAt && currentBeat >= triggerAt) || (lastBeat > beatFrame && currentBeat >= triggerAt)

                if(shouldTrigger) {
                    logger.info { "$deckName triggered at $currentBeat ($triggerAt)" }
                    hasSwitchedNew.value = true
                    flowScope.launch {
                        val transitionTime = ndTime.transitionTime.value.toDouble().seconds
                        delay(transitionTime)
                        hasSwitchedNew.value = false
                    }
                    doSwitch()
                }
            }
            currentBeat
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
                .map { it.preset.currentPreset.value }
                .joinToString(", ", "{", "}") { (k, v) -> "$k: \"$v\"" }
            appendWarn("SKIPPED $deckName preset: \"${preset.currentPreset.value.name}\" all decks: $otherDecks")
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

//        presetSwitching.currentPreset
//            .onEach {
//                preset.name.value = it?.preset ?: "unset"
//            }
//            .launchIn(flowScope)

//        presetQueues.startFlows()
//        presetQueue.startFlows()
        preset.startFlows()
        spriteQueues.startFlows()
//        spriteQueue.startFlows()
        spriteState.startFlows()
        imgSprite.startFlows()
        imgSpriteFx.startFlows()
        spoutQueue.startFlows()
        spout.startFlows()
    }

    @Immutable
    inner class Search : MutableStateFlow<PresetPlaylist?> by MutableStateFlow(null) {

        val autoChange = MutableStateFlow(false)

        suspend fun next() {
            if (id > enabled.value) {
                logger.info { "skip disabled $deckName" }
                return
            }
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

    @Serializable
    data class PresetData(
        val id: Int,
        val name: String,
    )

    @Immutable
    inner class Preset {
        val currentPreset = OscSynced.FlowCustom(
            "/Deck$id/Preset",
//            initialValue = PresetData(-1, "unitialized"),
            target = OscSynced.Target.Nestdrop
        ) { address, arguments ->
            PresetData(arguments[0] as Int, arguments[1] as String)
        }
            .stateIn(flowScope, SharingStarted.Eagerly, initialValue = PresetData(-1, "unitialized"))

        suspend fun startFlows() {
//            logger.info { "starting flows for $deckName-preset" }

            currentPreset
                .onEach {
                    logger.info { "$deckName preset syncedValue: $it" }
                }
                .launchIn(flowScope)
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

    @Serializable
    data class SpriteData(
        val id: Int = -1,
        val name: String = "unset",
        val mode: ImgMode = ImgMode.Overlay,
        val fx: Int = 0,
        val mystery: Int = 0,
        val enabled: Boolean = false,
        val isImg: Boolean = false,
    ) {
        val isSpout = !isImg
        val key = SpriteKey(id, name, mode, fx, mystery)
    }

    @Serializable
    data class SpriteKey(
        val id: Int = -1,
        val name: String = "unset",
        val mode: ImgMode = ImgMode.Overlay,
        val fx: Int = 0,
        val mystery: Int = 0
    )

    @Immutable
    inner class SpriteState {
        val imgTarget = MutableStateFlow(emptySet<SpriteKey>())
        val imgStates = MutableStateFlow<Map<String, SpriteKey>>(mapOf())

        val spoutTarget = MutableStateFlow(emptySet<SpriteKey>())
        val spoutStates = MutableStateFlow<Map<String, SpriteKey>>(mapOf())
        private val spriteStateFlow = OscSynced.FlowCustom(
            "/Deck$id/Sprite",
//            SpriteData(),
            target = OscSynced.Target.Nestdrop
        ) { _, args ->
            SpriteData(
                id = args[0] as Int,
                name = args[1] as String,
                mode = ImgMode.valueOf(args[2] as String),
                fx = args[3] as Int,
                mystery = args[4] as Int,
                enabled = (args[5] as Int) == 1,
            )
        }

//        suspend fun enabledSprites() = imgStates.value.values

        suspend fun startFlows() {
//
//            syncedSpriteState
//                .sample(100.milliseconds)
//                .onEach {
//                    logger.info { "syncedSpriteState: $it" }
//                }
//                .launchIn(flowScope)
//            val imgSpriteIds = imgSpritesMap.map { it.values.map { it.id }.toSet() } //.stateIn(flowScope)
            spriteStateFlow
                .onEach {
                    logger.info { "$deckName sprite: $it" }
                }

                .combine(
                    imgSpritesMap.map { it.values.map { it.id }.toSet() }
                ) { spriteData, imgSpriteIds ->
                    spriteData.copy(isImg = spriteData.id in imgSpriteIds)
                }
                //TODO: remove workaround when bug is fixed in nestdrop
                .runningHistoryNotNull(SpriteData())
                .map { (spriteData, previousSpriteData) ->
//                    logger.info { "new $spriteData" }
//                    logger.info { "old $previousSpriteData" }
                    val skipSpoutSprite =
                        spriteData.isSpout && spriteData.fx == 0 && spriteData == previousSpriteData.copy(
                            id = spriteData.id,
                            fx = 0
                        )
                    if (skipSpoutSprite) {
                        previousSpriteData
                    } else {
                        spriteData
                    }
                }
                .onEach { spriteState ->
                    if (spriteState.isImg) {
//                        logger.info { "is img $spriteState" }
                        val state = imgStates.value
                        val mutableState = state.toMutableMap()
                        if (spriteState.enabled) {
                            mutableState += (spriteState.name to spriteState.key)
                        } else {
                            mutableState -= spriteState.name
                        }
                        if (state != mutableState) {
                            imgStates.value = mutableState.toMap()
                        }
                    } else {
//                        logger.info { "is spout: $spriteState" }
                        val state = spoutStates.value
                        val mutableState = state.toMutableMap()
                        if (spriteState.enabled) {
                            mutableState += spriteState.name to spriteState.key
                        } else {
                            mutableState -= spriteState.name
                        }
                        if (state != mutableState) {
                            spoutStates.value = mutableState.toMap()
                        }

                    }
                }
                .launchIn(flowScope)
        }

        private suspend fun setSpoutFx(rawFx: Int) {
            logger.info { "setting $deckName FX to $rawFx" }
            val arg = rawFx / 99.0f

            logger.info { "NESTDROP OUT: ${nestdropDeckAddress("sSpoutFx")} $arg" }
            nestdropSendChannel.send(OSCMessage(nestdropDeckAddress("sSpoutFx"), arg))
        }
    }

    val spriteState = SpriteState()

    @Immutable
    inner class ImgSprite {
        //TODO: replace with a map ?
        val toggles = MutableStateFlow<Set<String>>(emptySet())

        //        private val trigger = MutableStateFlow(0)
        val autoChange = MutableStateFlow(false)

        val spriteImgLocation = MutableStateFlow<PresetLocation.Img?>(null)
        private val spriteImgTarget = MutableStateFlow<SpriteKey?>(null)

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
            if (id > enabled.value) {
                return
            }
            logger.info { "$deckName sprite.next()" }

            //TODO: load all available sprite locations

            // TODO: filter enabled sprites in deck?

            val enabledKeys = this.toggles.value
            val nextRandom = imgSpritesMap.value.filterKeys { it in enabledKeys }.values.randomOrNull()

            if (nextRandom != null) {
                spriteImgTarget.value = SpriteKey(
                    id = nextRandom.id,
                    name = nextRandom.name,
                    mode = ImgMode.Nested,
                    fx = -1 // to be set later
                )
                spriteImgLocation.value = nextRandom
            }
        }

        suspend fun startFlows() {
            logger.info { "starting coroutines on $deckName img-sprite" }

            spriteImgTarget
                .combine(imgSpriteFx.rawFx) { key, fx -> key?.copy(fx = fx) }
                .runningHistory()
                .sample(50.milliseconds)
                .onEach { (next, previous) ->
                    val imgTarget = spriteState.imgTarget.value
                    val imgTargetNew = imgTarget.toMutableSet()
                    if (previous != null) {
                        logger.info { "$deckName removing $previous" }
                        imgTargetNew -= previous
                    }
                    if (next != null) {
                        logger.info { "$deckName adding $next" }
                        imgTargetNew += next
                    }
                    if (imgTargetNew != imgTarget) {
                        spriteState.imgTarget.value = imgTargetNew.toSet()
                    }
                    //TODO: implement multiple sprites
                    // manage the set of active sprites
                    // adding and removing via OSC commands
                }
                .launchIn(flowScope)
            spriteImgLocation
                .runningHistory()
                .sample(50.milliseconds)
                .onEach { (next, previous) ->
                    name.value = next?.name ?: "-"

//                    val previousData = previous?.let {
//                        SpriteKey(
//                            previous.id, previous.name, mode = ImgMode.Nested, previous.fx
//                        )
//                    }
//
//                    val spriteData = SpriteData(
//
//                    )
//                    spriteState.target += spriteData.key to spriteData

                    if (next == null && previous != null) {
                        nestdropSetSprite(previous.id, id, single = true)
                    } else if (next != null) {
                        nestdropSetSprite(next.id, id, single = true)
                    }
                }
                .launchIn(flowScope)

        }
    }

    val imgSprite = ImgSprite()

    @Immutable
    inner class ImgSpriteFX {
        val autoChange = MutableStateFlow(true)
        val blendMode = MutableStateFlow(false)
        val index = MutableStateFlow(0)
        val rawFx = index.combine(blendMode) { fx, blendMode ->
            if (blendMode) 50 + fx else fx
        }
        val name = MutableStateFlow("uninitialized")
        val shortLabel = MutableStateFlow("uninitialized")
        val toggles = List(50) {
            MutableStateFlow(false)
//            OscSynced.Value("/deck$N/sprite_FX/toggle/${it}", false).apply {
//                logSending = false
//            }
        }
        private val sync = MutableStateFlow(0)
        private val trigger = MutableStateFlow(0)

        suspend fun next() {
            if (id > enabled.value) {
                return
            }
            logger.info { "$deckName spriteFX.next()" }
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

        suspend fun setSpriteFxRaw(rawFx: Int) {
            logger.info { "setting $deckName FX to $rawFx" }
            val arg = rawFx / 99.0f
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

            //TODO: remove
            //  handled via passing raw FX into SpriteKey
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
    inner class Spout
        : MutableStateFlow<nestdrop.Preset?> by MutableStateFlow(null) {
//        val toggles = List(20) {
//            MutableStateFlow(false)
//        }

        val sync = MutableStateFlow(0)

        // val autoChange = MutableStateFlow(false)
//        @Deprecated("use spoutImgTarget")
        val index = MutableStateFlow(-1)

        @Deprecated("use spoutImgTarget")
        val name = MutableStateFlow("uninitialized")

        @Deprecated("use spoutImgTarget")
        val fx = MutableStateFlow(0)
        val spriteTargetKey = MutableStateFlow<SpriteKey?>(null)

        private val nestdropSpout = NestdropSpriteQueue(
            nestdropSendChannel
        )

        suspend fun startFlows() {
            logger.info { "starting coroutines on $deckName spout" }
            nestdropSpout.startFlows()

            spriteTargetKey
                .runningHistory()
                .sample(50.milliseconds)
                .onEach { (next, previous) ->
                    val spoutTarget = spriteState.spoutTarget.value
                    val spoutTargetNew = spoutTarget.toMutableSet()
                    if (previous != null) {
                        logger.info { "$deckName removing $previous" }
                        spoutTargetNew -= previous
                    }
                    if (next != null) {
                        logger.info { "$deckName adding $next" }
                        spoutTargetNew += next
                    }
                    if (spoutTargetNew != spoutTarget) {
                        spriteState.spoutTarget.value = spoutTargetNew.toSet()
                    }
                }
                .launchIn(flowScope)

            index
//                .combine(resyncToTouchOSC) { a, _ -> a }
                .combine(spoutQueue) { index, queue ->
                    logger.info { "spout queue index $index" }
                    queue?.presets?.getOrNull(index)
                }.onEach { spoutPreset ->
//                    this.value = spoutPreset
                    logger.info { "spout queue preset $spoutPreset" }
//                    spriteTargetKey.value = spoutPreset?.let {
//                        SpriteKey(id = it.id, name = it.name, mode = if(it.overlay == true) ImgMode.Overlay else ImgMode.Nested, fx = it.effects ?: 0)
//                    }
                    logger.info { "$deckName spout name\n${spoutPreset?.prettyPrint()}" }
                    fx.value = spoutPreset?.effects ?: 0
                    name.value = spoutPreset?.label ?: "-"
                }
                .launchIn(flowScope)

            //TODO: compare if target spout queue is in active sprites and trigger change if necessary

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
//                    queue?.presets?.getOrNull(index)?.effects?.let { fx ->
//                        setSpoutFx(fx)
//                    }
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
//        }

        private suspend fun setSpoutFx(rawFx: Int) {
            logger.info { "setting $deckName FX to $rawFx" }
            val arg = rawFx / 99.0f

            logger.info { "NESTDROP OUT: ${nestdropDeckAddress("sSpoutFx")} $arg" }
            nestdropSendChannel.send(OSCMessage(nestdropDeckAddress("sSpoutFx"), arg))
        }
    }

    val spout = Spout()


//    val skipHistory = MutableStateFlow(false)


    @Serializable
    data class DeckState(
        val deck: Int,
        val timestamp: String,
        val preset: PresetData,
//        val presetQueue: String,
        val imgSprite: String,
        val imgFx: Int,
        val spoutSpriteKey: SpriteKey?,
    ) {
        val timestampInstant get() = Instant.parse(timestamp)
    }

    val currentState: Flow<DeckState>
        get() = combine(
            preset.currentPreset,
            combine(imgSprite.name, imgSpriteFx.index) { a, b -> a to b },
            spout.spriteTargetKey
        ) { presetData, (imgSprite, imgFx), spoutSpriteKey ->
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
                preset = presetData,
//                presetQueue = presetQueue.name.value,
                imgSprite = imgSprite,
                imgFx = imgFx,
                spoutSpriteKey = spoutSpriteKey,
            )
        }
//            .filter { !skipHistory.value }
//            .combine(skipHistory) { state, skipHistory ->
//                state.takeUnless { skipHistory }
//            }.filterNotNull()

    fun nestdropDeckAddress(address: String) = "/Controls/Deck$id/$address"


    val deckName: String = "Deck $id"
}

