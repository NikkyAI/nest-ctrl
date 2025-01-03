package osc

import com.illposed.osc.*
import com.illposed.osc.messageselector.OSCPatternAddressMessageSelector
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking


private val logger = KotlinLogging.logger { }


sealed interface OscSynced<T: Any> {
    val receive: Boolean
    val send: Boolean
    var logReceived: Boolean
    var logSending: Boolean
    val flow: Flow<*>
    val messageSelector: MessageSelector
    val target: Target
    var dropFirst: Int
    val name: String

    enum class Target(val label :String) {
        Nestdrop("Nestdrop"),
        ResolumeArena("ResolumeArena"),
    }

    suspend fun onMessageEvent(event: OSCMessageEvent)

//    suspend fun generateOscMessage(value: Any): OSCPacket
    suspend fun generateOscMessagesUntyped(value: Any): List<OSCPacket> {
        return generateOscMessages(value as T)
    }

    suspend fun generateOscMessages(value: T): List<OSCPacket>

    interface ListenAddress<T: Any>: OscSynced<T> {
        override val name: String
            get() = listenAddress
        val listenAddress: String
        override val messageSelector: MessageSelector get() = OSCPatternAddressMessageSelector(listenAddress)
    }


    interface SendingSingle<T: Any> : OscSynced<T> {
        val sendAddress: String

        suspend fun trySetValue(value: Any) {
            try {
                @Suppress("UNCHECKED_CAST")
                setValue(value as T)
            } catch (e: TypeCastException) {
                logger.error(e) { "failed to sync value $value to $sendAddress (${value::class.qualifiedName})" }
            }
        }

        override suspend fun generateOscMessages(value: T): List<OSCPacket> {
            return listOf(
                generateOscMessage(value)
            )
        }

        override suspend fun onMessageEvent(event: OSCMessageEvent) {
            val value = event.message.arguments.first()
            trySetValue(value)
        }

        suspend fun generateOscMessage(value: T): OSCPacket {
            return OSCMessage(sendAddress, value)
        }

        suspend fun setValue(value: T)
    }

//    interface IList<T, Q> : OscSynced, Address {
//        suspend fun trySetValue(value1: Any, value2: Any) {
//            try {
//                @Suppress("UNCHECKED_CAST")
//                setValue(value1 as T, value2 as Q)
//
//            } catch (e: TypeCastException) {
//                logger.error(e) { "unexpected type in osc message arguments $value1 $value2" }
//                logger.error(e) { "failed to sync value $value1, $value2 to $address (${value1::class.qualifiedName} ${value2::class.qualifiedName})" }
//            }
//        }
//
//
//        override suspend fun generateOscMessages(value: Any): List<OSCPacket> {
//            return listOf(
//                generateOscMessage(value)
//            )
//        }
//
//        override suspend fun onMessageEvent(event: OSCMessageEvent) {
//            val msg = event.message
//            val value1 = msg.arguments[0]
//            val value2 = msg.arguments[1]
//            trySetValue(value1, value2)
//        }
//
//        suspend fun generateOscMessage(value: Any): OSCPacket {
//            val (value1, value2) = value as kotlin.Pair<*, *>
//            return OSCMessage(
//                address,
//                listOf(value1!!, value2!!)
//            )
//        }
//
//        suspend fun setValue(value1: T, value2: Q)
//    }

    class ValueSingle<T: Any> private constructor(
        address: String,
        private val state: MutableStateFlow<T>,
        override val receive: Boolean,
        override val send: Boolean,
        override val target: Target,
        val valueToMessages: suspend (String, T) -> List<OSCMessage>,
    ) : MutableStateFlow<T> by state, SendingSingle<T>, ListenAddress<T> {
        override val flow = state
        override val listenAddress: String = address
        override val sendAddress: String = address

        override suspend fun generateOscMessages(value: T): List<OSCPacket> {
            return valueToMessages(sendAddress, value)
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
            send: Boolean = true,
            target: Target,
            valueToMessages: suspend (String, T) -> List<OSCMessage> = { sendAddress, value ->
                listOf(
                    OSCMessage(sendAddress, value)
                )
            },
        ) : this(
            address = address,
            state = MutableStateFlow(initialValue),
            receive = receive,
            send = send,
            target = target,
            valueToMessages = valueToMessages
        )

        init {
            syncedValues += this
            runBlocking {
                logger.info { "creating synced value for $address" }
            }
        }
    }

