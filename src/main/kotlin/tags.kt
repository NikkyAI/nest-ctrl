import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import nestdrop.deck.PresetQueues
import ui.screens.presetsMap
import utils.KWatchEvent
import utils.asWatchChannel
import utils.runningHistoryNotNull
import java.io.File

val tagMap = MutableStateFlow<Map<String, Set<String>>>(
    emptyMap()
)


val tagsFolder = File("tags")

data class Tag(
    val name: String,
    val namespace: List<String> = emptyList()
) {
    val label = "${namespace.joinToString(":")} : $name"
    val file by lazy {
        namespace.fold(tagsFolder) { file, namespace ->
            file.resolve(namespace)
        }.resolve("$name.txt")
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

val customTags = MutableStateFlow<Map<Tag, Set<String>>>(emptyMap())


suspend fun startTagsFileWatcher(presetQueues: PresetQueues) {
    tagsFolder.mkdirs()

    combine(presetsMap, presetQueues, customTags) { presetsMap, queues, customTags ->
        tagMap.value = presetsMap.mapValues { (_, entry) ->
            val categoryTag = entry.category.let { "cat:$it" }
            val subCategoryTag = entry.subCategory?.let { "sub:$it" }
            val queueTags = queues.filter {
//                    System.err.println(it.presets)
                it.presets.any { it.name.substringBeforeLast(".milk") == entry.name }
            }.map { "q:${it.name}" }.toSet()

            val c = customTags.filterValues { tagEntries ->
                entry.name in tagEntries
            }.keys.map { (it.namespace + it.name).joinToString(":") }

            setOfNotNull(categoryTag, subCategoryTag) + queueTags + c
        }
    }.launchIn(flowScope)

    flowScope.launch(Dispatchers.IO) {

        customTags.value = tagsFolder
            .walkTopDown().filter {  file -> file.isFile && file.extension == "txt" }
            .associate { file ->
                val tag = Tag.fromFile(file)

                val entries = file.readLines().toSet()
                tag to entries
            }
            ?: emptyMap()



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
                            customTags.value += tag to entries
                        }
                    }

                    KWatchEvent.Kind.Deleted -> {
                        // delete key if it exists
                        if (event.file.isFile && event.file.extension == "txt") {
                            // add new key
                            val tag = Tag.fromFile(event.file)
                            customTags.value -= tag
                        }

                    }
                }
            }
    }

    customTags
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