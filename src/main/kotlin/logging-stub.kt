package logging

import io.github.oshai.kotlinlogging.KLogger

//import io.github.oshai.kotlinlogging.KotlinLogging
//
//@Deprecated("use KotlinLogging.logger", ReplaceWith("KotlinLogging.logger {  }", "io.github.oshai.kotlinlogging.KotlinLogging"))
//fun logger(classname: String) = KotlinLogging.logger(classname)

@Deprecated("inline", ReplaceWith("trace(message)"))
fun KLogger.traceF( message: () -> Any?) {
    trace(message)
}
@Deprecated("inline", ReplaceWith("debug(message)"))
fun KLogger.debugF( message: () -> Any?) {
    debug(message)
}
@Deprecated("inline", ReplaceWith("info(message)"))
fun KLogger.infoF( message: () -> Any?) {
    info(message)
}
@Deprecated("inline", ReplaceWith("warn(message)"))
fun KLogger.warnF( message: () -> Any?) {
    warn(message)
}
@Deprecated("inline", ReplaceWith("error(message)"))
fun KLogger.errorF( message: () -> Any?) {
    error(message)
}
@Deprecated("inline", ReplaceWith("error(exception, message)"))
fun KLogger.errorF(exception: Throwable, message: () -> Any?) {
    error(exception, message)
}