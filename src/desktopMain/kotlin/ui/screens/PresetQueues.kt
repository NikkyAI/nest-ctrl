package ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import decks
import nestdrop.deck.Deck
import presetQueues
import tags.nestdropQueueSearches
import ui.components.HorizontalRadioButton
import ui.components.VerticalRadioButton
import ui.components.verticalScroll

@Composable
fun presetQueuesScreen(
) {
////    val presetQueues = decks[0].presetQueues
//    val queues by presetQueues.collectAsState()
//    val decksEnabled by Deck.enabled.collectAsState()
//
////    Column {
////        Row {
////            Text("Preset Queues")
////        }
//    Row {
//        decks.forEach { deck ->
//            if (deck.N > decksEnabled) return@forEach
//
//            val activeIndex by deck.presetQueue.index.collectAsState()
//
//            Column {
//                queues.forEachIndexed { i, queue ->
//                    val deckSwitch = presetQueues.deckSwitches.getOrNull(i) ?: return@forEachIndexed
//                    val deckNumber by deckSwitch.collectAsState()
//
//                    Row(
//                        modifier = Modifier
//                            .height(36.dp)
//                    ) {
//                        VerticalRadioButton(
//                            selected = (activeIndex == i),
//                            onClick = {
//                                deck.presetQueue.index.value = i
//                            },
//                            colors = RadioButtonDefaults.colors(
//                                selectedColor = deck.color,
//                                unselectedColor = deck.dimmedColor
//                            ),
//                            enabled = (deckNumber == deck.N),
//                            height = 36.dp,
//                            connectTop = i > 0,
//                            connectBottom = i < queues.size - 1
//                        )
//
////                            Button(onClick = {
////                                deck.presetQueue.index.value = i
////                            },
////                                colors = ButtonDefaults.buttonColors(
////                                    backgroundColor = if(activeIndex == i) deck.color else deck.dimmedColor,
////
////                                ),
////                                enabled = (deckNumber == deck.N)
////                            ) {
////
////                            }
//                    }
//                }
//            }
//        }
//        decks.forEach { deck ->
//            if (deck.N > decksEnabled) return@forEach
//            Column {
//                queues.forEachIndexed { i, queue ->
//                    val deckSwitch = presetQueues.deckSwitches.getOrNull(i) ?: return@forEachIndexed
//                    val deckNumber by deckSwitch.collectAsState()
//
//                    val toggledStateflow = deck.presetQueue.toggles.getOrNull(i) ?: return@forEachIndexed
//                    val toggled by toggledStateflow.collectAsState()
//                    Row(
//                        modifier = Modifier
//                            .height(36.dp)
//                    ) {
////                            Switch(
////                                checked = toggled, onCheckedChange = {
////                                    toggledStateflow.value = it
////                                },
////                                colors = SwitchDefaults.colors(
////                                    checkedThumbColor = deck.color,
////                                    checkedTrackColor = deck.color,
////                                    uncheckedThumbColor = deck.dimmedColor,
//////                                    uncheckedTrackColor = dimmedColor
////                                ),
////                                enabled = (deckNumber == deck.N)
////                            )
//
//                        Checkbox(
//                            checked = toggled,
//                            onCheckedChange = {
//                                toggledStateflow.value = it
//                            },
//                            colors = CheckboxDefaults.colors(
//                                checkmarkColor = deck.dimmedColor,
//                                uncheckedColor = deck.color,
//                                checkedColor = deck.color,
//                                disabledColor = Color.DarkGray
//                            ),
//                            enabled = (deckNumber == deck.N)
//                        )
//                    }
//                }
//            }
//        }
//
//        Column {
//            queues.forEachIndexed { i, queue ->
//                Row(
//                    modifier = Modifier
//                        .height(36.dp)
//                ) {
//
//                    Text(text = queue.name)
//                }
//            }
//        }
//
//        //TODO: make "connected" horizontal slider toggle with multiple colors
//        Column {
//            queues.forEachIndexed { i, queue ->
//                val deckSwitch = presetQueues.deckSwitches.getOrNull(i) ?: return@forEachIndexed
//                val deckNumber by deckSwitch.collectAsState()
//
//                Row(
//                    modifier = Modifier
//                        .height(36.dp)
//                ) {
//                    decks.forEach { deck ->
//                        val enabled = (deck.N <= decksEnabled)
////                            if(!enabled) return@forEach
//                        HorizontalRadioButton(
//                            selected = (deckNumber == deck.N),
//                            onClick = {
//                                deckSwitch.value = deck.N
//                            },
//                            colors = RadioButtonDefaults.colors(
//                                selectedColor = deck.color,
//                                unselectedColor = deck.dimmedColor
//                            ),
//
//                            connectStart = !deck.first,
//                            connectEnd = !deck.last,
////                                width = 16.dp,
//                            enabled = enabled
//                        )
//                    }
//                }
//            }
//        }
//    }
//    }
}

