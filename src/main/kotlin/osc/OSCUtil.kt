package osc

import com.illposed.osc.OSCBadDataEvent
import com.illposed.osc.OSCBundle
import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCPacket
import com.illposed.osc.OSCPacketEvent
import com.illposed.osc.OSCPacketListener
import com.illposed.osc.messageselector.OSCPatternAddressMessageSelector
import com.illposed.osc.transport.OSCPortIn
import com.illposed.osc.transport.OSCPortInBuilder
import com.illposed.osc.transport.OSCPortOutBuilder
import flowScope
import io.github.xn32.json5k.Json5
import io.klogging.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import logging.debugF
import logging.errorF
import logging.infoF
import logging.traceF
import logging.warnF
import utils.receiveAvailable
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


private val logger = logger("osc.OSCUtil")


val controlSendChannel = Channel<OSCPacket>(Channel.BUFFERED)
val nestdropSendChannel = Channel<OSCPacket>(Channel.BUFFERED)
val arenaSendChannel = Channel<OSCPacket>(Channel.BUFFERED)

@Deprecated("stop using touch osc")
suspend fun runControlSend() {
    coroutineScope {
        val controlPortSender = OSCPortOutBuilder()
            .setRemoteSocketAddress(
                InetSocketAddress(
//                    InetAddress.getByName("127.0.0.1"),
                    InetAddress.getLocalHost(),
                    8003
                )
            )
            .build()
        controlPortSender.connect()
        logger.debugF { "constructed OSC port" }
        val job = launch {
            while (true) {
                delay(50)
                val oscMessages = controlSendChannel.receiveAvailable(32)
                if (oscMessages.isNotEmpty()) {
//                            oscMessages.forEach {
//                                logger.debugF { "TOOSC OUT: ${it.stringify()}" }
//                            }
//                logger.debugF { "sending ${packets.size} packets" }
                    val oscPacket = if (oscMessages.size == 1) {
                        oscMessages.first()
                    } else {
                        OSCBundle(oscMessages)
                    }
                    try {
                        controlPortSender.send(oscPacket)
                    } catch (e: Exception) {
                        logger.errorF(e) { "failed to send messages: ${oscMessages.joinToString { it.stringify() }} " }
                    }
                }
            }
        }
        job.join()
        logger.error { "port closed" }
        controlPortSender.close()
    }
}

suspend fun runNestDropSend() {
    coroutineScope {
        val nestdropPort = // OSCPortOut(InetAddress.getByName("127.0.0.1"), 8000)
            OSCPortOutBuilder()
                .setRemoteSocketAddress(
                    InetSocketAddress(
                        InetAddress.getByName("127.0.0.1"),
                        8000
                    )
                )
                .build()
        nestdropPort.connect()
        logger.debugF { "constructed OSC port" }
        for (oscPacket in nestdropSendChannel) {
            try {
                logger.traceF { "ND OUT: ${oscPacket.stringify()}" }
                nestdropPort.send(oscPacket)
            } catch (e: Exception) {
                logger.error(e) {}
            }
        }
        logger.error { "port closed" }
        nestdropPort.close()
    }
}

suspend fun runResolumeSend() {
    coroutineScope {
        val arenaPort = // OSCPortOut(InetAddress.getByName("127.0.0.1"), 8000)
            OSCPortOutBuilder()
                .setRemoteSocketAddress(
                    InetSocketAddress(
                        InetAddress.getLocalHost(),
                        7000
                    )
                )
                .build()
        arenaPort.connect()

        logger.debugF { "constructed OSC port" }
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
                        logger.errorF(e) { "failed to send messages: ${oscMessages.joinToString { it.stringify() }} " }
                    }
                }
            }
        }
        job.join()
        logger.error { "port closed" }
        arenaPort.close()
    }
}

suspend fun controlPortSend(packet: OSCMessage) {
    controlSendChannel.send(packet)
}

suspend fun controlPortSend(packet: OSCPacket) {
    controlSendChannel.send(packet)
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

private val connectedClipSyncedValues = mutableMapOf<Int, OscSynced.Value<Int>>()

fun connectSpecificClip(
    group: Int,
    layer: Int,
): OscSynced.Value<Int> {
    val layerIndex = resolumeLayerIndex(group, layer)
    return connectedClipSyncedValues[layerIndex] ?: run {
        OscSynced.Value(
            "/composition/layers/$layerIndex/connectspecificclip",
            -1,
            target = OscSynced.Target.ResolumeArena
        ) { address, value ->
//            updateResolumeLayerState(resolumeLayerIndex(group, layer), -1, false)
            if(value < 0) {
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
    return resolumeGroupSizes.take(group-1).sum() + layer
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
    connectSpecificClip(group, layer).value = clip-1
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
    val receiver = OSCPortInBuilder()
        .setSocketAddress(InetSocketAddress(withContext(Dispatchers.IO) {
            InetAddress.getByName("127.0.0.1")
        }, 7001))
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
                            logger.debugF { "Arena IN: ${msg.stringify()}" }
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
                            logger.debugF { "layer: $layer, clip: $clip=$arg ($description)" }
                            updateResolumeLayerState(
                                layer = layer,
                                clip = clip,
                                connected = connected
                            )
                        }
                    } catch (e: Exception) {
                        logger.errorF(e) { "unhandled exception" }
                    }

                    val arguments = message.arguments.joinToString(" ")
                    messages.send(address to arguments)
                }
            }

            override fun handleBadData(event: OSCBadDataEvent) {
                runBlocking {
                    logger.warnF { "osc bad data: $event" }
                }
                // TODO("Not yet implemented")
            }

        })
        .addMessageListener(
            OSCPatternAddressMessageSelector("/*")
        ) { messageEvent ->
            runBlocking {
                val message = messageEvent.message
                logger.debugF { "RESOLUME IN: ${message.stringify()}" }
                val address = message.address
                val arguments = message.stringifyArguments()
                messages.send(address to arguments)
            }
        }
        .build()
        .also {
            it.startListening()
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
            logger.infoF { it }
        }
        .onEach {
            resolumeLayerStates.value = it
        }
        .launchIn(flowScope)


    resolumeGroupSizes.forEachIndexed { groupIndex, groupSize ->
        (1..groupSize).forEach { layer ->
            val resolumeLayerIndex = resolumeLayerIndex(groupIndex+1, layer)
            logger.infoF { "creating buttons for group ${groupIndex+1} layer ${layer} ($resolumeLayerIndex)" }
            val connectSpecificClip = connectSpecificClip(groupIndex + 1, layer)

            val exclusiveSwitch = MutableStateFlow(-1) // OscSynced.ExclusiveSwitch("/resolume/group${groupIndex+1}/layer${layer}", 6, -1)

            exclusiveSwitch
                .drop(1)
                .onEach { clipIndex ->
                    logger.infoF { "resolume switch group ${groupIndex+1} layer ${layer} changed to index $clipIndex" }

                    connectSpecificClip.value = clipIndex
                }
                .launchIn(flowScope)

            resolumeLayerStates
                .map { it[resolumeLayerIndex] }
                .distinctUntilChanged()
                .onEach { clip ->
                    logger.infoF { "resolume group ${groupIndex + 1} layer ${layer} changed to $clip" }
                    connectSpecificClip.value = clip?.let { it - 1 } ?: -1
                }
                .launchIn(flowScope)

            connectSpecificClip
                .onEach {
                    logger.infoF { "resolume group ${groupIndex+1} layer ${layer} connectSpecificClip index $it" }
                    exclusiveSwitch.value = if(it < 0) -1 else it
                }
                .launchIn(flowScope)
        }
    }

    delay(1000)
}