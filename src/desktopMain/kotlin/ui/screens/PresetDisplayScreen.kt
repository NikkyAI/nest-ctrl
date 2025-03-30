package ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import decks
import nestdrop.PresetLocation
import nestdrop.deck.Deck
import nestdropFolder
import presetsMap
import tags.Tag
import tags.presetTagsMapping
import ui.components.verticalScroll
import ui.components.verticalScrollStart
import java.io.File


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun presetDisplayScreen() {
    val decksEnabled by Deck.enabled.collectAsState()
    Row(
        modifier = Modifier
            .height(300.dp)
            .padding(8.dp),
//        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        /*
        Column(
            modifier = Modifier
//                .weight(0.1f)
                .fillMaxHeight(0.9f)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(Color.DarkGray),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.End,
            ) {

                Box(
                    modifier = Modifier
                        .padding(4.dp)
//                        .background(deck.disabledColor)
                ) {
                    Text(
                        text = "Preset\nPreview\n&\nTags",
                        color = Color.LightGray,
                        textAlign = TextAlign.Right,
                        modifier = Modifier
                            .height(135.dp)
                            .padding(vertical = 4.dp),
                    )
                }
                Text("Preset ID", color = Color.LightGray, textAlign = TextAlign.Right)
                Text("IMG", color = Color.LightGray, textAlign = TextAlign.Right)
                Text("IMG FX", color = Color.LightGray, textAlign = TextAlign.Right)
                Text("Spout", color = Color.LightGray, textAlign = TextAlign.Right)
                Text("Playlist", color = Color.LightGray, textAlign = TextAlign.Right)
            }
        }
        */
        decks.forEach { deck ->
            if (deck.id > decksEnabled) return@forEach
            PresetDisplay(deck)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun RowScope.PresetDisplay(
    deck: Deck,
) {
    val presetsFolder = nestdropFolder.resolve("Plugins").resolve("Milkdrop2").resolve("Presets")
    val presetsMap by presetsMap.collectAsState()
    val tagMap by presetTagsMapping.collectAsState()

//                Column {
//                    Row {
//                        Text("ID: ", color = Color.LightGray)
//                        Text(currentPreset.presetId.toString())
//                    }
//
//                    val imgStates by deck.spriteState.imgStates.collectAsState()
//                    val imgLabel = imgStates.values.firstOrNull()?.label
//
//                    Text(imgLabel ?: "-")
//
//                    val imgspriteFx by deck.imgSpriteFx.rawFx.collectAsState(0)
//
//                    Text("FX: $imgspriteFx")
//
//                    val spoutStates by deck.spriteState.spoutStates.collectAsState()
//                    val spoutLabel = spoutStates.values.firstOrNull()?.label
//
//                    Text(spoutLabel ?: "-")
//
//                    val currentPlaylist by deck.search.collectAsState()
//                    Text(currentPlaylist?.label ?: "-")
//                }

    Column(
        modifier = Modifier
            .weight(0.25f)
            .fillMaxSize()
//            .padding(horizontal = 8.dp, vertical = 0.dp)
            .background(deck.dimmedColor),
    ) {
        val currentPreset by deck.preset.currentPreset.collectAsState()
        val presetName = currentPreset.name
        val presetLocation = presetsMap[presetName + ".milk"]

//        Box(
//            modifier = Modifier
//                .height(150.dp)
//                .padding(horizontal = 8.dp, vertical = 4.dp)
//
//        ) {
        Row(
            modifier = Modifier
                .background(deck.disabledColor)
                .fillMaxWidth()
                .height(150.dp)
//                    .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (presetLocation != null) {
                val image =
                    remember(presetLocation) { imageFromFile(presetsFolder.resolve(presetLocation.previewPath)) }
                Box(
                    modifier = Modifier
//                                .padding(4.dp)
//                        .width(145.dp)
                        .background(deck.disabledColor)
                ) {
                    Image(
                        bitmap = image,
                        contentDescription = presetLocation.previewPath,
                        modifier = Modifier
//                                    .size(135.dp)
                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
            }

            Box(
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {

                Text(
                    text = presetName,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                )
            }
        }
//        }

//        Column {


        verticalScroll {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                itemVerticalAlignment = Alignment.Top,
            ) {
                val tags = tagMap[presetName + ".milk"] ?: emptySet()
                tags
                    .sortedWith(
                        compareBy<Tag> {
                            it.namespace.first() == "nestdrop"
                        }.thenBy {
                            it.namespace.first() == "queue"
                        }.thenBy {
                            it.sortableString()
                        }
                    )
                    .forEach {
                        it.Chip()
                    }
            }
        }
    }
}

//    Column(
//        horizontalAlignment = Alignment.Start,
//        modifier = Modifier
//            .padding(4.dp)
//            .weight(0.3f)
//    ) {
//        verticalScroll {
//
//            Column(
//                horizontalAlignment = Alignment.Start,
//                verticalArrangement = Arrangement.SpaceBetween,
//                modifier = Modifier
////                                .padding(4.dp)
//            ) {
//                Text(
//                    text = presetName,
//                    textAlign = TextAlign.Start,
//                    modifier = Modifier
//                )
//            }
//        }
//    }

//@Composable
//fun presetScreenSingle(deck: Deck) {
//    val presetsFolder = nestdropFolder.resolve("Plugins").resolve("Milkdrop2").resolve("Presets")
//    val presetsMap by presetsMap.collectAsState()
//    val tagMap by presetTagsMapping.collectAsState()
//
//    val currentPreset by deck.preset.currentPreset.collectAsState()
//    val presetName = currentPreset.name
//    val presetEntry = presetsMap[presetName]
//
//    Row(
//        modifier = Modifier
//            .width(400.dp)
//            .aspectRatio(4f / 3f)
//            .background(deck.dimmedColor)
//            .padding(4.dp),
////        verticalAlignment = Alignment.CenterVertically,
////        horizontalArrangement = Arrangement.SpaceBetween,
//    ) {
//        Column(
//            modifier = Modifier.weight(0.5f)
//        ) {
//            verticalScroll {
//                Column(
//                    horizontalAlignment = Alignment.Start,
//                    modifier = Modifier
////                                .padding(4.dp)
//                ) {
//                    val tags = tagMap[presetName] ?: emptySet()
//                    tags.forEach {
//                        Text(it.label)
//                    }
//                }
//            }
//        }
//
//        Column(
//            verticalArrangement = Arrangement.SpaceBetween,
//            modifier = Modifier
//                .weight(0.5f)
////                .fillMaxHeight()
//        ) {
//            if (presetEntry != null) {
//                val image = remember(presetEntry) { imageFromFile(presetsFolder.resolve(presetEntry.previewPath)) }
////                        System.err.println("loaded image ${presetEntry.previewPath}")
//                Image(
//                    bitmap = image,
//                    contentDescription = presetEntry.previewPath,
//                    modifier = Modifier
//                        .aspectRatio(1f)
//                        .fillMaxSize()
////                        .padding(4.dp)
//                )
//
//                Text(
//                    text = presetName,
//                    textAlign = TextAlign.End,
//                    modifier = Modifier
////                                    .padding(horizontal = 4.dp, vertical = 4.dp)
////                                    .weight(0.6f)
//                )
//            }
//        }
//
//    }
//}
