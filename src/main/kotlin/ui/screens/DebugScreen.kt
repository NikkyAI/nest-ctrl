package ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import decks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import nestdrop.deck.Deck
import nestdropFolder
import presetTags
import presetsFolder
import ui.components.fontDseg14
import ui.components.lazyList
import java.io.File
import kotlin.time.Duration
import kotlin.time.measureTime

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun debugScreen() {
    var scanRunning by remember {
        mutableStateOf(false)
    }
    var scanDuration by remember {
        mutableStateOf(Duration.ZERO)
    }
    val scope = rememberCoroutineScope()
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NEST\nCTRL",
                fontFamily = fontDseg14,
                fontSize = 30.sp,
                lineHeight = 45.sp,
                modifier = Modifier.padding(32.dp)
            )
            Spacer(modifier = Modifier.width(30.dp))
            Button(
                {
                    scope.launch {
                        scanRunning = true
                        scope.launch(Dispatchers.IO) {
                            scanDuration = measureTime {
                                scanPresets()
                            }
                            scanRunning = false
                        }
                    }
                }, enabled = !scanRunning
            ) {
                Text("scan presets")
            }

            if (scanDuration > Duration.ZERO) {
                Text("Scan took $scanDuration")
            }


        }

        val presetsMap by presetsMap.collectAsState()
        val tagMap by presetTags.collectAsState()

        lazyList {
            var lastCategory: Pair<String, String?>? = null
            presetsMap.forEach { (name, presetEntry) ->
                val currentCategory = presetEntry.category to presetEntry.subCategory
                if(currentCategory != lastCategory) {
                    stickyHeader(currentCategory) {
                        Row(
                            modifier = Modifier
                                .background(Brush.verticalGradient(
                                    listOf(
                                        Color.Black,
                                        MaterialTheme.colors.background,
                                    )
                                ))
//                                .background(MaterialTheme.colors.background)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
//                            Spacer(modifier = Modifier.width(30.dp))
                            Text(currentCategory.first, modifier = Modifier.padding(16.dp))
                            val subCategory = currentCategory.second
                            if(subCategory != null) {
                                Text(" > ", modifier = Modifier.padding(16.dp))
                                Text(subCategory, modifier = Modifier.padding(16.dp))
                            }
                        }
                    }

                    lastCategory = currentCategory
                }

                item(key = name) {
                    val image = remember { imageFromFile(presetsFolder.resolve(presetEntry.previewPath)) }
                    Row {
                        Column {
                            Image(bitmap = image, contentDescription = presetEntry.previewPath)
                            Text(presetEntry.id.toString())
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            decks.forEach { deck ->
                                val enabled by deck.enabled.collectAsState()

                                Button(
                                    onClick = {
                                        scope.launch {
                                            nestdropSetPreset(presetEntry.id, deck = deck.N)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = deck.dimmedColor
                                    ),
                                    enabled = enabled
                                ) {
                                    Text("deck: ${deck.N}")
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.width(300.dp)
                        ) {
                            val tags = tagMap[name] ?: emptySet()
                            tags.forEach {
                                Text(it.label)
                            }
                        }
//                        Text("${presetEntry.id}")
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(presetEntry.name)
                    }
                }
            }
        }
    }
}


fun imageFromFile(file: File): ImageBitmap {
    return org.jetbrains.skia.Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
}

val presetsMap = MutableStateFlow<Map<String, PresetLocation.Milk>>(emptyMap())
val spritesMap = MutableStateFlow<Map<String, PresetLocation.Img>>(emptyMap())

data class AutoplayState(
    val presetQueue: Boolean,
    val preset: Boolean,
    val imgSprite: Boolean,
    val imgSpriteFx: Boolean,
) {
    suspend fun apply(deck: Deck) {
        deck.presetQueue.autoChange.value = presetQueue
        deck.preset.autoChange.value = preset
        deck.imgSprite.autoChange.value = imgSprite
        deck.imgSpriteFx.autoChange.value = imgSpriteFx
    }
}

enum class PresetType {
    MILK,
    IMG,
}

sealed class PresetLocation {
    abstract val id: Int
    data class Milk(
        val name: String,
        override val id: Int,
        val path: String,
        val previewPath: String,
        val category: String,
        val subCategory: String? = null,
    ): PresetLocation()

    data class Img(
        val name: String,
        override val id: Int,
        val path: String,
        val category: String,
        val subCategory: String? = null,
    ): PresetLocation()
}

fun scanPresets() {
    val presetsFolder = nestdropFolder.resolve("Plugins").resolve("Milkdrop2").resolve("Presets")

    var id: Int = 0
    val milkPresets = presetsFolder.listFiles().orEmpty().filter { it.isDirectory }.flatMap { categoryFolder ->
        val categoryFiles = categoryFolder.listFiles().orEmpty().filter { it.isFile }.filter { it.extension == "milk" }
        val categoryPresets = categoryFiles.filterNotNull().map { file ->
            val name = file.nameWithoutExtension
            val path = file.toRelativeString(presetsFolder)
            val previewPath = file.resolveSibling(file.nameWithoutExtension + ".jpg").toRelativeString(presetsFolder)

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


//    val map = presetsFolder.walkTopDown().filter { it.extension == "milk" }
//
//
//        .mapIndexed { index, file ->
//        val previewFile = file.resolveSibling(file.nameWithoutExtension + ".jpg")
//        val previewPath = if(previewFile.exists()) {
//            previewFile.toRelativeString(presetsFolder)
//        } else {
//            ""
//        }
//
//        val name = file.nameWithoutExtension
//        val path = file.toRelativeString(presetsFolder)
//
//        name to PresetLocation(
//            name = name,
//            id = index,
//            path = path,
//            previewPath = previewPath
//        )
//    }.toMap()

    presetsMap.value = milkPresets
    spritesMap.value = imgPresets
}