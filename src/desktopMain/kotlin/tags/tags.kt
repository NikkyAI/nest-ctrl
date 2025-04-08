package tags

import androidx.compose.foundation.layout.height
import androidx.compose.material.Chip
import androidx.compose.material.ChipColors
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import flowScope
import io.github.oshai.kotlinlogging.KotlinLogging
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
import nestdrop.QueueType
import nestdrop.deck.Queues
import presetsMap
import tagsFolder
import utils.KWatchEvent
import utils.asWatchChannel
import utils.runningHistoryNotNull
import java.io.File
import kotlin.time.measureTimedValue

val presetTagsMapping = MutableStateFlow<Map<String, Set<Tag>>>(
    emptyMap()
)

val customTagsMapping = MutableStateFlow<Map<Tag, Set<String>>>(emptyMap())

//val nestdropCategoryTagsSet = MutableStateFlow<Set<Tag>>(emptySet())
val customTagsSet = MutableStateFlow<Set<Tag>>(emptySet())
val queueTagsSet = MutableStateFlow<Set<Tag>>(emptySet())
val nestdropQueueSearches = MutableStateFlow<List<PresetPlaylist>>(emptyList())
val queueTagsInitialized = MutableStateFlow(false)

private val logger = KotlinLogging.logger {}

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

@Immutable
@Serializable(with = TagSerializer::class)
data class Tag(
    val name: String,
    val namespace: List<String> = emptyList()
) { //}: Comparable<Tag> {
    val namespaceLabel by lazy {
        namespace.joinToString(":")
    }
    val label by lazy { "${namespace.joinToString(":")} : $name" }
    val file by lazy {
        namespace.fold(tagsFolder) { file, namespace ->
            file.resolve(namespace)
        }.resolve("$name.txt")
    }

    @Composable
    fun Chip(
        onClick: () -> Unit = {},
        enabled: Boolean = false,
        @OptIn(ExperimentalMaterialApi::class)
        colors: ChipColors = ChipDefaults.chipColors()
    ) {
        @OptIn(ExperimentalMaterialApi::class)
        Chip(
            onClick = onClick,
            enabled = enabled,
            colors = colors,
            modifier = Modifier
                .height(24.dp)
        ) {
            Text("$namespaceLabel:", color = Color.LightGray, softWrap = false)
            Text(
                name,
                color = Color.White,
                softWrap = false,
                fontWeight = FontWeight.Bold
            )
        }
    }
//    override fun compareTo(other: Tag): Int {
//        val ordering = (namespace + name).zip(other.namespace + other.name) { first, second ->
//            first.compareTo(second, ignoreCase = true)
//        }
//
//    }

    override fun toString(): String {
        return label
    }

    fun sortableString(): String = "${namespace.joinToString(":")}:$name"

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


suspend fun startTagsFileWatcher(queues: Queues) {
    tagsFolder.mkdirs()

    combine(presetsMap, queues.allQueues, customTagsMapping) { presetsMap, queues, customTagsMapping ->
        presetTagsMapping.value = presetsMap.mapValues { (_, entry) ->
//            val categoryTag = entry.categoryPath.let { Tag(it, listOf("nestdrop")) }

//            val categoryTags = entry.categoryPath.mapIndexed { i, category ->
//                Tag(category, listOf("nestdrop") + entry.categoryPath.subList(0, i))
//            }
            val categoryTag = Tag(entry.categoryPath.first(), listOf("nestdrop") + entry.categoryPath.dropLast(1))
            val categoryTags = setOf(categoryTag)

            val queueTags = queues.values
                .filter { it.type == QueueType.PRESET }
//                .map { it as Queue<Preset.Milkdrop> }
                .filter {
//                    System.err.println(it.presets)
                it.presets.any { it.name.endsWith(".milk") && it.name == entry.nameWithExtension }
            }.map { Tag(it.name, listOf("queue")) }.toSet()

            val customTags = customTagsMapping.filterValues { tagEntries ->
                entry.name in tagEntries
            }.keys // .map { (it.namespace + it.name).joinToString(":") }
//            val customCategories = customTagsKeys.filter { it.namespace.size > 1 }
//                .map {
//                    val name = it.namespace.last()
//                    val namespace = it.namespace.dropLast(1)
//                    Tag(name = name, namespace = namespace)
//                }

            categoryTags.toSet() + queueTags + customTags // + customCategories
        }
    }.launchIn(flowScope)

    customTagsMapping
        .onEach {
            customTagsSet.value = it.keys
        }
        .launchIn(flowScope)

    queues.allQueues
        .onEach { queues ->
            val queueTagsAndSearches = measureTimedValue {
                queues
                    .values
                    .filter { it.type == QueueType.PRESET && !it.isFileExplorer }
                    .associate { queue ->

                        val tag = Tag(queue.name, listOf("queue"))
                        val search = PresetPlaylist(
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
                        tag to search
                    }
            }.apply {
                logger.info { "processed tags on queues in $duration" }
            }.value
            queueTagsSet.value = queueTagsAndSearches.keys
            nestdropQueueSearches.value = queueTagsAndSearches.values.toList()
            queueTagsInitialized.value = true
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