package utils

import java.io.File
import java.io.IOException


fun runCommand(vararg command: String, workingDir: File): Process {
    return ProcessBuilder(*command)
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
//        .inheritIO()
        .start()!!
//        .waitFor(120, TimeUnit.SECONDS)
}

fun runCommandCaptureOutput(vararg command: String, workingDir: File): String {
    try {
        val proc = ProcessBuilder(*command)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            //        .inheritIO()
            .start()!!
        //        .waitFor(120, TimeUnit.SECONDS)
        return proc.inputStream.bufferedReader().readText()

    } catch (e: IOException) {
        e.printStackTrace()
        throw Exception("cannot execute '${command.joinToString(" ")}'")
    }
}
fun runCommandCaptureOutput(vararg command: String, workingDir: File, input: File): String {
//    println("running ${listOf(*command).joinToString(" ")}")
    try {
        val proc = ProcessBuilder(*command)
            .directory(workingDir)
            .redirectInput(input)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            //        .inheritIO()
            .start()!!
        //        .waitFor(120, TimeUnit.SECONDS)
        return proc.inputStream.bufferedReader().readText()

    } catch (e: IOException) {
        e.printStackTrace()
        throw Exception("cannot execute '${command.joinToString(" ")}'")
    }
}