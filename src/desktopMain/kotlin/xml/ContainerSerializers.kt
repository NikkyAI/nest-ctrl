package xml

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.serializer
import nestdrop.NestdropSettings

class LibraryClosedSectionsSerializer() : CommonContainerSerializer<NestdropSettings.LibraryClosedSections,NestdropSettings.LibraryClosedSections.LibraryClosedSection>() {
    override fun constructContainer(list: List<NestdropSettings.LibraryClosedSections.LibraryClosedSection>): NestdropSettings.LibraryClosedSections {
        return NestdropSettings.LibraryClosedSections(list)
    }
    override val elementSerializer = serializer<NestdropSettings.LibraryClosedSections.LibraryClosedSection>()
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("LibraryClosedSections") {
        element("sections", ListSerializer(elementSerializer).descriptor)
    }
    override val prefix: String = "Section_"
}

class QueueWindowsSerializer() : CommonContainerSerializer<NestdropSettings.QueueWindows, NestdropSettings.QueueWindows.Queue>() {
    override fun constructContainer(list: List<NestdropSettings.QueueWindows.Queue>): NestdropSettings.QueueWindows {
        return NestdropSettings.QueueWindows(list)
    }
    override val elementSerializer = serializer<NestdropSettings.QueueWindows.Queue>()
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("QueueWindows") {
        element("queues", ListSerializer(elementSerializer).descriptor)
    }
    override val prefix: String = "Queue"
}

class FavoriteListSerializer() : CommonContainerSerializer<NestdropSettings.FavoriteList, NestdropSettings.FavoriteList.Favorite>() {
    override fun constructContainer(list: List<NestdropSettings.FavoriteList.Favorite>): NestdropSettings.FavoriteList {
        return NestdropSettings.FavoriteList(list)
    }
    override val elementSerializer = serializer<NestdropSettings.FavoriteList.Favorite>()
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FavoriteList") {
        element("favorites", ListSerializer(elementSerializer).descriptor)
    }
    override val prefix: String = "Favorite"
}

