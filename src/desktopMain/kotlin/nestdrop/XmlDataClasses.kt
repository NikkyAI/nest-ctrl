package nestdrop

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nestdropConfigFile
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlConfig.Companion.DEFAULT_UNKNOWN_CHILD_HANDLER
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import xml.Container
import xml.Element
import xml.FavoriteListSerializer
import xml.LibraryClosedSectionsSerializer
import xml.QueueWindowsSerializer

@Serializable
data class NestdropSettings(
    @XmlSerialName("MainWindow")
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
        override val elements: List<LibraryClosedSection>
    ) : Container<LibraryClosedSections.LibraryClosedSection> {
        @Serializable
        data class LibraryClosedSection(
            override val index: Int,
            @SerialName("Value")
            val value: String
        ) : Element

        val sections get() = elements
    }

    @Serializable(with = QueueWindowsSerializer::class)
    data class QueueWindows(
        override val elements: List<Queue>
    ) : Container<QueueWindows.Queue> {
        val queues get() = elements

        @Serializable
        data class Queue(
            override val index: Int,
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
            val typeIndex: Int,
            @SerialName("Deck")
            val deck: Int? = null,
            @SerialName("Open")
            val open: Boolean,
            @SerialName("AlwaysOnTop")
            val alwaysOnTop: Boolean,
            @SerialName("Magnetic")
            val magnetic: Boolean,
            @SerialName("BeatOffset")
            val beatOffset: Float = 1f,
            @SerialName("BeatMulti")
            val beatMulti: Float = 1f,
            @SerialName("DefaultSpriteOverlay")
            val defaultSpriteOverlay: Int = -1,
            @SerialName("Active")
            val active: Boolean,
            @SerialName("MidiDevice")
            val midiDevice: String? = null,
            @SerialName("IsFileExplorer")
            val isFileExplorer: Boolean = false,
            @SerialName("FileExplorerPath")
            val fileExplorerPath: String = "",
//            @XmlElement
//            @SerialName("Presets")
//            val presetsContainer: Presets,
            @XmlElement
            @SerialName("Presets")
            @XmlChildrenName("Presets")
            val presets: List<Preset> = emptyList(),
        ) : Element {
            fun type() = QueueType.entries[typeIndex]

            @Serializable
            @SerialName("Preset")
            data class Preset(
                @SerialName("Name")
                val name: String,
                @SerialName("Id")
                val id: Int? = null,
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
                val typeNumber: Int? = null,
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
                @SerialName("MidiPresetQueue")
                val midiPresetQueue: String? = null,
                @SerialName("MidiHotKey")
                val midiHotKey: String? = null,
                @SerialName("Comments")
                val comments: String? = null,
            ) {
                fun type() = typeNumber?.let {
                    PresetType.entries.firstOrNull() { it.type == typeNumber }
                        ?: error("unknown preset type $typeNumber")
                }
            }
        }
    }

    @Serializable(with = FavoriteListSerializer::class)
    data class FavoriteList(
        override val elements: List<Favorite>,
    ) : Container<FavoriteList.Favorite> {
        val favorites: List<Favorite> get() = elements

        @Serializable
        data class Favorite(
            override val index: Int,
            val favorites: List<Preset> = emptyList()
        ) : Element {
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

fun main(args: Array<String>) {
    val logger = KotlinLogging.logger {}

    val xml = XML {
        recommended {
            repairNamespaces = false
            unknownChildHandler = UnknownChildHandler { input, inputKind, descriptor, name, candidates ->
                logger.info { "unknown child handler" }
                logger.info { "inputKind: $inputKind" }
                logger.info { "descriptor: $descriptor" }
                logger.info { "name: $name" }
                logger.info { "candidates: $candidates" }

                DEFAULT_UNKNOWN_CHILD_HANDLER.handleUnknownChildRecovering(input, inputKind, descriptor, name, candidates)
            }
            this.verifyElementOrder = false
            this.isStrictAttributeNames = true
            this.isStrictOtherAttributes = false
            this.ignoreUnknownChildren()
            pedantic = false
            isStrictBoolean = false
            policy
//        autoPolymorphic = true
        }

        xmlDeclMode = XmlDeclMode.Charset
//    this.defaultToGenericParser = true
        xmlVersion = XmlVersion.XML10
//    xmlDeclMode = XmlDeclMode.Charset
//    this.repairNamespaces = true
//    this.autoPolymorphic = true
//    this.isInlineCollapsed
//    this.xmlDeclMode
    }

    XML.decodeFromString(
        NestdropSettings.serializer(),
        nestdropConfigFile.readText().also {
            logger.info { "parsing xml: $it" }
        }
//            .substringAfter(
//                """<?xml version="1.0" encoding="utf-8"?>"""
//            )
    )
}