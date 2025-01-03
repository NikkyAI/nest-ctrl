import dev.atsushieno.ktmidi.Midi1Message
import dev.atsushieno.ktmidi.Midi1SysExChunkProcessor
import dev.atsushieno.ktmidi.RtMidiAccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.nanoseconds

@OptIn(ExperimentalStdlibApi::class)
fun main(args: Array<String>): Unit = runBlocking {
    val midi = RtMidiAccess()
    println(midi.name)
    midi.stateChanged = {
        state, details ->
        println("state: $state on $details")
    }
    midi.inputs.forEach {
        midiPortDetails ->
        println()
        println(midiPortDetails.id)
        println(midiPortDetails.name)
        println(midiPortDetails.version)
        println(midiPortDetails.manufacturer)
        println(midiPortDetails.midiTransportProtocol)

//        val input = midi.openInput(midiPortDetails.id)
//        input.setMessageReceivedListener { data: ByteArray, start: Int, length: Int, timestampInNanoseconds: Long ->
//            println("received from : " +input.details.name)
//            println("data: " + data.toHexString())
//            println("start: " + start)
//            println("length: " + length)
//            println("timestampInNanoseconds: " + Instant.fromEpochMilliseconds(timestampInNanoseconds / 1000))
//        }
//        println("opened")
    }
    val inputDetails = midi.inputs.first { it.name == "2- Launch Control XL" }
    val input = midi.openInput(inputDetails.id)
    val sysExChunkProcessor = Midi1SysExChunkProcessor()
    input.setMessageReceivedListener { data: ByteArray, start: Int, length: Int, timestampInNanoseconds: Long ->
//        println("data: " + data.toHexString())
//        println("start: $start")
//        println("length: $length")
        println("timestampInNanoseconds: ${timestampInNanoseconds.nanoseconds}")
        val message = Midi1Message.convert(
            bytes = data,
            index = start,
            size = length,
            sysExChunkProcessor = sysExChunkProcessor
        )
        message.forEach {
            println("value: " + it.value.toHexString(HexFormat.UpperCase))
            println("channel: " + it.channel)
//            println(it.statusCode)
//            println(it.statusByte)
            println("metaType: " + it.metaType)
            println("LSB: " + it.lsb)
            println("LSB: " + it.msb)
        }
    }

    println("listening")

    while (true) {
        delay(1000)
    }
}
