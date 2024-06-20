package ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import beatFrame
import beatProgress
import bpmRoundedInt
import kotlinx.coroutines.launch
import nestdrop.deck.Deck
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun beatProgressScreen(
    vararg decks: Deck,
    size: Dp = 200.dp,
    strokeWidth: Dp = 32.dp
) {
    val beatProgress by beatProgress.collectAsState(0f)
//    val beatProgress  = 0f // 0.25f

    //TODO: display current BPM

    val fontFamily = FontFamily(
        Font(
            resource = "fonts-DSEG_v046/DSEG14-Classic/DSEG14Classic-Regular.ttf",
            weight = FontWeight.W400,
            style = FontStyle.Normal
        )
    )

    val bpm by Link.bpm.collectAsState()
    val bpmRounded by bpmRoundedInt.collectAsState()
    val frame by beatFrame.collectAsState()
    val currentBeat = (beatProgress * frame).roundToInt()

    val tailSweep = 1.0f / frame * 16


    val stroke = with(LocalDensity.current) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
    }
    val strokeTail = with(LocalDensity.current) {
        Stroke(width = (strokeWidth / 4).toPx(), cap = StrokeCap.Butt)
    }
    val strokeThin = with(LocalDensity.current) {
        Stroke(width = (strokeWidth / 8).toPx(), cap = StrokeCap.Butt)
    }

    val triggerTimes = decks.map { deck ->
        val triggerTime by deck.triggerTime.collectAsState()

        deck.color to triggerTime
    }

    Row(
        modifier = Modifier
//            .padding(16.dp, 8.dp)
    ) {

        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
        ) {

            Text(
                text = "BPM: $bpmRounded",
                fontFamily = fontFamily,
//                modifier = Modifier.padding(8.dp, 0.dp)
            )
            val beatFrameText = frame.toString()
            Text(
                text = "BEAT: ${currentBeat.toString().padStart(beatFrameText.length, '0')} / $beatFrameText",
                fontFamily = fontFamily,
//                modifier = Modifier.padding(8.dp, 0.dp)
            )
//        Text(
//            text = "$beatProgress",
//            fontFamily = fontFamily,
//            textAlign = TextAlign.Center
//        )

            Canvas(
                modifier = Modifier
                    .size(size)
                    .aspectRatio(1f)
//                .rotate(
//                    beatProgress * 360f + 135f
//                )
                    .padding(strokeWidth / 2)
            ) {
                drawArc(
                    color = Color.DarkGray,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = strokeThin,
                )
                rotate(
                    beatProgress * 360f,
                ) {
//                drawArc(
//                    color = Color.White,
//                    startAngle = - 90f - 2f,
//                    sweepAngle = 2f,
//                    useCenter = false,
//                    style = stroke,
//                )
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.75f - tailSweep to Color.Transparent,
                            0.75f to Color.White,
                        ),
                        startAngle = 270f - (tailSweep * 360f),
                        sweepAngle = (tailSweep * 360f),
                        useCenter = false,
                        style = strokeTail,
                    )
                }
//            drawArc(
//                brush = Brush.sweepGradient( // !!! that what
//                    0f to Color.Transparent,
//                    0.3f to Color.White,
//                    1f to Color.Transparent,
//                ),
//                startAngle = 0f,
//                sweepAngle = 120f,
//                useCenter = false,
//                style = stroke,
//            )

                triggerTimes.forEach { (color, triggerTime) ->

                    drawArc(
                        color = color,
                        startAngle = triggerTime * 360f - 90f - 2f,
                        sweepAngle = 2f,
                        useCenter = false,
                        style = stroke,
                    )
                }

                (0f..1f step (1.0f / frame)).forEach { beat ->
                    drawArc(
                        color = Color.Black,
                        startAngle = beat * 360f - 0.5f,
                        sweepAngle = 1f,
                        useCenter = false,
                        style = strokeThin,
                    )
                }

            }

            decks.forEach { deck ->
                val triggerTime by deck.triggerTime.collectAsState()

                Row(
                    modifier = Modifier.width(200.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
//                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "%3.2f".format(triggerTime),
                        textAlign = TextAlign.Center,
                    )
                    Slider(
                        value = triggerTime,
                        onValueChange = {
                            deck.triggerTime.value = it
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = deck.color,
                            activeTrackColor = deck.dimmedColor,
                        ),
                        valueRange = (0f..1f),
                        steps = 7,
                    )
                }
            }

        }

        Column(
            modifier = Modifier
                .padding(end = 16.dp)
        ) {
            val scope = rememberCoroutineScope()

            Button(
                onClick = {
                    scope.launch {
                        beatFrame.value = max(8, frame + 8)
                    }
                },
//                colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Text("+8", fontFamily = fontFamily)
            }
            Button(
                onClick = {
                    scope.launch {
                        beatFrame.value = max(8, frame - 8)
                    }
                },
//                colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Text("-8", fontFamily = fontFamily)
            }
            Button(
                onClick = {
                    scope.launch {
                        beatFrame.value = 32
                    }
                },
//                colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Text("32", fontFamily = fontFamily)
            }
            Button(
                onClick = {
                    scope.launch {
                        beatFrame.value = 64
                    }
                },
//                colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Text("64", fontFamily = fontFamily)
            }
            Button(
                onClick = {
                    scope.launch {
                        beatFrame.value = 128
                    }
                },
//                colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Text("128", fontFamily = fontFamily)
            }
        }
    }
}

infix fun ClosedRange<Float>.step(step: Float): Iterable<Float> {
    require(start.isFinite())
    require(endInclusive.isFinite())
    require(step > 0.0) { "Step must be positive, was: $step." }
    val sequence = generateSequence(start) { previous ->
        if (previous == Float.POSITIVE_INFINITY) return@generateSequence null
        val next = previous + step
        if (next > endInclusive) null else next
    }
    return sequence.asIterable()
}