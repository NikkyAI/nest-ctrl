import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import osc.OSCMessage
import osc.OscSynced
import osc.nestdropSendChannel
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

//val beatFrame = MutableStateFlow(64) // OscSynced.Value("/beats", 64, target = OscSynced.Target.TouchOSC)
val beatProgress = MutableStateFlow(0f)


private val logger = KotlinLogging.logger { }

val controlBeatSlider = OscSynced.ValueSingle<Float>(
    "/Controls/sBeat",
    64f,
//    send = false,
    dropFirst = 1,
    target = OscSynced.Target.Nestdrop
).also {
    it.logReceived = false
}


val controlShuffleButton = OscSynced.ValueSingle<Int>(
    address = "/Controls/btRandom",
    initialValue = 0,
    dropFirst = 1,
    target = OscSynced.Target.Nestdrop
)
val controlAutoButton = OscSynced.ValueSingle<Int>(
    address = "/Controls/btAuto",
    initialValue = 0,
    dropFirst = 1,
    target = OscSynced.Target.Nestdrop
)
val beatFrame = controlBeatSlider.flow
val controlBeatCounter = OscSynced.ValueSingle<Int>(
    "/Controls/sBpmCnt",
    0,
    send = false,
    dropFirst = 1,
    target = OscSynced.Target.Nestdrop
).also {
    it.logReceived = false
}
suspend fun beatResync() {
    nestdropSendChannel.send(
        OSCMessage("/Controls/btResync", 1)
    )
}

val controlBpm = OscSynced.ValueSingle<Float>(
    "/Controls/sBpm",
    120f, send = false,
    dropFirst = 1,
    target = OscSynced.Target.Nestdrop
).also {
    it.logReceived = false
}
val bpmRounded = MutableStateFlow(120f)
val bpmInt = MutableStateFlow(120)
val secondsPerBeat = MutableStateFlow(0.5f)

val beatCounter = MutableStateFlow(0.0)

suspend fun startBeatCounter() {
    // force initialization
    controlShuffleButton.flow.onEach {

    }.launchIn(flowScope)

    @OptIn(FlowPreview::class)
    controlBpm
        .flow
        .sample(100.milliseconds)
        .map { bpm ->
            (bpm * 10).roundToInt() / 10.0f
        }
        .onEach {
            bpmRounded.value = it
            bpmInt.value = it.roundToInt()
            secondsPerBeat.value = 60f / it
        }
        .launchIn(flowScope)

    controlBeatSlider
        .flow
        .onEach {
            beatResync()
        }
        .launchIn(flowScope)

    decks.forEach { deck ->

        combine(
            secondsPerBeat,
//            beatFrame,
            deck.presetSwitching.transitTimeSync,
//            deck.presetSwitching.transitTimeBeatframeFraction,
            deck.presetSwitching.transitTimeBeats,
        ) { secondsPerBeat, enabled, transitTimeBeats ->
            if(enabled) {
                secondsPerBeat * transitTimeBeats
            } else {
                null
            }
        }.distinctUntilChanged()
            .filterNotNull()
            .map { value ->
                (value * 10).roundToInt() / 10.0f
            }
            .map { value ->
                value.coerceAtMost(30f)
            }
            .distinctUntilChanged()
            .onEach {
                logger.info { "updating ${deck.deckName} transit time to $it" }
                deck.ndTime.transitionTime.value = it
            }
            .launchIn(flowScope)
    }

    beatProgress
        .onEach { beatProgress ->
            logger.trace { "beat progress: $beatProgress" }
        }
        .launchIn(flowScope)
    beatFrame
        .onEach {
            updateConfig {
                copy(
                    beats = it.roundToInt()
                )
            }
        }
        .launchIn(flowScope)


    run {

        beatCounter.combine(beatFrame) { beats, beatFrame ->
            beats to beatFrame
        }.onEach { (beats, totalBeats) ->
            if (beats > totalBeats) {

                logger.trace { "reached $totalBeats beats, resetting to ${beats % totalBeats}" }
//                    var newBeatCounter = beatCounter.value
                beatCounter.value %= totalBeats

//                decks.forEach {
//                    it.presetSwitching.resetLatch()
//                }
            }
        }
            .launchIn(flowScope)

        beatCounter.combine(beatFrame) { beats, beatFrame ->
            (beats % beatFrame).toFloat() / beatFrame
        }
            .onEach {
                beatProgress.value = it
            }
            .launchIn(flowScope)

        flowScope.launch {
            var lastLoop = Clock.System.now()
//            var beats = 0.0
//            var deck1Switched = false
//            var deck2Switched = false
            while (true) {
                delay(1.seconds/60)
                val now = Clock.System.now()
                val timeDelta = now - lastLoop
                lastLoop = now
                val beatsPerMillisecond = controlBpm.flow.value / 60_000.0
                val beatsInDuration = beatsPerMillisecond * timeDelta.inWholeMilliseconds
                beatCounter.value += beatsInDuration
            }
        }
    }
}