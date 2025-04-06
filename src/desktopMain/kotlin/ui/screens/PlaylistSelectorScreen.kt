package ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import decks
import nestdrop.deck.Deck
import tags.PresetPlaylist
import tags.nestdropQueueSearches
import ui.Tabs
import ui.components.VerticalRadioButton
import ui.components.verticalScroll
import ui.mainMenuTabState

@Composable
fun PlaytlistSelectorScreen(
) {
    val decksEnabled by Deck.enabled.collectAsState()
    val customSearches by customSearches.collectAsState()
    val nestdropQueueSearches by nestdropQueueSearches.collectAsState()

//    val combinedSearches = customSearches + nestdropQueueSearches

    val deckSearches = decks.associate { deck ->
        val deckSearch by deck.search.collectAsState()
        deck.id to deckSearch
    }

    verticalScroll {
        Column {
            customSearches.forEachIndexed { searchIndex, search ->
                PlaylistSelectorRow(
                    decksEnabled = decksEnabled,
                    deckSearches = deckSearches,
                    search = search,
                    searchIndex = searchIndex,
                    totalSize = customSearches.size + nestdropQueueSearches.size,
                ) {
                    editSearchSelected.value = searchIndex to search
                    mainMenuTabState.value = Tabs.EditPlaylist
                }
            }
            nestdropQueueSearches.forEachIndexed { searchIndex, search ->
                PlaylistSelectorRow(
                    decksEnabled = decksEnabled,
                    deckSearches = deckSearches,
                    search = search,
                    searchIndex = searchIndex,
                    totalSize = customSearches.size + nestdropQueueSearches.size
                )
            }
        }
    }
}

@Composable
private fun PlaylistSelectorRow(
    decksEnabled: Int,
    deckSearches: Map<Int, PresetPlaylist?>,
    search: PresetPlaylist,
    searchIndex: Int,
    totalSize: Int,
    onClickEdit: (() -> Unit)? = null
//    combinedSearches: List<PresetPlaylist>
) {
    val localDensity = LocalDensity.current
    var heightDp by remember {
        mutableStateOf(0.dp)
    }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                // Set column height using the LayoutCoordinates
                heightDp = with(localDensity) { coordinates.size.height.toDp() }
            }
    ) {
        decks.forEach { deck ->
            if (deck.id > decksEnabled) return@forEach

            val deckSearch = deckSearches.getValue(deck.id)

            val selected = (deckSearch == search)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
//                                .weight(0.1f)
                    .padding(horizontal = 20.dp)
            ) {
                VerticalRadioButton(
                    selected = selected,
                    onClick = {
                        // TODO: only store name and lookup from map ?
                        if (selected) {
                            deck.search.value = null
                        } else {
                            deck.search.value = search
                        }
                    },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = deck.color,
                        unselectedColor = deck.dimmedColor
                    ),
                    height = heightDp,
                    connectTop = searchIndex > 0,
                    connectBottom = searchIndex < totalSize - 1,
//                            modifier = Modifier.weight(0.2f)
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
//                            .weight(0.3f)
                .defaultMinSize(300.dp)
                .padding(horizontal = 20.dp)
        ) {
            OutlinedButton(
                onClick = {
                    debugSelectedPlaylistState.value = search
                    mainMenuTabState.value = Tabs.DebugPlaylists
                },
                contentPadding = PaddingValues(horizontal = 4.dp),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("Show Presets")
                }

            }
            Spacer(
                Modifier.width(16.dp)
            )
            Text(
                text = search.label,
//                            textAlign = TextAlign.End,
//                            modifier = Modifier.weight(0.5f)
            )
            Spacer(
                Modifier.weight(2f)
            )
            if(onClickEdit != null) {
                OutlinedButton(
                    onClick = {
                        onClickEdit()
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text("Edit")
                    }
                }
            }
        }
    }
}
