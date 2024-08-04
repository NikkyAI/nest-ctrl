package ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import decks
import nestdrop.Queue
import nestdrop.deck.Deck
import ui.components.VerticalRadioButton
import ui.components.lazyList

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun spoutScreen() {
    val maxQueueLength = remember(decks.map { it.spoutQueue.name }) {
        decks.maxOfOrNull {
            it.spoutQueue.value?.presets?.size ?: 0
        } ?: 0
    }

    val decksEnabled by Deck.enabled.collectAsState()
    lazyList {
        stickyHeader(key = "header") {
            Row(
//            modifier = Modifier
//                .width(200.dp),
                modifier = Modifier.background(
                    MaterialTheme.colors.background
                )
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                decks.forEach { deck ->
                    if (deck.N > decksEnabled) return@forEach

                    val presetNullable by deck.spout.collectAsState()
                    val preset = presetNullable

                    Row(
                        verticalAlignment = Alignment.CenterVertically,

                        modifier = Modifier
                            .weight(0.2f)
                    ) {
                        Row(

                            modifier = Modifier
                            .background(deck.dimmedColor)
                            .padding(8.dp)
                        ) {
                            Text(
                                "Spout:"
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            if (preset != null) {
                                Text(
                                    preset.label,
                                )
                                Text(
                                    "FX: ${preset.effects ?: 0}",
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            } else {
                                Text("-")
                            }
                        }
                    }
                }
            }
        }


        items(maxQueueLength) { i ->
            Row(
                modifier = Modifier
                    .height(36.dp)
            ) {
                decks.forEach { deck ->
                    if (deck.N > decksEnabled) return@forEach

                    val queue: Queue? by deck.spoutQueue.collectAsState()
                    val activeIndexState = deck.spout.index
                    val activeIndex by activeIndexState.collectAsState()
                    val preset = queue?.presets?.getOrNull(i)
                    val queueLength = queue?.presets?.size ?: 0


                    Row(
                        modifier = Modifier
                            .weight(0.2f),
//                            .width(400.dp)
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if(preset != null) {
                            VerticalRadioButton(
                                selected = (activeIndex == i),
                                onClick = {
                                    if (activeIndex == i) {
                                        activeIndexState.value = -1
                                    } else {
                                        activeIndexState.value = i
                                    }
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = deck.color,
                                    unselectedColor = deck.dimmedColor
                                ),
                                height = 36.dp,
                                connectTop = i > 0,
                                connectBottom = i < queueLength - 1,
                            )

                            Text(
                                "FX: ${preset.effects ?: 0}",
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                preset.label,
                            )
                        }
                    }
                }
            }
        }
    }
}