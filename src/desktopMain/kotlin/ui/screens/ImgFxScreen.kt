package ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import decks
import nestdrop.deck.Deck
import nestdrop.imgFxMap
import ui.components.VerticalRadioButton
import ui.components.lazyList

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun imgFxScreen() {
    val decksEnabled by Deck.enabled.collectAsState()
    val imgFxMap by imgFxMap.collectAsState()
    lazyList {
        stickyHeader(key = "header") {
            Row(
//            modifier = Modifier
//                .width(200.dp),
                modifier = Modifier.background(
                    MaterialTheme.colors.background
                )
                    .fillMaxWidth()
                ,
                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                decks.forEach { deck ->
                    if (deck.id > decksEnabled) return@forEach

                    val blendModeState = deck.imgSpriteFx.blendMode
                    val blendMode by blendModeState.collectAsState()
                    val currentIndex by deck.imgSpriteFx.index.collectAsState()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,

                        modifier = Modifier
                            .weight(0.2f)
                    ) {
                        Text(
                            text = "FX: $currentIndex",
                            modifier = Modifier
                                .background(deck.dimmedColor)
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Blendmode:")
                        Spacer(modifier = Modifier.width(10.dp))
                        Checkbox(
                            checked = blendMode,
                            onCheckedChange = {
                                blendModeState.value = it
                            },
                            colors = CheckboxDefaults.colors(
                                checkmarkColor = deck.dimmedColor,
                                uncheckedColor = deck.color,
                                checkedColor = deck.color,
                                disabledColor = Color.DarkGray
                            ),
                        )
                    }
                }
            }
        }

        items(50) loop@{ fxIndex ->

            val localDensity = LocalDensity.current
            var heightDp by remember {
                mutableStateOf(0.dp)
            }
            val fxText = imgFxMap[fxIndex] ?: return@loop
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        // Set column height using the LayoutCoordinates
                        heightDp = with(localDensity) { coordinates.size.height.toDp() }
                    }
            ) {
                decks.forEach { deck ->
                    if (deck.id > decksEnabled) return@forEach

                    val selectedIndexState = deck.imgSpriteFx.index
                    val selectedIndex by selectedIndexState.collectAsState()

                    val toggleState = deck.imgSpriteFx.toggles.getOrNull(fxIndex) ?: return@loop
                    val toggled by toggleState.collectAsState()

                    Row(
                        modifier = Modifier
//                            .weight(0.2f)
                            .padding(horizontal = 20.dp)
                    ) {
                        VerticalRadioButton(
                            selected = (selectedIndex == fxIndex),
                            onClick = {
                                selectedIndexState.value = fxIndex
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = deck.color,
                                unselectedColor = deck.dimmedColor
                            ),
                            height = heightDp,
                            connectTop = fxIndex > 0,
                            connectBottom = fxIndex < 49,
                        )

                        Checkbox(
                            checked = toggled,
                            onCheckedChange = {
                                toggleState.value = it
                            },
                            colors = CheckboxDefaults.colors(
                                checkmarkColor = deck.dimmedColor,
                                uncheckedColor = deck.color,
                                checkedColor = deck.color,
                                disabledColor = Color.DarkGray
                            ),
                        )
                    }
                }

                Text(
                    text = fxText,
                    modifier = Modifier
                    .padding(vertical = 4.dp)
                        .padding(horizontal = 10.dp)
//                    .weight(0.4f)
                )
            }
        }
    }
}
