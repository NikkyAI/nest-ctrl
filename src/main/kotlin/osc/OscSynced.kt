package osc

import com.illposed.osc.*
import com.illposed.osc.messageselector.OSCPatternAddressMessageSelector
import io.klogging.logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import logging.debugF
import logging.errorF
import logging.infoF
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


private val logger = logger(OscSynced::class.qualifiedName!!)


sealed interface OscSynced {
    val receive: Boolean
    var logReceived: Boolean
    var logSending: Boolean
    val flow: StateFlow<*>
    val messageSelector: MessageSelector
    val target: Target
    var dropFirst: Int

    enum class Target(val label :String) {
        ResolumeArena("ResolumeArena"),
    }

    suspend fun onMessageEvent(event: OSCMessageEvent)

//    suspend fun generateOscMessage(value: Any): OSCPacket
    suspend fun generateOscMessages(value: Any): List<OSCPacket>

    interface Address: OscSynced {
        val address: String
        override val messageSelector: MessageSelector get() = OSCPatternAddressMessageSelector(address)
    }


    interface SingleValue<T> : OscSynced, Address {
        suspend fun trySetValue(value: Any) {
            try {
                @Suppress("UNCHECKED_CAST")
                setValue(value as T)
            } catch (e: TypeCastException) {
                logger.errorF(e) { "failed to sync value $value to $address (${value::class.qualifiedName})" }
            }
        }

        override suspend fun generateOscMessages(value: Any): List<OSCPacket> {
            return listOf(
                generateOscMessage(value)
            )
        }

        override suspend fun onMessageEvent(event: OSCMessageEvent) {
            val value = event.message.arguments.first()
            trySetValue(value)
        }

        suspend fun generateOscMessage(value: Any): OSCPacket {
            return OSCMessage(address, value)
        }

        suspend fun setValue(value: T)
    }

    interface Value2<T, Q> : OscSynced, Address {
        suspend fun trySetValue(value1: Any, value2: Any) {
            try {
                @Suppress("UNCHECKED_CAST")
                setValue(value1 as T, value2 as Q)

            } catch(e: TypeCastException) {
                logger.errorF(e) { "unexpected type in osc message arguments $value1 $value2" }
               logger.errorF(e) { "failed to sync value $value1, $value2 to $address (${value1::class.qualifiedName} ${value2::class.qualifiedName})" }
            }
        }


        override suspend fun generateOscMessages(value: Any): List<OSCPacket> {
            return listOf(
                generateOscMessage(value)
            )
        }

        override suspend fun onMessageEvent(event: OSCMessageEvent) {
            val msg = event.message
            val value1 = msg.arguments[0]
            val value2 = msg.arguments[1]
            trySetValue(value1, value2)
        }

        suspend fun generateOscMessage(value: Any): OSCPacket {
            val (value1, value2) = value as Pair<*, *>
            return OSCMessage(
                address,
                listOf(value1!!, value2!!)
            )
        }

        suspend fun setValue(value1: T, value2: Q)
    }

    class Value<T: Any> private constructor(
        override val address: String,
        private val state: MutableStateFlow<T>,
        override val receive: Boolean,
        override val target: Target,
        val valueToMessages: suspend (String, T) -> List<OSCMessage>,
    ) : MutableStateFlow<T> by state, SingleValue<T> {
        override val flow = state
//        override suspend fun trySetValue(value: Any) {
//            try {
//                @Suppress("UNCHECKED_CAST")
//                state.value = value as T
//            } catch (e: TypeCastException) {
//                logger.errorF(e) { "failed to sync value $value to $address (${value::class.qualifiedName})" }
//            }
//        }

        override suspend fun generateOscMessages(value: Any): List<OSCPacket> {
            return valueToMessages(address, value as T)
        }

        override suspend fun setValue(value: T) {
            state.value = value
        }

        override var logReceived: Boolean = true
        override var logSending: Boolean = true
        override var dropFirst: Int = 0

        constructor(
            address: String,
            initialValue: T,
            receive: Boolean = true,
            target: Target,
            valueToMessages: suspend (String, T) -> List<OSCMessage> = { address, value ->
                listOf(
                    OSCMessage(address, value)
                )
            },
        ) : this(
            address = address,
            state = MutableStateFlow(initialValue),
            receive = receive,
            target = target,
            valueToMessages = valueToMessages
        )

        init {
            syncedValues += this
//            label(address)
            runBlocking {
                logger.infoF { "creating synced value for $address" }
            }
        }
    }

