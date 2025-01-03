package osc

import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCMessageInfo
import utils.className

//fun OSCMessage(address: String, arg: Any, vararg args: Any) = OSCMessage(
//    address, listOf(arg, *args)
//)

private val Any.typeTag
    get() = when (this) {
        is Float -> 'f'
        is String -> 's'
        is Char -> 'c'
        is Int -> 'i'
        is Long -> 'L'
        true -> 'T'
        false -> 'F'

        else -> null
    }

fun OSCMessage(address: String, arg: Any, vararg args: Any): OSCMessage {
    val allArgs = listOf(arg, *args)
    allArgs.forEach {
        require(it.className != null && it.typeTag != null)
    }

    return OSCMessage(
        address, listOf(arg), OSCMessageInfo(
            allArgs.mapNotNull { it.typeTag }.toCharArray().concatToString()
        )
    )
}

fun OSCMessage(address: String, arg: Int) = OSCMessage(
    address, listOf(arg), OSCMessageInfo("i")
)

fun OSCMessage(address: String, arg: Float) = OSCMessage(
    address, listOf(arg), OSCMessageInfo("f")
)

fun OSCMessage(address: String, arg: String) = OSCMessage(
    address, listOf(arg), OSCMessageInfo("s")
)

fun OSCMessage(address: String, arg: Boolean) = OSCMessage(
    address, listOf(arg), OSCMessageInfo(if (arg) "T" else "F")
)
