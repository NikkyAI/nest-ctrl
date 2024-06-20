package nestdrop.deck

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import beatFrame
import com.illposed.osc.OSCMessage
import flowScope
import io.klogging.logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import logging.debugF
import logging.infoF
import nestdrop.NestdropSpriteQueue
import nestdrop.PresetIdState
import nestdrop.NestdropControl
import nestdrop.PerformanceLogRow
import nestdrop.Queue
import nestdrop.imgFxMap
import osc.OSCMessage
import osc.OscSynced
import osc.OscSynced.Target
import osc.nestdropPortSend
import osc.nestdropSendChannel
import utils.HistoryNotNull
import utils.runningHistory
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class Deck(
    val N: Int,
    val first: Boolean,
    val last: Boolean,
    val hexColor: Long,
    val presetQueues: PresetQueues
) {
    val color = Color(hexColor)
    val dimmedColor = color.copy(alpha = 0.5f).compositeOver(Color.Black)

    companion object {
        private val logger = logger(Deck::class.qualifiedName!!)
    }


    val bpmSyncEnabled = OscSynced.Value<Boolean>("/deck$N/bpmSync", false, target = Target.TouchOSC)
    val bpmSyncMultiplier = OscSynced.Value<Int>("/deck$N/bpmSyncMultiplier", 4, target = Target.TouchOSC)

    inner class NdTime {

        // time
        val transitionTime =
            NestdropControl.SliderWithResetButton(N, "TransitTime", 0.0f..30.0f, 5.0f, sendResetMessage = false)
        val animationSpeed = NestdropControl.SliderWithResetButton(N, "AnimSpeed", 0f..2f, 1.0f)
        val zoomSpeed = NestdropControl.SliderWithResetButton(N, "ZoomSpeed", 0.5f..1.5f, 1.0f)
        val rotationSpeed = NestdropControl.SliderWithResetButton(N, "RotationSpeed", 0f..2f, 1.0f)
        val wrapSpeed = NestdropControl.SliderWithResetButton(N, "WrapSpeed", 0f..2f, 1.0f)
        val horizontalMotion = NestdropControl.SliderWithResetButton(N, "HorizonMotion", -0.5f..0.5f, 0.0f)
        val verticalMotion = NestdropControl.SliderWithResetButton(N, "VerticalMotion", -0.5f..0.5f, 0.0f)
        val stretchSpeed = NestdropControl.SliderWithResetButton(N, "StretchSpeed", 0.5f..1.5f, 1.0f)
        val waveMode = NestdropControl.SliderWithResetButton(N, "WaveMode", 0.5f..1.5f, 1.0f)

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

//            bpm
//                .map {
//                    val beatsPerSecond = it / 60.0f
//                    beatsPerSecond * 4.0f
//                }
//                .map {
//                    (it * 100).roundToInt() / 100f
//                }
//                .distinctUntilChanged()
//                .combine(syncBpmAndTransitionTime) { v, toggle ->
//                    v to toggle
//                }
//                .onEach { (v, toggle) ->
//                    if(toggle) {
//                        ndTransitionTime.value = v
//                    }
//                }
//                .launchIn(flowScope)
        }
    }

    val ndTime = NdTime()

    // color
    inner class NdColor {
        val negative = NestdropControl.Slider(N, "Negative", 0f..1f, 0f)
        val brightness = NestdropControl.SliderWithResetButton(N, "Brightness", 0.5f..1.5f, 1.0f)
        val contrast = NestdropControl.SliderWithResetButton(N, "Contrast", 0.5f..1.5f, 1.0f)
        val gamma = NestdropControl.SliderWithResetButton(N, "Gamma", 0.5f..1.5f, 1.0f)
        val hueShift = NestdropControl.SliderWithResetButton(N, "Hue", 0f..2f, 0f)
        val saturation = NestdropControl.SliderWithResetButton(N, "Saturation", 0f..1f, 1f)
        val lumaKey = NestdropControl.RangeSliderWithResetButton(N, "LumaKey", 0f..1f, 0f to 1f)
        val red = NestdropControl.SliderWithResetButton(N, "R", 0f..1f, 1f)
        val green = NestdropControl.SliderWithResetButton(N, "G", 0f..1f, 1f)
        val blue = NestdropControl.SliderWithResetButton(N, "B", 0f..1f, 1f)
        val alpha = NestdropControl.SliderWithResetButton(N, "Alpha", 0f..1f, 1f)

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
    inner class NdStrobe {

        val effect = NestdropControl.Dropdown(N, "StrobeEffect", {Effect.entries.indexOf(it)}, Effect.AnimationSpeed)
        val effectSpan = NestdropControl.RangeSliderWithResetButton(N, "StrobeEffectSpan", 0f..1f, 0f to 1f)
        val trigger = NestdropControl.Dropdown(N, "StrobeTrigger", {Trigger.entries.indexOf(it)}, Trigger.TimesPerSecond)
        val waveForm = NestdropControl.Dropdown(N, "StrobeRamp", {Waveform.entries.indexOf(it)}, Waveform.Square)
        val effectSpeed = NestdropControl.Slider(N, "StrobeSpeed", 0.01f..30f, 3f)
        val pulseWidth = NestdropControl.SliderWithResetButton(N, "StrobePulseWidth", 0f..1f, 1f)
        val enabled = NestdropControl.ToggleButton(N, "StrobeOnOff", false)

        suspend fun startFlows() {
            effectSpan.startFlows()
            effectSpeed.startFlows()
            pulseWidth.startFlows()
            enabled.startFlows()

            Link.bpm
                .combine(bpmSyncMultiplier) { bpm, multiplier ->
                    val beatsPerSecond = bpm / 60.0f
                    beatsPerSecond / multiplier
                }
                .map {
                    (it * 100).roundToInt() / 100f
                }
                .combine(bpmSyncEnabled) { a, b -> a to b }
                .distinctUntilChanged()
                .onEach { (value, toggle) ->
                    if (toggle) {
                        effectSpeed.value = value
                    }
                }
                .launchIn(flowScope)
        }
    }

    val ndStrobe = NdStrobe()

    // audio
    inner class NdAudio {

        val bass = NestdropControl.Slider(N, "Bass", 0f..1f, 1f)
        val mid = NestdropControl.Slider(N, "Mid", 0f..1f, 1f)
        val treble = NestdropControl.Slider(N, "Treble", 0f..1f, 1f)
        suspend fun startFlows() {
            bass.startFlows()
            mid.startFlows()
            treble.startFlows()
        }
    }

    val ndAudio = NdAudio()

    // output
    inner class NdOutput {
        val ndDeckPinToTop = NestdropControl.ToggleButton(N, "TopMost", false)

        suspend fun startFlows() {
            ndDeckPinToTop.startFlows()
        }
    }

    val ndOutput = NdOutput()

    val transitionTime: OscSynced.Value<Float> =
        OscSynced.Value("/deck$N/transitionTime", 1.0f, target = Target.TouchOSC)
    val triggerTime: OscSynced.Value<Float> = OscSynced.Value("/deck$N/triggerTime", 0.75f, target = Target.TouchOSC)
    val currentPreset = MutableStateFlow<PerformanceLogRow?>(null)

    val emitTransitionTime = OscSynced.Trigger("/deck$N/transitionTime/sync")

    suspend fun startFlows() {
        logger.infoF { "initializing $deckName" }

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

        currentPreset
            .onEach {
                preset.name.value = it?.preset ?: "unset"
            }
            .launchIn(flowScope)

//        presetQueues.startFlows()
        presetQueue.startFlows()
        preset.startFlows()
        spriteQueues.startFlows()
        spriteQueue.startFlows()
        imgSprite.startFlows()
        imgSpriteFx.startFlows()
        spoutQueue.startFlows()
        spout.startFlows()
    }


//    @Deprecated("use new preset queues")
//    inner class PresetQueues : MutableStateFlow<List<Queue>> by MutableStateFlow(emptyList()) {
//
//        suspend fun startFlows() {
//            logger.infoF { "initializing $deckName preset queues" }
//        }
//    }

//    val presetQueues = PresetQueues()

    inner class PresetQueue : MutableStateFlow<Queue?> by MutableStateFlow(null) {
        //        val index = OSCSynced.ValueWithState("/deck$N/queue/index", -1) { newValue, state ->
//            logger.warnF { "$newValue + $state" }
//            newValue.takeUnless { it == state } ?: -1
//        }
//        val index = OSCSynced.Value("/deck$N/queue/index", -1)
        private val trigger = OscSynced.Trigger("/deck$N/preset_queue/next")
        val name = OscSynced.Value("/deck$N/preset_queue/name", "uninitialized", receive = false)

        val index = MutableStateFlow(-1)

        //        @Deprecated("stop using touch osc")
        // val index = OscSynced.ExclusiveSwitch(addressPrefix = "/deck$N/preset_queue/index", n = 20, initialValue = -1)
//        @Deprecated("stop using touch osc")
//        private val enabled = List(20) {
//            OscSynced.Value(address = "/deck$N/preset_queue/enabled/$it", initialValue = true, receive = false)
//        }
//        private val labels = List(20) {
//            OscSynced.Value("/deck$n/preset_queue/label/$it", "", receive = false).apply {
//                logSending = false
//            }
//        }
        val toggles = List(20) {
            MutableStateFlow(false)
//            OscSynced.Value("/deck$N/preset_queue/toggle/${it}", false).apply {
////            logSending = false
//            }
        }
        val autoChange = OscSynced.Value("/deck$N/preset_queue/autoChange", false)

        suspend fun next() {
            logger.infoF { "$deckName.presetQueue.next()" }
            // change preset queue
//            logger.debugF { "available queues: ${presetQueues.value}" }
            val enabledQueues = presetQueues.value.filterIndexed { index, queue ->
                queue.deck == N && toggles[index].value
            }
            logger.debugF { "enabled queues: $enabledQueues" }
            if (enabledQueues.isNotEmpty()) {
                val next = enabledQueues.random()
                index.value = presetQueues.value.indexOfFirst {
                    it.deck == N && it.name == next.name
                }
                withTimeout(100.milliseconds) {
                    // block until new queue is loaded
                    this@PresetQueue.filterNotNull().first { it.name == next.name }.also { newQueue ->
                        logger.debugF { "queue loaded: ${newQueue.name}" }
                    }

                }
            }
        }

        suspend fun startFlows() {
            logger.infoF { "initializing $deckName preset queue" }

//            presetQueues
//                .onEach {
//                    labels.forEachIndexed { index, label ->
//                        label.value = it.getOrNull(index)
//                            ?.name
//                            ?: ""
//                    }
//                }
//                .launchIn(flowScope)

            trigger
                .drop(1)
                .onEach {
                    next()
                }
                .launchIn(flowScope)
//            index
//                .debounce(100)
//                .runningHistory(index.value)
//                .onEach { (current, last) ->
//                    if(current >= 0 && current == last)
//                    {
//                        logger.infoF { "detected clicking on already active queue" }
//                        index.value = -1
//                    }
//                }
//                .launchIn(flowScope)

//            presetQueues
//                .onEach { queues ->
//                    queues.mapIndexed { index, queue ->
//                        enabled[index].value = queue.deck == N
//                    }
//                }
//                .launchIn(flowScope)

            index
//                .combine(resyncToTouchOSC) { a, _ -> a }
                .combine(presetQueues) { index, queues ->
                    queues.getOrNull(index)
                        ?.takeIf { it.deck == N }
                }.onEach { queue ->
                    presetQueue.value = queue
                }
                .launchIn(flowScope)

            this
                .runningHistory(null)
//                .combine(resyncToTouchOSC) { a, _ -> a }
                .onEach { (queue, previousQueue) ->
                    logger.infoF { "$deckName queue: ${queue?.name}" }
                    presetQueue.name.value = queue?.name ?: "disabled"
                    if (queue != null && previousQueue?.name != queue.name) {
                        nestdropPortSend(
                            OSCMessage("/Queue/${queue.name}", listOf(1))
                        )
                    } else {
                        if (previousQueue != null) {

                            nestdropPortSend(
                                OSCMessage("/Queue/${previousQueue.name}", listOf(0))
                            )
                        }
                    }
                }
                .launchIn(flowScope)
        }
    }

    val presetQueue = PresetQueue()


    inner class Preset {
        private val trigger = OscSynced.Trigger("/deck$N/preset/next")
        val autoChange = OscSynced.Value("/deck$N/preset/autoChange", false)
        val name = OscSynced.Value("/deck$N/preset/name", "uninitialized", receive = false)

        suspend fun next() {
            logger.infoF { "$deckName.preset.next()" }
            emitTransitionTime.trigger()
            //    setTransitionTimeDeck1()
            // TODO: trigger this by emitting a history event (queue, deck, index or such)
            presetQueue.value?.also { queue ->
                val index = Random.nextInt(queue.presets.size)
                val preset1 = queue.presets.getOrNull(index) // ?: return
                logger.infoF { "switching ${queue.name} to $index '$preset1'" }
                nestdropPortSend(
                    OSCMessage(
                        "/PresetID/${queue.name}/$index",
                        listOf(
                            if (false) 0 else 1
                        )
                    )
                )
            }
        }


        suspend fun startFlows() {
            logger.infoF { "initializing $deckName preset" }

            trigger
                .drop(1)
                .onEach {
                    next()
                }
                .launchIn(flowScope)
        }
    }

    val preset = Preset()

    inner class SpriteQueues : MutableStateFlow<List<Queue>> by MutableStateFlow(emptyList()) {
        val labels = List(20) {
            OscSynced.Value("/deck$N/sprite_queues/label/$it", "", receive = false).apply {
                logSending = false
            }
        }

        suspend fun startFlows() {
            logger.infoF { "initializing $deckName sprite queues" }

            this
                .onEach {
                    spriteQueues.labels.forEachIndexed { index, label ->
                        label.value = it.getOrNull(index)
                            ?.name
                            ?: ""
                    }
                }
                .launchIn(flowScope)
        }
    }

    val spriteQueues = SpriteQueues()

    inner class SpriteQueue : MutableStateFlow<Queue?> by MutableStateFlow(null) {
        val index = OscSynced.Value("/deck$N/sprite_queue/index", -1)
        val name = OscSynced.Value("/deck$N/sprite_queue/name", "uninitialized", receive = false)

        suspend fun startFlows() {
            logger.infoF { "initializing $deckName sprite queue" }


            this
                .onEach {
                    name.value = it?.name ?: "unset"
                }
                .launchIn(flowScope)

            index
//                .combine(resyncToTouchOSC) { a, _ -> a }
                .combine(spriteQueues) { index, queues ->
                    queues.getOrNull(index)
                }
                .onEach {
                    spriteQueue.value = it
                }.launchIn(flowScope)

            this
                .onEach {
                    it?.presets?.let { presets ->
                        imgSprite.labels.forEachIndexed { index, label ->
                            label.value = presets.getOrNull(index)
                                ?.name
                                ?.substringBeforeLast(".jpg")
                                ?.substringBeforeLast(".png")
//                        ?.also {
//                            logger.debugF { "label: $it" }
//                        }
                                ?: ""
                        }
                    }
                }
                .launchIn(flowScope)
        }
    }

    val spriteQueue = SpriteQueue()

    inner class ImgSprite {
        val toggles = List(40) {
            OscSynced.Value("/deck$N/sprite/toggle/${it}", false).apply {
                logSending = false
            }
        }
        val labels = List(40) {
            OscSynced.Value("/deck$N/sprite/label/${it}", "", receive = false).apply {
                logSending = false
            }
        }
        private val trigger = OscSynced.Trigger("/deck$N/sprite/next")
        val autoChange = OscSynced.Value("/deck$N/sprite/autoChange", true)

        val index = MutableStateFlow(-2) // OscSynced.ExclusiveSwitch("/deck$N/sprite/index", 40, -2)
        val name =
            MutableStateFlow("uninitialized") // OscSynced.Value("/deck$N/sprite/name", "uninitialized", receive = false)

        private val nestdropSprite = NestdropSpriteQueue(
            nestdropSendChannel
        ) { spriteIndex ->
            if (spriteIndex != -1) {
//                logger.debugF { "syncing spout after sprite change" }
//                spout.sync.trigger()
                logger.debugF { "setting spritefx after sprite change" }
                imgSpriteFx.setSpriteFx(
                    imgSpriteFx.index.value,
                    !imgSpriteFx.blendMode.value,
                )
                imgSpriteFx.setSpriteFx(
                    imgSpriteFx.index.value,
                    imgSpriteFx.blendMode.value,
                )
//                spriteFX.sync.trigger()
//                spout.resend()
            }
        }

        suspend fun next() {
            logger.infoF { "$deckName.sprite.next()" }
            spriteQueue.value?.also { spriteQueue ->
                val enabledSprites = spriteQueue.presets.filterIndexed() { index, it ->
//                    logger.debugF { spriteQueue.name + " " +spriteQueue.type + " " + it }
                    toggles[index].value
                }
                logger.debugF { "enabled sprites: $enabledSprites" }
                if (enabledSprites.isNotEmpty()) {
                    val next = enabledSprites.random()
                    index.value = spriteQueue.presets.indexOf(next)
//                    withTimeout(100.milliseconds) {
//                        // block until new queue is loaded
//                        name.first { it == next }
//                    }
                }
            }
        }

        suspend fun startFlows() {
            logger.infoF { "initializing $deckName sprite" }

            nestdropSprite.startFlows()

            trigger
                .drop(1)
                .onEach {
                    next()
                    //spriteFX.sync.trigger()
                }
                .launchIn(flowScope)

            index
//                .combine(resyncToTouchOSC) { a, _ -> a }
                .combine(spriteQueue) { index, queue ->
                    queue?.presets?.getOrNull(index)
                }.onEach { spritePreset ->
//                    logger.infoF { "setting name of sprite $spritePreset" }
                    name.value = spritePreset?.name ?: "-"
                }
                .launchIn(flowScope)

            // do sprite change
            index
                // set current image sprite again if spout was enabled or changed
//                .combine(spout.index.filter { it != -1 }) { a, _ -> a }
                .combine(spriteQueue) { index, queue ->
                    when {
                        queue != null ->
                            PresetIdState.Data(
                                index = index,
                                queue = queue,
                            )

                        else -> PresetIdState.Unset

                    }
                }
                //maybe debounce ? does this break history ?
                .sample(50.milliseconds)
                .onEach { presetIdState ->
                    nestdropSprite.send(
                        when {
                            presetIdState is PresetIdState.Data && presetIdState.index != -1 -> presetIdState
//                                .also {
//                                    logger.warnF { "sending $presetIdState" }
//                                }
                            else -> PresetIdState.Unset
                        }
                    )
//                    spriteFX.sync.trigger()
                }
                .launchIn(flowScope)
        }
    }

    val imgSprite = ImgSprite()

    inner class ImgSpriteFX {
        val autoChange = MutableStateFlow(true) // OscSynced.Value("/deck$N/sprite_FX/autoChange", true)
        val blendMode = MutableStateFlow(true) //OscSynced.Value("/deck$N/sprite_FX/blendMode", false)
        val index = MutableStateFlow(0) // OscSynced.Value("/deck$N/sprite_FX/index", 0)
        val name =
            MutableStateFlow("uninitialized") //OscSynced.Value("/deck$N/sprite_FX/description", "uninitialized", receive = false)
        val shortLabel =
            MutableStateFlow("uninitialized") //OscSynced.Value("/deck$N/sprite_FX/description", "uninitialized", receive = false)
        val toggles = List(50) {
            MutableStateFlow(false)
//            OscSynced.Value("/deck$N/sprite_FX/toggle/${it}", false).apply {
//                logSending = false
//            }
        }
        val sync = OscSynced.Trigger("/deck$N/sprite_FX/sync")
        private val trigger = OscSynced.Trigger("/deck$N/sprite_FX/next")

        suspend fun next() {
            logger.infoF { "$deckName.spriteFX.next()" }
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
            logger.infoF { "setting $deckName FX to $fx" }
            val arg = fx / 99.0f
            nestdropSendChannel.send(OSCMessage(nestdropDeckAddress("sSpriteFx"), arg))
        }

        suspend fun startFlows() {
            logger.infoF { "initializing $deckName sprite fx" }

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
                    if (blendMode){
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

    inner class SpoutQueue : MutableStateFlow<Queue?> by MutableStateFlow(null) {
        val index = OscSynced.Value("/deck$N/spout_queue/index", -1)
        val name = OscSynced.Value("/deck$N/spout_queue/name", "uninitialized", receive = false)


        suspend fun startFlows() {
            logger.infoF { "initializing $deckName spout queue" }
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

            onEach {
                it?.presets?.let { presets ->
                    spout.labels.forEachIndexed { index, label ->
                        label.value = presets
                            .getOrNull(index)
                            ?.label
                            .orEmpty()
//                        ?.also {
//                            logger.debugF { "label: $it" }
//                        }
                    }
                }
            }
                .launchIn(flowScope)
        }
    }

    val spoutQueue = SpoutQueue()

    inner class Spout {
        //        val toggles = List(20) {
//            OSCSynced.Value("/deck$N/spout/toggle/${it}", false).apply {
//                logSending = false
//            }
//        }
        val labels = List(20) {
            OscSynced.Value("/deck$N/spout/label/${it}", "", receive = false).apply {
                logSending = false
            }
        }

        val sync = OscSynced.Trigger("/deck$N/spout/sync")

        //        val autoChange = OSCSynced.Value("/deck$N/spout/autoChange", true)
        val index = OscSynced.ExclusiveSwitch("/deck$N/spout/index", 20, -1)
        val name = OscSynced.Value("/deck$N/spout/name", "uninitialized", receive = false)
        val fx = MutableStateFlow(0)

        private val nestdropSpout = NestdropSpriteQueue(
            nestdropSendChannel
        )

        suspend fun startFlows() {
            logger.infoF { "initializing $deckName spout" }
            nestdropSpout.startFlows()

            index
//                .combine(resyncToTouchOSC) { a, _ -> a }
                .combine(spoutQueue) { index, queue ->
                    queue?.presets?.getOrNull(index)
                }.onEach { spoutPreset ->
                    logger.infoF { "$deckName spout name $spoutPreset" }
                    fx.value = spoutPreset?.effects ?: 0
                    name.value = spoutPreset?.label ?: "-"
                }
                .launchIn(flowScope)

            // do spout change
            index
                .onEach {
                    logger.infoF { "$deckName spout index changed: $it" }
                }
//                .combine(resyncToTouchOSC) { a, _ -> a }
                .combine(spoutQueue
                    .onEach {
                        logger.infoF { "$deckName spout queue changed: ${it?.name}" }
                    }) { index, queue ->
                    index to queue
                }
                .combine(
                    sync
//                        .drop(1)
                        .onEach {
                            logger.infoF { "spout sync triggered" }
                        }
                ) { a, _ -> a }
//                .runningHistory()
                .sample(100.milliseconds)
                .onEach { (index, queue) ->
                    logger.infoF { "$deckName spout change: $index in ${queue?.name}" }
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

        suspend fun resend() {
            delay(50)
            val currentIndex = index.value
            if (currentIndex >= 0) {
                index.value -= 1
                index.value += 1
            }

        }
    }

    val spout = Spout()


    val skipHistory = MutableStateFlow(false)


    @Serializable
    data class DeckState(
        val deck: Int,
        val timestamp: String,
        val preset: String,
        val presetQueue: String,
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
                deck = N,
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
                presetQueue = presetQueue.name.value,
                imgSprite = imgSprite,
                imgFx = imgFx,
                spoutSprite = spoutSprite,
                spoutFx = spoutFx
            )
        }.filter {
            !skipHistory.value
        }
//            .combine(skipHistory) { state, skipHistory ->
//                state.takeUnless { skipHistory }
//            }.filterNotNull()

    fun nestdropDeckAddress(address: String) = "/Controls/Deck$N/$address"

    private val hasSwitched: OscSynced.Value<Boolean> = OscSynced.Value("/deck$N/switched", false)

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
                logger.infoF { "triggered at $currentBeat, $triggerAt" }
                hasSwitched.value = true
                doSwitch()
            }
        }

    private suspend fun doSwitch() {
        // change preset queue
        if (presetQueue.autoChange.value) {
            presetQueue.next()
        }
        // change preset
        if (preset.autoChange.value) {
            preset.next()
        }
        // change sprite
        if (imgSprite.autoChange.value) {
            imgSprite.next()
        }

        if (imgSpriteFx.autoChange.value) {
            imgSpriteFx.next()
        }
    }


    val deckName: String = "deck$N"
}

