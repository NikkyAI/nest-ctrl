package utils

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


val Any?.className get() = if(this != null) this::class.qualifiedName else null

@OptIn(ExperimentalContracts::class)
fun <T : Any> T?.checkNotNullExt(lazyMessage: (() -> String)? = null): T {
    contract {
        returns() implies (this@checkNotNullExt != null)
    }
    if (this == null) {
        val message = lazyMessage?.invoke() ?: "Required value was null."
        throw IllegalStateException(message)
    } else {
        return this
    }
}
@OptIn(ExperimentalContracts::class)
fun <T : Any> checkNotNullDebug(value: T?, message: String = "Required value was null."): T {
    contract {
        returns() implies (value != null)
    }
    if (value == null) {
//        val message = lazyMessage()
        throw IllegalStateException(message)
    } else {
        return value
    }
//    return checkNotNullWrap(value) { message }
}
@OptIn(ExperimentalContracts::class)
fun <T : Any> checkNotNullDebug(value: T?, lazyMessage: (() -> String)): T {
    contract {
        returns() implies (value != null)
    }
    if (value == null) {
        val message = lazyMessage?.invoke() ?: "Required value was null."
        throw IllegalStateException(message)
    } else {
        return value
    }
}

public fun errorDebug(message: String): Nothing = throw IllegalStateException(message)