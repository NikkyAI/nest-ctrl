import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import nestdrop.Preset
import nestdrop.PresetLocation
import java.io.File
import kotlin.io.path.name
import kotlin.time.measureTimedValue

val presetsMap = MutableStateFlow<Map<String, PresetLocation.Milk>>(emptyMap())
val imgSpritesMap = MutableStateFlow<Map<String, PresetLocation.Img>>(emptyMap())

suspend fun scanMilkdrop() {
    var id: Int = 0

    val milkPresets = run {
        fun milkLocation(
            file: File,
            id: Int,
        ): PresetLocation.Milk {
            val relative = file.relativeToOrSelf(presetsFolder)
            val path = relative.path
            val previewPath = relative.resolveSibling(relative.nameWithoutExtension + ".jpg").path

//            categoryTagsSet += Tag(categoryFolder.name, listOf("nestdrop"))
            return PresetLocation.Milk(
                name = relative.nameWithoutExtension,
                nameWithExtension = relative.name,
                id = id,
                path = path,
                previewPath = previewPath,
                categoryPath = relative.parentFile?.toPath()?.map { it.name }.orEmpty(),
            )
        }

        val rootPresets =
            presetsFolder.listFiles().orEmpty().filter { it.isFile && it.extension == "milk" }
                .sortedBy { it.name.lowercase() }.map { file ->
                milkLocation(
                    file = file,
                    id = id++
                )
            }
        val categoryPresets = presetsFolder.listFiles().orEmpty().filter { it.isDirectory }.flatMap { categoryFolder ->
            val files = categoryFolder.walkTopDown().filter { it.isFile && it.extension == "milk" }.map {
                it.relativeToOrSelf(presetsFolder)
            }
            val sorted = files.sortedBy { it.path.lowercase() }
            var previousParent: String? = null
            sorted.map {
                val parent = it.parent
                if (previousParent != null && previousParent != parent) {
                    id++
                }
                previousParent = parent
                milkLocation(
                    file = it,
                    id = id++
                )
            }
        }
        rootPresets + categoryPresets
    }.associateBy { it.nameWithExtension }

    val imgPresets = run {
        fun imgLocation(
            file: File,
            id: Int,
        ): PresetLocation.Img {
            val relative = file.relativeToOrSelf(spritesFolder)

//            categoryTagsSet += Tag(categoryFolder.name, listOf("nestdrop"))
            return PresetLocation.Img(
                name = relative.nameWithoutExtension,
                nameWithExtension = relative.name,
                id = id,
                path = relative.path,
                categoryPath = relative.parentFile?.toPath()?.map { it.name }.orEmpty(),
            )
        }

        val rootPresets =
            spritesFolder.listFiles().orEmpty().sortedBy { it.name.lowercase() }.orEmpty().filter {
                it.isFile
                        && it.extension == "png" || it.extension == "jpg"
            }.map { file ->
                imgLocation(
                    file = file,
                    id = id++
                )
            }
        val categoryPresets = spritesFolder.listFiles().orEmpty().filter { it.isDirectory }.flatMap { categoryFolder ->
            val files = categoryFolder.walkTopDown().filter {
                it.isFile
                        && it.extension == "png" || it.extension == "jpg"
            }.map {
                it.relativeToOrSelf(spritesFolder)
            }
            val sorted = files.sortedBy { it.path.lowercase() }
            var previousParent: String? = null
            sorted.map {
                val parent = it.parent
                if (previousParent != null && previousParent != parent) {
                    id++
                }
                previousParent = parent
                imgLocation(
                    file = it,
                    id = id++
                )
            }
        }
        rootPresets + categoryPresets
    }.associateBy { it.nameWithExtension }
//    nestdropCategoryTagsSet.value = categoryTagsSet

    presetsMap.value = milkPresets
    imgSpritesMap.value = imgPresets

    flowScope.launch {
        measureTimedValue {
            imgPresets.values.map { sprite ->
                async(Dispatchers.IO) {
                    sprite.image
//                        .also { img ->
//                    logger.info { "loaded ${sprite.name} ${img.width}x${img.height}" }
//                    }
                }
            }.awaitAll()
        }.run {
            logger.info { "IMG sprites preloaded (${value.size}) in $duration" }
        }
    }
}

