package osc

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
import com.illposed.osc.transport.OSCPortOut
import flowScope
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.xn32.json5k.Json5
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import utils.receiveAvailable
import java.io.File
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
val arenaSendChannel = Channel<OSCPacket>(Channel.BUFFERED)

val nestdropSendAddress = MutableStateFlow(
    InetSocketAddress(
        InetAddress.getLoopbackAddress(), // InetAddress.getByName("127.0.0.1"),
        8000
    )
)
val nestdropListenAddress = MutableStateFlow(
    InetSocketAddress(
        InetAddress.getByAddress(byteArrayOf(127,0,0,1)),
        8001
    )
)
val resolumeArenaSendAddress = MutableStateFlow(
    InetSocketAddress(
        InetAddress.getLoopbackAddress(), // InetAddress.getByName("127.0.0.1"),
        7000
    )
)
val resolumeArenaReceiveAddress = MutableStateFlow(
    InetSocketAddress(
        InetAddress.getLoopbackAddress(),
        7001
    )
)

suspend fun runNestDropSend() {
    nestdropSendAddress.collectLatest { address ->
        coroutineScope {
//            val nestdropPort = OSCPortOut(
//                serializer,
//                address
//            )
            val nestdropPort = // OSCPortOut(InetAddress.getByName("127.0.0.1"), 8000)
                OSCPortOutBuilder()
                    .also {
                       it.serializerBuilder = serializer
                    }
                    .setRemoteSocketAddress(
                        address
                    )
                    .build()
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

suspend fun runResolumeSend() {

    resolumeArenaSendAddress.collectLatest { address ->
        coroutineScope {
            val arenaPort = OSCPortOut(serializer, address)
           // OSCPortOut(InetAddress.getByName("127.0.0.1"), 8000)
//                OSCPortOutBuilder()
//                    .setRemoteSocketAddress(address)
//                    .build()
            arenaPort.connect()

            logger.debug { "constructed OSC port" }
            val job = launch {
                while (true) {
                    delay(50)
                    val oscMessages = arenaSendChannel.receiveAvailable(32)
                    if (oscMessages.isNotEmpty()) {
//                    oscMessages.forEach {
//                        logger.debugF { "Arena OUT: ${it.stringify()}" }
//                    }
//                logger.debugF { "sending ${packets.size} packets" }
                        val oscPacket = if (oscMessages.size == 1) {
                            oscMessages.first()
                        } else {
                            OSCBundle(oscMessages)
                        }
                        try {
                            arenaPort.send(oscPacket)
                        } catch (e: Exception) {
                            logger.error(e) { "failed to send messages: ${oscMessages.joinToString { it.stringify() }} " }
                        }
                    }
                }
            }
            job.join()
            logger.error { "port closed" }
            arenaPort.close()
        }
    }
}

suspend fun nestdropPortSend(packet: OSCMessage) {
    nestdropSendChannel.send(packet)
}

val resolumeLayerStates = MutableStateFlow(mapOf<Int, Int?>())

private val resolumeLayerStateUpdates = Channel<Triple<Int, Int, Boolean>>(Channel.BUFFERED)

suspend fun updateResolumeLayerState(layer: Int, clip: Int, connected: Boolean) {
    resolumeLayerStateUpdates.send(
        Triple(layer, clip, connected)
    )
}

private val connectedClipSyncedValues = mutableMapOf<Int, OscSynced.ValueSingle<Int>>()

fun connectSpecificClip(
    group: Int,
    layer: Int,
): OscSynced.ValueSingle<Int> {
    val layerIndex = resolumeLayerIndex(group, layer)
    return connectedClipSyncedValues[layerIndex] ?: run {
        OscSynced.ValueSingle(
            "/composition/layers/$layerIndex/connectspecificclip",
            -1,
            target = OscSynced.Target.ResolumeArena
        ) { address, value ->
//            updateResolumeLayerState(resolumeLayerIndex(group, layer), -1, false)
            if (value < 0) {
                listOf(
                    OSCMessage("/composition/layers/$layerIndex/clear", 1),
                    OSCMessage("/composition/layers/$layerIndex/clear", 0),
                )
            } else {
                listOf(
                    OSCMessage(address, value),
                )
            }
        }.also {
            it.dropFirst = 1
            connectedClipSyncedValues[layerIndex] = it
        }
    }
}

val resolumeGroupSizes = sequence {
    repeat(4) {
        yield(3)
    }
    //footage
    yield(5)
    // stageflow
    yield(9)
    // nd test
    yield(1)
    // rest
//    repeat(10) {
//        yield(1)
//    }
}.toList()

fun resolumeLayerIndex(group: Int, layer: Int): Int {
    return resolumeGroupSizes.take(group - 1).sum() + layer
}

fun resolumeLayerAddr(
    group: Int,
    layer: Int,
): String {
    val layerIndex = resolumeLayerIndex(group, layer)
    return "/composition/layers/$layerIndex"
}

fun resolumeLayerClearAddr(
    group: Int,
    layer: Int,
): String {
    return resolumeLayerAddr(group = group, layer = layer) + "/clear"
}

fun resolumeClipAddr(
    group: Int,
    layer: Int,
    clip: Int
): String {
    return resolumeLayerAddr(group = group, layer = layer) + "/clips/$clip"
}

fun resolumeClipConnectAddr(
    group: Int,
    layer: Int,
): String {
    return resolumeLayerAddr(group = group, layer = layer) + "/connectspecificclip"
}

fun resolumeDashboardLinkAddr(
    group: Int,
    layer: Int,
    clip: Int,
    link: Int
): String {
    return resolumeClipAddr(group = group, layer = layer, clip = clip) + "/dashboard/link$link"
}

suspend fun resolumeClipConnect(
    group: Int,
    layer: Int,
    clip: Int
) {
    updateResolumeLayerState(resolumeLayerIndex(group, layer), clip, true)
    connectSpecificClip(group, layer).value = clip - 1
}

suspend fun resolumeLayerClear(
    group: Int,
    layer: Int,
) {
    updateResolumeLayerState(resolumeLayerIndex(group, layer), 0, false)
    connectSpecificClip(group, layer).value = -1
}

suspend fun startResolumeListener() {
//    val resolumeState = MutableStateFlow<Map<String, String>>(emptyMap())
    val messages = Channel<Pair<String, String>>(Channel.BUFFERED)
    var receiver: OSCPortIn? = null
    flowScope.launch(Dispatchers.IO) {
        resolumeArenaReceiveAddress.collectLatest { address ->
            receiver?.close()
            logger.info { "connecting to resolume arena ${address}" }
            receiver = OSCPortInBuilder()
                .setSocketAddress(address)
                .setPacketListener(OSCPortIn.defaultPacketListener())
                .let { inBuilder ->
                    OscSynced.syncedValues.filter {
                        it.receive && it.target == OscSynced.Target.ResolumeArena
                    }.fold(inBuilder) { builder, syncedValue ->
                        builder.addMessageListener(
                            syncedValue.messageSelector
                        ) { messageEvent ->
                            runBlocking {
                                val msg = messageEvent.message
                                if (syncedValue.logReceived) {
                                    logger.debug { "Arena IN: ${msg.stringify()}" }
                                }
                                syncedValue.onMessageEvent(messageEvent)
                            }
                        }
                    }
                }
                .addPacketListener(object : OSCPacketListener {
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

                    val layerClipConnectedPattern = """/composition/layers/(\d+)/clips/(\d+)/connected""".toRegex()

                    fun handleMessage(message: OSCMessage) {
                        runBlocking {
//                    logger.debugF { "RESOLUME IN: ${message.stringify()}" }
                            val address = message.address

//                    if(address.endsWith("connected")) {
//                        logger.infoF { message.stringify() }
//                    }

                            try {
                                layerClipConnectedPattern.matchEntire(message.address)?.let { matchResult ->
//                            val (_, layer, clip) = matchResult.groupValues
                                    val layer = matchResult.groupValues[1].toInt()
                                    val clip = matchResult.groupValues[2].toInt()
                                    val arg = message.arguments.first() as Int

//                            val binaryString = arg.toString(2).padStart(4, ' ')
                                    val description = when (arg) {
                                        0 -> "_"
                                        1 -> "_"
                                        2 -> "selected"
                                        3 -> "connected"
                                        4 -> "connected and selected"
                                        else -> "unknown"
                                    }
                                    val connected = arg == 3 || arg == 4
                                    logger.debug { "layer: $layer, clip: $clip=$arg ($description)" }
                                    updateResolumeLayerState(
                                        layer = layer,
                                        clip = clip,
                                        connected = connected
                                    )
                                }
                            } catch (e: Exception) {
                                logger.error(e) { "unhandled exception" }
                            }

                            val arguments = message.arguments.joinToString(" ")
                            messages.send(address to arguments)
                        }
                    }

                    override fun handleBadData(event: OSCBadDataEvent) {
                        runBlocking {
                            logger.warn { "osc bad data: $event" }
                        }
                        // TODO("Not yet implemented")
                    }

                })
                .addMessageListener(
                    OSCPatternAddressMessageSelector("/*")
                ) { messageEvent ->
                    runBlocking {
                        val message = messageEvent.message
                        logger.debug { "RESOLUME IN: ${message.stringify()}" }
                        val address = message.address
                        val arguments = message.stringifyArguments()
                        messages.send(address to arguments)
                    }
                }
                .build()
                .also {
                    it.startListening()
                }
        }

    }

    val resolumeStateFile = File("resolume_state.json")
    val json = Json5 {
        prettyPrint = true
    }

    messages.consumeAsFlow()
        .runningFold(
            emptyMap<String, String>()
        ) { acc, (k, v) ->
            val map = acc.toMutableMap()
            map.remove(k)
            map[k] = v
            map.toMap()
        }
        .sample(1.seconds)
        .onEach { stateMap ->
            resolumeStateFile.writeText(
                json.encodeToString(MapSerializer(String.serializer(), String.serializer()), stateMap)
            )
        }
        .launchIn(flowScope)

    resolumeLayerStateUpdates
        .consumeAsFlow()
        .runningFold(emptyMap<Int, Map<Int, Boolean>>()) { acc, (layer, clip, state) ->
            val lastState = acc[layer]?.toMutableMap() ?: mutableMapOf()
            lastState[clip] = state
            acc + (layer to lastState.toMap())
        }
        .map { layerMap ->
            layerMap.mapValues { (_, stateMap) ->
                stateMap.toList().firstOrNull() { (_, value) -> value }?.first
            }
        }
        .debounce(50.milliseconds)
        .onEach {
            logger.info { it }
        }
        .onEach {
            resolumeLayerStates.value = it
        }
        .launchIn(flowScope)


    resolumeGroupSizes.forEachIndexed { groupIndex, groupSize ->
        (1..groupSize).forEach { layer ->
            val resolumeLayerIndex = resolumeLayerIndex(groupIndex + 1, layer)
            logger.info { "creating buttons for group ${groupIndex + 1} layer ${layer} ($resolumeLayerIndex)" }
            val connectSpecificClip = connectSpecificClip(groupIndex + 1, layer)

            val exclusiveSwitch =
                MutableStateFlow(-1) // OscSynced.ExclusiveSwitch("/resolume/group${groupIndex+1}/layer${layer}", 6, -1)

            exclusiveSwitch
                .drop(1)
                .onEach { clipIndex ->
                    logger.info { "resolume switch group ${groupIndex + 1} layer ${layer} changed to index $clipIndex" }

                    connectSpecificClip.value = clipIndex
                }
                .launchIn(flowScope)

            resolumeLayerStates
                .map { it[resolumeLayerIndex] }
                .distinctUntilChanged()
                .onEach { clip ->
                    logger.info { "resolume group ${groupIndex + 1} layer ${layer} changed to $clip" }
                    connectSpecificClip.value = clip?.let { it - 1 } ?: -1
                }
                .launchIn(flowScope)

            connectSpecificClip
                .onEach {
                    logger.info { "resolume group ${groupIndex + 1} layer ${layer} connectSpecificClip index $it" }
                    exclusiveSwitch.value = if (it < 0) -1 else it
                }
                .launchIn(flowScope)
        }
    }

//    delay(1000)
}

suspend fun startNestdropListener() {
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
                    OscSynced.syncedValues.filter {
                        it.receive && it.target == OscSynced.Target.Nestdrop
                    }.fold(inBuilder) { builder, syncedValue ->
                        logger.info { "adding listener for ${syncedValue.name}" }
                        builder.addMessageListener(
                            syncedValue.messageSelector
                        ) { messageEvent ->
                            runBlocking {
                                val msg = messageEvent.message
                                if (syncedValue.logReceived) {
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
                    while(receiver?.isConnected == false) {
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
}