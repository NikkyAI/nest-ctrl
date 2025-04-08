package ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.runBlocking

@Composable
@Preview
fun Reproducer() {

    MaterialTheme {
        Scaffold {
            Row {
                Column(
                    modifier = Modifier.width(300.dp)
                ) {
                    var text by remember { mutableStateOf("2") }
                    OutlinedTextField(text, onValueChange = {})

                    val items = listOf(
                        "1", "2", "3"
                    )
                        Row {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            expanded = true
                        },
                    ) {
                        Icon(Icons.Filled.ArrowDropDown, "select")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        },
                    ) {
                        items.forEach { item ->
                            DropdownMenuItem(
                                onClick = {
                                    text = item
                                    expanded = false
                                },
                                enabled = item != text
                            ) {
                                Text(item)
                            }
                        }
                    }
                            }


                }

            }

        }

    }
}

fun main() = runBlocking {
    awaitApplication {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Nest Ctrl",
            state = rememberWindowState(width = 1600.dp, height = 1200.dp),
            icon = BitmapPainter(
                useResource("drawable/blobhai_trans.png", ::loadImageBitmap)
            )
        ) {
            Reproducer()
        }
    }
}

