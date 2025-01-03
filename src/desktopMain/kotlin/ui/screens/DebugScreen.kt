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
import nestdrop.PresetLocation
import nestdrop.deck.Deck
import nestdrop.nestdropSetPreset
import tags.presetTagsMapping
import presetsFolder
import tags.PresetPlaylist
import tags.nestdropQueueSearches
import ui.components.lazyList
import ui.components.verticalScroll
import kotlin.time.Duration

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun debugScreen() {
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

    var tagScore by remember { mutableStateOf<PresetPlaylist?>(null) }
    Column {
        /*
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NEST\nCTRL",
                fontFamily = fontDseg14,
                fontSize = 30.sp,
                lineHeight = 45.sp,
                modifier = Modifier.padding(32.dp)
            )
            Spacer(modifier = Modifier.width(30.dp))
            Text(tagScore?.label ?: "All")
            Spacer(modifier = Modifier.width(30.dp))
            Button(
                {
                    scope.launch {
                        scanRunning = true
                        scope.launch(Dispatchers.IO) {
                            scanDuration = measureTime {
                                scanPresets()
                            }
                            scanRunning = false
                        }
                    }
                }, enabled = !scanRunning
            ) {
                Text("scan presets")
            }

            if (scanDuration > Duration.ZERO) {
                Text("Scan took $scanDuration")
            }
        }
        */
        val scope = rememberCoroutineScope()
        Row {
            verticalScroll {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            tagScore = null
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
                                tagScore = it
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
                    var lastCategory: Pair<String, String?>? = null
                    renderPresets.forEach { (name, presetEntry, score) ->
                        if (tagScore == null) {
                            val currentCategory = presetEntry.category to presetEntry.subCategory
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
                                        Text(currentCategory.first, modifier = Modifier.padding(16.dp))
                                        val subCategory = currentCategory.second
                                        if (subCategory != null) {
                                            Text(" > ", modifier = Modifier.padding(16.dp))
                                            Text(subCategory, modifier = Modifier.padding(16.dp))
                                        }
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


val presetsMap = MutableStateFlow<Map<String, PresetLocation.Milk>>(emptyMap())
val imgSpritesMap = MutableStateFlow<Map<String, PresetLocation.Img>>(emptyMap())

data class AutoplayState(
    val presetQueue: Boolean,
    val preset: Boolean,
    val imgSprite: Boolean,
    val imgSpriteFx: Boolean,
) {
    suspend fun apply(deck: Deck) {
//        deck.presetQueue.autoChange.value = presetQueue
//        deck.preset.autoChange.value = preset
        deck.imgSprite.autoChange.value = imgSprite
        deck.imgSpriteFx.autoChange.value = imgSpriteFx
    }
}

