import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import osc.OscSynced
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val beatFrame = MutableStateFlow(64) // OscSynced.Value("/beats", 64, target = OscSynced.Target.TouchOSC)
val beatProgress = MutableStateFlow(0f)


private val logger = KotlinLogging.logger { }

val bpmSynced = OscSynced.ValueSingle<Float>(
    "/Controls/sBpm",
    120f, send = false,
    target = OscSynced.Target.Nestdrop
).also {
    it.logReceived = false
}
val bpmRounded = MutableStateFlow(120f)
val bpmInt = MutableStateFlow(120)

val beatCounter = MutableStateFlow(0.0)

suspend fun startBeatCounter() {

    @OptIn(FlowPreview::class)
    bpmSynced
        .sample(100.milliseconds)
        .map { bpm ->
            (bpm * 10).roundToInt() / 10.0f
        }
        .onEach {
            bpmRounded.value = it
            bpmInt.value = it.roundToInt()
        }
        .launchIn(flowScope)
    beatProgress
        .onEach { beatProgress ->
            logger.trace { "beat progress: $beatProgress" }
        }
        .launchIn(flowScope)
    beatFrame
        .onEach {
            config.value = config.value.copy(beats = it)
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
                val beatsPerMillisecond = bpmSynced.value / 60_000.0
                val beatsInDuration = beatsPerMillisecond * timeDelta.inWholeMilliseconds
                beatCounter.value += beatsInDuration
            }
        }
    }
}