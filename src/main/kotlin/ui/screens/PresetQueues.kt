package ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import nestdrop.deck.Deck

@Composable
fun presetQueues(vararg decks: Deck) {
    val presetQueues = decks[0].presetQueues
    val queues by presetQueues.collectAsState()

    Column {
        Row {
            Text("Preset Queues")
        }
        Row {
            decks.forEach { deck ->

                val activeIndex by deck.presetQueue.index.collectAsState()

                Column {
                    queues.forEachIndexed { i, queue ->
                        val deckSwitch = presetQueues.deckSwitches.getOrNull(i) ?: return@forEachIndexed
                        val deckNumber by deckSwitch.collectAsState()

                        Row(
                            modifier = Modifier
                                .height(36.dp)
                        ) {
//                            RadioButton(
//                                selected = (activeIndex == i),
//                                onClick = {
//                                    deck.presetQueue.index.value = i
//                                },
//                                colors = RadioButtonDefaults.colors(
//                                    selectedColor = deck.color,
//                                    unselectedColor = deck.dimmedColor
//                                ),
//                                enabled = (deckNumber == deck.N)
//                            )

                            Button(onClick = {
                                deck.presetQueue.index.value = i
                            },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if(activeIndex == i) deck.color else deck.dimmedColor,

                                ),
                                enabled = (deckNumber == deck.N)
                            ) {

                            }
                        }
                    }
                }
            }
            decks.forEach { deck ->
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
                            RadioButton(
                                selected = (deckNumber == deck.N),
                                onClick = {
                                    deckSwitch.value = deck.N
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = deck.color,
                                    unselectedColor = deck.dimmedColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}