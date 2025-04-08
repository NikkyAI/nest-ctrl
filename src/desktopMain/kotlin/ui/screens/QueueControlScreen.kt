package ui.screens

//import androidx.compose.material3.MaterialTheme
import QUEUES
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import beatFrame
import controlAutoButton
import controlBeatCounter
import controlBeatSlider
import controlShuffleButton
import decks
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import nestdrop.QueueType
import nestdrop.deck.Deck
import osc.OSCMessage
import osc.nestdropPortSend
import ui.components.Dseg14ClassicFontFamily
import ui.components.HorizontalRadioButton
import ui.components.WithTooltipAbove
import ui.components.WithTooltipAtPointer
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
    val beats by controlBeatSlider.flow.collectAsState()

    val nameWidth = allQueues.keys.maxOf { it.length }

    val activeQueues = allQueues.values.filter { it.active }.groupBy { it.deck }
    val beatCount by controlBeatCounter.flow.collectAsState()

    val controlAutoInt by controlAutoButton.flow.collectAsState()
    val autoPlayEnabled = controlAutoInt == 1

    lazyList {
        stickyHeader {
            Box(
                Modifier
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colors.background)
                ) {
                    LinearProgressIndicator(
                        progress = { (beatCount % beats) / beats },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WithTooltipAbove(
                            tooltip = { Text("Auto switch") }
                        ) {
                            Checkbox(
                                checked = autoPlayEnabled,
                                onCheckedChange = {
                                    scope.launch {
                                        controlAutoButton.setValue(if (it) 1 else 0)
                                    }
                                }
                            )
                        }

                        val controlShuffleInt by controlShuffleButton.flow.collectAsState()
//                val isRandom = controlShuffleInt == 1
                        WithTooltipAbove(
                            tooltip = {
                                if (controlShuffleInt == 1) {
                                    Text("auto switch: random")
                                } else {
                                    Text("auto switch: sequential")
                                }
                            }
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (controlShuffleInt == 1) {
                                            controlShuffleButton.setValue(0)
                                        } else {
                                            controlShuffleButton.setValue(1)
                                        }
                                    }
                                },
//                            shape = MaterialTheme.shapes.small,
//                            colors = ButtonDefaults.outlinedButtonColors(
//                                contentColor = Color.White
//                            ),
                            ) {
                                if (controlShuffleInt == 1) {
//                                Text("RANDOM")
                                    Icon(Icons.Outlined.Shuffle, "shuffle")
                                } else {
//                                Text("SEQUENTIAL")
                                    Icon(Icons.AutoMirrored.Filled.ArrowRightAlt, "sequential")
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(0.5f)
//                                .fillMaxWidth(0.8f)
                                .padding(horizontal = 16.dp)
                        ) {

                            Box {
                                var tempBeats by remember("beats", beats) { mutableStateOf(beats - 8) }
                                Slider(
                                    value = tempBeats,
                                    onValueChange = {
                                        tempBeats = it
                                    },
                                    onValueChangeFinished = {
                                        scope.launch {
                                            controlBeatSlider.setValue(tempBeats + 8f)
                                        }
                                    },
                                    valueRange = 0f..120f,
                                    steps = 14,
                                    modifier = Modifier.padding(top = 8.dp, end = 8.dp),
                                )
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    (8..128 step 8).forEach {
                                        Box(
                                            modifier = Modifier
                                                .width(30.dp),
                                            contentAlignment = Alignment.CenterStart,
                                        ) {
                                            Text(it.toString(), textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }
                        Text("Beats: $beats")
                    }

                    val fontDseg14 = Dseg14ClassicFontFamily()

                    Row {
                        Button(
                            onClick = {
                                scope.launch {
//                            controlBeatSlider.setValue(max(16, beats + 8))
                                    controlBeatSlider.setValue(
                                        (beats + 8).coerceAtLeast(8f)
                                    )
                                }
                            },
                            shape = MaterialTheme.shapes.small,
//                colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
//                    contentPadding = PaddingValues(all = 4.dp),
                        ) {
                            Text("+8", fontFamily = fontDseg14)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    controlBeatSlider.setValue(
                                        (beats - 8).coerceAtLeast(8f)
                                    )
//                            beatFrame.value = max(16, beats - 8)
                                }
                            },
                            shape = MaterialTheme.shapes.small,
//                colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
//                    contentPadding = PaddingValues(all = 4.dp),
                        ) {
                            Text("-8", fontFamily = fontDseg14)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    beatFrame.value = 32f
                                }
                            },
                            shape = MaterialTheme.shapes.small,
//                colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
//                    contentPadding = PaddingValues(all = 4.dp),
                        ) {
                            Text("32", fontFamily = fontDseg14)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    beatFrame.value = 64f
                                }
                            },
                            shape = MaterialTheme.shapes.small,
//                colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
//                    contentPadding = PaddingValues(all = 4.dp),
                        ) {
                            Text("64", fontFamily = fontDseg14)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    beatFrame.value = 128f
                                }
                            },
                            shape = MaterialTheme.shapes.small,
