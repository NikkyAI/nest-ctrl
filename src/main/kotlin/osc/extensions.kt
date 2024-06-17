package osc

import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCMessageInfo
import nl.adaptivity.xmlutil.core.impl.multiplatform.name

fun OSCMessage(address: String, arg: Any, vararg args: Any) = OSCMessage(
    address, listOf(arg, *args)
)
fun OSCMessage(address: String, arg: Any) = OSCMessage(
    address, listOf(arg), OSCMessageInfo(
        when(arg) {
            is Float -> "f"
            is String -> "s"
            is Int -> "i"
            is Long -> "L"
            true -> "T"
            false -> "F"

            else -> error("unhandlded type ${arg::class.name}")
        }
    )
)
fun OSCMessage(address: String, arg: Int) = OSCMessage(
    address, listOf(arg)
)
fun OSCMessage(address: String, arg: Float) = OSCMessage(
    address, listOf(arg)
)
fun OSCMessage(address: String, arg: String) = OSCMessage(
    address, listOf(arg)
)
fun OSCMessage(address: String, arg: Boolean) = OSCMessage(
    address, listOf(arg)
)
