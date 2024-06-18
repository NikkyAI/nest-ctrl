package ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import nestdrop.deck.Deck
import kotlin.math.roundToInt

@Composable
fun beatProgressScreen(
    vararg decks: Deck,
    size: Dp = 200.dp,
    strokeWidth: Dp = 32.dp
) {
    val beatProgress by beatProgress.collectAsState(0f)
//    val beatProgress  = 0.25f

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
    val beatFrame by beatFrame.collectAsState()
    val currentBeat = (beatProgress * beatFrame).roundToInt()

    val tailSweepAngle = 1.0f / beatFrame * 8 * 360f


    val stroke = with(LocalDensity.current) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
    }
    val strokeTail = with(LocalDensity.current) {
        Stroke(width = (strokeWidth / 4).toPx(), cap = StrokeCap.Round)
    }
    val strokeThin = with(LocalDensity.current) {
        Stroke(width = (strokeWidth / 8).toPx(), cap = StrokeCap.Round)
    }

    val triggerTimes = decks.map { deck ->
        val triggerTime by deck.triggerTime.collectAsState()

        deck.color to triggerTime
    }

    Column(
        modifier = Modifier
            .padding(16.dp, 8.dp)
    ) {

        Text(
            text = "BPM: $bpmRounded",
            fontFamily = fontFamily,
        )
        val beatFrameText = beatFrame.toString()
        Text(
            text = "BEAT: ${currentBeat.toString().padStart(beatFrameText.length, '0')} / $beatFrameText",
            fontFamily = fontFamily,
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
                .padding(strokeWidth / 2 + 16.dp)
        ) {
            drawArc(
                color = Color.DarkGray,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = strokeThin,
            )
            drawArc(
                color = Color.White,
                startAngle = beatProgress * 360f - 90f - 2f,
                sweepAngle = 2f,
                useCenter = false,
                style = stroke,
            )
            drawArc(
                color = Color.White,
                startAngle = beatProgress * 360f - 90f - tailSweepAngle - 2f,
                sweepAngle = tailSweepAngle,
                useCenter = false,
                style = strokeTail,
            )
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

            //TODO: display trigger points as lines

        }

    }
}