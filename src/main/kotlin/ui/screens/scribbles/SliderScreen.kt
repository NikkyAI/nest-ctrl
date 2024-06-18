package ui.screens.scribbles

import VerticalSlider
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Composable
fun SliderScreen(sliderMutableFlow: MutableStateFlow<Float>) {

//    val sliderValue by sliderMutableFlow.collectAsState()
//    val sliderText by sliderMutableFlow.map {
//        it.toString()
//    }.collectAsState("")
//
//    Column {
//        Text(
//            sliderText
//        )
//
//        VerticalSlider(
//            value = sliderValue,
//            onValueChange = {
//                sliderMutableFlow.value = it
//            },
//            valueRange = 0.0f .. 100.0f,
//            steps = 10,
////            modifier = Modifier
////                .width(200.dp)
////                .height(50.dp)
////                .background(Color(0xffdedede))
//        )
//        Spacer(modifier = Modifier.height(50.dp))
//        Slider(
//            sliderValue,
//            onValueChange = {
//                sliderMutableFlow.value = it
//            },
//            valueRange = 0.0f .. 100.0f,
//            steps = 10,
//            modifier = Modifier
////                        .rotate(90f),
//        )
//    }
    Column {
        Row {
            FaderVertical(sliderMutableFlow, 0.0f..100.0f)
            FaderVertical(sliderMutableFlow, 0.0f..100.0f)
            FaderVertical(sliderMutableFlow, 0.0f..100.0f)

            Column {
                FaderHorizontal(sliderMutableFlow, 0.0f..100.0f)
                FaderHorizontal(sliderMutableFlow, 0.0f..100.0f)
                FaderHorizontal(sliderMutableFlow, 0.0f..100.0f)
            }
        }
    }
}

@Composable
fun FaderVertical(
    mutableStateflow: MutableStateFlow<Float>,
    valueRange: ClosedFloatingPointRange<Float> = 0.0f..1.0f,
    steps: Int = 0,
) {
    FaderVertical(
        flow = mutableStateflow,
        onValueChange = { mutableStateflow.value = it },
        valueRange = valueRange,
        steps = steps,
    )
}

@Composable
fun FaderVertical(
    flow: Flow<Float>,
    onValueChange: (valuye: Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0.0f..1.0f,
    steps: Int = 0,
) {
    val sliderValue by flow.collectAsState(0.0f)
    val sliderText by flow.map {
        "%5.2f".format(it)
    }.collectAsState("")
    Column {
        Text(
            text = sliderText,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(10.dp))
        VerticalSlider(
            value = sliderValue,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
//            modifier = Modifier
//                .width(200.dp)
//                .height(50.dp)
//                .background(Color(0xffdedede))
        )
    }
}

@Composable
fun FaderHorizontal(
    mutableStateflow: MutableStateFlow<Float>,
    valueRange: ClosedFloatingPointRange<Float> = 0.0f..1.0f,
    steps: Int = 0,
) {
    FaderHorizontal(
        flow = mutableStateflow,
        onValueChange = { mutableStateflow.value = it },
        valueRange = valueRange,
        steps = steps,
    )
}

@Composable
fun FaderHorizontal(
    flow: Flow<Float>,
    onValueChange: (valuye: Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0.0f..1.0f,
    steps: Int = 0,
) {
    val sliderValue by flow.collectAsState(0.0f)
    val sliderText by flow.map {
        "%5.2f".format(it)
    }.collectAsState("")
    Row {
        Text(
            text = sliderText,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Slider(
            value = sliderValue,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
//            modifier = Modifier
//                .width(200.dp)
//                .height(50.dp)
//                .background(Color(0xffdedede))
        )
    }
}