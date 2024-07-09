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
import utils.runningHistoryNotNull
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

val beatFrame = MutableStateFlow(64) // OscSynced.Value("/beats", 64, target = OscSynced.Target.TouchOSC)
val beatProgress = MutableStateFlow(0f)

// OscSynced.Value("/beatProgress", 0.0f, receive = false, target = OscSynced.Target.TouchOSC).apply {
//    logSending = false
//}
val bpmRounded = MutableStateFlow(120f)

//    OscSynced.Value("/bpmRounded", 120.0f).apply {
//    logSending = false
//}
val bpmRoundedInt = MutableStateFlow(120)

private val logger = KotlinLogging.logger { }

suspend fun startBeatCounter(
//    vararg decks: Deck
) {
    @OptIn(FlowPreview::class)
    Link.bpm
        .sample(100.milliseconds)
        .map { bpm ->
            (bpm * 10).roundToInt() / 10.0f
        }
        .onEach {
            bpmRounded.value = it
            bpmRoundedInt.value = it.roundToInt()
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
        val beatCounter = MutableStateFlow(0.0)

        beatCounter.combine(beatFrame) { beats, beatFrame ->
            beats to beatFrame
        }.onEach { (beats, totalBeats) ->
            if (beats > totalBeats) {
//                beats -= totalBeats
//                    start += ((bpmFromLink.value / 60_000.0) * totalBeats).milliseconds

                logger.trace { "reached $totalBeats beats, resetting to ${beats % totalBeats}" }
//                    var newBeatCounter = beatCounter.value
                beatCounter.value %= totalBeats

                decks.forEach {
                    it.resetLatch()
                }
            }
        }
            .launchIn(flowScope)

        beatCounter.combine(beatFrame) { beats, beatFrame ->
            beats.toFloat() / beatFrame
        }.onEach {
            beatProgress.value = it
        }
            .launchIn(flowScope)

        decks.forEach { deck ->
            deck.beatFlow(beatCounter.runningHistoryNotNull())
                .launchIn(flowScope)
        }

        flowScope.launch {
            var lastLoop = Clock.System.now()
//            var beats = 0.0
//            var deck1Switched = false
//            var deck2Switched = false
            while (true) {
                delay(10)
                val now = Clock.System.now()
                val timeDelta = now - lastLoop
                lastLoop = Clock.System.now()
                val beatsPerMillisecond = Link.bpm.value / 60_000.0
                val beatsInDuration = beatsPerMillisecond * timeDelta.inWholeMilliseconds
                beatCounter.value += beatsInDuration
            }
        }
    }
}