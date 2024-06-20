package ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import nestdrop.Queue
import nestdrop.deck.Deck
import ui.components.VerticalRadioButton

@Composable
fun spoutScreen(deck: Deck) {

    Column(
        modifier = Modifier
            .width(300.dp)
    ) {
        val queue: Queue? by deck.spoutQueue.collectAsState()
        val activeIndexState = deck.spout.index
        val activeIndex by activeIndexState.collectAsState()

        val presetLength = queue?.presets?.size ?: 0

        queue?.presets?.forEachIndexed { i, preset ->
//            val toggledStateflow = deck.spout.toggles.getOrNull(i) ?: return@forEachIndexed
//            val toggled by toggledStateflow.collectAsState()

            Row(
                modifier = Modifier.height(36.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                    connectBottom = i < presetLength - 1,
                )

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

                Text(
                    "FX: ${preset.effects ?: 0}",
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    preset.label,
//                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}