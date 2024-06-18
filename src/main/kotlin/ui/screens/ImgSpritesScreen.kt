package ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import nestdrop.Queue
import nestdrop.deck.Deck

@Composable
fun imgSpritesScreen(deck: Deck) {
    Column(
        modifier = Modifier
            .defaultMinSize(300.dp)
            .width(400.dp)
    ) {
        val spriteQueue: Queue? by deck.spriteQueue.collectAsState()
        val activeIndex by deck.sprite.index.collectAsState()
//        if(spriteQueue.index > -1) {
            spriteQueue?.presets?.forEachIndexed { i, preset ->
                val toggledStateflow = deck.sprite.toggles.getOrNull(i) ?: return@forEachIndexed
                val toggled by toggledStateflow.collectAsState()

                Row(modifier = Modifier.height(36.dp)) {
                    RadioButton(
                        selected = (activeIndex == i),
                        onClick = {
                            deck.sprite.index.value = i
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = deck.color,
                            unselectedColor = deck.dimmedColor
                        ),
//                        enabled = (deckNumber == deck.N)
                    )

//                    Switch(
//                        checked = toggled, onCheckedChange = {
//                            toggledStateflow.value = it
//                        },
//                        colors = SwitchDefaults.colors(
//                            checkedThumbColor = deck.color,
//                            checkedTrackColor = deck.color,
//                            uncheckedThumbColor = deck.dimmedColor,
//                        ),
//                    )

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
//        } else {
//            Text("sprite queue unset")
//        }
    }
}