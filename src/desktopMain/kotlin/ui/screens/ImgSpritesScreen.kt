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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandCircleDown
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import decks
import imgSpritesMap
import kotlinx.coroutines.flow.MutableStateFlow
import nestdrop.deck.Deck
import ui.components.VerticalRadioButton
import ui.components.lazyList

val spritesSectionOpenedState = MutableStateFlow<Map<String, Boolean>>(emptyMap())

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun imgSpritesScreenNew() {
    val decksEnabled by Deck.enabled.collectAsState()
    val spriteStuff = decks.associate { deck ->
        val current by deck.imgSprite.spriteImgLocation.collectAsState()
        val enabledSprites by deck.imgSprite.toggles.collectAsState()
        deck.id to (current to enabledSprites)
    }//.withDefault { false }

    val spritesMap by imgSpritesMap.collectAsState()
    val spriteLocations = spritesMap.values.sortedBy {
        it.id
    }

    val maxQueueLength = spriteLocations.size

    val groupedByCategories = spriteLocations.groupBy { it.categoryPath }
    val spritesSectionOpened by spritesSectionOpenedState.collectAsState()

    Column {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colors.background)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            decks.forEach { deck ->
                if (deck.id > decksEnabled) return@forEach

//                val current by deck.imgSprite.name.collectAsState()
                val imgStates by deck.spriteState.imgStates.collectAsState() // .name.collectAsState()
                val enabledImgSprites = imgStates.values
                val current = enabledImgSprites.joinToString(" | ", "[ ", " ]") {
                    it.name + " FX: " + it.fx
                }

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
            groupedByCategories.forEach { (categoryPath, sprites) ->
                val sectionKey = categoryPath.joinToString("/")
                val isOpened = spritesSectionOpened[sectionKey] ?: true
                stickyHeader(key = sectionKey) {
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
                        IconButton(
                            onClick = {
                                spritesSectionOpenedState.value += (sectionKey to !isOpened)
                            }
                        ) {
                            if (isOpened) {
                                Icon(Icons.Outlined.ExpandCircleDown, "expand")
                            } else {
                                Icon(Icons.Outlined.RemoveCircleOutline, "expand")
                            }
                        }
                        Checkbox(
                            checked = isOpened,
                            onCheckedChange = { newValue ->
                                spritesSectionOpenedState.value += (sectionKey to newValue)
                            },
                            colors = CheckboxDefaults.colors()
                        )
                        categoryPath.forEachIndexed() { i, pathFragment ->
                            if (i > 0) {
                                Text(" > ", modifier = Modifier.padding(16.dp))
                            }
                            Text(pathFragment, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
                if (isOpened) {
                    items(sprites) { sprite ->
                        val localDensity = LocalDensity.current
                        var heightDp by remember {
                            mutableStateOf(0.dp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .onGloballyPositioned { coordinates ->
                                    // Set column height using the LayoutCoordinates
                                    heightDp = with(localDensity) { coordinates.size.height.toDp() }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
//                                .weight(0.4f)
                            ) {
                                decks.forEach { deck ->
                                    if (deck.id > decksEnabled) return@forEach

                                    val (current, enabledSprites) = spriteStuff.getValue(deck.id)
//                        val current by deck.imgSprite.spriteImgLocation.collectAsState()
//                        val enabledSprites by deck.imgSprite.enabledSprites.collectAsState()

//                        val image = remember { imageFromFile(spritesFolder.resolve(sprite.path)) }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
//                                        .weight(0.1f)
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
                                            height = heightDp,
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

//                        Spacer(modifier = Modifier.width(40.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(horizontal = 50.dp)
//                                .weight(0.6f)
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
}