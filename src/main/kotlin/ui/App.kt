package ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nestdrop.deck.Deck
import nestdrop.deck.Effect
import nestdrop.deck.PresetQueues
import nestdrop.deck.Trigger
import nestdrop.deck.Waveform
import ui.components.Dropdown
import ui.components.verticalScroll
import ui.screens.ColorControl
import ui.screens.autoChangeScreen
import ui.screens.beatProgressScreen
import ui.screens.debugScreen
import ui.screens.imgFxScreen
import ui.screens.imgSpritesScreen
import ui.screens.presetQueuesScreen
import ui.screens.presetScreen
import ui.screens.scribbles.ButtonScreen
import ui.screens.scribbles.SliderScreen
import ui.screens.spoutScreen
import ui.screens.tagEditScreen
import ui.screens.tagSearchScreen

@Composable
@Preview
fun App(
    presetQueues: PresetQueues,
    decks: List<Deck>,
) {

    MaterialTheme(colors = darkColors()) {
        Scaffold {
            Row {
                Column(
//                    modifier = Modifier.fillMaxWidth()
                ) {
                    beatProgressScreen(decks)
                    decks.forEach { deck ->
                        val enabled by deck.enabled.collectAsState()
                        if (enabled) {
                            ColorControl(deck)
                        }
                    }
                    decks.forEach { deck ->
                        val enabled by deck.enabled.collectAsState()
                        if (enabled) {
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val effect by deck.ndStrobe.effect.collectAsState()
                                    Dropdown(deck.dimmedColor, deck.color, Effect.entries, effect, renderItem = { selected ->
                                        Text(selected.toString())
                                    }) {
                                        deck.ndStrobe.effect.value = it
                                    }
                                    Text("Effect")
                                }

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val trigger by deck.ndStrobe.trigger.collectAsState()
                                    Dropdown(
                                        deck.dimmedColor,
                                        deck.color,
                                        Trigger.entries,
                                        trigger,
                                        renderItem = { selected ->
                                            Text(selected.toString())
                                        }) {
                                        deck.ndStrobe.trigger.value = it
                                    }
                                    Text("Trigger")
                                }

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val waveForm by deck.ndStrobe.waveForm.collectAsState()
                                    Dropdown(
                                        deck.dimmedColor,
                                        deck.color,
                                        Waveform.entries,
                                        waveForm,
                                        renderItem = { selected ->
                                            Text(selected.toString())
                                        }) {
                                        deck.ndStrobe.waveForm.value = it
                                    }
                                    Text("Waveform")
                                }
                            }
                        }
                    }
                }
//                Column {
//                    decks.forEach {
//                        presetScreenSingle(it)
//                    }
//                }
                Column {
                    presetScreen(decks)
                    tabScreen(presetQueues, decks)
                    autoChangeScreen(decks)
                }
            }

        }
    }
}

enum class Tabs(
    val label: String,
    val getName: ((Deck) -> StateFlow<String>)? = null
) {
    PresetQueues(
        "Preset Queues",
        {
            it.presetQueue.name
        }
    ),
    ImgSprites(
        "IMG Sprites",
        {
            it.imgSprite.name
        }
    ),
    ImgFx(
        "IMG FX",
        {
            it.imgSpriteFx.shortLabel
        }
    ),
    SpoutSprites(
        "Spout Sprites",
        {
            it.spout.name
        }
    ),
    TagSearch("Tag Search"),
    TagEdit("Edit Tags"),
    Debug("Debug"),
    ;
}

@Composable
fun ColumnScope.tabScreen(
    presetQueues: PresetQueues,
    decks: List<Deck>,
) {
    var currentTab by remember { mutableStateOf(Tabs.PresetQueues) }
    val tabs = Tabs.entries
    Column(modifier = Modifier.fillMaxWidth().weight(0.6f)) {
        TabRow(
            selectedTabIndex = Tabs.entries.indexOf(currentTab),
            modifier = Modifier
                .height(decks.size * 36.dp + 20.dp)
                .padding(PaddingValues())
        ) {
            tabs.forEach { tab ->
                Tab(
                    text = {
                        val getName = tab.getName
                        if (getName != null) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                decks.forEach { deck ->
                                    val enabled by deck.enabled.collectAsState()
                                    if(enabled) {
                                        val nameMutableStateFlow = getName(deck)
                                        val name by nameMutableStateFlow.collectAsState()
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(deck.dimmedColor)
                                                .padding(vertical = 4.dp)
                                                .height(24.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = name,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(tab.label)
                            }
                        } else {
                            Text(tab.label)
                        }
                    },
                    selected = currentTab == tab,
                    onClick = { currentTab = tab }
                )
            }
        }
        when (currentTab) {
            Tabs.PresetQueues -> {
                verticalScroll {
                    presetQueuesScreen()
                }
            }

            Tabs.ImgSprites -> {
                Row(modifier = Modifier.fillMaxWidth()) {
                    imgSpritesScreen()
//                        decks.forEach {
//                        }

                }
            }

            Tabs.ImgFx -> {
                Row(modifier = Modifier.fillMaxWidth()) {
                    imgFxScreen()
                }
//                verticalScroll {
//                    Row(modifier = Modifier.fillMaxWidth()) {
//                        decks.forEach {
//                        }
//                    }
//                }
            }

            Tabs.SpoutSprites -> {
                Row(modifier = Modifier.fillMaxWidth()) {
                    spoutScreen()
                }
            }

            Tabs.TagSearch -> {
                Row(modifier = Modifier.fillMaxWidth()) {
                    tagSearchScreen()
                }
            }

            Tabs.TagEdit -> {
                Row(modifier = Modifier.fillMaxWidth()) {
                    tagEditScreen()
                }
            }

            Tabs.Debug -> {
                debugScreen()
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