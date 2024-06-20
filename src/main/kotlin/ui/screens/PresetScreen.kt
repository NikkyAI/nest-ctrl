package ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nestdrop.deck.Deck
import nestdropFolder

@Composable
fun presetScreen(vararg decks: Deck) {
    val presetsFolder = nestdropFolder.resolve("Plugins").resolve("Milkdrop2").resolve("Presets")
    val presetsMap by presetsMap.collectAsState()
    Row(
        modifier = Modifier.height(150.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        decks.forEach { deck ->
                Row(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxSize(0.9f)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .background(deck.dimmedColor),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    val currentPreset by deck.preset.name.collectAsState()
                    val presetEntry = presetsMap[currentPreset]

                    if (presetEntry != null) {
                        val image = remember(presetEntry) { imageFromFile(presetsFolder.resolve(presetEntry.previewPath)) }
//                        System.err.println("loaded image ${presetEntry.previewPath}")
                        Image(
                            bitmap = image,
                            contentDescription = presetEntry.previewPath,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    }

                    val path = presetEntry?.path?.substringBeforeLast('\\')?.split('\\')

                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .padding(4.dp)
                    ) {
                        if(path != null) {
                            Text(
                                text = path.joinToString( " > "),
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .weight(0.3f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        //TODO: set up min height with text layout ?

                        Text(
                            text = currentPreset,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .weight(0.6f)
                        )
                    }

                }
//            }
        }
    }
}