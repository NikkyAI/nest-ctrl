package tags

import flowScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nestdrop.deck.PresetQueues
import tagsFolder
import ui.screens.presetsMap
import utils.KWatchEvent
import utils.asWatchChannel
import utils.runningHistoryNotNull
import java.io.File

val presetTagsMapping = MutableStateFlow<Map<String, Set<Tag>>>(
    emptyMap()
)

val customTagsMapping = MutableStateFlow<Map<Tag, Set<String>>>(emptyMap())

val nestdropCategoryTagsSet = MutableStateFlow<Set<Tag>>(emptySet())
val customTagsSet = MutableStateFlow<Set<Tag>>(emptySet())
val queueTagsSet = MutableStateFlow<Set<Tag>>(emptySet())
val nestdropQueueSearches = MutableStateFlow<List<TagScoreEval>>(emptyList())

//@Serializer(forClass = tags.Tag::class)
object TagSerializer : KSerializer<Tag> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("tags.Tag", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Tag {
        return Tag.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Tag) {
        encoder.encodeString(value.encode())
    }
}

@Serializable(with = TagSerializer::class)
data class Tag(
    val name: String,
    val namespace: List<String> = emptyList()
) {
    val label by lazy { "${namespace.joinToString(":")} : $name" }
    val file by lazy {
        namespace.fold(tagsFolder) { file, namespace ->
            file.resolve(namespace)
        }.resolve("$name.txt")
    }

    override fun toString(): String {
        return label
    }

    fun encode(): String {
        return if (namespace.isEmpty()) {
            name
        } else {
            namespace.joinToString(":") + ":" + name
        }
    }

    companion object {
        fun parse(text: String): Tag {
            val name = text.substringAfterLast(":")
            val namespaceString = text.substringBeforeLast(":$name", "")
            val namespace = namespaceString.split(':').filter { it.isNotEmpty() }

            return Tag(
                name = name,
                namespace = namespace,
            )
        }

        fun fromFile(file: File): Tag {
            val tagName = file.nameWithoutExtension
            val namespace = file.toRelativeString(tagsFolder)
                .replace('\\', '/')
                .substringBeforeLast('/')
                .split('/')

            return Tag(tagName, namespace)
        }
    }
}


suspend fun startTagsFileWatcher(presetQueues: PresetQueues) {
    tagsFolder.mkdirs()

    combine(presetsMap, presetQueues.queues, customTagsMapping) { presetsMap, queues, customTags ->
        presetTagsMapping.value = presetsMap.mapValues { (_, entry) ->
            val categoryTag = entry.category.let { Tag(it, listOf("nestdrop")) }
            val subCategoryTag = entry.subCategory?.let { Tag(it, listOf("nestdrop", entry.category)) }
            val queueTags = queues.filter {
//                    System.err.println(it.presets)
                it.presets.any { it.name.substringBeforeLast(".milk") == entry.name }
            }.map { Tag(it.name, listOf("queue")) }.toSet()

            val c = customTags.filterValues { tagEntries ->
                entry.name in tagEntries
            }.keys // .map { (it.namespace + it.name).joinToString(":") }

            setOfNotNull(categoryTag, subCategoryTag) + queueTags + c
        }
    }.launchIn(flowScope)

    customTagsMapping
        .onEach {
            customTagsSet.value = it.keys
        }
        .launchIn(flowScope)

    presetQueues.queues
        .onEach {
            val queueTags = it.map { Tag(it.name, listOf("queue")) }.toSet()
            queueTagsSet.value = queueTags
        }
        .launchIn(flowScope)

//    presetTagsMapping
//        .onEach { tagsMapping ->
//            val allTags = tagsMapping.values.flatten().toSet().sortedBy { it.encode() }.toSet()
//
//            val queueTags = allTags.filter { it.namespace[0] == "queue" }.toSet()
//            val nestdropCategoryTags = allTags.filter { it.namespace[0] == "nestdrop" }.toSet()
////            val customTags = allTags - (queueTags + nestdropCategoryTags)
////            queueTagsSet.value = queueTags
//            nestdropCategoryTagsSet.value = nestdropCategoryTags
////            customTagsSet.value = customTags
//        }
//        .launchIn(flowScope)

    queueTagsSet
        .onEach { queueTags ->
            nestdropQueueSearches.value = queueTags.map { tag ->
                TagScoreEval(
                    label = "Queue ${tag.name}",
                    terms = listOf(
                        Term(
                            boost = 10.0,
                            matcher = TagMatcher(
                                include = setOf(tag)
                            )
                        )
                    )
                )
            }
        }
        .launchIn(flowScope)

    flowScope.launch(Dispatchers.IO) {

        customTagsMapping.value = tagsFolder
            .walkTopDown().filter { file -> file.isFile && file.extension == "txt" }
            .associate { file ->
                val tag = Tag.fromFile(file)

                val entries = file.readLines().toSet()
                tag to entries
            }

        tagsFolder
            .asWatchChannel()
            .consumeEach { event ->
                when (event.kind) {
                    KWatchEvent.Kind.Initialized -> {}
                    KWatchEvent.Kind.Created, KWatchEvent.Kind.Modified -> {
                        if (event.file.isFile && event.file.extension == "txt") {
                            // add new key
                            val tag = Tag.fromFile(event.file)
                            val entries = event.file.readLines().toSet()
                            customTagsMapping.value += tag to entries
                        }
                    }

                    KWatchEvent.Kind.Deleted -> {
                        // delete key if it exists
                        if (event.file.isFile && event.file.extension == "txt") {
                            // add new key
                            val tag = Tag.fromFile(event.file)
                            customTagsMapping.value -= tag
                        }
                    }
                }
            }
    }

    customTagsMapping
        .runningHistoryNotNull()
        .onEach { (current, old) ->
            val newKeys = (current.keys - old.keys)
            val deletedKeys = (old.keys - current.keys)
            val differentKeys = (current.keys + old.keys - newKeys - deletedKeys).filter { key ->
                current[key] != old[key]
            }

            newKeys.forEach { tag ->
                val entries = current[tag] ?: return@forEach
                tag.file.parentFile.mkdirs()
                tag.file.writeText(
                    entries.joinToString("\n")
                )
            }
            differentKeys.forEach { tag ->
                val entries = current[tag] ?: return@forEach
                tag.file.parentFile.mkdirs()
                tag.file.writeText(
                    entries.joinToString("\n")
                )
            }
            deletedKeys.forEach { tag ->
                tag.file.delete()
            }
        }
        .launchIn(flowScope)

}