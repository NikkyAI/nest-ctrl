package nestdrop.deck

import QUEUES
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import beatCounter
import beatFrame
import com.illposed.osc.OSCMessage
import configFolder
import decks
import flowScope
import imgSpritesMap
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
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
import osc.nestdropPortSend
import osc.nestdropSendChannel
import osc.stringify
import presetsFolder
import presetsMap
import queueFolder
import tags.PresetPlaylist
import tags.pickItemToGenerate
import tags.presetTagsMapping
import utils.className
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

    val dimmedColor = color.copy(alpha = 0.5f).compositeOver(Color.Black)
    val disabledColor = color.copy(alpha = 0.25f).compositeOver(Color.Black)
    val dimmedColorTransparent = color.copy(alpha = 0.5f) //.compositeOver(Color.Black)
    val disabledColorTransparent = color.copy(alpha = 0.25f) //.compositeOver(Color.Black)

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
        val waveMode = NestdropControl.SliderWithResetButton(id, "WaveMode", -15f..15f, 0f)

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
        val brightness = NestdropControl.SliderWithResetButton(id, "Brightness", 0.5f..1.0f, 1.0f)
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
        val transitionTime = MutableStateFlow(1f)
        val triggerTime = MutableStateFlow(0.75f)
        private val switchingLocked = MutableStateFlow(false)
        val isLocked = switchingLocked.asStateFlow()
        val transitTimeSync = MutableStateFlow(true)
        val transitTimeBeatframeFraction = MutableStateFlow(0.125f)
        val transitTimeBeats = MutableStateFlow(8)

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

        suspend fun startFlows() {
            combine(
                beatCounter.runningHistoryNotNull(),
                beatFrame,
                triggerTime,
                isEnabled,
            ) { (currentBeat, lastBeat), beatFrame, triggerTime, isEnabled ->
                val triggerAt = triggerTime * beatFrame
                if (isEnabled && !switchingLocked.value) {
                    val shouldTrigger =
                        (lastBeat < triggerAt && currentBeat >= triggerAt) || (lastBeat > beatFrame && currentBeat >= triggerAt)

                    if (shouldTrigger) {
//                        logger.debug { "$deckName triggered at $currentBeat $triggerAt" }
                        switchingLocked.emit(true)
                        flowScope.launch {
                            val transitionTime = ndTime.transitionTime.value.toDouble().seconds
                            delay(transitionTime)
                            switchingLocked.emit(false)
                        }
                        flowScope.launch {
                            doSwitch()
                        }
                    }
                }
                currentBeat
            }
                .launchIn(flowScope)
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
        presetSwitching.startFlows()
        search.startFlows()

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

                val selectedPreset = pickItemToGenerate(filtered)
                val pickedWeight = filtered[selectedPreset]
                logger.debug {
                    val selectedPresetTags = presetTags[selectedPreset.name]
                        .orEmpty()
                        .filter { it.namespace.first() != "nestdrop" }
                        .filter { it.namespace.first() != "queue" }

                    "picked ($pickedWeight / ${filtered.values.sum()}) ${selectedPreset.name}  out of ${filtered.size} options" +
                            "\n tags: ${selectedPresetTags.joinToString { it.toString() }}"
                }
                nestdropSetPreset(selectedPreset.id, deck = this@Deck.id)
            }
        }

        fun startFlows() {
            search.filterNotNull()
                .distinctUntilChanged()
                .onEach { search ->
                    val presets = presetsMap.value
                    val presetTags = presetTagsMapping.value
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

                    val folder = queueFolder.resolve("deck_$id").also {
                        it.mkdirs()
                    }
                    folder.listFiles()?.forEach { it.deleteRecursively() }

                    filtered.mapValues { it.value.roundToInt() }.forEach { (location, weight) ->
                        val presetFile = presetsFolder.resolve(location.path)
                        val previewFile = presetsFolder.resolve(location.previewPath)
                        if (weight == 1) {
                            val targetFolder = folder.resolve(location.path).parentFile!!
                            targetFolder.mkdirs()
                            targetFolder.resolve(presetFile.name).also {
                                presetFile.copyTo(it)
                            }
                            targetFolder.resolve(previewFile.name).also {
                                previewFile.copyTo(it)
                            }
                        } else {
                            repeat(weight) { i ->
                                val targetFolder = folder.resolve(location.path).parentFile!!
                                    .resolve("${location.name}_$i")
                                targetFolder.mkdirs()
                                targetFolder.resolve(presetFile.name).also {
                                    presetFile.copyTo(it)
                                }
                                targetFolder.resolve(previewFile.name).also {
                                    previewFile.copyTo(it)
                                }
                            }
                        }
                    }
                    nestdropPortSend(
                        OSCMessage("/Queue/<${folder.name}>/Refresh", 1)
                    )
                }
                .flowOn(Dispatchers.IO)
                .launchIn(flowScope)


        }