    class ExclusiveSwitch private constructor(
        private val addressPrefix: String,
        private val n: Int,
        override val target: Target,
//        private val activeIndex: MutableStateFlow<Int> = MutableStateFlow(initialValue),
        private val stateFlow: MutableStateFlow<Int>, // = MutableStateFlow(initialValue),
    ): MutableStateFlow<Int> by stateFlow, OscSynced {
        override val receive: Boolean = true

        override var logReceived: Boolean = true
        override var logSending: Boolean = true
        override var dropFirst: Int = 0
        override val flow = stateFlow

        override val messageSelector: MessageSelector = OSCPatternAddressMessageSelector("$addressPrefix/*")

        override suspend fun onMessageEvent(event: OSCMessageEvent) {
            val args = event.message.arguments
            val address = event.message.address

            val index = address.substringAfterLast('/').toInt()
            val bool = try {
                args.first() as Boolean
            } catch(e: TypeCastException) {
                logger.errorF(e) { "unexpected type in osc message ${event.message}" }
                return
            }
            if(bool) {
                logger.debugF { "$addressPrefix switches to $index" }
                stateFlow.value = index
            } else if(index == stateFlow.value) {
                logger.debugF { "$addressPrefix switches off" }
                stateFlow.value = -1
            }
//            toggleStates.value += (index to bool)
        }

        private var previousState: Int = -1

        override suspend fun generateOscMessages(value: Any): List<OSCMessage> {
            fun oscMessage(index: Int, state: Boolean): OSCMessage {
                return OSCMessage("$addressPrefix/$index", state)
            }
            value as Int
            val messages = if(value !in 0 until n && previousState in 0 until n) {
                listOf(
                    oscMessage(previousState, false),
                )
            } else if(previousState == value){
                listOf(
//                    oscMessage(value, false),
                    oscMessage(value, true)
                )
            } else if (previousState in 0 until n) {
                listOf(
                    oscMessage(previousState,false),
                    oscMessage(value, true)
                )
            } else {
                listOf(
                    oscMessage(value, true)
                )
            }
            previousState = value as Int
            return messages
        }


        constructor(
            addressPrefix: String,
            n: Int,
            initialValue: Int,
            target: Target,
        ) : this(
            addressPrefix = addressPrefix,
            n = n,
            target = target,
            stateFlow = MutableStateFlow(initialValue),
        )
        init {
            syncedValues += this
//            label(addressPrefix)
        }
    }


    class Trigger(
        override val address: String,
        override val target: Target,
        private val stateFlow: MutableStateFlow<Int>,
    ): Flow<Int> by stateFlow, OscSynced, Address {
        override val receive: Boolean = true

        override var logReceived: Boolean = true
        override var logSending: Boolean = true
        override var dropFirst: Int = 0
        override val flow = stateFlow

        suspend fun trigger() {
            stateFlow.value++
        }
        private var lastTime = Instant.DISTANT_PAST
        override suspend fun onMessageEvent(event: OSCMessageEvent) {
            val now = Clock.System.now()
            if(now - lastTime > 500.milliseconds) {
                trigger()
                lastTime = now
            }
        }

        override suspend fun generateOscMessages(value: Any): List<OSCMessage> {
            return emptyList()
        }

        constructor(
            address: String,
            target: Target,
        ) : this(
            address = address,
            target = target,
            stateFlow = MutableStateFlow(0)
        )
        init {
            syncedValues += this
//            label(address)
        }
    }


    class TriggerWithValue<T>(
        override val address: String,
        private val timeout: Duration,
        override val target: Target,
        private val stateFlow: MutableStateFlow<Pair<Int, T>>,
    ): Flow<Pair<Int, T>> by stateFlow, OscSynced, Address {
        override val receive: Boolean = true

        override var logReceived: Boolean = true
        override var logSending: Boolean = true
        override var dropFirst: Int = 0
        override val flow = stateFlow

        suspend fun trigger(value: T) {
            stateFlow.value = stateFlow.value.first+1 to value
        }
        private var lastTime = Instant.DISTANT_PAST
        override suspend fun onMessageEvent(event: OSCMessageEvent) {
            val now = Clock.System.now()
            if(now - lastTime > timeout) {
                val value = try {
                    event.message.arguments[0] as T
                } catch(e: TypeCastException) {
                    logger.errorF(e) { "unexpected type in osc message ${event.message}" }
                    return
                }
                trigger(value)
                lastTime = now
            }
        }

        override suspend fun generateOscMessages(value: Any): List<OSCMessage> {
            return emptyList()
        }

        constructor(
            address: String,
            initialValue: T,
            timeout: Duration = 500.milliseconds,
            target: Target,
        ) : this(
            address = address,
            timeout = timeout,
            target = target,
            stateFlow = MutableStateFlow(0 to initialValue)
        )
        init {
            syncedValues += this
//            label(address)
        }
    }

    companion object {
        val syncedValues = mutableListOf<OscSynced>()
    }
}