    class FlowSingle<T: Any> private constructor(
        address: String,
        private val mutableFlow: MutableSharedFlow<T>,
        override val receive: Boolean,
        override val send: Boolean,
        override val target: Target,
        val valueToMessages: suspend (String, T) -> List<OSCMessage>,
    ) : SharedFlow<T> by mutableFlow, SendingSingle<T>, ListenAddress<T> {
        override val flow = mutableFlow
        override val listenAddress: String = address
        override val sendAddress: String = address

        override suspend fun generateOscMessages(value: T): List<OSCPacket> {
            return valueToMessages(sendAddress, value)
        }

        override suspend fun setValue(value: T) {
            mutableFlow.emit(value)
        }

        override var logReceived: Boolean = true
        override var logSending: Boolean = true
        override var dropFirst: Int = 0

        constructor(
            address: String,
            receive: Boolean = true,
            send: Boolean = true,
            target: Target,
            valueToMessages: suspend (String, T) -> List<OSCMessage> = { sendAddress, value ->
                listOf(
                    OSCMessage(sendAddress, value)
                )
            },
        ) : this(
            address = address,
            mutableFlow = MutableSharedFlow(),
            receive = receive,
            send = send,
            target = target,
            valueToMessages = valueToMessages
        )

        init {
            syncedValues += this
            runBlocking {
                logger.info { "creating synced value for $address" }
            }
        }
    }

    class FlowCustom <T: Any> private constructor(
        address: String,
        private val mutableSharedFlow: MutableSharedFlow<T>,
        override val receive: Boolean,
        override val target: Target,
        val argToValue: suspend (String, List<Any>) -> T,
    ): SharedFlow<T> by mutableSharedFlow, ListenAddress<T> {
        override val flow = mutableSharedFlow
        override val listenAddress: String = address

        //NOTE: maybe allow sending if required
        override val send: Boolean = false

        override var logReceived: Boolean = true
        override var logSending: Boolean = true
        override var dropFirst: Int = 0

        override suspend fun onMessageEvent(event: OSCMessageEvent) {
            logger.info { "onMessageEvent ${event.message.stringify()}" }
            mutableSharedFlow.emit(argToValue(event.message.address, event.message.arguments))
        }

        override suspend fun generateOscMessages(value: T): List<OSCPacket> {
            return listOf(
                //TODO: generate message as required
//                generateOscMessage(value)
            )
        }

        constructor(
            address: String,
            receive: Boolean = true,
            target: Target,
            argToValue: suspend (String, List<Any>) -> T,
        ) : this(
            address = address,
            mutableSharedFlow = MutableSharedFlow(),
            receive = receive,
            target = target,
            argToValue = argToValue
        )

        init {
            syncedValues += this
            runBlocking {
                logger.info { "creating synced value for $listenAddress" }
            }
        }
    }