//                colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
//                    contentPadding = PaddingValues(all = 4.dp),
                        ) {
                            Text("128", fontFamily = fontDseg14)
                        }
                    }
                }
            }
        }

        items(allQueues.values.sortedBy { it.index }.sortedBy { it.type.ordinal }) { queue ->
            if (!queue.open) return@items

            val deck = decks[queue.deck - 1]
//            val color = colors[queue.deck - 1]
//            val disabledColor = disabledColors[queue.deck - 1]

            Column(
                modifier = Modifier
                    .padding(4.dp)

            ) {
                if (queue.active) {
                    LinearProgressIndicator(
                        progress = {
                            val modifiedBeats = (beats) / queue.beatMultiplier
                            (((beatCount % modifiedBeats) / modifiedBeats) - queue.beatOffset + 1f) % 1f
                        },
                        modifier = Modifier
                            .scale(scaleX = 1f, scaleY = 3f)
                            .fillMaxWidth()
//                            .background(deck.color),
                            .padding(start = 4.dp, end = 4.dp, bottom = 2.dp),
//                        strokeCap = StrokeCap.Square,
                        color = if (autoPlayEnabled) {
                            deck.color
                        } else {
                            deck.disabledColor
                        },
                        trackColor = if (autoPlayEnabled) {
                            deck.disabledColor
                        } else {
                            Color.DarkGray
                        }, // MaterialTheme.colors.surface,
//                        drawStopIndicator = { },
//                        gapSize = 32.dp
                    )
                }
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

                            WithTooltipAbove(
                                tooltip = {
                                    Text(queue.type.name)
                                }
                            ) {
                                Box(
                                    modifier = Modifier
//                                .aspectRatio(1.0f)
                                        .size(32.dp)
                                        .background(Color.Black)
                                        .padding(2.dp)
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colors.onSurface,
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
                    }

                    Spacer(Modifier.width(8.dp))


                    Row(
                        modifier = Modifier.width(150.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {

                        Text("${(beats / queue.beatMultiplier).roundToInt()} Beats")
                        Text("(x${queue.beatMultiplier})")
                    }

                    WithTooltipAbove(
                        tooltip = { Text("slower") }
                    ) {

                        IconButton(
                            onClick = {
                                val newValue = (queue.beatMultiplier / 2) // .coerceAtLeast(1f)
                                scope.launch {
                                    nestdropPortSend(
                                        OSCMessage("/Queue/${queue.name}/sBmul", newValue)
                                    )
                                }
                            },
                        ) {
                            Icon(Icons.Outlined.KeyboardDoubleArrowDown, "slower")
                        }
                    }

                    WithTooltipAbove(
                        tooltip = { Text("faster") }
                    ) {
                        IconButton(
                            onClick = {
                                val newValue = (queue.beatMultiplier * 2) // .coerceAtLeast(1f)
                                scope.launch {
                                    nestdropPortSend(
                                        OSCMessage("/Queue/${queue.name}/sBmul", newValue)
                                    )
                                }
                            },
                        ) {
                            Icon(Icons.Outlined.KeyboardDoubleArrowUp, "faster")
                        }
                    }
                    Spacer(Modifier.width(8.dp))

                    var beatoffset by remember(queue.name, "sBof", queue.beatOffset) {
                        mutableStateOf(queue.beatOffset)
                    }

                    WithTooltipAbove(
                        tooltip = { Text("%.2f".format(queue.beatOffset)) }
                    ) {
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
                    }
                    Text("%.2f".format(queue.beatOffset))
                    Spacer(Modifier.width(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {

                        decks.forEach { deck ->
//                        if (deck.id > decksEnabled) return@forEach
                            WithTooltipAbove(
                                tooltip = { Text(deck.deckName) }
                            ) {
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
                                            nestdropPortSend(
                                                OSCMessage("/Queue/${queue.name}", if (queue.active) 1 else 0)
                                            )
                                        }
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = deck.color,
                                        unselectedColor = deck.dimmedColor,
                                        disabledColor = deck.disabledColor,
                                    ),
                                    connectStart = deck.id in 2..decksEnabled,
                                    connectEnd = deck.id < decksEnabled,
                                    enabled = deck.id <= decksEnabled
//                            height = heightDp,
//                            connectTop = fxIndex > 0,
//                            connectBottom = fxIndex < 49,
                                )
                            }
                        }
                    }

//                logger.info { "queue: ${queue.name}: ${queue.isFileExplorer}" }
                    Row(
                        modifier = Modifier.width(75.dp),
                        horizontalArrangement = Arrangement.Start
//                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        if (queue.isFileExplorer) {
                            WithTooltipAbove(
                                tooltip = { Text("refresh presets") }
                            ) {
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
                            }
                            val folder = File(queue.fileExplorerPath)
                            if (folder.exists()) {
//                                Spacer(modifier = Modifier.width(50.dp))
                                WithTooltipAbove(
                                    tooltip = { Text("open folder in explorer") }
                                ) {
                                    IconButton(
                                        onClick = {
                                            runCommand("explorer", folder.absolutePath, workingDir = File("."))
                                        }
                                    ) {
                                        Icon(Icons.Outlined.FolderOpen, contentDescription = "open folder")
                                    }
                                }
                            }
                        } else {
                            var showConfirmation by mutableStateOf(false)
                            //TODO: confirm ?

                            WithTooltipAbove(
                                tooltip = { Text("shuffle items") }
                            ) {
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
                        }
                    }
//                    Spacer(modifier = Modifier.width(25.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
                        horizontalArrangement = Arrangement.End
                    ) {
//                        Spacer(modifier = Modifier.width(25.dp))

                        WithTooltipAbove(
                            tooltip = {
                                if (queue.active) {
                                    Text("disable ${queue.name} on ${deck.deckName}")
                                } else {
                                    Text("enable ${queue.name} on ${deck.deckName}")
                                }
                            }
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        nestdropPortSend(
                                            OSCMessage("/Queue/${queue.name}", if (queue.active) 0 else 1)
                                        )
//                                    if (!queue.active && queue.type == QueueType.PRESET) {
//                                        activeQueues[queue.deck].orEmpty().filter {
//                                            it.type == QueueType.PRESET && it.name != queue.name
//                                        }.forEach { otherQueue ->
//                                            nestdropPortSend(
//                                                OSCMessage("/Queue/${otherQueue.name}", 0)
//                                            )
//                                        }
//                                    }
                                    }
                                },
                                enabled = !(queue.name.startsWith("spout") && !queue.active)
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
                        }
                    }
                }
            }
        }
    }
}