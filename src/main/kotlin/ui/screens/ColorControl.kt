package ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import nestdrop.deck.Deck
import ui.components.asFader

@Composable
fun ColorControl(deck: Deck) {
    Row(
        modifier = Modifier
            .height(200.dp)
            .padding(16.dp, 0.dp)
//            .defaultMinSize(300.dp)
//            .width(300.dp)
    ) {
        deck.ndColor.brightness.asFader(Color(deck.hexColor))
        deck.ndColor.contrast.asFader(Color(deck.hexColor))
        deck.ndColor.gamma.asFader(Color(deck.hexColor))
    }
}