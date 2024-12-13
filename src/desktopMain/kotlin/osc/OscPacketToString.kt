package osc

import com.illposed.osc.OSCBundle
import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCPacket

fun OSCPacket.stringify(): String {
    return when(this) {
        is OSCMessage -> {
            "$address << ${this.stringifyArguments()}"
        }
        is OSCBundle -> {
            packets.joinToString { it.stringify() }
        }

        else -> { "unknown type ${this::class.qualifiedName}"}
    }
}
fun OSCMessage.stringifyArguments(): String {
    val typeTags = info?.argumentTypeTags ?: ""
    return arguments.mapIndexed{ i, arg ->
        typeTags.getOrNull(i)?.let {
            when(it) {
                's' -> "$it\"$arg\""
                else -> "$it$arg"
            }
        } ?: when (arg) {
            is String -> "s\"$arg\""
            is Float -> "f${arg}"
            is Double -> "d${arg}"
            is Boolean -> if(arg) "Ttrue" else "Ffalse"
            else -> arg.toString()
        }
    }.joinToString()

}