package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nestdrop.deck.Deck

@Composable
fun autoChangeScreen(vararg decks: Deck) {
    // autochange
    // next
    // current

    Column(
        modifier = Modifier
            .fillMaxWidth(0.9f),
        horizontalAlignment = Alignment.CenterHorizontally,

    ) {
        decks.forEach { deck ->
            val autoChangeState = deck.presetQueue.autoChange
            val autoChange by autoChangeState.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Preset Queue", modifier = Modifier.width(150.dp))
//                Switch(
//                    checked = autoChange,
//                    onCheckedChange = {
//                        deck.presetQueue.autoChange.value = it
//                    },
//                    colors = SwitchDefaults.colors(
//                        checkedThumbColor = deck.color,
//                        checkedTrackColor = deck.color,
//                        uncheckedThumbColor = deck.dimmedColor,
//                    ),
//                )
                Checkbox(
                    checked = autoChange,
                    onCheckedChange = {
                        autoChangeState.value = it
                    },
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = deck.dimmedColor,
                        uncheckedColor = deck.color,
                        checkedColor = deck.color,
                        disabledColor = Color.DarkGray
                    ),
                )
                Spacer(modifier = Modifier.width(50.dp))
                val name by deck.presetQueue.name.collectAsState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .background(deck.dimmedColor)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name,
                        modifier = Modifier
                            .fillMaxHeight(0.8f)
                            .fillMaxWidth(0.8f)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))
                val scope = rememberCoroutineScope()
                Button(
                    onClick = {
                        scope.launch { deck.presetQueue.next() }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
                    contentPadding = PaddingValues(),
                ) {
                    Text("Next Preset Queue")
                }
            }
        }


        decks.forEach { deck ->
            val autoChangeState = deck.preset.autoChange
            val autoChange by autoChangeState.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Preset", modifier = Modifier.width(150.dp))
//                Switch(
//                    checked = autoChange,
//                    onCheckedChange = {
//                        deck.preset.autoChange.value = it
//                    },
//                    colors = SwitchDefaults.colors(
//                        checkedThumbColor = deck.color,
//                        checkedTrackColor = deck.color,
//                        uncheckedThumbColor = deck.dimmedColor,
//                    ),
//                )
                Checkbox(
                    checked = autoChange,
                    onCheckedChange = {
                        autoChangeState.value = it
                    },
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = deck.dimmedColor,
                        uncheckedColor = deck.color,
                        checkedColor = deck.color,
                        disabledColor = Color.DarkGray
                    ),
                )

                val name by deck.preset.name.collectAsState()
                Spacer(modifier = Modifier.width(50.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .background(deck.dimmedColor)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name,
                        modifier = Modifier
                            .fillMaxHeight(0.8f)
                            .fillMaxWidth(0.8f)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                val scope = rememberCoroutineScope()
                Button(
                    onClick = {
                        scope.launch { deck.preset.next() }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
                    contentPadding = PaddingValues(),
                ) {
                    Text("Next Preset")
                }
            }
        }


        decks.forEach { deck ->

            val autoChangeState = deck.sprite.autoChange
            val autoChange by autoChangeState.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sprite", modifier = Modifier.width(150.dp))
//                Switch(
//                    checked = autoChange,
//                    onCheckedChange = {
//                        autoChangeState.value = it
//                    },
//                    colors = SwitchDefaults.colors(
//                        checkedThumbColor = deck.color,
//                        checkedTrackColor = deck.color,
//                        uncheckedThumbColor = deck.dimmedColor,
//                    ),
//                )
                Checkbox(
                    checked = autoChange,
                    onCheckedChange = {
                        deck.sprite.autoChange.value = it
                    },
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = deck.dimmedColor,
                        uncheckedColor = deck.color,
                        checkedColor = deck.color,
                        disabledColor = Color.DarkGray
                    ),
                )

                val name by deck.sprite.name.collectAsState()
                Spacer(modifier = Modifier.width(50.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .background(deck.dimmedColor)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name,
                        modifier = Modifier
                            .fillMaxHeight(0.8f)
                            .fillMaxWidth(0.8f)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                val scope = rememberCoroutineScope()
                Button(
                    onClick = {
                        scope.launch { deck.sprite.next() }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
                    contentPadding = PaddingValues(),
                ) {
                    Text("Next Sprite")
                }
            }
        }
    }
}