    class ValueCustom <T: Any> private constructor(
        address: String,
        private val stateFlow: MutableStateFlow<T>,
        override val receive: Boolean,
        override val target: Target,
        val argToValue: suspend (address: String, arguments: List<Any>) -> T,
    ): StateFlow<T> by stateFlow, ListenAddress<T> {
        override val flow = stateFlow
        override val listenAddress: String = address

        //NOTE: maybe allow sending if required
        override val send: Boolean = false

        override var logReceived: Boolean = true
        override var logSending: Boolean = true
        override var dropFirst: Int = 0

        override suspend fun onMessageEvent(event: OSCMessageEvent) {
//            logger.info { "parsing ${event.message.stringify()}" }
            stateFlow.emit(argToValue(event.message.address, event.message.arguments))
//            state.value = argToValue(event.message.address, event.message.arguments)
        }

        override suspend fun generateOscMessages(value: T): List<OSCPacket> {
            return listOf(
                //TODO: generate message as required
//                generateOscMessage(value)
            )
        }

        constructor(
            address: String,
            initialValue: T,
            receive: Boolean = true,
            target: Target,
            argToValue: suspend (address: String, arguments: List<Any>) -> T,
        ) : this(
            address = address,
            stateFlow = MutableStateFlow(initialValue),
            receive = receive,
            target = target,
            argToValue = argToValue
        )

        init {
            syncedValues += this
            runBlocking {
                logger.info { "creating synced value for $listenAddress" }
            }
        }
    }

//    class ExclusiveSwitch private constructor(
//        private val addressPrefix: String,
//        private val n: Int,
//        override val target: Target,
////        private val activeIndex: MutableStateFlow<Int> = MutableStateFlow(initialValue),
//        private val stateFlow: MutableStateFlow<Int>, // = MutableStateFlow(initialValue),
//    ): MutableStateFlow<Int> by stateFlow, OscSynced {
//        override val receive: Boolean = true
//
//        override var logReceived: Boolean = true
//        override var logSending: Boolean = true
//        override var dropFirst: Int = 0
//        override val flow = stateFlow
//
//        override val messageSelector: MessageSelector = OSCPatternAddressMessageSelector("$addressPrefix/*")
//
//        override suspend fun onMessageEvent(event: OSCMessageEvent) {
//            val args = event.message.arguments
//            val address = event.message.address
//
//            val index = address.substringAfterLast('/').toInt()
//            val bool = try {
//                args.first() as Boolean
//            } catch (e: TypeCastException) {
//                logger.error(e) { "unexpected type in osc message ${event.message}" }
//                return
//            }
//            if (bool) {
//                logger.debug { "$addressPrefix switches to $index" }
//                stateFlow.value = index
//            } else if (index == stateFlow.value) {
//                logger.debug { "$addressPrefix switches off" }
//                stateFlow.value = -1
//            }
////            toggleStates.value += (index to bool)
//        }
//
//        private var previousState: Int = -1
//
//        override suspend fun generateOscMessages(value: Any): List<OSCMessage> {
//            fun oscMessage(index: Int, state: Boolean): OSCMessage {
//                return OSCMessage("$addressPrefix/$index", state)
//            }
//            value as Int
//            val messages = if(value !in 0 until n && previousState in 0 until n) {
//                listOf(
//                    oscMessage(previousState, false),
//                )
//            } else if(previousState == value){
//                listOf(
////                    oscMessage(value, false),
//                    oscMessage(value, true)
//                )
//            } else if (previousState in 0 until n) {
//                listOf(
//                    oscMessage(previousState,false),
//                    oscMessage(value, true)
//                )
//            } else {
//                listOf(
//                    oscMessage(value, true)
//                )
//            }
//            previousState = value as Int
//            return messages
//        }
//
//
//        constructor(
//            addressPrefix: String,
//            n: Int,
//            initialValue: Int,
//            target: Target,
//        ) : this(
//            addressPrefix = addressPrefix,
//            n = n,
//            target = target,
//            stateFlow = MutableStateFlow(initialValue),
//        )
//        init {
//            syncedValues += this
////            label(addressPrefix)
//        }
//    }


//    class Trigger(
//        override val address: String,
//        override val target: Target,
//        private val stateFlow: MutableStateFlow<Int>,
//    ): Flow<Int> by stateFlow, OscSynced, Address {
//        override val receive: Boolean = true
//
//        override var logReceived: Boolean = true
//        override var logSending: Boolean = true
//        override var dropFirst: Int = 0
//        override val flow = stateFlow
//
//        suspend fun trigger() {
//            stateFlow.value++
//        }
//        private var lastTime = Instant.DISTANT_PAST
//        override suspend fun onMessageEvent(event: OSCMessageEvent) {
//            val now = Clock.System.now()
//            if(now - lastTime > 500.milliseconds) {
//                trigger()
//                lastTime = now
//            }
//        }
//
//        override suspend fun generateOscMessages(value: Any): List<OSCMessage> {
//            return emptyList()
//        }
//
//        constructor(
//            address: String,
//            target: Target,
//        ) : this(
//            address = address,
//            target = target,
//            stateFlow = MutableStateFlow(0)
//        )
//        init {
//            syncedValues += this
////            label(address)
//        }
//    }
//
//
//    class TriggerWithValue<T>(
//        override val address: String,
//        private val timeout: Duration,
//        override val target: Target,
//        private val stateFlow: MutableStateFlow<kotlin.Pair<Int, T>>,
//    ): Flow<kotlin.Pair<Int, T>> by stateFlow, OscSynced, Address {
//        override val receive: Boolean = true
//
//        override var logReceived: Boolean = true
//        override var logSending: Boolean = true
//        override var dropFirst: Int = 0
//        override val flow = stateFlow
//
//        suspend fun trigger(value: T) {
//            stateFlow.value = stateFlow.value.first+1 to value
//        }
//        private var lastTime = Instant.DISTANT_PAST
//        override suspend fun onMessageEvent(event: OSCMessageEvent) {
//            val now = Clock.System.now()
//            if(now - lastTime > timeout) {
//                val value = try {
//                    event.message.arguments[0] as T
//                } catch (e: TypeCastException) {
//                    logger.error(e) { "unexpected type in osc message ${event.message}" }
//                    return
//                }
//                trigger(value)
//                lastTime = now
//            }
//        }
//
//        override suspend fun generateOscMessages(value: Any): List<OSCMessage> {
//            return emptyList()
//        }
//
//        constructor(
//            address: String,
//            initialValue: T,
//            timeout: Duration = 500.milliseconds,
//            target: Target,
//        ) : this(
//            address = address,
//            timeout = timeout,
//            target = target,
//            stateFlow = MutableStateFlow(0 to initialValue)
//        )
//        init {
//            syncedValues += this
////            label(address)
//        }
//    }

    companion object {
        val syncedValues = mutableListOf<OscSynced<*>>()
    }
}


