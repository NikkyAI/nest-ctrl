package nestdrop

data class Queue<PRESET: Preset>(
    val index: Int,
    val name: String,
    val type: QueueType,
    val deck: Int,
    val open: Boolean,
    val beatOffset: Float = 1.0f,
    val beatMultiplier: Float = 1.0f,
//    val defaultSpriteOverlay: Int = -1,
    val active: Boolean = false,
    val presets: List<PRESET> = emptyList()
) {
//    val xpath = "/NestDropSettings/QueueWindows/*[@Name='$name']"
}

enum class PresetType(val type: Int){
    Mildrop(1),
    ImgSprite(2),
    MIDI(4),
    SettingsPreset(5),
    SpoutSprite(12),
}

sealed interface Preset {
    abstract val id: Int

    data class Milkdrop(
        val index: Int,
        val name: String,
        override val id: Int,
        val effects: Int?,
        val overlay: Boolean?,
//    val settingsCapture: List<Int> = emptyList(),
//    val settingsCaptureValues: List<Float> = emptyList()
    ) : Preset {
        val label = name +
                if (effects != 0) {
                    " FX: $effects"
                } else {
                    ""
                }
    }

    data class ImageSprite(
        val index: Int,
        val name: String,
        override val id: Int,
        val effects: Int?,
        val overlay: Boolean?,
    ): Preset {
        val label = name
            .substringBeforeLast(".jpg")
            .substringBeforeLast(".png") +
                if (effects != 0) {
                    " FX: $effects"
                } else {
                    ""
                }
    }
    data class SpoutSprite(
        val index: Int,
        val name: String,
        override val id: Int,
        val effects: Int?,
        val overlay: Boolean?,
    ): Preset {
        val label = name +
                if (effects != 0) {
                    " FX: $effects"
                } else {
                    ""
                }
    }
}
