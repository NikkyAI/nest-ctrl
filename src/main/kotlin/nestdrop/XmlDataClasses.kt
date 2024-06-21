package nestdrop

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

//@Serializable
//data class SettingsGeneral(
//    val Top: Int,
//    val Left: Int,
//    val Width: Int,
//    val Height: Int,
//    val ShowName: Boolean,
//    val LoadPreview: Boolean,
//    val NbDecks: Int,
//    val ResyncOnPreset: Boolean,
//    val LogPerformanceHistory: Boolean,
//    val SmoothChange: Boolean,
//    val RedBlue3D: Boolean,
//    val BpmModulateSpeed: Boolean,
//    val BpmModulateSpeedRef: Int,
//    val SpoutFirst: Boolean,
//    val AnimateZoom: Boolean,
//    val PreviewZoomSize: Float,
//    val DefaultAudioDevice: String = "Out: VoiceMeeter Aux Input (VB-Audio VoiceMeeter AUX VAIO)",
//    val MidiOutputDevice: String = "LoopBe Internal MIDI",
//    val HardCutThreshold: Int,
//    val OscEnable: Boolean,
//    val OscPort: Int = 8000,
//    val AutoChangeInstant: Boolean = false,
//    val BeatThreshold: Int = 20,
//    val ManualBPM: Int = 0,
//    val UseAbleton: Boolean = false,
//    val AutoChangeEnable: Boolean = false,
//    val ShuffleEnable: Boolean=true,
//    val AlwaysOnTop: Boolean = false,
//)

class XmlDataClasses {
    @Serializable
    data class QueueWindow(
        @SerialName("Name")
        val name: String,
        @SerialName("Top")
        val top: Int,
        @SerialName("Left")
        val left: Int,
        @SerialName("Width")
        val width: Int,
        @SerialName("Height")
        val height: Int,
        @SerialName("Type")
        val type: Int,
        @SerialName("Deck")
        val deck: Int,
        @SerialName("Open")
        val open: Boolean,
        @SerialName("AlwaysOnTop")
        val alwaysOnTop: Boolean,
        @XmlElement
        @SerialName("Presets")
        val presetsWrappers: Presets,
    ) {
        val presets get() = presetsWrappers.presets
    }

    @Serializable
    data class Presets(
        @XmlElement
        @SerialName("Presets")
        val presets: List<Preset>,
    )

    @Serializable
    @XmlSerialName("Preset")
    data class Preset(
        @SerialName("Name")
        val name: String,
        @SerialName("Effects")
        val effect: Int? = null,
        @SerialName("Overlay")
        val overlay: Boolean? = null,
        @SerialName("Sprite0")
        val sprite0: String? = null,
        @SerialName("Sprite1")
        val sSprite1: String? = null,
        @SerialName("Sprite2")
        val sprite2: String? = null,
        @SerialName("HotKey")
        val hotkey: String? = null,
        @SerialName("SettingCapture")
        val settingCapture: String? = null,
        @SerialName("SettingCaptureValues")
        val settingCaptureValues: String? = null,
        @SerialName("Type")
        val type: String? = null,
        @SerialName("Comments")
        val comments: String? = null,
    )

