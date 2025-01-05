package ui.screens

//import Link
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
    decks: List<Deck>,
    size: Dp = 200.dp,
    strokeWidth: Dp = 32.dp
) {
    val fontDseg14 = FontFamily(
        androidx.compose.ui.text.platform.Font(
            resource = "fonts-DSEG_v046/DSEG14-Classic/DSEG14Classic-Regular.ttf",
            weight = FontWeight.W400,
            style = FontStyle.Normal
        )
    )
//    val fontDseg14 = FontFamily(
//        Font(
//            resource = Res.font.DSEG14_Classic,
//            weight = FontWeight.W400,
//            style = FontStyle.Normal
//        ),
//    )

    val beatProgress by beatProgress.collectAsState(0f)
//    val beatProgress  = 0f // 0.25f

    //TODO: display current BPM

//    val bpm by Link.bpm.collectAsState()
    val bpmRounded by bpmRoundedInt.collectAsState()
    val frame by beatFrame.collectAsState()
    val currentBeat = (beatProgress * frame).roundToInt()

    val tailSweep = 1.0f / frame * 8

    val strokeTransition = with(LocalDensity.current) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
    }
    val strokeTail = with(LocalDensity.current) {
        Stroke(width = (strokeWidth / 4).toPx(), cap = StrokeCap.Butt)
    }
    val strokeThin = with(LocalDensity.current) {
        Stroke(width = (strokeWidth / 8).toPx(), cap = StrokeCap.Butt)
    }

    val decksEnabled by Deck.enabled.collectAsState()
//    val decksSwitched =  decks.mapNotNull { deck ->
//        val enabled = (deck.id <= decksEnabled)
//
//        if (!enabled) {
//            return@mapNotNull null
//        }
//
//        val hasSwitched by deck.presetSwitching.hasSwitched.collectAsState()
//        deck.id to hasSwitched
//    }.toMap()
    val deckData = decks.mapNotNull { deck ->
        val enabled by deck.isEnabled.collectAsState()
        if(!enabled) return@mapNotNull null

        val isLocked by deck.presetSwitching.isLocked.collectAsState()
        val triggerTime by deck.presetSwitching.triggerTime.collectAsState()

        val transitionTime by deck.ndTime.transitionTime.collectAsState()
        val beatsPerSecond = 60.0f / bpmRounded.toFloat()
        val beatsInTransitionTime = transitionTime / beatsPerSecond
        val sweepAngle = 1.0f / frame * beatsInTransitionTime

        val brush = if (!isLocked)
            Brush.sweepGradient(
                0f to deck.color,
                sweepAngle*0.2f to deck.dimmedColor,
                sweepAngle to deck.color.copy(alpha = 0.25f).compositeOver(Color.Black),
            )
        else {
            Brush.sweepGradient(
                0f to deck.color,
                sweepAngle*0.2f to deck.dimmedColor,
                sweepAngle to deck.color.copy(alpha = 0.4f).compositeOver(Color.Black),
            )
        }
        Triple(brush, triggerTime, sweepAngle)
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
                fontFamily = fontDseg14,
//                modifier = Modifier.padding(8.dp, 0.dp)
            )
            val beatFrameText = frame.toString()
            Text(
                text = "BEAT: ${currentBeat.toString().padStart(beatFrameText.length, '0')} / $beatFrameText",
                fontFamily = fontDseg14,
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
                deckData.forEach { (brush, triggerTime, sweepAngle) ->
                    rotate(
                        (triggerTime * 360f )+270f,
                    ) {
                        drawArc(
                            brush = brush,
                            startAngle = 0f , //+ (1.0f / frame * 360f), //triggerTime * 360f - 90f,
                            sweepAngle = (sweepAngle * 360f) , // - (2.0f / frame * 360f),
                            useCenter = false,
                            style = strokeTransition,
                        )
                    }
                }
                drawArc(
                    color = Color.DarkGray,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = strokeThin,
                )
                rotate(
                    (beatProgress * 360f) - 90f,
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
                            1f - tailSweep to Color.Transparent,
                            1f to Color.White,
                        ),
                        startAngle = (1f-tailSweep)*360f, // 270f - (tailSweep * 360f),
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
                if (deck.id > decksEnabled) return@forEach

                val triggerTime by deck.presetSwitching.triggerTime.collectAsState()

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
                            deck.presetSwitching.triggerTime.value = it
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
                        beatFrame.value = max(16, frame + 8)
                    }
                },
//                colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Text("+8", fontFamily = fontDseg14)
            }
            Button(
                onClick = {
                    scope.launch {
                        beatFrame.value = max(16, frame - 8)
                    }
                },
//                colors = ButtonDefaults.buttonColors(backgroundColor = deck.color),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Text("-8", fontFamily = fontDseg14)
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
                Text("32", fontFamily = fontDseg14)
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
                Text("64", fontFamily = fontDseg14)
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
                Text("128", fontFamily = fontDseg14)
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