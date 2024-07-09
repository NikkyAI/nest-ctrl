package osc


import flowScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger { }
suspend fun initializeSyncedValues() {
    OscSynced.syncedValues.filter {
        it.target == OscSynced.Target.ResolumeArena
    }.forEach { oscSyncedValue ->
        if(oscSyncedValue is OscSynced.Address) {
            val lastChanged = MutableStateFlow(Instant.DISTANT_PAST)
            flowScope.launch {
                oscSyncedValue.flow.onEach {
                    lastChanged.value = Clock.System.now()
                }
                while (true) {
                    if(lastChanged.value < Clock.System.now() - 500.milliseconds) {
                        arenaSendChannel.send(
                            OSCMessage(oscSyncedValue.address, "?")
                        )
                        delay(500)
                    } else {
                        delay(10)
                    }
                }
            }
        }

        @OptIn(FlowPreview::class)
        oscSyncedValue
            .flow
            .drop(oscSyncedValue.dropFirst)
            .sample(100.milliseconds)
//            .combine(resyncToTouchOSC) { a, _ -> a }
            .onEach { value ->
                val oscMessages = oscSyncedValue.generateOscMessages(value!!)
                oscMessages.forEach {
                    if (oscSyncedValue.logSending) {
                        logger.debug { "Arena OUT: ${it.stringify()}" }
                    } else {
                        logger.trace { "Arena OUT: ${it.stringify()}" }
                    }
                    arenaSendChannel.send(it)
                }
            }.launchIn(flowScope)
    }
}