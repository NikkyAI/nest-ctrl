package osc

import com.illposed.osc.MessageSelector
import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCMessageEvent
import com.illposed.osc.OSCPacket
import com.illposed.osc.messageselector.OSCPatternAddressMessageSelector
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking


private val logger = KotlinLogging.logger { }


sealed interface OscSynced<T : Any> {
    val flow: MutableSharedFlow<T>
    val messageSelector: MessageSelector
    val target: Target
    var dropFirst: Int
    val label: String

    enum class Target(val label: String) {
        Nestdrop("Nestdrop"),
        ResolumeArena("ResolumeArena"),
    }

    suspend fun onMessageEvent(event: OSCMessageEvent)

    interface Receiving<T : Any> : OscSynced<T> {
        override val label: String
            get() = listenAddress
        val receive: Boolean
        var logReceived: Boolean
        val listenAddress: String
        override val messageSelector: MessageSelector get() = OSCPatternAddressMessageSelector(listenAddress)
    }

    interface MessageConverter<T : Any> : Receiving<T> {
        fun convertMessage(message: OSCMessage): T

        suspend fun tryConvertAndSet(value: OSCMessage) {
            val converted = try {
                @Suppress("UNCHECKED_CAST")
                convertMessage(value)
            } catch (e: TypeCastException) {
                logger.error(e) { "failed to convert message ${value.stringify()} (${value::class.qualifiedName})" }
                return
            }
            setValue(converted)
        }

        override suspend fun onMessageEvent(event: OSCMessageEvent) {
            tryConvertAndSet(event.message)
        }

        suspend fun setValue(value: T) {
            flow.emit(value as T)
        }
    }

    interface ArgConverter<T : Any> : MessageConverter<T> {
        fun convertArg(input: Any): T = input as T

        override fun convertMessage(message: OSCMessage): T {
            return convertArg(message.arguments.first())
        }
    }


    interface Sending<T : Any> : OscSynced<T> {
        override val label: String
            get() = sendAddress
        val send: Boolean
        val sendAddress: String
        var logSending: Boolean


        //    suspend fun generateOscMessage(value: Any): OSCPacket
        suspend fun generateOscMessagesUntyped(value: Any): List<OSCPacket> {
            return generateOscMessages(value as T)
        }

        suspend fun generateOscMessages(value: T): List<OSCPacket>

        suspend fun generateOscMessage(value: T): OSCPacket {
            return OSCMessage(sendAddress, value)
        }
    }

    interface SendingSingle<T : Any> : OscSynced<T>, Sending<T> {

        override suspend fun generateOscMessages(value: T): List<OSCPacket> {
            return listOf(
                generateOscMessage(value)
            )
        }

        override suspend fun generateOscMessage(value: T): OSCPacket {
            return OSCMessage(sendAddress, value)
        }
    }

    open class ValueSingle<T : Any> (
        address: String,
        private val state: MutableStateFlow<T>,
        override val receive: Boolean,
        override val send: Boolean,
        override val target: Target,
    ) : MutableStateFlow<T> by state, SendingSingle<T>, ArgConverter<T> {
        override val flow = state
        override val listenAddress: String = address
        override val sendAddress: String = address

        override var logReceived: Boolean = true
        override var logSending: Boolean = true
        override var dropFirst: Int = 0

        override val label: String = address

        constructor(
            address: String,
            initialValue: T,
            receive: Boolean = true,
            send: Boolean = true,
            target: Target,
        ) : this(
            address = address,
            state = MutableStateFlow(initialValue),
            receive = receive,
            send = send,
            target = target,
        )

        init {
            syncedValues += this
            runBlocking {
                logger.info { "creating synced value for $address" }
            }
        }
    }

    open class FlowSingle<T : Any> private constructor(
        address: String,
        private val mutableFlow: MutableSharedFlow<T>,
        override val receive: Boolean,
        override val send: Boolean,
        override val target: Target
    ) : SharedFlow<T> by mutableFlow, SendingSingle<T>, ArgConverter<T> {
        override val flow = mutableFlow
        override val listenAddress: String = address
        override val sendAddress: String = address

        override var logReceived: Boolean = true
        override var logSending: Boolean = true
        override var dropFirst: Int = 0

        override val label: String = address

        constructor(
            address: String,
            receive: Boolean = true,
            send: Boolean = true,
            target: Target,
        ) : this(
            address = address,
            mutableFlow = MutableSharedFlow(),
            receive = receive,
            send = send,
            target = target,
        )

        init {
            syncedValues += this
            runBlocking {
                logger.info { "creating synced value for $address" }
            }
        }
    }

    abstract class FlowBase<T : Any> private constructor(
        address: String,
        private val mutableSharedFlow: MutableSharedFlow<T>,
        override val receive: Boolean,
        override val target: Target,
//        val argToValue: suspend (String, List<Any>) -> T,
    ) : SharedFlow<T> by mutableSharedFlow, MessageConverter<T> {
        override val flow = mutableSharedFlow
        override val listenAddress: String = address

        //NOTE: maybe allow sending if required
//        override val send: Boolean = false

        override var logReceived: Boolean = true
        override var dropFirst: Int = 0

        override suspend fun setValue(value: T) {
            mutableSharedFlow.emit(value)
        }

        constructor(
            address: String,
            receive: Boolean = true,
            target: Target,
//            argToValue: suspend (String, List<Any>) -> T,
        ) : this(
            address = address,
            mutableSharedFlow = MutableSharedFlow(),
            receive = receive,
            target = target,
//            argToValue = argToValue
        )

        init {
            syncedValues += this
            runBlocking {
                logger.info { "creating synced value for $listenAddress" }
            }
        }
    }

    companion object {
        val syncedValues = mutableListOf<OscSynced<*>>()
    }
}


