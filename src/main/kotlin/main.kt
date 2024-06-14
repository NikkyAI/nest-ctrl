import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    val sliderMutableFlow = MutableStateFlow<Float>(0.0f)


    GlobalScope.launch {
        while (true) {
            delay(100)
            val v = sliderMutableFlow.value
//            println(v)
            if (v > 0) {
                sliderMutableFlow.value -= 0.1f
            } else {
                sliderMutableFlow.value = 0.0f
            }
        }
    }

    MaterialTheme(colors = darkColors()) {
        Scaffold {
            var tabIndex by remember { mutableStateOf(0) }
            val tabs = listOf("1", "2", "3")
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = tabIndex
                ) {
                    tabs.forEachIndexed { index, tabTitle ->
                        Tab(
                            text = { Text(tabTitle) },
                            selected = tabIndex == index,
                            onClick = { tabIndex = index }
                        )
                    }
                }
                when (tabIndex) {
                    0 -> {
                        var value1 by remember {
                            mutableStateOf(0.15f)
                        }
                        var value2 by remember {
                            mutableStateOf(0.85f)
                        }
                        Row {
                            fader(
                                value = value1,
                                notches = 9,
                                color = Color.Red,
                                verticalText = "Red Fader",
                            ) {
                                value1 = it
                            }

                            fader(
                                value = value2,
                                notches = 19,
                                color = Color.Green,
                                verticalText = "Green Fader",
                            ) {
                                value2 = it
                            }
                        }

                    }

                    1 -> {
                        ButtonScreen()
                    }

                    2 -> {
                        SliderScreen(sliderMutableFlow)
                    }
                }
            }
        }

    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ButtonScreen() {
    var text by remember { mutableStateOf("Hello, World!") }

    var toggle by remember {
        mutableStateOf(false)
    }

    Button(
        onClick = { toggle = !toggle },
        modifier = Modifier
            .testTag("button"),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (toggle) {
                Color.Red
            } else {
                Color.Red.copy(alpha = 0.5f).compositeOver(Color.Black)
            }
        )
//        enabled = toggle
    ) {
        Text(text)
    }

    // radio buttons
    FlowColumn(
        verticalArrangement = Arrangement.Top,
//        horizontalAlignment = Alignment.CenterHorizontally,
        maxItemsInEachColumn = 10,
//        modifier = Modifier.width(300.dp)
    ) {
        var selected by remember {
            mutableStateOf(-1)
        }

        (0 until 30).forEach {
            Button(
                onClick = {
                    selected = if (selected == it) {
                        -1
                    } else {
                        it
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (selected == it) {
                        Color.Red
                    } else {
                        Color.Red.copy(alpha = 0.5f).compositeOver(Color.Black)
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected == it) {
                        Color.White
                    } else {
                        Color.Black
                    }
                ),
                contentPadding = PaddingValues(8.dp, 0.dp),
                modifier = Modifier
//                    .fillMaxWidth(0.9f)
//                    .defaultMinSize(minHeight = 10.dp)
                    .height(24.dp),
            ) {
                Text("select $it")
            }
        }

    }

}

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

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
//        fader(
//            notches = 9,
//            color = Color.Red,
//        )
    }
}
