package ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import nestdrop.deck.Deck
import presetsMap
import scanMilkdrop
import tags.PresetPlaylist
import tags.nestdropQueueSearches
import tags.presetTagsMapping
import ui.components.Dseg14ClassicFontFamily
import kotlin.time.Duration
import kotlin.time.measureTime

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

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NEST\nCTRL",
                fontFamily = Dseg14ClassicFontFamily(),
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
                                scanMilkdrop()
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
    }
}



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

