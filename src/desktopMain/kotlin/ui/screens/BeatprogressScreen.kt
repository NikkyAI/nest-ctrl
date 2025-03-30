package ui.screens

//import Link
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import beatFrame
import beatProgress
import bpmInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nestdrop.deck.Deck
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalLayoutApi::class)
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
//        androidx.compose.ui.text.platform.Font(
//            resource = "fonts-DSEG_v046/DSEG14-Modern/DSEG14Modern-Regular.ttf",
//            weight = FontWeight.W400,
//            style = FontStyle.Normal
//        )
//    )
//    val fontDseg14Mini = FontFamily(
//        androidx.compose.ui.text.platform.Font(
//            resource = "fonts-DSEG_v046/DSEG14-Classic-MINI/DSEG14ClassicMini-Regular.ttf",
//            weight = FontWeight.W400,
//            style = FontStyle.Normal
//        )
//    )
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
    val bpmRounded by bpmInt.collectAsState()
    val secondsPerBeat = 60.0f / bpmRounded.toFloat()
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
        if (!enabled) return@mapNotNull null

        val isLocked by deck.presetSwitching.isLocked.collectAsState()
        val triggerTime by deck.presetSwitching.triggerTime.collectAsState()

        val transitionTime by deck.ndTime.transitionTime.collectAsState()
        val beatsInTransitionTime = max(0.2f, transitionTime) / secondsPerBeat
        val sweepAngle = 1.0f / frame * beatsInTransitionTime


        //        val luminance = deck.color.luminance()
        fun Color.atLuminance(target: Float = 1f): Color {
            val currentLuminance = luminance()
            return copy(alpha = (target / currentLuminance))
        }

        val normalizedColor = deck.color.copy(alpha = 1 / deck.color.luminance())

        val steps = 8
        val brush = if (!isLocked) {
            Brush.sweepGradient(
                *List(steps + 1) { i ->
                    sweepAngle * (1f / steps) * i to ((i.toFloat() / steps) - 1f).pow(2) * 0.5f + 0.1f
                }
//                    .also { it ->
//                        println("a brushSteps: ${it.map { it.second }}")
//                    }
                    .map { (angle, alpha) ->
                        angle to deck.color.copy(alpha = alpha)
                    }.toTypedArray(),
                0f to deck.color.copy(alpha = .1f)
            )
        } else {
            Brush.sweepGradient(
                *List(steps + 1) { i ->
                    sweepAngle * (1f / steps) * i to ((i.toFloat() / steps) - 1f).pow(2) * 0.8f + 0.2f
                }
//                    .also { it ->
//                        println("b brushSteps: ${it.map { it.second }}")
//                    }
                    .map { (angle, alpha) ->
                        angle to deck.color.copy(alpha = alpha)
                    }.toTypedArray(),
                0f to deck.color.copy(alpha = .1f)
            )
        }
        Triple(brush, triggerTime, sweepAngle)
    }

    Column {
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
                            (triggerTime * 360f) + 270f,
                        ) {
                            drawArc(
                                brush = brush,
                                startAngle = 0f, //+ (1.0f / frame * 360f), //triggerTime * 360f - 90f,
                                sweepAngle = (sweepAngle * 360f), // - (2.0f / frame * 360f),
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
                        drawArc(
                            brush = Brush.sweepGradient(
                                1f - tailSweep to Color.Transparent,
                                1f to Color.White,
                            ),
                            startAngle = (1f - tailSweep) * 360f, // 270f - (tailSweep * 360f),
                            sweepAngle = (tailSweep * 360f),
                            useCenter = false,
                            style = strokeTail,
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

        Column(
            modifier = Modifier
//                    .padding(right = 8.dp)
        ) {
            decks.forEach { deck ->
                if (deck.id > decksEnabled) return@forEach

                val triggerTime by deck.presetSwitching.triggerTime.collectAsState()
                var tmpTriggerTime by remember(deck.id, "presetSwitching.triggerTime", triggerTime) {
                    mutableStateOf(triggerTime)
                }

                Row(
                    modifier = Modifier
                        .height(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "%3.2f".format(triggerTime),
                        textAlign = TextAlign.Center,
                    )
                    Slider(
                        value = triggerTime,
                        onValueChange = {
                            tmpTriggerTime = it
                        },
                        onValueChangeFinished = {
                            deck.presetSwitching.triggerTime.value = tmpTriggerTime
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = deck.color,
                            activeTrackColor = deck.dimmedColor,
                        ),
                        valueRange = (0f..1f),
//                            steps = 8 - 1,
                    )
                }
            }
            val maxRange = min(1.0f, 30f / (frame * secondsPerBeat))
            Row(
//                        modifier = Modifier.width(200.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Transition Time")
                Spacer(Modifier.weight(1.0f))
                Text(
                    text = "max: %3.2f".format(maxRange),
                    textAlign = TextAlign.Center,
                )
            }
            decks.forEach { deck ->
                if (deck.id > decksEnabled) return@forEach

                val transitTime by deck.ndTime.transitionTime.collectAsState()

                val transitTimeSync by deck.presetSwitching.transitTimeSync.collectAsState()
                val transitTimeBeats by deck.presetSwitching.transitTimeBeats.collectAsState()
                Row(
//                        modifier = Modifier.width(200.dp),
                    verticalAlignment = Alignment.Top,
                ) {

                    Column(
                        modifier = Modifier
                            .width(50.dp)
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = transitTimeBeats.toString(),
                            fontFamily = fontDseg14,
                            textAlign = TextAlign.Right,
                            color = deck.color,
                        )
                        Text(
                            text = "%3.1fs".format(transitTime),
                            textAlign = TextAlign.Right,
                        )
//                        Checkbox(
//                            checked = transitTimeSync,
//                            onCheckedChange = { isChecked ->
//                                println("setting transitTimeSync $isChecked")
//                                deck.presetSwitching.transitTimeSync.value = isChecked
//                            },
//                            colors = CheckboxDefaults.colors(
//                                checkmarkColor = deck.dimmedColor,
//                                uncheckedColor = deck.color,
//                                checkedColor = deck.color,
//                                disabledColor = Color.DarkGray
//                            ),
//
//
////                            modifier = Modifier.fillMaxWidth(0.1f)
//                        )
                    }

                    FlowRow(
                        modifier = Modifier
//                            .width(size)
//                            .fillMaxWidth(.9f)
                            .fillMaxWidth()
                    ) {
                        val scope = rememberCoroutineScope()
                        listOf<Pair<String, suspend CoroutineScope.(current: Int) -> Int>>(
//                            "*2" to {
//                                deck.presetSwitching.transitTimeBeats.value *= 2
//                            },
//                            "/2" to {
//                                deck.presetSwitching.transitTimeBeats.value /= 2
//                            },
                            "+1" to {
                                it + 1
                            },
                            "-1" to {
                                it - 1
                            },
                            "+4" to {
                                it + 4
                            },
                            "-4" to {
                                it - 4
                            },
                            "04" to {
                                4
                            },
                            "08" to {
                                8
                            },
                            "12" to {
                                12
                            },
                            "16" to {
                                16
                            },
                            "32" to {
                                32
                            },
                        ).forEachIndexed { i, (label, func) ->
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        deck.presetSwitching.transitTimeBeats.value = func(
                                            deck.presetSwitching.transitTimeBeats.value
                                        ).coerceAtLeast(0)
                                    }
                                },
                                enabled = transitTimeSync,
                                contentPadding = PaddingValues(4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = deck.color,
                                    disabledContentColor = deck.disabledColor,
//                                    backgroundColor = if(activeButton ==i) {
//                                        deck.disabledColor
//                                    } else {
//                                        androidx.compose.material.MaterialTheme.colors.surface
//                                    }
                                ),
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier
                                    .height(24.dp)
                                    .padding(0.dp)
                            ) {
                                Text(label, fontFamily = fontDseg14)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    deck.presetSwitching.transitTimeSync.value = !transitTimeSync
                                }
                            },

                            contentPadding = PaddingValues(4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = deck.color,

                                backgroundColor = if (transitTimeSync) {
                                    deck.disabledColor
                                } else {
                                    androidx.compose.material.MaterialTheme.colors.surface
                                }
                            ),
                            modifier = Modifier
                                .height(24.dp)
                                .padding(0.dp)
                        ) {
                            if (transitTimeSync) {
                                Text("ON", fontFamily = fontDseg14)
                            } else {
                                Text("OFF", fontFamily = fontDseg14)
                            }
                        }

                    }
//                    Slider(
//                        value = beatTimeFraction,
//                        onValueChange = {
//                            deck.presetSwitching.transitTimeBeatframeFraction.value = (it * 100).roundToInt() / 100f
//                        },
//                        colors = SliderDefaults.colors(
//                            thumbColor = deck.color,
//                            activeTrackColor = deck.dimmedColor,
//                        ),
////                            valueRange = (0f..maxRange),
//                        valueRange = (0f..1f),
////                            steps = 15,
//                        enabled = enabledTransitTimeSync,
//                        modifier = Modifier.fillMaxWidth(0.8f)
//                    )
                }
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