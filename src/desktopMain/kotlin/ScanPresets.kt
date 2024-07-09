import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import nestdrop.PresetLocation
import tags.Tag
import tags.nestdropCategoryTagsSet
import ui.screens.presetsMap
import ui.screens.imgSpritesMap
import java.io.File

suspend fun scanPresets() {

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

    coroutineScope {
        imgPresets.values.forEach { sprite ->
            launch(Dispatchers.IO) {
                sprite.image.let { img ->
                    logger.info { "loaded ${sprite.name} ${img.width}x${img.height}" }
                }
            }
        }
    }
}

private val logger = KotlinLogging.logger {}

private fun List<File>.sortFileNames() = sortedBy { file ->
    file.nameWithoutExtension.lowercase().replace("_", "#")
}