    @Serializable
    data class DeckSettings(
        @XmlElement
        @XmlSerialName("DeckPanelsIsOpen")
        val deckPanelsIsOpen: StringValue,
        @XmlElement
        @XmlSerialName("TransitTime")
        val transitTime: FloatValue,
        @XmlElement
        @XmlSerialName("FPS")
        val fps: IntValue,
        @XmlElement
        @XmlSerialName("AnimSpeed")
        val animationSpeed: FloatValue,
        @XmlElement
        @XmlSerialName("ZoomSpeed")
        val zoomSpeed: FloatValue,
        @XmlElement
        @XmlSerialName("ZoomExp")
        val ZoomExp: FloatValue,
        @XmlElement
        @XmlSerialName("RotationSpeed")
        val rotationSpeed: FloatValue,
        @XmlElement
        @XmlSerialName("WrapSpeed")
        val wrapSpeed: FloatValue,
        @XmlElement
        @XmlSerialName("HorizonMotion")
        val horizontalMotion: FloatValue,
        @XmlElement
        @XmlSerialName("VerticalMotion")
        val verticalMotion: FloatValue,
        @XmlElement
        @XmlSerialName("StretchSpeed")
        val stretchSpeed: FloatValue,
        @XmlElement
        @XmlSerialName("WaveMode")
        val waveMode: FloatValue,
        @XmlElement
        @XmlSerialName("Negative")
        val negative: FloatValue,
        @XmlElement
        @XmlSerialName("R")
        val red: FloatValue,
        @XmlElement
        @XmlSerialName("G")
        val green: FloatValue,
        @XmlElement
        @XmlSerialName("B")
        val blue: FloatValue,
        @XmlElement
        @XmlSerialName("Brightness")
        val brightness: FloatValue,
        @XmlElement
        @XmlSerialName("Contrast")
        val contrast: FloatValue,
        @XmlElement
        @XmlSerialName("Gamma")
        val gamma: FloatValue,
        @XmlElement
        @XmlSerialName("Hue")
        val hue: FloatValue,
        @XmlElement
        @XmlSerialName("Saturation")
        val saturation: FloatValue,
        @XmlElement
        @XmlSerialName("LumaKey")
        val lumaKey: RangeValue,
        @XmlElement
        @XmlSerialName("Alpha")
        val alpha: FloatValue,
        @XmlElement
        @XmlSerialName("Strobe")
        val strobe: Strobe,
        @XmlElement
        @XmlSerialName("Audio")
        val audio: Audio,
        @XmlElement
        @XmlSerialName("Spout")
        val spout: Spout,
        @XmlElement
        @XmlSerialName("VideoDeck")
        val videoDeck: VideoDeck,
        @XmlElement
        @XmlSerialName("OutputMonitor")
        val outputMonitor: IntValue,
        @XmlElement
        @XmlSerialName("NDI")
        val ndi: BooleanValue
    ) {

        @Serializable
        data class IntValue(
            @SerialName("Value")
            val value: Int
        )

        @Serializable
        data class FloatValue(
            @SerialName("Value")
            val value: Float
        )

        @Serializable
        data class RangeValue(
            @SerialName("Min")
            val min: Float,
            @SerialName("Max")
            val max: Float
        )

        @Serializable
        data class StringValue(
            @SerialName("Value")
            val value: String
        )

        @Serializable
        data class BooleanValue(
            @SerialName("Value")
            val value: Boolean
        )

        @Serializable
        data class Strobe(
            //Animation Speed, Zoom Speed, Rotation Speed, Wrap Speed, Horizon Motion, Vertical Motion, Random Motion, Strech Speed, Wave Mode, Solid Color, Negative, Brightness, Contrast, Gamma, Hue, Saturation, LumaKey Min, LumaKey Max, Green, Blue, Alpha
            @SerialName("Effect")
            val effectIndex: Int,
            @SerialName("Color")
            val color: String,
            @SerialName("EffectSpanMin")
            val effectSpanMin: Float,
            @SerialName("EffectSpanMax")
            val effectSpanMax: Float,
            // source: times/sec, bpm, volume peak, bass peak, mid peak, treble peak
            @SerialName("Trigger")
            val triggerIndex: Int,
            @SerialName("Speed")
            val speed: Float,
            @SerialName("PulseWidth")
            val pulseWidth: Float,
            // square, sawtooth, sine
            @SerialName("Waveform")
            val waveFormIndex: Int,
        )

        @Serializable
        data class Audio(
            @SerialName("Channel")
            val channel: Int,
            @SerialName("Bass")
            val bass: Float,
            @SerialName("Mid")
            val mid: Float,
            @SerialName("Treble")
            val treble: Float
        )
        @Serializable
        data class Spout(
            @SerialName("Width")
            val width: Int,
            @SerialName("Height")
            val height: Int,
        )
        @Serializable
        data class VideoDeck(
            @SerialName("Preview")
            val preview: Int,
            @SerialName("TopMost")
            val topMost: Boolean,
        )
    }

}