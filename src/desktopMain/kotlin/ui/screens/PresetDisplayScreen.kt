package ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import decks
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nestdrop.deck.Deck
import nestdropFolder
import osc.OSCMessage
import osc.nestdropSendChannel
import presetsMap
import tags.Tag
import tags.presetTagsMapping
import ui.components.verticalScroll


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun presetDisplayScreen() {
    val decksEnabled by Deck.enabled.collectAsState()
    Row(
        modifier = Modifier
//            .height(300.dp)
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
    val scope = rememberCoroutineScope()

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
//            .fillMaxWidth()
//            .padding(horizontal = 8.dp, vertical = 0.dp)
            .background(deck.dimmedColor),
    ) {
        val currentPreset by deck.preset.currentPreset.collectAsState()
        val presetName = currentPreset.name
        val presetLocation = presetsMap[presetName + ".milk"]
        val playlistLabel by deck.search.map { s -> s?.label ?: "-" }.collectAsState("-")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(playlistLabel)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 0.dp),
//                        horizontalArrangement = Arrangement.SpaceBetween,
            horizontalArrangement = Arrangement.Center
        ) {

            IconButton(
                onClick = {
                    scope.launch {
                        nestdropSendChannel.send(
                            OSCMessage("/Controls/Deck${deck.id}/btBack", 1)
                        )
                    }
                }
            ) {
                Icon(Icons.Outlined.SkipPrevious, "skipPrevious")
            }
            IconButton(
                onClick = {
                    scope.launch {
                        nestdropSendChannel.send(
                            OSCMessage("/Controls/Deck${deck.id}/btBack", 0)
                        )
                    }
                }
            ) {
                Icon(Icons.Outlined.KeyboardDoubleArrowLeft, "back")
            }
            IconButton(
                onClick = {
                    scope.launch {
                        nestdropSendChannel.send(
                            OSCMessage("/Controls/Deck${deck.id}/btSpace", 0)
                        )
                    }
                }
            ) {
                Icon(Icons.Outlined.KeyboardDoubleArrowRight, "next")
            }

            IconButton(
                onClick = {
                    scope.launch {
                        nestdropSendChannel.send(
                            OSCMessage("/Controls/Deck${deck.id}/btSpace", 1)
                        )
                    }
                }
            ) {
                Icon(Icons.Outlined.SkipNext, "skipNext")
            }
        }
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

        Column(
            modifier = Modifier
                .height(100.dp)
        ) {
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
}
