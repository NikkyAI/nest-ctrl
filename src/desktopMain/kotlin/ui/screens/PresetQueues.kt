package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import decks
import nestdrop.deck.Deck
import nestdrop.deck.PresetQueues
import presetQueues
import tags.nestdropQueueSearches
import ui.components.HorizontalRadioButton
import ui.components.VerticalRadioButton
import ui.components.verticalScroll

@Composable
fun presetQueuesScreen(
) {
//    val presetQueues = decks[0].presetQueues
    val queues by presetQueues.collectAsState()
    val decksEnabled by Deck.enabled.collectAsState()

//    Column {
//        Row {
//            Text("Preset Queues")
//        }
    Row {
        decks.forEach { deck ->
            if (deck.N > decksEnabled) return@forEach

            val activeIndex by deck.presetQueue.index.collectAsState()

            Column {
                queues.forEachIndexed { i, queue ->
                    val deckSwitch = presetQueues.deckSwitches.getOrNull(i) ?: return@forEachIndexed
                    val deckNumber by deckSwitch.collectAsState()

                    Row(
                        modifier = Modifier
                            .height(36.dp)
                    ) {
                        VerticalRadioButton(
                            selected = (activeIndex == i),
                            onClick = {
                                deck.presetQueue.index.value = i
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = deck.color,
                                unselectedColor = deck.dimmedColor
                            ),
                            enabled = (deckNumber == deck.N),
                            height = 36.dp,
                            connectTop = i > 0,
                            connectBottom = i < queues.size - 1
                        )

//                            Button(onClick = {
//                                deck.presetQueue.index.value = i
//                            },
//                                colors = ButtonDefaults.buttonColors(
//                                    backgroundColor = if(activeIndex == i) deck.color else deck.dimmedColor,
//
//                                ),
//                                enabled = (deckNumber == deck.N)
//                            ) {
//
//                            }
                    }
                }
            }
        }
        decks.forEach { deck ->
            if (deck.N > decksEnabled) return@forEach
            Column {
                queues.forEachIndexed { i, queue ->
                    val deckSwitch = presetQueues.deckSwitches.getOrNull(i) ?: return@forEachIndexed
                    val deckNumber by deckSwitch.collectAsState()

                    val toggledStateflow = deck.presetQueue.toggles.getOrNull(i) ?: return@forEachIndexed
                    val toggled by toggledStateflow.collectAsState()
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                    ) {
//                            Switch(
//                                checked = toggled, onCheckedChange = {
//                                    toggledStateflow.value = it
//                                },
//                                colors = SwitchDefaults.colors(
//                                    checkedThumbColor = deck.color,
//                                    checkedTrackColor = deck.color,
//                                    uncheckedThumbColor = deck.dimmedColor,
////                                    uncheckedTrackColor = dimmedColor
//                                ),
//                                enabled = (deckNumber == deck.N)
//                            )

                        Checkbox(
                            checked = toggled,
                            onCheckedChange = {
                                toggledStateflow.value = it
                            },
                            colors = CheckboxDefaults.colors(
                                checkmarkColor = deck.dimmedColor,
                                uncheckedColor = deck.color,
                                checkedColor = deck.color,
                                disabledColor = Color.DarkGray
                            ),
                            enabled = (deckNumber == deck.N)
                        )
                    }
                }
            }
        }

        Column {
            queues.forEachIndexed { i, queue ->
                Row(
                    modifier = Modifier
                        .height(36.dp)
                ) {

                    Text(text = queue.name)
                }
            }
        }

        //TODO: make "connected" horizontal slider toggle with multiple colors
        Column {
            queues.forEachIndexed { i, queue ->
                val deckSwitch = presetQueues.deckSwitches.getOrNull(i) ?: return@forEachIndexed
                val deckNumber by deckSwitch.collectAsState()

                Row(
                    modifier = Modifier
                        .height(36.dp)
                ) {
                    decks.forEach { deck ->
                        val enabled = (deck.N <= decksEnabled)
//                            if(!enabled) return@forEach
                        HorizontalRadioButton(
                            selected = (deckNumber == deck.N),
                            onClick = {
                                deckSwitch.value = deck.N
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = deck.color,
                                unselectedColor = deck.dimmedColor
                            ),

                            connectStart = !deck.first,
                            connectEnd = !deck.last,
//                                width = 16.dp,
                            enabled = enabled
                        )
                    }
                }
            }
        }
    }
//    }
}


@Composable
fun searchSelectorScreen(
) {
    val decksEnabled by Deck.enabled.collectAsState()
    val customSearches by customSearches.collectAsState()
    val nestdropQueueSearches by nestdropQueueSearches.collectAsState()

    val combinedSearches = customSearches + nestdropQueueSearches

    val deckSearches = decks.associate { deck ->
        val deckSearch by deck.search.collectAsState()
        deck.N to deckSearch
    }

    verticalScroll {
        Column {
            combinedSearches.forEachIndexed { searchIndex, search ->
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(36.dp)
                ) {

                    decks.forEach { deck ->
                        if (deck.N > decksEnabled) return@forEach

                        val deckSearch = deckSearches.getValue(deck.N)

                        val selected = (deckSearch == search)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(0.1f)
                        ) {
                            VerticalRadioButton(
                                selected = (deckSearch == search),
                                onClick = {
                                    if (selected) {
                                        deck.search.value = null
                                    } else {
                                        deck.search.value = search
                                    }
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = deck.color,
                                    unselectedColor = deck.dimmedColor
                                ),
                                height = 36.dp,
                                connectTop = searchIndex > 0,
                                connectBottom = searchIndex < combinedSearches.size - 1,
//                            modifier = Modifier.weight(0.2f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(0.1f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .weight(0.3f)
                            .defaultMinSize(300.dp)
                    ) {
                        Text(
                            text = search.label,
//                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.5f)
                        )
                    }
                }
            }
        }
    }
}
