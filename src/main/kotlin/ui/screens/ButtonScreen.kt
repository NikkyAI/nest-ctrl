package ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ButtonScreen() {
    var text by remember { mutableStateOf("Hello, World!") }

    var toggle by remember {
        mutableStateOf(false)
    }

    Button(
        onClick = { toggle = !toggle },
        modifier = Modifier
            .testTag("button"),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (toggle) {
                Color.Red
            } else {
                Color.Red.copy(alpha = 0.5f).compositeOver(Color.Black)
            }
        )
//        enabled = toggle
    ) {
        Text(text)
    }

    // radio buttons
    FlowColumn(
        verticalArrangement = Arrangement.Top,
//        horizontalAlignment = Alignment.CenterHorizontally,
        maxItemsInEachColumn = 10,
//        modifier = Modifier.width(300.dp)
    ) {
        var selected by remember {
            mutableStateOf(-1)
        }

        (0 until 30).forEach {
            Button(
                onClick = {
                    selected = if (selected == it) {
                        -1
                    } else {
                        it
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (selected == it) {
                        Color.Red
                    } else {
                        Color.Red.copy(alpha = 0.5f).compositeOver(Color.Black)
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected == it) {
                        Color.White
                    } else {
                        Color.Black
                    }
                ),
                contentPadding = PaddingValues(8.dp, 0.dp),
                modifier = Modifier
//                    .fillMaxWidth(0.9f)
//                    .defaultMinSize(minHeight = 10.dp)
                    .height(24.dp),
            ) {
                Text("select $it")
            }
        }

    }

}