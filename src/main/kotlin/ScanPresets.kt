import nestdrop.PresetLocation
import tags.Tag
import tags.nestdropCategoryTagsSet
import ui.screens.presetsMap
import ui.screens.spritesMap

fun scanPresets() {
    val presetsFolder = nestdropFolder.resolve("Plugins").resolve("Milkdrop2").resolve("Presets")

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

    val spritesFolder = nestdropFolder.resolve("Plugins").resolve("Milkdrop2").resolve("Sprites")

    val imgPresets = spritesFolder.listFiles().orEmpty().filter { it.isDirectory }.flatMap { categoryFolder ->
        val categoryFiles = categoryFolder.listFiles().orEmpty().filter { it.isFile }.filter { it.extension == "png" || it.extension == "jpg" }
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
                val name = file.name
                val path = file.toRelativeString(presetsFolder)

                PresetLocation.Img(
                    name = name,
                    id = id++,
                    path = path,
                    category = categoryFolder.name,
                )
            }
        }

        categoryPresets + subCategoryEntries
    }.associateBy { it.name }

    nestdropCategoryTagsSet.value = categoryTagsSet

    presetsMap.value = milkPresets
    spritesMap.value = imgPresets
}