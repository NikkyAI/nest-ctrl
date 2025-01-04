import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import nestdrop.PresetLocation
import tags.Tag
import tags.nestdropCategoryTagsSet
import ui.screens.imgSpritesMap
import ui.screens.presetsMap
import java.io.File
import kotlin.time.measureTimedValue

suspend fun scanMilkdrop() {

    val categoryTagsSet = mutableSetOf<Tag>()

    var id: Int = 0
    val milkPresets = presetsFolder.listFiles().orEmpty().filter { it.isDirectory }.flatMap { categoryFolder ->
        val categoryFiles = categoryFolder.listFiles().orEmpty().filter { it.isFile }.filter { it.extension == "milk" }
        val categoryPresets = categoryFiles.filterNotNull().map { file ->
            val name = file.nameWithoutExtension
            val path = file.toRelativeString(presetsFolder)
            val previewPath = file.resolveSibling(file.nameWithoutExtension + ".jpg").toRelativeString(presetsFolder)

            categoryTagsSet += Tag(categoryFolder.name, listOf("nestdrop"))
            PresetLocation.Milk(
                name = name,
                id = id++,
                path = path,
                previewPath = previewPath,
                category = categoryFolder.name,
            )
        }
        val subCategories = categoryFolder.listFiles().orEmpty().filter { it.isDirectory }

        val subCategoryEntries = subCategories.flatMapIndexed() { index, subCategoryFolder ->
            val subCategoryFiles = subCategoryFolder.listFiles().orEmpty().filter { it.isFile }.filter { it.extension == "milk" }
            if (subCategoryFiles.isNotEmpty()) {
                if (index == 0) {
                    if (categoryPresets.isNotEmpty()) {
                        id++
                    }
                } else {
                    id++
                }
            }
            subCategoryFiles.filterNotNull().map { file ->
                val name = file.nameWithoutExtension
                val path = file.toRelativeString(presetsFolder)
                val previewPath =
                    file.resolveSibling(file.nameWithoutExtension + ".jpg").toRelativeString(presetsFolder)

                categoryTagsSet += Tag(categoryFolder.name, listOf("nestdrop"))
                categoryTagsSet += Tag(subCategoryFolder.name, listOf("nestdrop", categoryFolder.name))

                PresetLocation.Milk(
                    name = name,
                    id = id++,
                    path = path,
                    previewPath = previewPath,
                    category = categoryFolder.name,
                    subCategory = subCategoryFolder.name,
                )
            }
        }

        categoryPresets + subCategoryEntries
    }.associateBy { it.name }


    val imgPresets = spritesFolder.listFiles().orEmpty().filter { it.isDirectory }.sortFileNames().flatMap { categoryFolder ->
        val categoryFiles = categoryFolder.listFiles().orEmpty().filter { it.isFile }.filter { it.extension == "png" || it.extension == "jpg" }.sortFileNames()
        val categoryPresets = categoryFiles.filterNotNull().map { file ->
            val name = file.name
            val path = file.toRelativeString(presetsFolder)

            PresetLocation.Img(
                name = name,
                id = id++,
                path = path,
                category = categoryFolder.name,
            )
        }
        val subCategories = categoryFolder.listFiles().orEmpty().filter { it.isDirectory }.sortFileNames()

        val subCategoryEntries = subCategories.flatMapIndexed() { index, subCategoryFolder ->
            val subCategoryFiles = subCategoryFolder.listFiles().orEmpty().filter { it.extension == "png" || it.extension == "jpg" }.sortFileNames()
            if (subCategoryFiles.isNotEmpty()) {
                if (index == 0) {
                    if (categoryPresets.isNotEmpty()) {
                        id++
                    }
                } else {
                    id++
                }
            }
            subCategoryFiles.filterNotNull().map { file ->
                val name = file.name
                val path = file.toRelativeString(presetsFolder)

                PresetLocation.Img(
                    name = name,
                    id = id++,
                    path = path,
                    category = categoryFolder.name,
                    subCategory = subCategoryFolder.name,
                )
            }
        }

        categoryPresets + subCategoryEntries
    }.associateBy { it.name }

    nestdropCategoryTagsSet.value = categoryTagsSet

    presetsMap.value = milkPresets
    imgSpritesMap.value = imgPresets

    flowScope.launch {
        measureTimedValue {
            imgPresets.values.map { sprite ->
                async (Dispatchers.IO) {
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
    file.nameWithoutExtension.lowercase().replace("_", "#")
}