//        val search = MutableStateFlow<TagScoreEval?>(null)
    }

    val search = Search()

    @Serializable
    data class PresetData(
//        val id: Int,
        val presetId: String,
        val name: String,
    ) {
        val id: Int = presetId.substringAfter("/PresetID/").toIntOrNull() ?: -1
    }

    @Immutable
    inner class Preset {
        val currentPreset = object : OscSynced.FlowBase<PresetData>(
            "/Deck$id/Preset",
//            initialValue = PresetData(-1, "unitialized"),
            target = OscSynced.Target.Nestdrop
        ) {
            override fun convertMessage(message: OSCMessage): PresetData {
                val input = message.arguments
                return try {
                    //
                    PresetData(input[0] as String, input[1] as String)
                } catch (e: ClassCastException) {
                    logger.info { "input:" }
                    input.forEachIndexed { index, any ->
                        logger.info { "$index: ${any.className} $any" }
                    }
                    logger.error(e) { "fix your types" }
                    throw e
                }
            }
        }
            .stateIn(flowScope, SharingStarted.Eagerly, initialValue = PresetData("", "unitialized"))

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

//    @Immutable
//    inner class SpriteQueues :
//        MutableStateFlow<List<Queue<nestdrop.Preset.ImageSprite>>> by MutableStateFlow(emptyList()) {
//        suspend fun startFlows() {
////            logger.info { "initializing $deckName sprite queues" }
//        }
//    }

    //    @Deprecated("lookup queues from QUEUES.allQueues")
//    val imgSpriteQueues: MutableStateFlow<List<Queue<nestdrop.Preset.ImageSprite>>> = MutableStateFlow(emptyList())
//    @Deprecated("lookup queues from QUEUES.allQueues")
//    val spoutSpriteQueues: MutableStateFlow<List<Queue<nestdrop.Preset.SpoutSprite>>> = MutableStateFlow(emptyList())

    @Serializable
    data class SpriteData(
        val path: String = "",
//        val id: Int = -1,
        val name: String = "unset",
        val active: Int = 0,
        val mode: ImgMode = ImgMode.Overlay,
        val fx: Int = 0,
        val overlayCount: Int = 0,
        val nestedCount: Int = 0,
//        val enabled: Boolean = false,
        val isImg: Boolean = false,
    ) {
        val isActive = active == 1
        val isSpout = !isImg
        val id: Int = path.substringAfter("/PresetID/").toIntOrNull() ?: -1
        val key = SpriteKey(id, name, mode, fx)
    }

    @Serializable
    data class SpriteKey(
        val id: Int,
        val name: String = "unset",
        val mode: ImgMode = ImgMode.Overlay,
        val fx: Int = 0,
//        val mystery: Int = 0
    ) {
        val label: String = "$name\nFX: $fx"
    }

    @Immutable
    inner class SpriteState {
        val imgTarget = MutableStateFlow(emptySet<SpriteKey>())
        val imgStates = MutableStateFlow<Map<String, SpriteKey>>(mapOf())

        //        val spoutTarget = MutableStateFlow(emptySet<SpriteKey>())
        val spoutStates = MutableStateFlow<Map<String, SpriteKey>>(mapOf())
        private val spriteStateFlow = object : OscSynced.FlowBase<SpriteData>(
            "/Deck$id/Sprite",
//            SpriteData(),
            target = OscSynced.Target.Nestdrop
        ) {
            override fun convertMessage(message: OSCMessage): SpriteData {
                val args = message.arguments
                return try {
                    // TODO: report this random extra parameter
//                    val hasActiveParam = args[2] is Int
//                    val modeIntOffset = if(hasActiveParam) 1 else 0
                    SpriteData(
                        path = args[0] as String,
                        name = args[1] as String,
                        active = args[2] as Int,
                        mode = ImgMode.valueOf(args[3] as String),
                        fx = args[4] as Int,
                        overlayCount = args[5] as Int,
                        nestedCount = args[6] as Int,
//                        mystery = args[4+modeIntOffset] as Int,
//                        enabled = (args[5+modeIntOffset] as Int) == 1,
                    )
                } catch (e: ClassCastException) {
                    logger.info { "input: ${message.stringify()}" }
//                    args.forEachIndexed { index, any ->
//                        logger.info { "$index: ${any.className} $any" }
//                    }
                    logger.error(e) { "fix your types" }
                    throw e
                }
            }
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
//                .onEach {
//                    logger.info { "$deckName sprite: $it" }
//                }
                .combine(
                    imgSpritesMap.map { map -> map.values.map { img -> img.name }.toSet() }
                ) { spriteData, imgSpriteNames ->
                    spriteData.copy(
                        isImg = spriteData.name in imgSpriteNames
                    )
                }
                //TODO: remove workaround when bug is fixed in nestdrop
                .runningHistoryNotNull(SpriteData())
                .map { (spriteData, previousSpriteData) ->
//                    logger.info { "new $spriteData" }
//                    logger.info { "old $previousSpriteData" }
                    val skipSpoutSprite =
                        spriteData.isSpout && spriteData.fx == 0 && spriteData == previousSpriteData.copy(
                            path = spriteData.path,
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
                        logger.info { "is img $spriteState" }
                        val state = imgStates.value
                        val mutableState = state.toMutableMap()
                        if (spriteState.isActive) {
                            mutableState += (spriteState.name to spriteState.key)
                        } else {
                            mutableState -= spriteState.name
                        }
                        if (state != mutableState) {
                            imgStates.value = mutableState.toMap()
                        }
                    } else {
                        logger.info { "is spout: $spriteState" }
                        val state = spoutStates.value
                        val mutableState = state.toMutableMap()
                        if (spriteState.isActive) {
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

            combine(spoutStates, spoutQueue) { state, queue ->
                val spriteKey = queue?.presets?.firstOrNull() { sprite ->
                    state.values.any { spriteKey ->
                        spriteKey.name == sprite.name && spriteKey.fx == (sprite.effects ?: 0)
                    } ?: false
                }

                if (spriteKey != null) {
                    val index = queue.presets.indexOf(spriteKey)
                    logger.warn { "$deckName setting spout index: $spriteKey at index: $index" }
                    spout.index.value = index
                }
            }.launchIn(flowScope)
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
                    logger.info { "triggering spriteFX sync" }
                    imgSpriteFx.sync()
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

        suspend fun sync() {
            sync.value++
        }

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
            logger.info { "setting $deckName Sprite FX to $fx" }
            val arg = fx / 99.0f
            nestdropSendChannel.send(OSCMessage(nestdropDeckAddress("sSpriteFx"), arg))
        }

        suspend fun setSpriteFxRaw(rawFx: Int) {
            logger.info { "setting $deckName Sprite FX to $rawFx" }
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
            combine(
                index, blendMode, sync,
            ) { index, blendMode, _ ->
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
    inner class SpoutQueue : MutableStateFlow<Queue<nestdrop.Preset.SpoutSprite>?> by MutableStateFlow(null) {
        val index = MutableStateFlow(-1)
        val name = MutableStateFlow("uninitialized")

        suspend fun startFlows() {
            logger.info { "starting coroutines on $deckName spout-queue" }

            val spoutQueues = QUEUES.spoutQueues.map { queueMap ->
                queueMap.values.filter { queue -> queue.open }
            }.map { it.filter { it.deck == id } }
            index
//                .combine(resyncToTouchOSC) { a, _ -> a }
                .combine(spoutQueues) { index, queues ->
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
        : MutableStateFlow<nestdrop.Preset.SpoutSprite?> by MutableStateFlow(null) {
//        val toggles = List(20) {
//            MutableStateFlow(false)
//        }

        // val autoChange = MutableStateFlow(false)
        @Deprecated("use spoutImgTarget")
        val index = MutableStateFlow(-1)

//        val spriteTargetKey = MutableStateFlow<SpriteKey?>(null)

        private val spoutState = NestdropSpriteQueue(
            nestdropSendChannel,
            spriteState.spoutStates,
            spoutQueue
        )

        suspend fun setSpout(index: Int) {
            spoutState.send(
                PresetIdState.Data(
                    index = index,
//                    queue = queue, // as Queue<nestdrop.Preset>,
                    force = false,
                )
            )
        }

        suspend fun clearSpout() {
            spoutState.send(PresetIdState.Unset)
        }

        suspend fun startFlows() {
            logger.info { "starting coroutines on $deckName spout" }
            spoutState.startFlows()
        }

        private suspend fun setSpoutFx(rawFx: Int) {
            logger.info { "setting $deckName FX to $rawFx" }
            val arg = rawFx / 99.0f

            logger.info { "NESTDROP OUT: ${nestdropDeckAddress("sSpoutFx")} $arg" }
            nestdropSendChannel.send(OSCMessage(nestdropDeckAddress("sSpoutFx"), arg))
        }
    }

    val spout = Spout()

    @Serializable
    data class DeckState(
        val deck: Int,
        val timestamp: String,
        val preset: PresetData,
//        val presetQueue: String,
        val imgSprite: String,
        val imgFx: Int,
        val spoutSpriteKeys: List<SpriteKey>,
    ) {
        val timestampInstant get() = Instant.parse(timestamp)
    }

    val currentState: Flow<DeckState>
        get() = combine(
            preset.currentPreset,
            combine(imgSprite.name, imgSpriteFx.index) { a, b -> a to b },
            spriteState.spoutStates
        ) { presetData, (imgSprite, imgFx), spoutStateMap ->
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
                spoutSpriteKeys = spoutStateMap.values.toList(),
            )
        }
//            .filter { !skipHistory.value }
//            .combine(skipHistory) { state, skipHistory ->
//                state.takeUnless { skipHistory }
//            }.filterNotNull()

    fun nestdropDeckAddress(address: String) = "/Controls/Deck$id/$address"


    val deckName: String = "Deck $id"
}

