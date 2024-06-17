package osc

import com.illposed.osc.messageselector.OSCPatternAddressMessageSelector
import com.illposed.osc.transport.OSCPortIn
import com.illposed.osc.transport.OSCPortInBuilder
import flowScope
import io.klogging.logger
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import logging.debugF
import logging.traceF
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.time.Duration.Companion.milliseconds

private val logger = logger("osc.initializeSyncedValuesKt")
suspend fun initializeSyncedValues() {
    OscSynced.syncedValues.filter {
        it.target == OscSynced.Target.TouchOSC
    }.forEach { oscSyncedValue ->
        @OptIn(FlowPreview::class)
        oscSyncedValue
            .flow
            .drop(oscSyncedValue.dropFirst)
            .sample(100.milliseconds)
            .combine(resyncToTouchOSC) { a, _ -> a }
            .onEach { value ->
                val oscMessages = oscSyncedValue.generateOscMessages(value!!)
                oscMessages.forEach {
                    if (oscSyncedValue.logSending) {
                        logger.traceF { "TOSC OUT: ${it.stringify()}" }
                    } else {
                        logger.traceF { "TOSC OUT: ${it.stringify()}" }
                    }
                    controlPortSend(it)
                }
            }.launchIn(flowScope)
    }
    OscSynced.syncedValues.filter {
        it.target == OscSynced.Target.Arena
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
                        logger.debugF { "Arena OUT: ${it.stringify()}" }
                    } else {
                        logger.traceF { "Arena OUT: ${it.stringify()}" }
                    }
                    arenaSendChannel.send(it)
                }
            }.launchIn(flowScope)
    }
    val controlPortReceiver = OSCPortInBuilder()
        .setSocketAddress(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8002))
        .setPacketListener(OSCPortIn.defaultPacketListener())
        .addMessageListener(OSCPatternAddressMessageSelector("/resync")) { messageEvent ->
            runBlocking {
                val msg = messageEvent.message
                logger.debugF { "IN: ${msg.stringify()}" }

                resyncToTouchOSC.value++
            }
        }
        .let { inBuilder ->
            OscSynced.syncedValues.filter {
                it.receive && it.target == OscSynced.Target.TouchOSC
            }.fold(inBuilder) { builder, syncedValue ->
                builder.addMessageListener(
                    syncedValue.messageSelector
                ) { messageEvent ->
                    runBlocking {
                        val msg = messageEvent.message
                        if (syncedValue.logReceived) {
                            logger.debugF { "IN: ${msg.stringify()}" }
                        }
                        syncedValue.onMessageEvent(messageEvent)
                    }
                }
            }
        }

        .build()
        .also {
            it.startListening()
        }

//    val receiver = OSCPortInBuilder()
//        .setSocketAddress(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 7001))
//        .setPacketListener(OSCPortIn.defaultPacketListener())
//        .let { inBuilder ->
//            OscSynced.syncedValues.filter {
//                it.receive && it.target == OscSynced.Target.Arena
//            }.fold(inBuilder) { builder, syncedValue ->
//                builder.addMessageListener(
//                    syncedValue.messageSelector
//                ) { messageEvent ->
//                    runBlocking {
//                        val msg = messageEvent.message
//                        if (syncedValue.logReceived) {
//                            logger.debugF { "Arena IN: ${msg.stringify()}" }
//                        }
//                        syncedValue.onMessageEvent(messageEvent)
//                    }
//                }
//            }
//        }
//        .addMessageListener(
//            OSCPatternAddressMessageSelector("/*")
//        ) { messageEvent ->
//            runBlocking {
//                val message = messageEvent.message
//                logger.debugF { "RESOLUME IN: ${message.stringify()}" }
////                val address = message.address
////                val arguments = message.stringifyArguments()
////                messages.send(address to arguments)
//            }
//        }
//        .build()
//        .also {
//            it.startListening()
//        }
}