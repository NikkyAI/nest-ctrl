import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun fader(
    value: Float,
    color: Color,
    notches: Int = 0,
    label: Boolean = true,
    verticalText: String = "",
    updateValue: (value: Float) -> Unit,
) {
    var canvasSize = IntSize(1, 1)
    val draggableState = rememberDraggableState {
        updateValue(
            (value - (it / canvasSize.height))
                .coerceIn(0.0f, 1.0f)
        )
    }

    val textMeasurer = rememberTextMeasurer()

    val style = TextStyle(
        fontSize = 20.sp,
        color = Color.White,
    )

    val textLayoutResult = remember(verticalText, style) {
        textMeasurer.measure(verticalText, style)
    }

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .aspectRatio(0.15f, matchHeightConstraintsFirst = true)
//                .padding(canvasSize.height.dp / 16f)
                .padding(
                    top = 16.dp,
                    start = 4.dp,
                    end = 4.dp,
                    bottom = 8.dp
                )
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
//                        println("offset: $offset, density: $density")
                        updateValue(
                            1f - (offset.y / canvasSize.height).coerceIn(0.0f, 1.0f)
                        )
                    }
                }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                )
                .onSizeChanged {
                    canvasSize = it
                }
        ) {
            drawRoundRect(
                color = color.copy(alpha = 0.5f).compositeOver(Color.Black),
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = CornerRadius(20f, 20f),
            )

            val handleSize = size.height / 16f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    0f to color.copy(alpha = 0.1f),
                    0.5f to color.copy(alpha = 0.9f),
                    1f to color.copy(alpha = 0.1f),
                    startY = (size.height * (1f - value)) - (handleSize / 2),
                    endY = (size.height * (1f - value)) + (handleSize / 2)
                ),
                topLeft = Offset(0f, (size.height * (1f - value)) - (handleSize / 2)),
                size = size.copy(height = handleSize),
                cornerRadius = CornerRadius(10f, 10f),
            )

            (0 until notches).forEach { notch ->
                val start = Offset(
                    size.width * 0.15f,
                    size.height / (notches + 1) * (notch + 1)
                )
                val end = Offset(
                    size.width * 0.85f,
                    size.height / (notches + 1) * (notch + 1)
                )

                drawLine(
                    color = Color.Black,
                    start = start,
                    end = end,
                    strokeWidth = 1.0f,
                )
            }

            drawLine(
                color = Color.White,
                start = Offset(0f, size.height * (1f - value)),
                end = Offset(size.width, size.height * (1f - value)),
            )

            if (verticalText != "") {
                rotate(degrees = 90f, center) {
                    this.drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = center.x - textLayoutResult.size.width / 2,
                            y = center.y - textLayoutResult.size.height / 2,
                        )
                    )
                }
            }
        }
        if (label) {
            Text(
                text = "${(value * 100).toInt()}",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}