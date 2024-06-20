package ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import nestdrop.deck.Deck

@Composable
private fun autoChangeRow(
    deck: Deck,
    label: String,
    checkedMutableStateflow: MutableStateFlow<Boolean>,
    nameMutableStateFlow: MutableStateFlow<String>,
    nextLabel: String,
    onNext: suspend CoroutineScope.() -> Unit
) {
    val autoChange by checkedMutableStateflow.collectAsState()
    Row(
        modifier = Modifier
//            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier
                .width(150.dp),
            textAlign = TextAlign.End
        )
        Spacer(modifier = Modifier.width(10.dp))
        Checkbox(
            checked = autoChange,
            onCheckedChange = {
                checkedMutableStateflow.value = it
            },
            colors = CheckboxDefaults.colors(
                checkmarkColor = deck.dimmedColor,
                uncheckedColor = deck.color,
                checkedColor = deck.color,
                disabledColor = Color.DarkGray
            ),
        )
//        Spacer(modifier = Modifier.width(50.dp))
//        val name by nameMutableStateFlow.collectAsState()
//        Row(
//            modifier = Modifier
//                .fillMaxWidth(0.75f)
//                .background(deck.dimmedColor)
//                .padding(4.dp),
//            verticalAlignment = Alignment.CenterVertically,
//        ) {
//            Text(
//                text = name,
//                modifier = Modifier
//                    .fillMaxHeight(0.8f)
//                    .fillMaxWidth(0.8f)
//            )
//        }

        Spacer(modifier = Modifier.width(10.dp))
        val scope = rememberCoroutineScope()
        Button(
            onClick = {
                scope.launch { onNext() }
            },
            colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            Text(nextLabel)
        }
    }
}


@Composable
fun autoChangeScreen(vararg decks: Deck) {
    // autochange
    // next
    // current

    val horizontal = Arrangement.Start

    Column(
        modifier = Modifier
            .fillMaxWidth(0.9f),
        horizontalAlignment = Alignment.CenterHorizontally,

    ) {
        Row(
            horizontalArrangement = horizontal,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            decks.forEach { deck ->
                autoChangeRow(
                    deck, "Preset Queue", deck.presetQueue.autoChange, deck.presetQueue.name, "Next"
                ) {
                    deck.presetQueue.next()
                }
            }
        }


        Row(
            horizontalArrangement = horizontal,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            decks.forEach { deck ->
                autoChangeRow(
                    deck, "Preset", deck.preset.autoChange, deck.preset.name, "Next"
                ) {
                    deck.preset.next()
                }
            }
        }


        Row(
            horizontalArrangement = horizontal,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            decks.forEach { deck ->
                autoChangeRow(
                    deck, "IMG Sprite", deck.imgSprite.autoChange, deck.imgSprite.name, "Next"
                ) {
                    deck.imgSprite.next()
                }
            }
        }

        Row(
            horizontalArrangement = horizontal,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            decks.forEach { deck ->
                autoChangeRow(
                    deck, "IMG Sprite FX", deck.imgSpriteFx.autoChange, deck.imgSpriteFx.name, "Next"
                ) {
                    deck.imgSpriteFx.next()
                }
            }
        }
    }
}