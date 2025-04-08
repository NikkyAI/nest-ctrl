package ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WithTooltipAtPointer(
    tooltip: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    TooltipArea(
        tooltip = {
            Surface(
                modifier = Modifier.shadow(4.dp),
                color = MaterialTheme.colors.background,
                shape = RoundedCornerShape(4.dp),
                elevation = 2.dp
//                border = BorderStroke(width = 1.dp, Color.White)
            ) {
                Box(
                    modifier = Modifier.padding(10.dp)
                ) {
                    tooltip()
                }
            }
        },
        modifier = Modifier, //.padding(start = 40.dp),
        delayMillis = 100, // In milliseconds
        tooltipPlacement = TooltipPlacement.CursorPoint(
            alignment = Alignment.BottomEnd,
            offset = DpOffset(
                (16).dp,
                (-16).dp
            ),
        )
    ) {
        content()
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WithTooltipAbove(
    tooltip: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    TooltipArea(
        tooltip = {
            Surface(
                modifier = Modifier.shadow(4.dp),
                color = MaterialTheme.colors.background,
                shape = RoundedCornerShape(4.dp),
                elevation = 2.dp
//                border = BorderStroke(width = 1.dp, Color.White)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    tooltip()
                }
            }
        },
        modifier = Modifier, //.padding(start = 40.dp),
        delayMillis = 100, // In milliseconds
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset(0.dp, 0.dp),
        )
    ) {
        content()
    }
}