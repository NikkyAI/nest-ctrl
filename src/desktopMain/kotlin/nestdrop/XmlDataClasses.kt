package nestdrop

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import xml.FavoriteListSerializer
import xml.LibraryClosedSectionsSerializer
import xml.QueueWindowsSerializer

@Serializable
data class NestdropSettings(
    val mainWindow: MainWindow,
    val queueWindows: QueueWindows,
    val favoriteList: FavoriteList,
    val hotKeyList: HotKeyList,
    val commentsList: CommentsList,
    val effectsList: EffectsList,
    val animSpeedList: AnimSpeedList
) {
    @Serializable
    data class MainWindow(
        @SerialName("Top")
        val top: Int,
        @SerialName("Left")
        val left: Int,
        @SerialName("Width")
        val width: Int,
        @SerialName("Height")
        val height: Int,

        @XmlElement
        @XmlSerialName("Settings_General")
        val settingsGeneral: SettingsGeneral,
        @XmlElement
        @XmlSerialName("LibraryClosedSections")
        val libraryClosedSections: LibraryClosedSections,
        @XmlElement
        @XmlSerialName("Settings_Deck1")
        val settingsDeck1: DeckSettings,
        @XmlElement
        @XmlSerialName("Settings_Deck2")
        val settingsDeck2: DeckSettings,
        @XmlElement
        @XmlSerialName("Settings_Deck3")
        val settingsDeck3: DeckSettings,
        @XmlElement
        @XmlSerialName("Settings_Deck4")
        val settingsDeck4: DeckSettings,
    ) {
        @Serializable
        data class SettingsGeneral(
            @SerialName("Top")
            val top: Int = 0,
            @SerialName("Left")
            val left: Int = 0,
            @SerialName("Width")
            val width: Int = 0,
            @SerialName("Height")
            val height: Int = 0,
            @SerialName("OpenSettingsAtStart")
            val openSettingsAtStart: Boolean = false,
            @SerialName("ShowName")
            val showName: Boolean,
            @SerialName("LoadPreview")
            val loadPreview: Boolean,
            @SerialName("NbDecks")
            val nbDecks: Int,
            @SerialName("ResyncOnPreset")
            val resyncOnPreset: Boolean,
            @SerialName("LogPerformanceHistory")
            val logPerformanceHistory: Boolean,
            @SerialName("AutoSave")
            val autoSave: Boolean = true,
            @SerialName("QueueTopMost")
            val queueTopMost: Boolean = true,
            @SerialName("QueueMagnet")
            val queueMagnet: Boolean = false,
//            @SerialName("SmoothChange")
//            val smoothChange: Boolean,
            @SerialName("RedBlue3D")
            val redBlue3D: Boolean,
            @SerialName("BpmModulateSpeed")
            val bpmModulateSpeed: Boolean,
            @SerialName("BpmModulateSpeedRef")
            val bpmModulateSpeedRef: Int,
            @SerialName("SpoutFirst")
            val spoutFirst: Boolean,
            @SerialName("AnimateZoom")
            val animateZoom: Boolean,
            @SerialName("PreviewZoomSize")
            val previewZoomSize: Float,
            @SerialName("DefaultAudioDevice")
            val defaultAudioDevice: String = "",
            @SerialName("MidiOutputDevice")
            val midiOutputDevice: String = "LoopBe Internal MIDI",
            @SerialName("HardCutThreshold")
            val hardCutThreshold: Int,
            @SerialName("OscInputEnable")
            val oscInputEnable: Boolean,
            @SerialName("OscOutputEnable")
            val oscOutputEnable: Boolean,
            @SerialName("OscPort")
            val oscPort: Int = 8000,
            @SerialName("OscOutputPort")
            val oscOutputPort: String,
            @SerialName("OscOutputIp")
            val oscOutputIp: String, // = "127.0.0.1",
            @SerialName("AutoChangeInstant")
            val autoChangeInstant: Boolean = false,
            @SerialName("BeatThreshold")
            val beatThreshold: Int = 20,
            @SerialName("ManualBPM")
            val manualBPM: Float = 120f,
            @SerialName("UseAbleton")
            val useAbleton: Boolean = false,
            @SerialName("AbletonMaster")
            val abletonMaster: Boolean = false,
            @SerialName("AutoChangeEnable")
            val autoChangeEnable: Boolean = false,
            @SerialName("ShuffleEnable")
            val shuffleEnable: Boolean = true,
            @SerialName("AlwaysOnTop")
            val alwaysOnTop: Boolean = false,
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

    @Serializable(with = LibraryClosedSectionsSerializer::class)
    data class LibraryClosedSections(
        val sections: List<LibraryClosedSection>
    ) {
        @Serializable
        data class LibraryClosedSection(
            val index: Int,
            @XmlSerialName("Value")
            val value: String
        )
    }

    @Serializable(with = QueueWindowsSerializer::class)
    data class QueueWindows(
        val queues: List<Queue>
    ) {
        @Serializable
        data class Queue(
            val index: Int,
            @SerialName("Name")
            val name: String,
            @SerialName("Top")
            val top: Double,
            @SerialName("Left")
            val left: Double,
            @SerialName("Width")
            val width: Double,
            @SerialName("Height")
            val height: Double,
            @SerialName("Type")
            val type: Int,
            @SerialName("Deck")
            val deck: Int? = null,
            @SerialName("Open")
            val open: Boolean,
            @SerialName("AlwaysOnTop")
            val alwaysOnTop: Boolean,
            @SerialName("Magnetic")
            val magnetic: Boolean,
            @SerialName("BeatOffset")
            val beatOffset: Int = 1,
            @SerialName("BeatMulti")
            val beatMulti: Int = 1,
            @SerialName("DefaultSpriteOverlay")
            val defaultSpriteOverlay: Int = -1,
            @SerialName("Active")
            val active: Boolean,
            @SerialName("MidiDevice")
            val midiDevice: String? = null,
            @XmlElement
            @SerialName("Presets")
            val presetsWrappers: Presets,
        ) {
            val presets get() = presetsWrappers.presets

            @Serializable
            data class Presets(
                @XmlElement
                @SerialName("Presets")
                val presets: List<Preset>,
            ) {
                @Serializable
                @XmlSerialName("Preset")
                data class Preset(
                    @SerialName("Name")
                    val name: String,
                    @SerialName("Id")
                    val id: Int,
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
                    @SerialName("MidiId")
                    val midiId: Int? = null,
                    @SerialName("MidiAction")
                    val midiAction: Int? = null,
                    @SerialName("MidiMode")
                    val midiMode: Int? = null,
                    @SerialName("MidiDeck")
                    val midiDeck: Int? = null,
                    @SerialName("MidiPresetType")
                    val midiPresetType: Int? = null,
                    @SerialName("MidiHotKey")
                    val midiHotKey: String? = null,
                    @SerialName("Comments")
                    val comments: String? = null,
                )
            }


        }
    }

    @Serializable(with = FavoriteListSerializer::class)
    data class FavoriteList(
        val favorites: List<Favorite>
    ) {
        @Serializable
        data class Favorite(
            val index: Int,
            val favorites: List<Preset> = emptyList()
        ) {
            @Serializable
            data class Preset(
                @SerialName("Name")
                val name: String
            )
        }
    }

    @Serializable
    data class HotKeyList(
        val unused: Int = 1
    )

    @Serializable
    data class CommentsList(
        val unused: Int = 1
    )

    @Serializable
    data class EffectsList(
        val effects: List<Effects>
    ) {
        @Serializable
        data class Effects(
            @SerialName("Effects")
            val effects: Int,
            @SerialName("Name")
            val name: String,
        )
    }

    @Serializable
    data class AnimSpeedList(
        val unused: Int = 1
    )
}