///**
// * A common base class that contains the actual code needed to serialize/deserialize the container.
// */
//abstract class CommonContainerSerializerOld<CONTAINER, ELEMENT> : KSerializer<CONTAINER> {
//    /** We need to have the serializer for the elements */
//    abstract val elementSerializer: KSerializer<ELEMENT>// = serializer<NestdropSettings.QueueWindows.Queue>()
//
//    /** Autogenerated descriptors don't work correctly here. */
////    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("QueueWindows") {
////        element("queues", ListSerializer(elementSerializer).descriptor)
////    }
//
//    abstract fun constructContainer(list: List<ELEMENT>): CONTAINER
//    abstract val prefix: String
//
//    override fun deserialize(decoder: Decoder): CONTAINER {
//        // XmlInput is designed as an interface to test for to allow custom serializers
//        if (decoder is XML.XmlInput) { // We treat XML different, using a separate method for clarity
//            return deserializeDynamic(decoder, decoder.input)
//        } else { // Simple default decoder implementation that delegates parsing the data to the ListSerializer
//            val data = decoder.decodeStructure(descriptor) {
//                decodeSerializableElement(descriptor, 0, ListSerializer(elementSerializer))
//            } as List<ELEMENT>
//            return constructContainer(data)
//        }
//    }
//
//    /**
//     * This function is the meat to deserializing the container with dynamic tag names. Note that
//     * because we use xml there is no point in going through the (anonymous) list dance. Doing that
//     * would be an additional complication.
//     */
//
//    private fun <D> deserializeDynamic(
//        decoder: D,
//        reader: XmlReader
//    ): CONTAINER where D : Decoder, D : XML.XmlInput {
//        val xml =
//            decoder.delegateFormat() // This delegate format allows for reusing the settings from the outer format.
////    fun deserializeDynamic(decoder: Decoder, reader: XmlReader): NestdropSettings.QueueWindows {
////        val xml = delegateFormat(decoder) // get the format for deserializing
//
//        // We need the descriptor for the element. xmlDescriptor returns a rootDescriptor, so the actual descriptor is
//        // its (only) child.
//        val elementXmlDescriptor = xml.xmlDescriptor(elementSerializer).getElementDescriptor(0)
//
//        // A list to collect the data
//        val queueList = mutableListOf<ELEMENT>()
//
//        decoder.decodeStructure(descriptor) {
//            // Finding the children is actually not left to the serialization framework, but
//            // done by "hand"
//            while (reader.next() != EventType.END_ELEMENT) {
//                when (reader.eventType) {
//                    EventType.COMMENT,
//                    EventType.IGNORABLE_WHITESPACE -> {
//                        // Comments and whitespace are just ignored
//                    }
//
//                    EventType.ENTITY_REF,
//                    EventType.TEXT ->
//                        if (reader.text.isNotBlank()) {
//                            // Some parsers can return whitespace as text instead of ignorable whitespace
//
//                            // Use the handler from the configuration to throw the exception.
////                        xml.config.unknownChildHandler(reader, InputKind.Text, null, emptyList())
//
//                            @OptIn(ExperimentalXmlUtilApi::class)
//                            xml.config.policy.handleUnknownContentRecovering(
//                                reader,
//                                InputKind.Text,
//                                elementXmlDescriptor,
//                                null,
//                                emptyList()
//                            )
//                        }
//                    // It's best to still check the name before parsing
//                    EventType.START_ELEMENT -> {
//                        if (reader.namespaceURI.isEmpty() && reader.localName.startsWith(prefix)) {
//                            // When reading the child tag we use the DynamicTagReader to present normalized XML to the
//                            // deserializer for elements
//                            val filter = DynamicTagReader(reader, elementXmlDescriptor, prefix)
//
//                            // The test element can now be decoded as normal (with the filter applied)
//                            val queueElement = xml.decodeFromReader(elementSerializer, filter) as ELEMENT
//                            queueList.add(queueElement)
//                        } else { // handling unexpected tags
////                        xml.config.unknownChildHandler(reader, InputKind.Element, reader.name, listOf("Queue_??"))
//
//                            @OptIn(ExperimentalXmlUtilApi::class)
//                            xml.config.policy.handleUnknownContentRecovering(
//                                reader,
//                                InputKind.Element,
//                                elementXmlDescriptor,
//                                reader.name,
//                                (0 until elementXmlDescriptor.elementsCount).map {
//                                    val e = elementXmlDescriptor.getElementDescriptor(it)
//                                    PolyInfo(e.tagName, it, e)
//                                }
//                            )
//                        }
//                    }
//
//                    else -> { // other content that shouldn't happen
//                        throw XmlException("Unexpected tag content")
//                    }
//                }
//            }
//        }
//        return constructContainer(queueList)
//    }
//
////    override fun serialize(encoder: Encoder, value: NestdropSettings.QueueWindows) {
////        if (encoder is XML.XmlOutput) { // When we are using the xml format use the serializeDynamic method
////            return serializeDynamic(encoder, encoder.target, value.queues)
////        } else { // Otherwise just manually do the encoding that would have been generated
////            encoder.encodeStructure(descriptor) {
////                encodeSerializableElement(descriptor, 0, ListSerializer(elementSerializer), value.queues)
////            }
////        }
////    }
//
//    override fun serialize(encoder: Encoder, value: CONTAINER) {
//        TODO()
////        when (encoder) {
////            is XML.XmlOutput -> // When we are using the xml format use the serializeDynamic method
////                return serializeDynamic(encoder, encoder.target, value.queues)
////
////            else -> // Otherwise just manually do the encoding that would have been generated
////                encoder.encodeStructure(descriptor) {
////                    encodeSerializableElement(descriptor, 0, ListSerializer(elementSerializer), value.queues)
////                }
////        }
//    }
//
////    /**
////     * This function provides the actual dynamic serialization
////     */
////    private fun <E> serializeDynamic(
////        encoder: E,
////        target: XmlWriter,
////        data: List<NestdropSettings.QueueWindows.Queue>
////    ) where E : Encoder, E : XML.XmlOutput {
//////    fun serializeDynamic(encoder: Encoder, target: XmlWriter, data: List<NestdropSettings.QueueWindows.Queue>) {
////        val xml = encoder.delegateFormat() // This format keeps the settings from the outer serializer, this allows for
////        // serializing the children
//////        val xml = delegateFormat(encoder) // get the format for deserializing
////
////        // We need the descriptor for the element. xmlDescriptor returns a rootDescriptor, so the actual descriptor is
////        // its (only) child.
////        val elementXmlDescriptor = xml.xmlDescriptor(elementSerializer).getElementDescriptor(0)
////
////        encoder.encodeStructure(descriptor) { // create the structure (will write the tags of Container)
////            for (element in data) { // write each element
////                // We need a writer that does the renaming from the normal format to the dynamic format
////                // It is passed the string of the id to add.
////                val writer = DynamicTagWriter(target, elementXmlDescriptor, element.id.toString(), prefix)
////
////                // Normal delegate writing of the element
////                xml.encodeToWriter(writer, elementSerializer, element)
////            }
////        }
////    }
//}