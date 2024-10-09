package ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Warning
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
    onNext: suspend CoroutineScope.() -> Unit,
    onWarn: (suspend CoroutineScope.() -> Unit)? = null,
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
        if(onWarn != null) {
            IconButton(
                onClick = {
                    scope.launch {
                        onWarn()
                        onNext()
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = "NextWarn",
                    tint = deck.color,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}


@Composable
fun autoChangeScreen(
    deck: Deck,
) {
    val decksEnabled by Deck.enabled.collectAsState()
    if (deck.id > decksEnabled) return

    // autochange
    // next
    // current

    val horizontal = Arrangement.Start

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
    ) {

        Row(
            horizontalArrangement = horizontal,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            autoChangeRow(
                deck = deck, label = "Preset", checkedMutableStateflow = deck.search.autoChange,
                onNext = {
                    deck.search.next()
                },
                onWarn = {
                    deck.presetSwitching.warn()
                }
            )
        }

        Row(
            horizontalArrangement = horizontal,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            autoChangeRow(
                deck = deck, label = "IMG Sprite", checkedMutableStateflow = deck.imgSprite.autoChange,
                onNext = {
                    deck.imgSprite.next()
                },
                onWarn = null,
            )
        }

        Row(
            horizontalArrangement = horizontal,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            autoChangeRow(
                deck = deck, label = "IMG Sprite FX", checkedMutableStateflow = deck.imgSpriteFx.autoChange,
                onNext = {
                    deck.imgSpriteFx.next()
                },
                onWarn = null,
            )
        }
    }
}