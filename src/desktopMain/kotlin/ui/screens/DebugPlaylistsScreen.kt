package ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import decks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import nestdrop.deck.Deck
import nestdrop.nestdropSetPreset
import presetsFolder
import presetsMap
import tags.PresetPlaylist
import tags.nestdropQueueSearches
import tags.presetTagsMapping
import ui.components.lazyList
import ui.components.verticalScroll
import kotlin.time.Duration

var debugSelectedPlaylistState = MutableStateFlow<PresetPlaylist?>(null)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun debugPlaylistsScreen() {
    val debugSelectedPlaylist by debugSelectedPlaylistState.collectAsState()
    var scanRunning by remember {
        mutableStateOf(false)
    }
    var scanDuration by remember {
        mutableStateOf(Duration.ZERO)
    }
    val scope = rememberCoroutineScope()
    val decksEnabled by Deck.enabled.collectAsState()
    val customSearches by customSearches.collectAsState()
    val nestdropQueueSearches by nestdropQueueSearches.collectAsState()

    val combinedSearches = (customSearches + nestdropQueueSearches)

    val presets by presetsMap.collectAsState()
    val presetTags by presetTagsMapping.collectAsState()

    val state = rememberLazyListState()

//    var tagScore by remember { mutableStateOf<PresetPlaylist?>(null) }
    val tagScore = combinedSearches.firstOrNull() {
        it.label == debugSelectedPlaylist?.label
    } ?: debugSelectedPlaylist
    Column {
        val scope = rememberCoroutineScope()
        Row {
            verticalScroll {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            debugSelectedPlaylistState.value = null
                            scope.launch {
//                            LaunchedEffect( null) {
                                state.scrollToItem(0)
//                            }
                            }

                        },
                        colors = if (tagScore == null) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("All")
                    }
                    combinedSearches.forEach { it ->
                        Button(
                            onClick = {
                                debugSelectedPlaylistState.value = it
                                scope.launch {
//                                LaunchedEffect(it.label) {
                                    state.scrollToItem(0)
//                                }
                                }
                            },
                            colors = if (tagScore?.label == it.label) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(it.label)
                        }
                    }
                }
            }

            Row {
                val renderPresets = tagScore?.let { tagScore ->
                    presets.mapNotNull { (key, preset) ->
                        val tags = presetTags[key].orEmpty()

                        val score = tagScore.score(tags)
                        if (score > 0.0) {
                            Triple(key, preset, score)
                        } else {
                            null
                        }
                    }
                        .sortedByDescending { it.third }
                } ?: presets.map { (key, preset) ->
                    Triple(key, preset, 1.0)
                }


                lazyList(state = state) {
                    var lastCategory: List<String>? = null
                    renderPresets.forEach { (name, presetEntry, score) ->
                        if (tagScore == null) {
                            val currentCategory = presetEntry.categoryPath
                            if (currentCategory != lastCategory) {
                                stickyHeader(currentCategory) {
                                    Row(
                                        modifier = Modifier
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.Black,
                                                        MaterialTheme.colors.background,
                                                    )
                                                )
                                            )
//                                .background(MaterialTheme.colors.background)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
//                            Spacer(modifier = Modifier.width(30.dp))
                                        currentCategory
                                            .forEachIndexed() { i, pathFragment ->
                                                if (i > 0) {
                                                    Text(" > ", modifier = Modifier.padding(16.dp))
                                                }
                                                Text(pathFragment, modifier = Modifier.padding(16.dp))
                                            }
//                                        if (subCategory != null) {
//                                            Text(" > ", modifier = Modifier.padding(16.dp))
//                                            Text(subCategory, modifier = Modifier.padding(16.dp))
//                                        }
                                    }
                                }

                                lastCategory = currentCategory
                            }
                        }

                        item(key = name to tagScore?.label) {
                            val image = remember { imageFromFile(presetsFolder.resolve(presetEntry.previewPath)) }
                            Row {
                                Column {
                                    Image(bitmap = image, contentDescription = presetEntry.previewPath)
                                    Text("ID: ${presetEntry.id}")
                                    if (tagScore != null) {
                                        Text("Score: $score")
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    decks.forEach { deck ->

                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    nestdropSetPreset(presetEntry.id, deck = deck.id)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = deck.dimmedColor
                                            ),
                                            enabled = deck.id <= decksEnabled
                                        ) {
                                            Text("deck: ${deck.id}")
                                        }
                                    }
                                }
                                Column(
                                    modifier = Modifier.width(300.dp)
                                ) {
                                    val tags = presetTags[name] ?: emptySet()
                                    tags.forEach {
                                        Text(it.label)
                                    }
                                }
//                        Text("${presetEntry.id}")
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(presetEntry.name)
                            }
                        }
                    }
                }
            }
        }
    }
}