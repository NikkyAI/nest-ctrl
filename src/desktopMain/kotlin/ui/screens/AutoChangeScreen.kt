package ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
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
    onNext: suspend CoroutineScope.() -> Unit
) {
    val autoChange by checkedMutableStateflow.collectAsState()
    Row(
        modifier = Modifier
//            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            modifier = Modifier
                .width(150.dp),
            textAlign = TextAlign.End
        )
        Spacer(modifier = Modifier.width(10.dp))
        val scope = rememberCoroutineScope()
        IconButton(
            onClick = {
                scope.launch { onNext() }
            },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next",
                tint = deck.color,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}


@Composable
fun autoChangeScreen(
    deck: Deck,
) {
    val decksEnabled by Deck.enabled.collectAsState()
    if (deck.N > decksEnabled) return

    // autochange
    // next
    // current

    val horizontal = Arrangement.Start

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth(0.9f)
    ) {
        Row(
            horizontalArrangement = horizontal,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            autoChangeRow(
                deck, "Preset Queue", deck.presetQueue.autoChange
            ) {
                deck.presetQueue.next()
            }
        }

        Row(
            horizontalArrangement = horizontal,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            autoChangeRow(
                deck, "Preset", deck.preset.autoChange
            ) {
                deck.preset.next()
            }
        }

        Row(
            horizontalArrangement = horizontal,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            autoChangeRow(
                deck, "Search", deck.search.autochange
            ) {
                deck.search.next()
            }
        }

        Row(
            horizontalArrangement = horizontal,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            autoChangeRow(
                deck, "IMG Sprite", deck.imgSprite.autoChange
            ) {
                deck.imgSprite.next()
            }
        }

        Row(
            horizontalArrangement = horizontal,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            autoChangeRow(
                deck, "IMG Sprite FX", deck.imgSpriteFx.autoChange
            ) {
                deck.imgSpriteFx.next()
            }
        }
    }
}