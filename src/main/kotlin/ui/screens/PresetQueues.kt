package ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
                val color = Color(deck.hexColor)
                val dimmedColor = color.copy(alpha = 0.5f).compositeOver(Color.Black)

                val activeIndex by deck.presetQueue.index.collectAsState()

                Column {
                    queues.forEachIndexed { i, queue ->
                        val deckSwitch = presetQueues.deckSwitches.getOrNull(i) ?: return@forEachIndexed
                        val deckNumber by deckSwitch.collectAsState()

                        Row(
                            modifier = Modifier
                                .height(36.dp)
                        ) {
//                        if (deckNumber == deck.N) {
                            RadioButton(
                                selected = (activeIndex == i),
                                onClick = {
                                    deck.presetQueue.index.value = i
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = color,
                                    unselectedColor = dimmedColor
                                ),
                                enabled = (deckNumber == deck.N)
                            )
//                        }
                        }
                    }
                }
            }
            decks.forEach { deck ->
                val color = Color(deck.hexColor)
                val dimmedColor = color.copy(alpha = 0.5f).compositeOver(Color.Black)

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
//                        if (deckNumber == deck.N) {
                            Switch(
                                checked = toggled, onCheckedChange = {
                                    toggledStateflow.value = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = color,
                                    checkedTrackColor = color,
                                    uncheckedThumbColor = dimmedColor,
//                                    uncheckedTrackColor = dimmedColor
                                ),
                                enabled = (deckNumber == deck.N)
                            )
//                        }
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


            Column {
                queues.forEachIndexed { i, queue ->
                    val deckSwitch = presetQueues.deckSwitches.getOrNull(i) ?: return@forEachIndexed
                    val deckNumber by deckSwitch.collectAsState()

                    Row(
                        modifier = Modifier
                            .height(36.dp)
                    ) {
                        decks.forEach { deck ->
                            val color = Color(deck.hexColor)
                            val dimmedColor = color.copy(alpha = 0.5f).compositeOver(Color.Black)
                            RadioButton(
                                selected = (deckNumber == deck.N),
                                onClick = {
                                    deckSwitch.value = deck.N
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = color,
                                    unselectedColor = dimmedColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}