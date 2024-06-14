import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
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
            delay(10)
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
            val tabs = listOf("1", "2")
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
                        ButtonScreen()
                    }

                    1 -> {
                        SliderScreen(sliderMutableFlow)
                    }
                }
            }
        }

    }
}

@Composable
fun ButtonScreen() {
    var text by remember { mutableStateOf("Hello, World!") }

    Button(
        onClick = { text = "Hello, Desktop!" },
        modifier = Modifier.testTag("button")
    ) {
        Text(text)
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
    steps: Int = 0
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
    steps: Int = 0
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
    steps: Int = 0
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
    steps: Int = 0
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
    }
}