private val logger = KotlinLogging.logger {}

private fun List<File>.sortFileNames() = sortedBy { file ->
    file.nameWithoutExtension.lowercase().replace("_", "|") + "_"
}

fun scanFileSystemQueueForMilk(path: String): List<Preset.Milkdrop> {
    val presetsFolder = File(path)
    // id is sequential, starting at 1
    var id = 1

    logger.debug { "scanning $presetsFolder" }

    fun milkLocation(
        file: File,
        id: Int,
    ): PresetLocation.Milk {
        val relative = file.relativeToOrSelf(presetsFolder)
        val path = relative.path
        val previewPath = relative.resolveSibling(relative.nameWithoutExtension + ".jpg").path

//            categoryTagsSet += Tag(categoryFolder.name, listOf("nestdrop"))
        return PresetLocation.Milk(
            name = relative.nameWithoutExtension,
            nameWithExtension = relative.name,
            id = id,
            path = path,
            previewPath = previewPath,
            categoryPath = relative.parentFile?.toPath()?.map { it.name }.orEmpty(),
        )
    }

    val rootPresets =
        presetsFolder.listFiles().orEmpty().filter { it.isFile && it.extension == "milk" }
            .sortedBy { it.name.lowercase() }.map { file ->
            milkLocation(
                file = file,
                id = id++
            )
        }

    val categoryPresets = presetsFolder.listFiles().orEmpty().filter { it.isDirectory }.flatMap { categoryFolder ->
        val files = categoryFolder.walkTopDown().filter { it.isFile && it.extension == "milk" }.map {
            it.relativeToOrSelf(presetsFolder)
        }
        val sorted = files.sortedBy { it.path.lowercase() }
        sorted.map {
            milkLocation(
                file = it,
                id = id++
            )
        }
    }

    return (rootPresets + categoryPresets).mapIndexed() { index, location ->
        Preset.Milkdrop(
            name = location.name,
            id = location.id,
            effects = null,
            overlay = null,
            comments = null,
            location = location
        )
    }
//        .also {
//            it.forEach {
//                logger.info { it }
//            }
//        }
}

fun scanFileSystemQueueForImgSprites(path: String): List<Preset.ImageSprite> {
    val spritesFolder = File(path)
    // id is sequential, starting at 1
    var id = 1
    logger.info { "scanning $spritesFolder" }

    fun imgLocation(
        file: File,
        id: Int,
    ): PresetLocation.Img {
        val relative = file.relativeToOrSelf(spritesFolder)
        val path = relative.path

        return PresetLocation.Img(
            name = relative.nameWithoutExtension,
            nameWithExtension = relative.name,
            id = id,
            path = path,
            categoryPath = relative.parentFile?.toPath()?.map { it.name }.orEmpty(),
        )
    }

    val rootPresets =
        spritesFolder.listFiles().orEmpty().filter { it.isFile && it.extension == "png" || it.extension == "jpg" }
            .sortedBy { it.name.lowercase() }.map { file ->
            imgLocation(
                file = file,
                id = id++
            )
        }

    val categoryPresets = spritesFolder.listFiles().orEmpty().filter { it.isDirectory }.flatMap { categoryFolder ->
        val files =
            categoryFolder.walkTopDown().filter { it.isFile && it.extension == "png" || it.extension == "jpg" }.map {
                it.relativeToOrSelf(spritesFolder)
            }
        val sorted = files.sortedBy { it.path.lowercase() }
        sorted.map {
            imgLocation(
                file = it,
                id = id++
            )
        }
    }

    return (rootPresets + categoryPresets).mapIndexed() { index, location ->
//        logger.info { location.path }
        Preset.ImageSprite(
            name = location.name,
            id = location.id,
            effects = null,
            overlay = null,
            comments = null,
            location = location,
        )
    }
}