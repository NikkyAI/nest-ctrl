package nestdrop

data class Queue(
    val index: Int,
    val name: String,
    val type: QueueType,
    val open: Boolean,
    val deck: Int,
    val presets: List<Preset> = emptyList()
) {
    val xpath = "/NestDropSettings/QueueWindows/*[@Name='$name']"
}

data class Preset(
    val index: Int,
    val name: String,
    val effects: Int?,
    val overlay: Boolean?,
//    val settingsCapture: List<Int> = emptyList(),
//    val settingsCaptureValues: List<Float> = emptyList()
) {
    val label = name
        .substringBeforeLast(".jpg")
        .substringBeforeLast(".png") +
            if (effects != 0) {
                " FX: $effects"
            } else {
                ""
            }
}