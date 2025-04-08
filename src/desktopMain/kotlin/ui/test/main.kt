package ui.test

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalFoundationApi::class)
fun main() = singleWindowApplication(
    WindowState(width = 300.dp, height = 350.dp),
    title = "Tooltip Example"
) {
    val buttons = listOf("Button A", "Button B", "Button C", "Button D", "Button E", "Button F")
    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
        buttons.forEachIndexed { index, name ->
            // Wrap the button in TooltipArea
            if (index == 0) {
                Button(onClick = {}) { Text(text = name) }
            } else {
                Box(modifier = Modifier.background(Color.Red)) {
                    TooltipArea(
                        tooltip = {
                            // Composable tooltip content:
                            Surface(
                                modifier = Modifier.shadow(4.dp),
                                color = Color(255, 255, 210),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Tooltip for $name",
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        },
                        modifier = Modifier, //.padding(start = 40.dp),
                        delayMillis = 600, // In milliseconds
                        tooltipPlacement = TooltipPlacement.CursorPoint(
                            alignment = Alignment.BottomEnd,
                            offset = if (index % 2 == 0) DpOffset(
                                (-16).dp,
                                0.dp
                            ) else DpOffset.Zero // Tooltip offset
                        )
                    ) {
                        Button(onClick = {}) { Text(text = name) }
                    }
                }
            }
        }
    }
}