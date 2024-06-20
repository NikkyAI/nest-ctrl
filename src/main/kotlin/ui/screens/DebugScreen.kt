package ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import nestdrop.deck.Deck
import nestdropFolder
import osc.OSCMessage
import osc.nestdropPortSend
import ui.components.lazyList
import ui.components.verticalScroll
import java.io.File
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

@Composable
fun debugScreen(
    vararg decks: Deck
) {
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

        lazyList {
            val presetsFolder = nestdropFolder.resolve("Plugins").resolve("Milkdrop2").resolve("Presets")
            presetsMap.forEach { (name, presetEntry) ->
                item(key = name) {
                    val image = remember { imageFromFile(presetsFolder.resolve(presetEntry.previewPath)) }
                    Row {
                        Image(bitmap = image, contentDescription = presetEntry.previewPath)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            decks.forEach { deck ->
                                Button(
                                    onClick = {
                                        scope.launch {
                                            nestdropSetPreset(presetEntry.id, deck = deck.N)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = deck.dimmedColor
                                    )
                                ) {
                                    Text(presetEntry.id.toString())
                                }
                            }
                        }
//                        Text("${presetEntry.id}")
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(presetEntry.path)
                    }
                }
            }
        }
    }
}


fun imageFromFile(file: File): ImageBitmap {
    return org.jetbrains.skia.Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
}

val presetsMap = MutableStateFlow<Map<String, PresetLocation>>(emptyMap())

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

suspend fun nestdropSetPreset(id: Int, deck: Int, hardcut: Boolean = false) {
    nestdropPortSend(
        OSCMessage("/PresetID/$id/Deck$deck", if (hardcut) 0 else 1)
    )
}

data class PresetLocation(
    val name: String,
    val id: Int,
    val path: String,
    val previewPath: String,
)

fun scanPresets() {
    val presetsFolder = nestdropFolder.resolve("Plugins").resolve("Milkdrop2").resolve("Presets")

    var id: Int = 0
    val categories = presetsFolder.listFiles().filter { it.isDirectory }
    val map = categories.flatMap { folder ->
        val categoryFiles = folder.listFiles().filter { it.isFile }.filter { it.extension == "milk" }
        val categoryPresets = categoryFiles.filterNotNull().map { file ->
            val name = file.nameWithoutExtension
            val path = file.toRelativeString(presetsFolder)
            val previewPath = file.resolveSibling(file.nameWithoutExtension + ".jpg").toRelativeString(presetsFolder)

            PresetLocation(
                name = name,
                id = id++,
                path = path,
                previewPath = previewPath
            )
        }
        val subCategories = folder.listFiles().filter { it.isDirectory }

        val subCategoryEntries = subCategories.flatMapIndexed() { index, folder ->
            val subCategoryFiles = folder.listFiles().filter { it.isFile }.filter { it.extension == "milk" }
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

                PresetLocation(
                    name = name,
                    id = id++,
                    path = path,
                    previewPath = previewPath
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

    presetsMap.value = map
}