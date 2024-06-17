package ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nestdrop.deck.Deck

@Composable
fun autoChangeScreen(vararg decks: Deck) {
    // autochange
    // next
    // current

    Column {
        decks.forEach { deck ->
            val autoChange by deck.presetQueue.autoChange.collectAsState()
            Row {
                Text("Preset Queue", modifier = Modifier.width(150.dp))
                Switch(
                    checked = autoChange,
                    onCheckedChange = {
                        deck.presetQueue.autoChange.value = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = deck.color,
                        checkedTrackColor = deck.color,
                        uncheckedThumbColor = deck.dimmedColor,
                    ),
                )
                Spacer(modifier = Modifier.width(50.dp))
                val scope = rememberCoroutineScope()
                Button(
                    onClick = {
                        scope.launch { deck.presetQueue.next() }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = deck.color)
                ) {
                    Text("Next Preset Queue")
                }
            }
        }


        decks.forEach { deck ->
            val autoChange by deck.preset.autoChange.collectAsState()
            Row {
                Text("Preset", modifier = Modifier.width(150.dp))
                Switch(
                    checked = autoChange,
                    onCheckedChange = {
                        deck.preset.autoChange.value = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = deck.color,
                        checkedTrackColor = deck.color,
                        uncheckedThumbColor = deck.dimmedColor,
                    ),
                )
                Spacer(modifier = Modifier.width(50.dp))
                val scope = rememberCoroutineScope()
                Button(
                    onClick = {
                        scope.launch { deck.preset.next() }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = deck.color)
                ) {
                    Text("Next Preset")
                }
            }
        }
    }
}