package ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import decks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import nestdrop.NestdropControl
import nestdrop.deck.Deck
import ui.components.Dropdown
import ui.components.verticalScroll

private val selectedTab = MutableStateFlow(0)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun deckSettingsScreen() {
    val currentTab by selectedTab.collectAsState()
    val tabs = listOf("Time", "Color", "Strobe", "Audio", "Output")

    verticalScroll {
        Column(modifier = Modifier.fillMaxWidth()) {
            TabRow(
                selectedTabIndex = currentTab,
            ) {
                tabs.forEachIndexed() { index, tab ->
                    Tab(
                        text = {
                            Text(tab)
                        },
                        selected = currentTab == index,
                        onClick = { selectedTab.value = index }
                    )
                }
            }

            when (currentTab) {
                0 -> {
                    Row {
                        decks.forEach { deck ->
                            val enabled by deck.enabled.collectAsState()
                            if (!enabled) return@forEach
                            Column(
                                modifier = Modifier
                                    .weight(0.2f),
                            ) {
                                deck.ndTime.transitionTime.asSlider(deck)
                                deck.ndTime.animationSpeed.asSlider(deck)
                                deck.ndTime.zoomSpeed.asSlider(deck)
                                deck.ndTime.rotationSpeed.asSlider(deck)
                                deck.ndTime.wrapSpeed.asSlider(deck)
                                deck.ndTime.horizontalMotion.asSlider(deck)
                                deck.ndTime.verticalMotion.asSlider(deck)
                                deck.ndTime.stretchSpeed.asSlider(deck)
                                deck.ndTime.waveMode.asSlider(deck)
                            }
                        }
                    }
                }

                1 -> {
                    Row {
                        decks.forEach { deck ->
                            val enabled by deck.enabled.collectAsState()
                            if (!enabled) return@forEach
                            Column(
                                modifier = Modifier
                                    .weight(0.2f),
                            ) {
                                deck.ndColor.negative.asSlider(deck)
                                deck.ndColor.brightness.asSlider(deck)
                                deck.ndColor.contrast.asSlider(deck)
                                deck.ndColor.gamma.asSlider(deck)
                                deck.ndColor.hueShift.asSlider(deck)
                                deck.ndColor.saturation.asSlider(deck)
                                deck.ndColor.lumaKey.asSlider(deck)
                                deck.ndColor.red.asSlider(deck)
                                deck.ndColor.green.asSlider(deck)
                                deck.ndColor.blue.asSlider(deck)
                                deck.ndColor.alpha.asSlider(deck)
                            }
                        }
                    }
                }

                2 -> {
                    Row {
                        decks.forEach { deck ->
                            val enabled by deck.enabled.collectAsState()
                            if (!enabled) return@forEach
                            Column(
                                modifier = Modifier
                                    .weight(0.2f),
                            ) {
                                deck.ndStrobe.effect.asDropdown(deck)
                                deck.ndStrobe.effectSpan.asSlider(deck)
                                deck.ndStrobe.trigger.asDropdown(deck)
                                deck.ndStrobe.effectSpeed.asSlider(deck)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val scope = rememberCoroutineScope()
                                    listOf(0.1f, 0.125f, 0.25f, 0.5f, 1.0f).forEach { presetValue ->
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    deck.ndStrobe.effectSpeed.value = presetValue
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = deck.dimmedColor
                                            ),
                                            modifier = Modifier
                                                .weight(0.2f)
                                                .padding(4.dp, 0.dp)
                                        ) {
                                            Text(
                                                text = "$presetValue",
                                            )
                                        }
                                    }

                                }
                                deck.ndStrobe.pulseWidth.asSlider(deck)
                                deck.ndStrobe.waveForm.asDropdown(deck)
                                deck.ndStrobe.enabled.asCheckbox(deck)
                            }
                        }
                    }
                }

                3 -> {
                    Row {
                        decks.forEach { deck ->
                            val enabled by deck.enabled.collectAsState()
                            if (!enabled) return@forEach
                            Column(
                                modifier = Modifier
                                    .weight(0.2f),
                            ) {
                                deck.ndAudio.bass.asSlider(deck)
                                deck.ndAudio.mid.asSlider(deck)
                                deck.ndAudio.treble.asSlider(deck)
                            }
                        }
                    }
                }

                4 -> {
                    Row {
                        decks.forEach { deck ->
                            val enabled by deck.enabled.collectAsState()
                            if (!enabled) return@forEach
                            Column(
                                modifier = Modifier
                                    .weight(0.2f),
                            ) {
                                deck.ndOutput.ndDeckPinToTop.asCheckbox(deck)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NestdropControl.Slider.asSlider(deck: Deck) {
    val value by collectAsState()
//    Column {
    val scope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            this@asSlider.propertyName,
            modifier = Modifier
                .weight(0.3f)
                .padding(8.dp, 0.dp)
        )
        Text(
            text = "value\n%6.3f".format(value),
            modifier = Modifier
                .weight(0.3f)
                .padding(8.dp, 0.dp)
        )
//        Text("min\n${range.start}")
        Text("${range.start}", color = deck.color)
        Slider(
            value = value,
            onValueChange = {
                this@asSlider.value = it
            },
            valueRange = range,
            steps = ((range.endInclusive - range.start) / 0.1f).toInt() - 1,
            colors = SliderDefaults.colors(
                thumbColor = deck.color,
                activeTrackColor = deck.dimmedColor,
            ),

            modifier = Modifier
                .weight(0.6f)
        )
//        Text("max\n${range.endInclusive}")
        Text("${range.endInclusive}", color = deck.color)
        IconButton(
            onClick = {
                scope.launch {
                    doReset()
                }
            },
            modifier = Modifier
                .weight(0.2f)
                .padding(8.dp, 0.dp)
        ) {
            Icon(Icons.Filled.Refresh, modifier = Modifier.scale(scaleX = -1f, scaleY = 1f), contentDescription = "reset value")
        }
    }
//    }
}

@Composable
fun NestdropControl.SliderWithResetButton.asSlider(deck: Deck) {
    val value by collectAsState()
//    Column {
    val scope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            this@asSlider.propertyName,
            modifier = Modifier
                .weight(0.3f)
                .padding(8.dp, 0.dp)
        )
        Text(
            text = "value\n%6.3f".format(value),
            modifier = Modifier
                .weight(0.3f)
                .padding(8.dp, 0.dp)
        )
//        Text("min\n${range.start}")
        Text("${range.start}", color = deck.color)
        Slider(
            value = value,
            onValueChange = {
                this@asSlider.value = it
            },
            valueRange = range,
            steps = ((range.endInclusive - range.start) / 0.1f).toInt() - 1,
            colors = SliderDefaults.colors(
                thumbColor = deck.color,
                activeTrackColor = deck.dimmedColor,
            ),

            modifier = Modifier
                .weight(0.6f)
        )
//        Text("max\n${range.endInclusive}")
        Text("${range.endInclusive}", color = deck.color)
        IconButton(
            onClick = {
                scope.launch {
                    doReset()
                }
            },
            modifier = Modifier
                .weight(0.2f)
                .padding(8.dp, 0.dp)
        ) {
            Icon(Icons.Filled.Refresh, modifier = Modifier.scale(scaleX = -1f, scaleY = 1f), contentDescription = "reset value")
        }
    }
//    }
}

@Composable
fun NestdropControl.RangeSliderWithResetButton.asSlider(deck: Deck) {
    val minValue by minState.collectAsState()
    val maxValue by maxState.collectAsState()
    val scope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            this@asSlider.propertyName,
            modifier = Modifier
                .weight(0.3f)
                .padding(8.dp, 0.dp)
        )

        Text(
            text = "value\n%5.2f - %5.2f".format(minValue, maxValue),
            modifier = Modifier
                .weight(0.3f)
                .padding(8.dp, 0.dp)
        )

//        Text("min\n${range.start}")
        Text("${range.start}", color = deck.color)
        Column(
            modifier = Modifier
                .weight(0.6f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
//                Text("min")
                Slider(
                    value = minValue,
                    onValueChange = {
                        minState.value = it
                    },
                    valueRange = range,
                    steps = ((range.endInclusive - range.start) / 0.1f).toInt() - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = deck.color,
                        activeTrackColor = deck.dimmedColor,
                    ),

                    modifier = Modifier
                        .weight(0.6f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
//                Text("max")
                Slider(
                    value = maxValue,
                    onValueChange = {
                        maxState.value = it
                    },
                    valueRange = range,
                    steps = ((range.endInclusive - range.start) / 0.1f).toInt() - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = deck.color,
                        activeTrackColor = deck.dimmedColor,
                    ),
                    modifier = Modifier
                        .weight(0.6f)
                )
            }
        }


//        Text("max\n${range.endInclusive}")
        Text("${range.endInclusive}", color = deck.color)
        IconButton(
            onClick = {
                scope.launch {
                    doReset()
                }
            },
            modifier = Modifier
                .weight(0.2f)
                .padding(8.dp, 0.dp)
        ) {
            Icon(Icons.Filled.Refresh, modifier = Modifier.scale(scaleX = -1f, scaleY = 1f), contentDescription = "reset value")
        }
    }
}


@Composable
fun <T : Any> NestdropControl.Dropdown<T>.asDropdown(deck: Deck) {
    val value: T by collectAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            this@asDropdown.propertyName,
            modifier = Modifier
                .weight(0.3f)
                .padding(8.dp, 0.dp)
        )

        Dropdown(
            color = deck.dimmedColor,
            activeColor = deck.color,
            itemList = options,
            selectedItem = value,
            renderItem = { selected ->
                Text(selected.toString())
            }
        ) {
            this@asDropdown.value = it
        }
    }
}


@Composable
fun NestdropControl.ToggleButton.asCheckbox(deck: Deck) {
    val value by collectAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            this@asCheckbox.propertyName,
            modifier = Modifier
                .weight(0.3f)
                .padding(8.dp, 0.dp)
        )

        Checkbox(
            value,
            onCheckedChange = {
                this@asCheckbox.value = it
            },
            colors = CheckboxDefaults.colors(
                checkmarkColor = deck.dimmedColor,
                uncheckedColor = deck.color,
                checkedColor = deck.color,
                disabledColor = Color.DarkGray
            ),
            modifier = Modifier
                .weight(0.3f)
                .padding(8.dp, 0.dp)
        )
    }
}

