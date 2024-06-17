package ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import nestdrop.NestdropControl

private fun valueToSlider(range: ClosedFloatingPointRange<Float>, value: Float) =
    (value - range.start) / (range.endInclusive - range.start)

private fun sliderToValue(range: ClosedFloatingPointRange<Float>, value: Float) =
    (value * (range.endInclusive - range.start)) + range.start

@Composable
fun fader(
    value: Float,
    color: Color,
    notches: Int = 0,
    verticalText: String = "",
    valueRange: ClosedFloatingPointRange<Float>,
    updateValue: (value: Float) -> Unit,
) {
    var canvasSize = IntSize(1, 1)
    val draggableState = rememberDraggableState {
        updateValue(
            (value - (it / canvasSize.height * (valueRange.endInclusive - valueRange.start)))
            .coerceIn(valueRange)
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

    Canvas(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .aspectRatio(0.3f, matchHeightConstraintsFirst = true)
//                .padding(canvasSize.height.dp / 16f)
            .padding(
                top = 8.dp,
                start = 2.dp,
                end = 2.dp,
                bottom = 8.dp
            )
            .pointerInput(Unit) {
                detectTapGestures { offset ->
//                        println("offset: $offset, density: $density")
                    updateValue(
                        sliderToValue(
                            valueRange,
                            1f - (offset.y / canvasSize.height)
                        ).coerceIn(valueRange)
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
            cornerRadius = CornerRadius(10f, 10f),
        )

        val handleSize = size.height / 8f
        val sliderPosY = size.height * (1f - valueToSlider(valueRange,value))


        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to color.copy(alpha = 0.4f),
                0.5f to color.copy(alpha = 1f),
                1f to color.copy(alpha = 0.24f),
                startY = sliderPosY - (handleSize / 2),
                endY = sliderPosY + (handleSize / 2)
            ),
            topLeft = Offset(0f, sliderPosY - (handleSize / 2)),
            size = size.copy(height = handleSize),
            cornerRadius = CornerRadius(10f, 10f),
        )

        drawLine(
            color = Color.White,
            start = Offset(0f, sliderPosY),
            end = Offset(size.width, sliderPosY),
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

        if (verticalText != "") {
            rotate(degrees = 90f, center) {
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = center.x - textLayoutResult.size.width / 2,
                        y = center.y - textLayoutResult.size.height / 2,
                    )
                )
            }
        }
    }
//        if (label) {
//            Text(
//                text = "${(value * 100).toInt()}",
//                modifier = Modifier
//                    .align(Alignment.CenterHorizontally)
//            )
//        }
//    }
}


@Composable
fun NestdropControl.SliderWithResetButton.asFader(color: Color) {
    val value by collectAsState()
    Column {
        val scope = rememberCoroutineScope()
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            fader(value, color, notches = 9, verticalText = propertyName, valueRange = range) {
                this@asFader.value = it
            }
        }
        Button(
            onClick = {
                scope.launch {
                    doReset()
                }
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = color
            ),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
//                .height(24.dp),
//            contentPadding = PaddingValues(4.dp, 0.dp)
        ) {
            Text(
                text = "%5.2f".format(value),
            )
        }
    }
}