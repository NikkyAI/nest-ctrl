package ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import nestdrop.Queue
import nestdrop.deck.Deck
import ui.components.VerticalRadioButton
import ui.components.lazyList

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun imgSpritesScreen(
    decks: List<Deck>,
) {

    val maxQueueLength = remember(decks.map { it.spriteQueue.name }) {
        decks.maxOfOrNull {
            it.spriteQueue.value?.presets?.size ?: 0
        } ?: 0
    }

    lazyList {
        stickyHeader(key = "header") {
            Row(
//            modifier = Modifier
//                .width(200.dp),
                modifier = Modifier
                    .background(MaterialTheme.colors.background)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                decks.forEach { deck ->
                    val enabled by deck.enabled.collectAsState()
                    if(!enabled) return@forEach

                    val current by deck.imgSprite.name.collectAsState()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(0.2f)
                    ) {
                        Text(
                            text = "SPRITE: $current",
                            modifier = Modifier.background(deck.dimmedColor)
                                .padding(8.dp)
                        )
//                        Spacer(modifier = Modifier.width(10.dp))
//                        Text("Blendmode:")
//                        Spacer(modifier = Modifier.width(10.dp))
//                        Checkbox(
//                            checked = blendMode,
//                            onCheckedChange = {
//                                blendModeState.value = it
//                            },
//                            colors = CheckboxDefaults.colors(
//                                checkmarkColor = deck.dimmedColor,
//                                uncheckedColor = deck.color,
//                                checkedColor = deck.color,
//                                disabledColor = Color.DarkGray
//                            ),
//                        )
                    }
                }
            }
        }

        items(maxQueueLength) { i ->
            Row(
                modifier = Modifier
                    .height(36.dp)
            ) {
                decks.forEach { deck ->
                    val enabled by deck.enabled.collectAsState()
                    if(!enabled) return@forEach

                    val queue: Queue? by deck.spriteQueue.collectAsState()
                    val activeIndexState = deck.imgSprite.index
                    val activeIndex by activeIndexState.collectAsState()
//                    val (queue, activeIndex) = pairs.getValue(deck.N)

                    val toggledStateflow = deck.imgSprite.toggles.getOrNull(i) ?: return@forEach
                    val toggled by toggledStateflow.collectAsState()

                    val preset = queue?.presets?.getOrNull(i)
                    val queueLength = queue?.presets?.size ?: 0

                    Row(
                        modifier = Modifier
                            .weight(0.2f)
//                            .width(400.dp)
                    ) {
                        if(preset != null) {
                            VerticalRadioButton(
                                selected = (activeIndex == i),
                                onClick = {
                                    if (activeIndex == i) {
                                        activeIndexState.value = -1
                                    } else {
                                        activeIndexState.value = i
                                    }
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = deck.color,
                                    unselectedColor = deck.dimmedColor
                                ),
                                height = 36.dp,
                                connectTop = i > 0,
                                connectBottom = i < queueLength - 1,
                            )

                            Checkbox(
                                checked = toggled,
                                onCheckedChange = {
                                    toggledStateflow.value = it
                                },
                                colors = CheckboxDefaults.colors(
                                    checkmarkColor = deck.dimmedColor,
                                    uncheckedColor = deck.color,
                                    checkedColor = deck.color,
                                    disabledColor = Color.DarkGray
                                ),
                            )

                            Text(preset.name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
//    Column(
//        modifier = Modifier
//            .width(400.dp)
//    ) {
//        val queue: Queue? by deck.spriteQueue.collectAsState()
//
//        val activeIndexState = deck.imgSprite.index
//        val activeIndex by activeIndexState.collectAsState()
////        if(spriteQueue.index > -1) {
//        val presetLength = queue?.presets?.size ?: 0
//        queue?.presets?.forEachIndexed { i, preset ->
//            val toggledStateflow = deck.imgSprite.toggles.getOrNull(i) ?: return@forEachIndexed
//            val toggled by toggledStateflow.collectAsState()
//
//            Row(modifier = Modifier.height(36.dp)) {
//                VerticalRadioButton(
//                    selected = (activeIndex == i),
//                    onClick = {
//                        if (activeIndex == i) {
//                            activeIndexState.value = -1
//                        } else {
//                            activeIndexState.value = i
//                        }
//                    },
//                    colors = RadioButtonDefaults.colors(
//                        selectedColor = deck.color,
//                        unselectedColor = deck.dimmedColor
//                    ),
//                    height = 36.dp,
//                    connectTop = i > 0,
//                    connectBottom = i < presetLength - 1,
//                )
//
//                Checkbox(
//                    checked = toggled,
//                    onCheckedChange = {
//                        toggledStateflow.value = it
//                    },
//                    colors = CheckboxDefaults.colors(
//                        checkmarkColor = deck.dimmedColor,
//                        uncheckedColor = deck.color,
//                        checkedColor = deck.color,
//                        disabledColor = Color.DarkGray
//                    ),
//                )
//
//                Text(preset.name, modifier = Modifier.fillMaxWidth())
//            }
//        }
//    }
}