package ui.screens

import QUEUES
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Deck
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayDisabled
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import controlBeat
import decks
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import nestdrop.QueueType
import nestdrop.deck.Deck
import osc.OSCMessage
import osc.nestdropPortSend
import ui.components.HorizontalRadioButton
import ui.components.lazyList
import utils.runCommand
import java.io.File
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger {}

@Composable
fun QueueControlScreen() {

    val decksEnabled by Deck.enabled.collectAsState()
    val allQueues by QUEUES.allQueues.collectAsState()

//    val colors = decks.map { it.color }
//    val disabledColors = decks.map { it.disabledColor }
    val scope = rememberCoroutineScope()
    val beats by controlBeat.flow.collectAsState()

    val nameWidth = allQueues.keys.maxOf { it.length }

    val activeQueues = allQueues.values.filter { it.active }.groupBy { it.deck }

    lazyList {

        items(allQueues.values.sortedBy { it.index }.sortedBy { it.type.ordinal }) { queue ->
            if (!queue.open) return@items

            val deck = decks[queue.deck - 1]
//            val color = colors[queue.deck - 1]
//            val disabledColor = disabledColors[queue.deck - 1]

            Row(
                Modifier
                    .padding(4.dp)
                    .border(
                        width = 2.dp,
                        color = if (queue.active) deck.color else deck.disabledColor,
                        shape = CutCornerShape(5.dp),
                    )
                    .padding(4.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(16.dp))
                Row(
//                    modifier = Modifier.width(150.dp),
                    modifier = Modifier.width((nameWidth * 10).dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(queue.name)
//                    Text(queue.name.padEnd(nameWidth, ' '))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
//                                .aspectRatio(1.0f)
                                .size(32.dp)
                                .background(Color.Black)
                                .padding(2.dp)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CutCornerShape(0.dp),
                                )
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (queue.type) {
                                QueueType.PRESET -> Text("P")
                                QueueType.SPRITE -> Text("S")
                                QueueType.TEXT -> Text("T")
                                QueueType.MIDI -> Text("M")
                                QueueType.SETTING -> Icon(
                                    Icons.Outlined.Settings, queue.type.name
                                )

                                QueueType.`DECK SETTINGS` -> Icon(
                                    Icons.Outlined.Settings, queue.type.name
                                )

                                else -> Text("?")
                            }
                        }

                    }
                }

                Spacer(Modifier.width(16.dp))

                Text("${(beats * queue.beatMultiplier).roundToInt()} Beats".padStart(10, ' '))
                Spacer(Modifier.width(16.dp))

                OutlinedButton(
                    onClick = {
                        // TODO: set beat multiplier
                        val newValue = queue.beatMultiplier * 2
                        scope.launch {
                            nestdropPortSend(
                                OSCMessage("/Queue/${queue.name}/sBmul", newValue)
                            )
                        }
                    },
//                    modifier = Modifier
//                        .padding(2.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                ) {
                    Text("x2", color = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        // TODO: set beat multiplier
                        val newValue = queue.beatMultiplier / 2
                        scope.launch {
                            nestdropPortSend(
                                OSCMessage("/Queue/${queue.name}/sBmul", newValue)
                            )
                        }
                    },
//                    modifier = Modifier
//                        .padding(2.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                ) {
                    Text("/2", color = Color.White)
                }

                var beatoffset by remember(queue.name, "sBof", queue.beatOffset) {
                    mutableStateOf(queue.beatOffset)
                }
                Slider(
                    beatoffset,
                    onValueChange = { newBeatOffset ->
                        beatoffset = newBeatOffset
                    },
                    onValueChangeFinished = {
                        scope.launch {
                            nestdropPortSend(
                                OSCMessage("/Queue/${queue.name}/sBof", beatoffset)
                            )
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = deck.color,
                        activeTrackColor = deck.dimmedColor,
                    ),

                    valueRange = (0f..1f),
                    steps = 3,
                    modifier = Modifier
                        .width(100.dp),
                )
                Text("%.2f".format(queue.beatOffset))
                Spacer(Modifier.width(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    decks.forEach { deck ->
                        if (deck.id > decksEnabled) return@forEach
                        HorizontalRadioButton(
                            selected = (queue.deck == deck.id),
                            onClick = {
                                if (queue.deck == deck.id) {
                                    return@HorizontalRadioButton
                                }
                                scope.launch {
                                    nestdropPortSend(
                                        OSCMessage("/Queue/${queue.name}/Deck", deck.id)
                                    )
                                }
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = deck.color,
                                unselectedColor = deck.dimmedColor
                            ),
                            connectStart = deck.id > 1,
                            connectEnd = deck.id < decksEnabled,
//                            height = heightDp,
//                            connectTop = fxIndex > 0,
//                            connectBottom = fxIndex < 49,
                        )
                    }
                }

//                logger.info { "queue: ${queue.name}: ${queue.isFileExplorer}" }
                if (queue.isFileExplorer) {
                    IconButton(
                        onClick = {
                            // TODO: send
                            scope.launch {
                                nestdropPortSend(
                                    OSCMessage("/Queue/${queue.name}/Refresh", 1)
                                )
                            }
                        }
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            modifier = Modifier.scale(scaleX = -1f, scaleY = 1f),
                            contentDescription = "refresh items"
                        )
                    }
                    val folder = File(queue.fileExplorerPath)
                    if (folder.exists()) {
                        Spacer(modifier = Modifier.width(50.dp))
                        IconButton(
                            onClick = {
                                runCommand("explorer", folder.absolutePath, workingDir = File("."))
                            }
                        ) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = "open folder")
                        }
                    }
                } else {
                    var showConfirmation by mutableStateOf(false)
                    //TODO: confirm ?
                    IconButton(
                        onClick = {
                            if (!showConfirmation) {
                                showConfirmation = true
                            } else {
                                // TODO: send
                                scope.launch {
                                    nestdropPortSend(
                                        OSCMessage("/Queue/${queue.name}/Shuffle", 1)
                                    )
                                }
                                showConfirmation = false
                            }

                        }
                    ) {
                        Icon(
                            if (showConfirmation) {
                                Icons.Filled.Shuffle
                            } else {
                                Icons.Outlined.Shuffle
                            }, "shuffle items",
                            tint = Color.Red
                        )
                    }

                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {

                    IconButton(
                        onClick = {
                            scope.launch {
                                nestdropPortSend(
                                    OSCMessage("/Queue/${queue.name}", if (queue.active) 0 else 1)
                                )
                                if(!queue.active && queue.type == QueueType.PRESET) {
                                    activeQueues[queue.deck].orEmpty().filter {
                                        it.type == QueueType.PRESET && it.name != queue.name
                                    }.forEach { otherQueue ->
                                        nestdropPortSend(
                                            OSCMessage("/Queue/${otherQueue.name}", 0)
                                        )
                                    }

                                }
                                //TODO: automatically disable the other preset queues
                            }
                        },
//                        enabled = queue.type != QueueType.PRESET || activeQueues.isEmpty()
                    ) {
                        if (queue.active) {
                            Icon(Icons.Filled.Pause, "pause")
//                            Text("-", color = Color.White)
                        } else {
                            Icon(Icons.Outlined.PlayArrow, "play")
//                        Text("+", color = Color.White)
                        }
                    }
//                    OutlinedButton(
//                        onClick = {
//
//                        },
//                        shape = MaterialTheme.shapes.extraSmall,
//                        colors = ButtonDefaults.outlinedButtonColors(
//                            contentColor = Color.White
//                        ),
//                    ) {
//
//                    }
                }
            }
        }
    }
}