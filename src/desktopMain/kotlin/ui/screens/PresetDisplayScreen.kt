package ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import decks
import nestdrop.deck.Deck
import nestdropFolder
import tags.presetTagsMapping
import ui.components.verticalScroll


@Composable
fun presetScreenSingle(deck: Deck) {
    val presetsFolder = nestdropFolder.resolve("Plugins").resolve("Milkdrop2").resolve("Presets")
    val presetsMap by presetsMap.collectAsState()
    val tagMap by presetTagsMapping.collectAsState()

    val currentPreset by deck.preset.currentPreset.collectAsState()
    val presetName = currentPreset.name
    val presetEntry = presetsMap[presetName]

    Row(
        modifier = Modifier
            .width(400.dp)
            .aspectRatio(4f/3f)
            .background(deck.dimmedColor)
            .padding(4.dp),
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(0.5f)
        ) {
            verticalScroll {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
//                                .padding(4.dp)
                ) {
                    val tags = tagMap[presetName] ?: emptySet()
                    tags.forEach {
                        Text(it.label)
                    }
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .weight(0.5f)
//                .fillMaxHeight()
        ) {
            if (presetEntry != null) {
                val image = remember(presetEntry) { imageFromFile(presetsFolder.resolve(presetEntry.previewPath)) }
//                        System.err.println("loaded image ${presetEntry.previewPath}")
                Image(
                    bitmap = image,
                    contentDescription = presetEntry.previewPath,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxSize()
//                        .padding(4.dp)
                )

                Text(
                    text = presetName,
                    textAlign = TextAlign.End,
                    modifier = Modifier
//                                    .padding(horizontal = 4.dp, vertical = 4.dp)
//                                    .weight(0.6f)
                )
            }
        }

    }
}

@Composable
fun presetDisplayScreen() {
    val presetsFolder = nestdropFolder.resolve("Plugins").resolve("Milkdrop2").resolve("Presets")
    val presetsMap by presetsMap.collectAsState()
    val tagMap by presetTagsMapping.collectAsState()
    val decksEnabled by Deck.enabled.collectAsState()
    Row(
        modifier = Modifier.height(150.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        decks.forEach { deck ->
            if (deck.id > decksEnabled) return@forEach

            Row(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxSize(0.9f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .background(deck.dimmedColor),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Start,
            ) {
                val currentPreset by deck.preset.currentPreset.collectAsState()
                val presetName = currentPreset.name
                val presetEntry = presetsMap[presetName]

                if (presetEntry != null) {
                    val image = remember(presetEntry) { imageFromFile(presetsFolder.resolve(presetEntry.previewPath)) }
                    Image(
                        bitmap = image,
                        contentDescription = presetEntry.previewPath,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }


                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .padding(4.dp)
                        .weight(0.3f)
                ) {
                    verticalScroll {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier
                        ) {
                            val tags = tagMap[presetName] ?: emptySet()
                            tags.forEach {
                                Text(it.label)
                            }
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .padding(4.dp)
                        .weight(0.3f)
                ) {
                    verticalScroll {

                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
//                                .padding(4.dp)
                        ) {
                            Text(
                                text = presetName,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                            )
                        }
                    }
                }

            }
        }
    }
}