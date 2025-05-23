package osc

import QUEUES
import com.illposed.osc.OSCBadDataEvent
import com.illposed.osc.OSCBundle
import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCPacket
import com.illposed.osc.OSCPacketEvent
import com.illposed.osc.OSCPacketListener
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
import com.illposed.osc.messageselector.OSCPatternAddressMessageSelector
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import nestdrop.QueueType
import nestdrop.deck.OSCQueueUpdate
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.time.Duration.Companion.milliseconds
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
                    logger.debug { "ND OUT: ${oscPacket.stringify()}" }
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
//                .let { inBuilder ->
//                    OscSynced.syncedValues
//                        .filterIsInstance<OscSynced.Receiving<*>>()
//                        .filter { it.receive && it.target == OscSynced.Target.Nestdrop }
//                        .fold(inBuilder) { builder, syncedValue ->
//                            logger.debug { "adding listener for ${syncedValue.label}" }
//                            builder.addMessageListener(
//                                syncedValue.messageSelector
//                            ) { messageEvent ->
//                                runBlocking {
//                                    if (syncedValue.logReceived) {
//                                        val msg = messageEvent.message
////                                        val timestamp = messageEvent.time.ntpTime // .fraction.nanoseconds
//                                        logger.debug { "ND IN: ${msg.stringify()}" }
//                                    }
//                                    syncedValue.onMessageEvent(messageEvent)
//                                }
//                            }
//                        }
//                }
                .addMessageListener(
                    OSCPatternAddressMessageSelector("/Queue/*"),
                ) { messageEvent ->
                    val message = messageEvent.message
//                    logger.debug { "processing ${message.stringify()}" }

                    if (message.arguments.size == 6) {
                        val active = message.arguments[0] as Int == 1
                        val type = message.arguments[1] as String
                        val beatOffset = message.arguments[2] as Float
                        val beatMultiplier = message.arguments[3] as Float
                        val deckNumber = message.arguments[4] as Int
                        val presetCount = message.arguments[5] as Int

                        val queueName = message.address
                            .substringAfter("/Queue/")

                        launch {
                            QUEUES.updateQueueMessages.send(
                                OSCQueueUpdate.UpdateQueue(
                                    name = queueName,
                                    type = QueueType.valueOf(type),
                                    active = active,
                                    beatOffset = beatOffset,
                                    beatMultiplier = beatMultiplier,
                                    deckNumber = deckNumber,
                                    presetCount = presetCount,
                                )
                            )
                        }
                    } else {
                        logger.error { "unknowns message: ${message.stringify()}" }
                    }
                }
                .addMessageListener(
                    OSCPatternAddressMessageSelector("/Queue/*/Deck"),
                ) { messageEvent ->
                    val message = messageEvent.message
//                    logger.debug { "processing ${message.stringify()}" }

                    val deckNumber = message.arguments[0] as Int

                    val queueName = message.address
                        .substringAfter("/Queue/")
                        .substringBeforeLast("/")

                    launch {
                        QUEUES.updateQueueMessages.send(
                            OSCQueueUpdate.Deck(
                                name = queueName,
                                deckNumber = deckNumber,
                            )
                        )
                    }
                }

                .addMessageListener(
                    OSCPatternAddressMessageSelector("/Queue/*/sBof"),
                ) { messageEvent ->
                    val message = messageEvent.message
//                    logger.debug { "processing ${message.stringify()}" }

                    val beatOffset = message.arguments[0] as Float

                    val queueName = message.address
                        .substringAfter("/Queue/")
                        .substringBeforeLast("/")

                    launch {
                        QUEUES.updateQueueMessages.send(
                            OSCQueueUpdate.BeatOffset(
                                name = queueName,
                                beatOffset = beatOffset,
                            )
                        )
                    }
                }

                .addMessageListener(
                    OSCPatternAddressMessageSelector("/Queue/*/sMul"),
                ) { messageEvent ->
                    val message = messageEvent.message
//                    logger.debug { "processing ${message.stringify()}" }

                    val beatMultiplier = message.arguments[0] as Float

                    val queueName = message.address
                        .substringAfter("/Queue/")
                        .substringBeforeLast("/")

                    launch {
                        QUEUES.updateQueueMessages.send(
                            OSCQueueUpdate.BeatMultiplier(
                                name = queueName,
                                beatMultiplier = beatMultiplier,
                            )
                        )
                    }
                }
                .addMessageListener(
                    OSCPatternAddressMessageSelector("/Queue/*/*"),
                ) { messageEvent ->
                    val message = messageEvent.message

                    logger.error { "UNHANDLED on Queue: ${message.stringify()}" }
                }

                .addMessageListener(
                    OSCPatternAddressMessageSelector("/Queue/*/*"),
                ) { messageEvent ->
                    logger.error { "UNHANDLED OSC MSG: ${messageEvent.message.stringify()}" }
                }
                .addPacketListener(
                    object : OSCPacketListener {
                        val receivingSyncedValues = OscSynced.syncedValues
                            .filterIsInstance<OscSynced.Receiving<*>>()
                            .filter { it.receive && it.target == OscSynced.Target.Nestdrop }

                        //                        .fold(inBuilder) { builder, syncedValue ->
//                            logger.debug { "adding listener for ${syncedValue.label}" }
//                            builder.addMessageListener(
//                                syncedValue.messageSelector
//                            ) { messageEvent ->
//                                runBlocking {
//                                    if (syncedValue.logReceived) {
//                                        val msg = messageEvent.message
//                                        val timestamp = messageEvent.time.ntpTime // .fraction.nanoseconds
//                                        logger.debug { "ND IN: ${msg.stringify()}" }
//                                    }
//                                    syncedValue.onMessageEvent(messageEvent)
//                                }
//                            }
//                        }
                        override fun handlePacket(event: OSCPacketEvent) {
                            handlePacket(event.packet)
                        }

                        fun handlePacket(packet: OSCPacket) {
                            when (packet) {
                                is OSCBundle -> {
                                    packet.packets.forEach {
                                        handlePacket(it)
                                    }
                                }

                                is OSCMessage -> {
                                    handleMessage(packet)
                                }
                            }
                        }

                        fun handleMessage(message: OSCMessage) {
                            receivingSyncedValues.firstOrNull {
                                it.listenAddress == message.address
                            }?.also { syncedValue ->
                                runBlocking {
                                    if (syncedValue.logReceived) {
                                        logger.debug { "ND IN: ${message.stringify()}" }
                                    }
                                    syncedValue.onMessageEvent(message)
                                }
                            }
                        }

                        override fun handleBadData(event: OSCBadDataEvent) {
                            runBlocking {
                                logger.warn { "osc bad data: $event" }
                            }
                            // TODO("Not yet implemented")
                        }
                    }
                )
                .addPacketListener(debugListener)
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
//            if (oscSyncedValue.dropFirst > 0) {
//                var lastValue: List<OSCPacket> = emptyList()
//                oscSyncedValue
//                    .flow
//                    .take(oscSyncedValue.dropFirst)
//                    .distinctUntilChanged()
//                    .onEach { value ->
//                        val oscMessages = oscSyncedValue.generateOscMessagesUntyped(value!!)
//                        val message = "NESTDROP OUT dropping \n      " + (lastValue.map {
//                            "last ${it.stringify()}"
//                        } + oscMessages.map {
//                            "next ${it.stringify()}"
//                        }).joinToString("\n      ")
//
//                        if (oscSyncedValue.logSending) {
//                            logger.debug { message }
//                        } else {
//                            logger.trace { message }
//                        }
//                        lastValue = oscMessages
//                    }
//                    .launchIn(flowScope)
//            }

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
            if (lastChanged.value < Clock.System.now() - 30.seconds) {
                logger.warn { "triggering sync controls" }
                nestdropSendChannel.send(
                    OSCMessage("/Controls", "?")
                )
                lastChanged.value = Clock.System.now()
                delay(10.seconds)
            } else {
                delay(100)
            }
        }
    }
}

private val debugListener = object : OSCPacketListener {
    override fun handlePacket(event: OSCPacketEvent) {
        val packet = event.packet

        handlePacket(packet)
    }

    fun handlePacket(packet: OSCPacket) {
        when (packet) {
            is OSCBundle -> {
                packet.packets.forEach {
                    handlePacket(it)
                }
            }

            is OSCMessage -> {
                handleMessage(packet)
            }
        }
    }

    fun handleMessage(message: OSCMessage) {
        runBlocking {
//                    logger.debug { "NESTDROP IN: ${message.stringify()}" }
            val address = message.address

//            val arguments = message.arguments.joinToString(" ")
            when {
                address.endsWith("sBpm") -> {
//                    logger.trace { "receiving: $address ${message.arguments}" }
                }

                address.endsWith("sBpmCnt") -> {
//                    logger.trace { "receiving: $address ${message.arguments}" }
                }

                else -> {
                    logger.debug { "NESTDROP IN: $address ${message.arguments}" }
                }
            }

//                            messages.send(address to arguments)
        }
    }

    override fun handleBadData(event: OSCBadDataEvent) {
        runBlocking {
            logger.warn { "osc bad data: $event" }
        }
        // TODO("Not yet implemented")
    }

}
