package osc

import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCPacket
import com.illposed.osc.OSCSerializerAndParserBuilder
import com.illposed.osc.argument.handler.BlobArgumentHandler
import com.illposed.osc.argument.handler.BooleanFalseArgumentHandler
import com.illposed.osc.argument.handler.BooleanTrueArgumentHandler
import com.illposed.osc.argument.handler.CharArgumentHandler
import com.illposed.osc.argument.handler.ColorArgumentHandler
import com.illposed.osc.argument.handler.DoubleArgumentHandler
import com.illposed.osc.argument.handler.FloatArgumentHandler
import com.illposed.osc.argument.handler.ImpulseArgumentHandler
import com.illposed.osc.argument.handler.IntegerArgumentHandler
import com.illposed.osc.argument.handler.LongArgumentHandler
import com.illposed.osc.argument.handler.MidiMessageArgumentHandler
import com.illposed.osc.argument.handler.NullArgumentHandler
import com.illposed.osc.argument.handler.StringArgumentHandler
import com.illposed.osc.argument.handler.SymbolArgumentHandler
import com.illposed.osc.argument.handler.TimeTag64ArgumentHandler
import com.illposed.osc.argument.handler.UnsignedIntegerArgumentHandler
import com.illposed.osc.transport.OSCPortIn
import com.illposed.osc.transport.OSCPortInBuilder
import com.illposed.osc.transport.OSCPortOut
import flowScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import utils.runningHistory
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds


private val logger = KotlinLogging.logger { }


val serializer = OSCSerializerAndParserBuilder().also { serializer ->
    serializer.setUsingDefaultHandlers(false)
    serializer.registerArgumentHandler(BlobArgumentHandler.INSTANCE)
    serializer.registerArgumentHandler(BooleanFalseArgumentHandler.INSTANCE)
    serializer.registerArgumentHandler(BooleanTrueArgumentHandler.INSTANCE)
    serializer.registerArgumentHandler(CharArgumentHandler.INSTANCE)
    serializer.registerArgumentHandler(ColorArgumentHandler.INSTANCE)
    serializer.registerArgumentHandler(DoubleArgumentHandler.INSTANCE)
    serializer.registerArgumentHandler(FloatArgumentHandler.INSTANCE)
    serializer.registerArgumentHandler(ImpulseArgumentHandler.INSTANCE)
    serializer.registerArgumentHandler(IntegerArgumentHandler.INSTANCE)
    serializer.registerArgumentHandler(LongArgumentHandler.INSTANCE)
    serializer.registerArgumentHandler(MidiMessageArgumentHandler.INSTANCE)
    serializer.registerArgumentHandler(NullArgumentHandler.INSTANCE)
    serializer.registerArgumentHandler(TimeTag64ArgumentHandler.INSTANCE)
    serializer.registerArgumentHandler(UnsignedIntegerArgumentHandler.INSTANCE)
    serializer.registerArgumentHandler(StringArgumentHandler())
    serializer.registerArgumentHandler(SymbolArgumentHandler())
//    serializer.registerArgumentHandler(CustomAwtColorArgumentHandler.INSTANCE)
}


val nestdropSendChannel = Channel<OSCPacket>(Channel.BUFFERED)

val nestdropSendAddress = MutableStateFlow(
    InetSocketAddress(
        InetAddress.getLoopbackAddress(), // InetAddress.getByName("127.0.0.1"),
        8000
    )
)
val nestdropListenAddress = MutableStateFlow(
    InetSocketAddress(
        InetAddress.getLoopbackAddress(), // InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1)),
        8001
    )
)

suspend fun runNestDropSend() {
    nestdropSendAddress.collectLatest { address ->
        coroutineScope {
            val nestdropPort = OSCPortOut(serializer, address)
            nestdropPort.connect()
            logger.debug { "constructed OSC port" }
            for (oscPacket in nestdropSendChannel) {
                try {
                    logger.trace { "ND OUT: ${oscPacket.stringify()}" }
                    nestdropPort.send(oscPacket)
                } catch (e: Exception) {
                    logger.error(e) {}
                }
            }
            logger.error { "port closed" }
            nestdropPort.close()
        }

    }
}

suspend fun nestdropPortSend(packet: OSCMessage) {
    nestdropSendChannel.send(packet)
}

