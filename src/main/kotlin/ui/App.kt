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
import androidx.compose.foundation.layout.width
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
import decks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import nestdrop.deck.Deck
import ui.components.verticalScroll
import ui.screens.autoChangeScreen
import ui.screens.beatProgressScreen
import ui.screens.debugScreen
import ui.screens.deckSettingsScreen
import ui.screens.editSearchesScreen
import ui.screens.imgFxScreen
import ui.screens.imgSpritesScreenNew
import ui.screens.presetQueuesScreen
import ui.screens.presetScreen
import ui.screens.searchSelectorScreen
import ui.screens.spoutScreen
import ui.screens.tagEditScreen

@Composable
@Preview
fun App() {
    val nestdropDeckCount by Deck.enabled.collectAsState()

    MaterialTheme(colors = darkColors()) {
        Scaffold {
            Row {
                Column(
                    modifier = Modifier.width(300.dp)
                ) {
                    beatProgressScreen(decks)
                    decks.forEach { deck ->
                        if (deck.N > nestdropDeckCount) return@forEach
                        autoChangeScreen(deck)
                    }
                }
//                Column {
//                    decks.forEach {
//                        presetScreenSingle(it)
//                    }
//                }
                Column {
                    presetScreen()
                    tabScreen()
                }
            }

        }
    }
}

enum class Tabs(
    val label: String,
    val getName: ((Deck) -> Flow<String>)? = null
) {
    PresetQueues(
        "Preset Queues",
        {
            it.presetQueue.name
        }
    ),
    SearchSelector(
        "Preset Playlists",
        { deck ->
            deck.search.map { s -> s?.label ?: "-" }
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
    Tagging("Tags"),
    Searches("Searches"),
    NestdropControls("Nestdrop\nDeck\nSettings"),
    Debug("Debug"),
    ;
}

@Composable
fun ColumnScope.tabScreen(
) {
    val decksEnabled by Deck.enabled.collectAsState()
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
                                    if (deck.N > decksEnabled) return@forEach
                                    val nameMutableStateFlow = getName(deck)
                                    val name by nameMutableStateFlow.collectAsState("unitialized")
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

            Tabs.SearchSelector -> {
                Row(modifier = Modifier.fillMaxWidth()) {
                    searchSelectorScreen()
                }
            }

            Tabs.ImgSprites -> {
                Row(modifier = Modifier.fillMaxWidth()) {
                    imgSpritesScreenNew()
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

            Tabs.Tagging -> {
                Row(modifier = Modifier.fillMaxWidth()) {
                    tagEditScreen()
                }
            }

            Tabs.Searches -> {
                Row(modifier = Modifier.fillMaxWidth()) {
//                    tagSearchScreen()
                    editSearchesScreen()
                }
            }


            Tabs.NestdropControls -> {
                Row(modifier = Modifier.fillMaxWidth()) {
                    deckSettingsScreen()
                }
            }

            Tabs.Debug -> {
                debugScreen()
            }
        }
    }
}
