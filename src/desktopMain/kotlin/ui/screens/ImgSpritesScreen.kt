package ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import decks
import nestdrop.deck.Deck
import ui.components.VerticalRadioButton
import ui.components.lazyList

//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//fun imgSpritesScreen() {
//
//    val maxQueueLength = remember(decks.map { it.spriteQueue.name }) {
//        decks.maxOfOrNull {
//            it.spriteQueue.value?.presets?.size ?: 0
//        } ?: 0
//    }
//
//    lazyList {
//        stickyHeader(key = "header") {
//            Row(
//                modifier = Modifier
//                    .background(MaterialTheme.colors.background)
//                    .fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically,
//            ) {
//                decks.forEach { deck ->
//                    val enabled by deck.enabled.collectAsState()
//                    if (!enabled) return@forEach
//
//                    val current by deck.imgSprite.name.collectAsState()
//
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        modifier = Modifier
//                            .weight(0.2f)
//                    ) {
//                        Text(
//                            text = "SPRITE: $current",
//                            modifier = Modifier.background(deck.dimmedColor)
//                                .padding(8.dp)
//                        )
//                    }
//                }
//            }
//        }
//
//        items(maxQueueLength) { i ->
//            Row(
//                modifier = Modifier
//                    .height(36.dp)
//            ) {
//                decks.forEach { deck ->
//                    val enabled by deck.enabled.collectAsState()
//                    if (!enabled) return@forEach
//
//                    val queue: Queue? by deck.spriteQueue.collectAsState()
//                    val activeIndexState = deck.imgSprite.index
//                    val activeIndex by activeIndexState.collectAsState()
////                    val (queue, activeIndex) = pairs.getValue(deck.N)
//
//                    val toggledStateflow = deck.imgSprite.toggles.getOrNull(i) ?: return@forEach
//                    val toggled by toggledStateflow.collectAsState()
//
//                    val preset = queue?.presets?.getOrNull(i)
//                    val queueLength = queue?.presets?.size ?: 0
//
//                    Row(
//                        modifier = Modifier
//                            .weight(0.2f)
////                            .width(400.dp)
//                    ) {
//                        if (preset != null) {
//                            VerticalRadioButton(
//                                selected = (activeIndex == i),
//                                onClick = {
//                                    if (activeIndex == i) {
//                                        activeIndexState.value = -1
//                                    } else {
//                                        activeIndexState.value = i
//                                    }
//                                },
//                                colors = RadioButtonDefaults.colors(
//                                    selectedColor = deck.color,
//                                    unselectedColor = deck.dimmedColor
//                                ),
//                                height = 36.dp,
//                                connectTop = i > 0,
//                                connectBottom = i < queueLength - 1,
//                            )
//
//                            Checkbox(
//                                checked = toggled,
//                                onCheckedChange = {
//                                    toggledStateflow.value = it
//                                },
//                                colors = CheckboxDefaults.colors(
//                                    checkmarkColor = deck.dimmedColor,
//                                    uncheckedColor = deck.color,
//                                    checkedColor = deck.color,
//                                    disabledColor = Color.DarkGray
//                                ),
//                            )
//
//                            Text(preset.name, modifier = Modifier.fillMaxWidth())
//                        }
//                    }
//                }
//            }
//        }
//    }
//}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun imgSpritesScreenNew() {
    val decksEnabled by Deck.enabled.collectAsState()
    val spriteStuff = decks.associate { deck ->
        val current by deck.imgSprite.spriteImgLocation.collectAsState()
        val enabledSprites by deck.imgSprite.toggles.collectAsState()
        deck.N to (current to enabledSprites)
    }//.withDefault { false }

    val spritesMap by imgSpritesMap.collectAsState()
    val spriteLocations = spritesMap.values.sortedBy {
        it.id
    }

    val maxQueueLength = spriteLocations.size

    val groupedByCategories = spriteLocations.groupBy { it.category to it.subCategory }

    Column {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colors.background)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            decks.forEach { deck ->
                if (deck.N > decksEnabled) return@forEach

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
                }
            }
        }
        lazyList {
            groupedByCategories.forEach { (category, subCategory), sprites ->
                stickyHeader(key = "$category-$subCategory") {
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
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(category, modifier = Modifier.padding(16.dp))
                        if (subCategory != null) {
                            Text(" > ", modifier = Modifier.padding(16.dp))
                            Text(subCategory, modifier = Modifier.padding(16.dp))
                        }
                    }
                }

                items(sprites) { sprite ->
//                    var heightDp by remember {
//                        mutableStateOf(0.dp)
//                    }
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
////                horizontalArrangement = Arrangement.SpaceEvenly,
//                        modifier = Modifier
//                            .onGloballyPositioned { coordinates ->
//                                // Set column height using the LayoutCoordinates
//                                heightDp = with(localDensity) { coordinates.size.height.toDp() }
//                            }
//                    ) {
//
//                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .weight(0.4f)
                        ) {
                            decks.forEach { deck ->
                                if (deck.N > decksEnabled) return@forEach

                                val (current, enabledSprites) = spriteStuff.getValue(deck.N)
//                        val current by deck.imgSprite.spriteImgLocation.collectAsState()
//                        val enabledSprites by deck.imgSprite.enabledSprites.collectAsState()

//                        val image = remember { imageFromFile(spritesFolder.resolve(sprite.path)) }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(0.1f)
                                        .padding(horizontal = 20.dp)
                                ) {
                                    VerticalRadioButton(
                                        selected = current?.name == sprite.name,
                                        onClick = {
                                            if (current?.name == sprite.name) {
                                                deck.imgSprite.spriteImgLocation.value = null
                                            } else {
                                                deck.imgSprite.spriteImgLocation.value = sprite
                                            }
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = deck.color,
                                            unselectedColor = deck.dimmedColor
                                        ),
                                        height = 40.dp,
//                                connectTop = spriteLocations.indexOf(sprite) > 0, // i > 0,
//                                connectBottom = spriteLocations.indexOf(sprite) < spriteLocations.size - 1, // i < queueLength - 1,
                                    )
                                    Checkbox(
                                        checked = sprite.name in enabledSprites,
                                        onCheckedChange = {
                                            if (sprite.name in enabledSprites) {
                                                deck.imgSprite.toggles.value -= sprite.name
                                            } else {
                                                deck.imgSprite.toggles.value += sprite.name
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkmarkColor = deck.dimmedColor,
                                            uncheckedColor = deck.color,
                                            checkedColor = deck.color,
                                            disabledColor = Color.DarkGray
                                        ),
                                    )

//                        Text("${presetEntry.id}")
//                                Text(sprite.name)
                                }
                            }
                        }

//                        Spacer(modifier = Modifier.width(200.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(0.6f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .height(36.dp)
                                    .width(150.dp)
                            ) {
                                Image(bitmap = sprite.image, contentDescription = sprite.path)
                            }
                            Text(sprite.id.toString())
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(sprite.name, modifier = Modifier.padding(16.dp, 0.dp))
                        }

                    }
                }
            }
        }
    }
}