package utils

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import nestdrop.NestdropSettings
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.InputKind
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlConfig.Companion.DEFAULT_UNKNOWN_CHILD_HANDLER
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor

private val logger = KotlinLogging.logger {}

val xml = XML(
//    serializersModule = SerializersModule {
//    polymorphic(NestdropSettings.MainWindow.OpenDeckSettings::class) {
////        defaultDeserializer {
////            NestdropSettings.MainWindow.DeckSettings.serializer()
////        }
//
////        subclass(NestdropSettings.MainWindow.DeckSettings::class)
//        subclass(NestdropSettings.MainWindow.DeckSettings1::class)
//        subclass(NestdropSettings.MainWindow.DeckSettings2::class)
//        subclass(NestdropSettings.MainWindow.DeckSettings3::class)
//        subclass(NestdropSettings.MainWindow.DeckSettings4::class)
//    }
//}
) {
    this.recommended {
        unknownChildHandler = UnknownChildHandler { input, inputKind, descriptor, name, candidates ->
            logger.info { "unknown child handler" }
            logger.info { "inputKind: $inputKind" }
            logger.info { "descriptor: $descriptor" }
            logger.info { "name: $name" }
            logger.info { "candidates: $candidates" }

            DEFAULT_UNKNOWN_CHILD_HANDLER.handleUnknownChildRecovering(input, inputKind, descriptor, name, candidates)
        }
        pedantic = false
//        autoPolymorphic = true
    }
    this.xmlVersion = XmlVersion.XML10
//    this.repairNamespaces = true
//    this.autoPolymorphic = true
//    this.isInlineCollapsed
//    this.xmlDeclMode
}