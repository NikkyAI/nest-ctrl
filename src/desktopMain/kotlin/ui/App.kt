package ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import decks
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import io.github.kdroidfilter.platformtools.getAppVersion
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import io.github.oshai.kotlinlogging.KotlinLogging
import isObfuscated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nestdrop.deck.Deck
import ui.components.verticalScroll
import ui.screens.PlaytlistSelectorScreen
import ui.screens.QueueControlScreen
import ui.screens.autoChangeScreen
import ui.screens.beatProgressScreen
import ui.screens.debugPlaylistsScreen
import ui.screens.debugScreen
import ui.screens.deckSettingsScreen
import ui.screens.editSearchesScreen
import ui.screens.imgFxScreen
import ui.screens.imgSpritesScreenNew
import ui.screens.presetDisplayScreen
import ui.screens.spoutScreen
import ui.screens.tagEditScreen
import utils.runCommand

@Composable
@Preview
fun App() {
    val logger = KotlinLogging.logger {}
    val nestdropDeckCount by Deck.enabled.collectAsState()

    MaterialTheme(
        colors = if (isSystemInDarkMode()) darkColors() else lightColors()
    ) {
        Scaffold {
            Row {
                verticalScroll {
                    Column(
                        modifier = Modifier
                            .width(320.dp)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {

                            beatProgressScreen(decks, size = 220.dp)
                            Text("Auto Change")
                            decks.forEach { deck ->
                                if (deck.id > nestdropDeckCount) return@forEach
                                autoChangeScreen(deck)
                            }
                            Spacer(modifier = Modifier.weight(1f))
//                        Box(
//                            contentAlignment = Alignment.BottomStart,
//                        ) {

                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.Start) {


                                androidx.compose.material3.Text(
                                    "O.S. : ", // + getOperatingSystem().name.lowercase()
                                       // .replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.body1,
                                    color = MaterialTheme.colors.onBackground
                                )

                                androidx.compose.material3.Text(
                                    "Version: ", // + getAppVersion(),
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onBackground
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                androidx.compose.material3.Text(
                                    getOperatingSystem().name.lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.body1,
                                    color = MaterialTheme.colors.onBackground
                                )

                                androidx.compose.material3.Text(
                                    getAppVersion(),
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onBackground
                                )
                            }
                            UpdateCheckerUI(GitHubReleaseFetcher(owner = "nikkyai", repo = "nest-ctrl"))
                        }

//                        Column(
//                            horizontalAlignment = Alignment.Start,
//                            verticalArrangement = Arrangement.Bottom,
//                            modifier = Modifier
//                                .fillMaxWidth(0.9f),
//                        ) {
//                            OutlinedButton(
//                                onClick = {
//                                    logger.info { "opening windows terminal to watch logs" }
//                                    runCommand(
//                                        "wt", "new-tab",
//                                        "-p", "Windows Powershell",
//                                        "--title", "NEST CTRL LOGS",
//                                        "-d", configFolder.path,
//                                        "powershell",
//                                        "Get-Content",
//                                        "-Path", "logs/latest.log",
//                                        "-Wait",
//                                        workingDir = configFolder
//                                    )
//                                },
//                                contentPadding = PaddingValues(8.dp, 0.dp),
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Outlined.Search,
//                                    contentDescription = "Watch Logs (INFO)",
////                                tint = deck.color,
//                                    modifier = Modifier.padding(8.dp)
//                                )
//
//                                Text("Watch Logs (INFO)")
//                            }
//                            MaterialTheme(
//                                colors = darkColors(
//                                    primary = MaterialTheme.colors.error,
//                                    onSurface = MaterialTheme.colors.error
//                                )
//                            ) {
//                                OutlinedButton(
//                                    onClick = {
//                                        logger.info { "opening windows terminal to watch logs" }
//                                        runCommand(
//                                            "wt", "new-tab",
//                                            "-p", "Windows Powershell",
//                                            "--title", "NEST CTRL LOGS",
//                                            "-d", configFolder.path,
//                                            "powershell",
//                                            "Get-Content",
//                                            "-Path", "logs/latest-debug.log",
//                                            "-Wait",
//                                            workingDir = configFolder
//                                        )
//                                    },
//                                    colors = ButtonDefaults.outlinedButtonColors(),
//                                    contentPadding = PaddingValues(8.dp, 0.dp),
////                                border = BorderStroke(
////                                    OutlinedBorderSize, MaterialTheme.colors.onSecondary.copy(alpha = OutlinedBorderOpacity)
////                                ),
//
//                                ) {
//                                    Icon(
//                                        imageVector = Icons.Outlined.Search,
//                                        contentDescription = "Watch LOgs (DEBUG)",
////                                tint = deck.color,
//                                        modifier = Modifier.padding(8.dp)
//                                    )
//
//                                    Text("Watch Logs (DEBUG)")
//                                }
//                            }
//                            MaterialTheme(
//                                colors = darkColors(
//                                    primary = MaterialTheme.colors.error,
//                                    onSurface = MaterialTheme.colors.error
//                                )
//                            ) {
//                                OutlinedButton(
//                                    onClick = {
//                                        logger.info { "opening windows terminal to watch logs" }
//                                        runCommand(
//                                            "wt", "new-tab",
//                                            "-p", "Windows Powershell",
//                                            "--title", "NEST CTRL LOGS",
//                                            "-d", configFolder.path,
//                                            "powershell",
//                                            "Get-Content",
//                                            "-Path", "logs/latest-trace.log",
//                                            "-Wait",
//                                            workingDir = configFolder
//                                        )
//                                    },
//                                    colors = ButtonDefaults.outlinedButtonColors(),
////                                shape = MaterialTheme.shapes.large,
//                                    contentPadding = PaddingValues(8.dp, 0.dp)
////                                border = BorderStroke(
////                                    OutlinedBorderSize, MaterialTheme.colors.onSecondary.copy(alpha = OutlinedBorderOpacity)
////                                ),
//
//                                ) {
//                                    Icon(
//                                        imageVector = Icons.Outlined.Search,
//                                        contentDescription = "Watch LOgs (TRACE)",
////                                tint = deck.color,
//                                        modifier = Modifier.padding(8.dp)
//                                    )
//
//                                    Text("Watch Logs (TRACE)")
//                                }
//                            }
//                        }
                    }
                }
//                Column {
//                    decks.forEach {
//                        presetScreenSingle(it)
//                    }
//                }
                Column {
                    presetDisplayScreen()
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
    //    PresetQueues(
//        "Preset Queues",
//        {
//            it.presetQueue.name
//        }
//    ),
    QueueControls("Queue Controls"),
    PresetPlaylist(
        "Preset Playlists",
        { deck ->
            deck.search.map { s -> s?.label ?: "-" }
        }
    ),
    SpoutSprites(
        "SPT Sprites",
        {
            it.spriteState.spoutStates.map { it.values.firstOrNull()?.label ?: "-" }
        }
    ),
    ImgSprites(
        "IMG Sprites",
        {
            it.spriteState.imgStates.map { it.values.firstOrNull()?.label ?: "-" }
        }
    ),
    ImgFx(
        "IMG FX",
        {
            it.imgSpriteFx.shortLabel
        }
    ),
    Tagging("Tags"),
    EditPlaylist("Playlist\nEditor"),
    NestdropControls("Nestdrop\nSettings"),
    DebugPlaylists("Debug\nPlaylists"),
    Debug("Debug"),
    ;
}

val mainMenuTabState: MutableStateFlow<Tabs> = MutableStateFlow(Tabs.QueueControls)

@Composable
fun ColumnScope.tabScreen(
) {
    val decksEnabled by Deck.enabled.collectAsState()
    val mainMenuCurrentTab by mainMenuTabState.collectAsState()
    val tabs = Tabs.entries
    Column(
        modifier = Modifier
            .fillMaxWidth()
//            .background(Color.LightGray)
//            .weight(0.6f)
    ) {
        TabRow(
            selectedTabIndex = Tabs.entries.indexOf(mainMenuCurrentTab),
            modifier = Modifier
//                .height(decksEnabled * 40.dp + 20.dp)
//                .padding(PaddingValues())
        ) {
            tabs.forEach { tab ->
                Tab(
                    text = {
//                        val getName = tab.getName
//                        if (getName != null) {
//                            Column(
//                                verticalArrangement = Arrangement.Top,
//                                horizontalAlignment = Alignment.CenterHorizontally,
//                            ) {
//                                decks.forEach { deck ->
//                                    if (deck.id > decksEnabled) return@forEach
//                                    val nameMutableStateFlow = getName(deck)
//                                    val name by nameMutableStateFlow.collectAsState("unitialized")
//                                    Row(
//                                        verticalAlignment = Alignment.CenterVertically,
//                                        modifier = Modifier
//                                            .fillMaxWidth()
//                                            .background(deck.dimmedColor)
//                                            .padding(vertical = 0.dp, horizontal = 0.dp)
//                                            .defaultMinSize(minHeight = 36.dp)
////                                            .height(24.dp),
//                                    ) {
//                                        Text(
//                                            text = name,
//                                            modifier = Modifier
//                                                .fillMaxWidth()
//                                        )
//                                    }
//                                }
//                                Spacer(modifier = Modifier.height(10.dp))
//                                Text(tab.label)
//                            }
//                        } else {
//                            Text(tab.label)
//                        }
                        Text(tab.label)
                    },
                    selected = mainMenuCurrentTab == tab,
                    onClick = { mainMenuTabState.value = tab }
                )
            }
        }
        when (mainMenuCurrentTab) {
            Tabs.QueueControls -> {
                Row(modifier = Modifier.fillMaxWidth()) {
                    QueueControlScreen()
                }
            }
//            Tabs.PresetQueues -> {
//                verticalScroll {
//                    presetQueuesScreen()
//                }
//            }

            Tabs.PresetPlaylist -> {
                Row(modifier = Modifier.fillMaxWidth()) {
                    PlaytlistSelectorScreen()
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

            Tabs.EditPlaylist -> {
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

            Tabs.DebugPlaylists -> {
                debugPlaylistsScreen()
            }

            Tabs.Debug -> {
                debugScreen()
            }
        }
    }
}