suspend fun startNestdropOSC() {
//    val resolumeState = MutableStateFlow<Map<String, String>>(emptyMap())
    val messages = Channel<Pair<String, String>>(Channel.BUFFERED)
    var receiver: OSCPortIn? = null
    flowScope.launch(Dispatchers.IO) {
        nestdropListenAddress.collectLatest { address ->
            receiver?.close()
            logger.info { "connecting to nestdrop ${address}" }
            receiver = OSCPortInBuilder()
                .setSocketAddress(address)
                .setPacketListener(OSCPortIn.defaultPacketListener())
                .let { inBuilder ->
                    OscSynced.syncedValues
                        .filterIsInstance<OscSynced.Receiving<*>>()
                        .filter { it.receive && it.target == OscSynced.Target.Nestdrop }
                        .fold(inBuilder) { builder, syncedValue ->
                            logger.info { "adding listener for ${syncedValue.label}" }
                            builder.addMessageListener(
                                syncedValue.messageSelector
                            ) { messageEvent ->
                                runBlocking {
                                    if (syncedValue.logReceived) {
                                        val msg = messageEvent.message
//                                        val timestamp = messageEvent.time.ntpTime // .fraction.nanoseconds
                                        logger.info { "NESTDROP IN: ${msg.stringify()}" }
                                    }
                                    syncedValue.onMessageEvent(messageEvent)
                                }
                            }
                        }
                }
//                .addPacketListener(object : OSCPacketListener {
//                    override fun handlePacket(event: OSCPacketEvent) {
//                        val packet = event.packet
//
//                        handlePacket(packet)
//                    }
//
//                    fun handlePacket(packet: OSCPacket) {
//                        when (packet) {
//                            is OSCBundle -> {
//                                packet.packets.forEach {
//                                    handlePacket(it)
//                                }
//                            }
//
//                            is OSCMessage -> {
//                                handleMessage(packet)
//                            }
//                        }
//                    }
//
//                    fun handleMessage(message: OSCMessage) {
//                        runBlocking {
////                    logger.debugF { "RESOLUME IN: ${message.stringify()}" }
//                            val address = message.address
//
////                    if(address.endsWith("connected")) {
////                        logger.infoF { message.stringify() }
////                    }
//
//                            val arguments = message.arguments.joinToString(" ")
//                            when {
//                                address.endsWith("sBpm") -> {
//                                    logger.trace { "receiving: $address ${message.arguments}" }
//                                }
//                                address.endsWith("sBpmCnt") -> {
//                                    logger.trace { "receiving: $address ${message.arguments}" }
//                                }
//                                else -> {
//                                    logger.info { "receiving: $address ${message.arguments}" }
//                                }
//                            }
//
////                            messages.send(address to arguments)
//                        }
//                    }
//
//                    override fun handleBadData(event: OSCBadDataEvent) {
//                        runBlocking {
//                            logger.warn { "osc bad data: $event" }
//                        }
//                        // TODO("Not yet implemented")
//                    }
//
//                })
                .build()
                .also {
                    logger.info { "start listening on $address" }
                    while (receiver?.isConnected == false) {
                        delay(100)
                    }
                    it.startListening()

                    delay(100)
//                    logger.info { "isListening ${it.isListening}" }
//                    logger.info { "isConnected ${it.isConnected}" }
                }

        }

//        while(receiver?.isConnected == true) {
//            delay(10_000)
//            logger.info { "isConnected ${receiver?.isConnected}" }
//        }
    }
//    delay(1000)


    logger.info { "initializing OSC synced values" }

    OscSynced.syncedValues
        .filter { it.target == OscSynced.Target.Nestdrop }
        .filterIsInstance<OscSynced.Sending<*>>()
        .filter { it.send }
        .forEach { oscSyncedValue ->

            if(oscSyncedValue.dropFirst > 0) {
                var lastValue:  List<OSCPacket> = emptyList()
                oscSyncedValue
                    .flow
                    .take(oscSyncedValue.dropFirst)
                    .distinctUntilChanged()
                    .onEach { value ->
                        val oscMessages = oscSyncedValue.generateOscMessagesUntyped(value!!)
                        val message = "NESTDROP OUT dropping \n      " +(lastValue.map {
                            "last ${it.stringify()}"
                        } + oscMessages.map {
                            "next ${it.stringify()}"
                        }).joinToString("\n      ")

                        if (oscSyncedValue.logSending) {
                            logger.debug { message }
                        } else {
                            logger.trace { message }
                        }
                        lastValue = oscMessages
                    }
                    .launchIn(flowScope)
            }

            @OptIn(FlowPreview::class)
            oscSyncedValue
                .flow
                .drop(oscSyncedValue.dropFirst)
                .distinctUntilChanged()
                .sample(100.milliseconds)
                .onEach { value ->
                    val oscMessages = oscSyncedValue.generateOscMessagesUntyped(value!!)
                    oscMessages.forEach {
                        if (oscSyncedValue.logSending) {
                            logger.debug { "Nestdrop OUT: ${it.stringify()}" }
                        } else {
                            logger.trace { "Nestdrop OUT: ${it.stringify()}" }
                        }
                        nestdropSendChannel.send(it)
                    }
                }.launchIn(flowScope)
        }
    flowScope.launch {
        delay(100)
        val lastChanged = MutableStateFlow(Instant.DISTANT_PAST)
        while (true) {
            if (lastChanged.value < Clock.System.now() - 5.minutes) {
                logger.warn { "triggering sync controls" }
                nestdropSendChannel.send(
                    OSCMessage("/Controls", "?")
                )
                delay(1.minutes)
            } else {
                delay(100)
            }
        }
    }
}