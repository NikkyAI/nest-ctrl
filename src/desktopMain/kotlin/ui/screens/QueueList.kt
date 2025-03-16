package ui.screens

import QUEUES
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import decks
import nestdrop.deck.Deck
import ui.components.lazyList

@Composable
fun QueueList() {
    val decksEnabled by Deck.enabled.collectAsState()
    val allQueues by QUEUES.allQueues.collectAsState()

    val colors = decks.map { it.color }

    lazyList {

        items(allQueues) { queue ->

            val color = colors[queue.deck - 1]

            Row {
                Text(queue.name)

                Button(
                    onClick = {

                    },
                ) {
                    Text("x2")
                }
            }
        }
    }

}
