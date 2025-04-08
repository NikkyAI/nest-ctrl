package osc

import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCPacket
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking


private val logger = KotlinLogging.logger { }


@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
sealed interface OscSynced<T : Any> {
    val flow: MutableSharedFlow<T>
//    val messageSelector: MessageSelector
    val target: Target
    val dropFirst: Int
    val label: String

    enum class Target(val label: String) {
        Nestdrop("Nestdrop"),
        ResolumeArena("ResolumeArena"),
    }

    interface Receiving<T : Any> : OscSynced<T> {
        override val label: String
            get() = listenAddress
        val receive: Boolean
        var logReceived: Boolean
        val listenAddress: String
//        override val messageSelector: MessageSelector get() = OSCPatternAddressMessageSelector(listenAddress)
        suspend fun onMessageEvent(message: OSCMessage)
    }

    interface MessageConverter<T : Any> : Receiving<T> {
        fun convertMessage(message: OSCMessage): T

        suspend fun tryConvertAndSet(value: OSCMessage) {
            val converted = try {
                convertMessage(value)
            } catch (e: TypeCastException) {
                logger.error(e) { "failed to convert message ${value.stringify()} (${value::class.qualifiedName})" }
                return
            }
            setValue(converted)
        }

        override suspend fun onMessageEvent(message: OSCMessage) {
            tryConvertAndSet(message)
        }

        suspend fun setValue(value: T) {
            flow.emit(value)
        }
    }

    interface ArgConverter<T : Any> : MessageConverter<T> {
        fun convertArg(input: Any): T {
            @Suppress("UNCHECKED_CAST")
            return input as T
        }

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
            @Suppress("UNCHECKED_CAST")
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
        override val dropFirst: Int = 0
    ) : /*MutableStateFlow<T> by state,*/ SendingSingle<T>, ArgConverter<T> {
        override val flow = state
        override val listenAddress: String = address
        override val sendAddress: String = address

        override var logReceived: Boolean = true
        override var logSending: Boolean = true

        override val label: String = address

        constructor(
            address: String,
            initialValue: T,
            receive: Boolean = true,
            send: Boolean = true,
            dropFirst: Int = 0,
            target: Target,
        ) : this(
            address = address,
            state = MutableStateFlow(initialValue),
            receive = receive,
            send = send,
            target = target,
            dropFirst = dropFirst
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
        override val target: Target,
        override val dropFirst: Int = 0,
    ) : /*SharedFlow<T> by mutableFlow,*/ SendingSingle<T>, ArgConverter<T> {
        override val flow = mutableFlow
        override val listenAddress: String = address
        override val sendAddress: String = address

        override var logReceived: Boolean = true
        override var logSending: Boolean = true

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
        override val dropFirst: Int = 0
//        val argToValue: suspend (String, List<Any>) -> T,
    ) : SharedFlow<T> by mutableSharedFlow, MessageConverter<T> {
        override val flow = mutableSharedFlow
        override val listenAddress: String = address

        //NOTE: maybe allow sending if required
//        override val send: Boolean = false

        override var logReceived: Boolean = true

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


