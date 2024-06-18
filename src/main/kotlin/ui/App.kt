package ui

import ui.screens.scribbles.ButtonScreen
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import nestdrop.deck.Deck
import ui.screens.ColorControl
import ui.screens.scribbles.SliderScreen
import ui.screens.autoChangeScreen
import ui.screens.beatProgressScreen
import ui.screens.imgSpritesScreen
import ui.screens.presetQueues
import ui.screens.presetScreen

@Composable
@Preview
fun App(
    vararg decks: Deck,
) {

    MaterialTheme(colors = darkColors()) {
        Scaffold {
            Row {
                Column(
//                    modifier = Modifier.fillMaxWidth()
                ) {
                    beatProgressScreen(*decks)
                    decks.forEach {
                        ColorControl(it)
                    }
                }
                Column {
                    autoChangeScreen(*decks)
                    tabScreen(*decks)
                    presetScreen(*decks)
                }
            }

        }
    }
}

@Composable
fun tabScreen(
    vararg decks: Deck,
) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(
        "Preset Queues",
        "IMG Sprites",
        "TODO: IMG FX",
        "TODO: Spout",
        "TODO: Spout FX",
        "TODO: Text FX",
        "TODO: Resolume Arena"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(
            selectedTabIndex = tabIndex
        ) {
            tabs.forEachIndexed { index, tabTitle ->
                Tab(
                    text = { Text(tabTitle) },
                    selected = tabIndex == index,
                    onClick = { tabIndex = index }
                )
            }
        }
        when (tabIndex) {
            0 -> {
                presetQueues(*decks)
            }

            1 -> {
                Row {
                    decks.forEach {
                        imgSpritesScreen(it)
                    }

                }

            }
        }
    }
}

@Composable
@Preview
fun App(
) {
    val sliderMutableFlow = MutableStateFlow<Float>(0.0f)


    GlobalScope.launch {
        while (true) {
            delay(100)
            val v = sliderMutableFlow.value
//            println(v)
            if (v > 0) {
                sliderMutableFlow.value -= 0.1f
            } else {
                sliderMutableFlow.value = 0.0f
            }
        }
    }

    MaterialTheme(colors = darkColors()) {
        Scaffold {
            var tabIndex by remember { mutableStateOf(0) }
            val tabs = listOf("1", "2", "3")
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = tabIndex
                ) {
                    tabs.forEachIndexed { index, tabTitle ->
                        Tab(
                            text = { Text(tabTitle) },
                            selected = tabIndex == index,
                            onClick = { tabIndex = index }
                        )
                    }
                }
                when (tabIndex) {
                    0 -> {
                        var value1 by remember {
                            mutableStateOf(0.15f)
                        }
                        var value2 by remember {
                            mutableStateOf(0.85f)
                        }
//                        Row {
//                            fader(
//                                value = value1,
//                                notches = 9,
//                                color = Color.Red,
//                                verticalText = "Red Fader",
//                            ) {
//                                value1 = it
//                            }
//
//                            fader(
//                                value = value2,
//                                notches = 19,
//                                color = Color.Green,
//                                verticalText = "Green Fader",
//                            ) {
//                                value2 = it
//                            }
//                        }

                    }

                    1 -> {
                        ButtonScreen()
                    }

                    2 -> {
                        SliderScreen(sliderMutableFlow)
                    }
                }
            }
        }

    }
}