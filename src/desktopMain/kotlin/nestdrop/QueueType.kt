package nestdrop

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = QueueTypeSerializer::class)
enum class QueueType {
    Undefined,
    Preset,
    Sprite,
    Text,
    MIDI,
    SettingsPresets,
    NestdropSettings,
}

@Serializer(forClass = QueueType::class)
class QueueTypeSerializer: KSerializer<QueueType> {
    override fun deserialize(decoder: Decoder): QueueType {
        return QueueType.entries[decoder.decodeInt()]
    }

    override fun serialize(encoder: Encoder, value: QueueType) {
        encoder.encodeInt(value.ordinal)
    }
}