package ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import customTags
import decks
import ui.components.lazyList

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun tagEditScreen(
) {

    val customTagsCollected by customTags.collectAsState()

//    val maxQueueLength = remember(decks.map { it.spriteQueue.name }) {
//        decks.maxOfOrNull {
//            it.spoutQueue.value?.presets?.size ?: 0
//        } ?: 0
//    }

    lazyList {
        stickyHeader(key = "header") {
            Row(
//            modifier = Modifier
//                .width(200.dp),
                modifier = Modifier.background(
                    MaterialTheme.colors.background
                )
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                decks.forEach { deck ->
                    val enabled by deck.enabled.collectAsState()
                    if(!enabled) return@forEach

                    var newTagName by remember { mutableStateOf(TextFieldValue("")) }

                    val newTag = if (newTagName.text.length > 3) {
                        Tag.parse(newTagName.text)
                    } else {
                        null
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,

                        modifier = Modifier
                            .weight(0.2f)
                    ) {
                        TextField(
                            value = newTagName,
                            onValueChange = { newText ->
                                newTagName = newText
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(0.6f)
                        )
                        Button(
                            onClick = {
                                if (newTag != null) {
                                    if (newTag !in customTagsCollected.keys) {
                                        customTags.value += newTag to emptySet()
                                    } else {
                                        System.err.println("prevented overriding tag $newTag")
                                    }
                                    newTagName = TextFieldValue("")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
                            modifier = Modifier
                                .weight(0.3f)
                        ) {
                            if(newTag == null) {
                                Text("Enter text")
                            } else {
                                Text("Add $newTag")
                            }
                        }
                    }
                }
            }
        }


        items(customTagsCollected.keys.toList()) { tag ->
            val entries = customTagsCollected[tag] ?: emptySet()
            Row(
                modifier = Modifier
                    .height(36.dp)
            ) {
                decks.forEach { deck ->
                    val enabled by deck.enabled.collectAsState()
                    if(!enabled) return@forEach

                    val preset by deck.preset.name.collectAsState()
                    val isTagged = preset.substringBeforeLast(".milk") in entries

                    Row(
                        modifier = Modifier
                            .weight(0.2f),
//                            .width(400.dp)
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isTagged,
                            onCheckedChange = {
                                if (it) {
                                    customTags.value += tag to entries + preset.substringBeforeLast(".milk")
                                } else {
                                    customTags.value += tag to entries - preset.substringBeforeLast(".milk")
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkmarkColor = deck.dimmedColor,
                                uncheckedColor = deck.color,
                                checkedColor = deck.color,
                                disabledColor = Color.DarkGray
                            ),
                        )
                        Text(
                            tag.label
                        )
                    }
                }
            }
        }
    }
}