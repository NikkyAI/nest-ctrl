package nestdrop

import flowScope
import io.klogging.logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import osc.OSCMessage
import osc.nestdropSendChannel
import kotlin.math.roundToInt

sealed interface NestdropControl {
    companion object {
        private val logger = logger(NestdropControl::class.qualifiedName!!)

        fun valueToSlider(range: ClosedFloatingPointRange<Float>, value: Float) =
            (value - range.start) / (range.endInclusive - range.start)

        fun sliderToValue(range: ClosedFloatingPointRange<Float>, value: Float) =
            (value * (range.endInclusive - range.start)) + range.start
    }

    val propertyName: String

    val deck: Int

    suspend fun startFlows()


    fun round(value: Float) = (value * 100).roundToInt() / 100.0f

    class Slider(
        override val deck: Int,
        override val propertyName: String,
        val range: ClosedFloatingPointRange<Float>,
        private val initialValue: Float,
        private val stateFlow: MutableStateFlow<Float> = MutableStateFlow(initialValue),
    ) : MutableStateFlow<Float> by stateFlow, NestdropControl {
        private val sliderAddress: String get() = "/Controls/Deck$deck/s$propertyName"

        private val valueLabel = MutableStateFlow("")
        private suspend fun onSliderValueChanged(value: Float) {
            nestdropSendChannel.send(
                OSCMessage(
                    sliderAddress,
                    value
                )
            )
        }


        suspend fun doReset() {
            stateFlow.value = initialValue
//            nestdropPortSend(
//                buttonAddress,
//                1,
//            )
        }

        override suspend fun startFlows() {
            stateFlow
                .map {
                    it.coerceIn(range.start, range.endInclusive)
                }
                .onEach {
                    valueLabel.value = ((it * 100).roundToInt() / 100f).toString()
                    onSliderValueChanged(it)
                }
                .launchIn(flowScope)
        }
    }

    class SliderWithResetButton(
        override val deck: Int,
        override val propertyName: String,
        val range: ClosedFloatingPointRange<Float>,
        private val initialValue: Float,
        private val defaultValue: Float = initialValue,
        private val sendResetMessage: Boolean = true,
        private val stateFlow: MutableStateFlow<Float> = MutableStateFlow(initialValue),
    ) : MutableStateFlow<Float> by stateFlow, NestdropControl {
        private val resetButtonAddress: String get() = "/Controls/Deck$deck/bt$propertyName"
        private val sliderAddress: String get() = "/Controls/Deck$deck/s$propertyName"

        private val valueLabel = MutableStateFlow("")

        suspend fun doReset() {
            stateFlow.value = defaultValue
            if (sendResetMessage) {
                nestdropSendChannel.send(
                    OSCMessage(
                        resetButtonAddress,
                        1
                    )
                )
            }
        }

        private suspend fun onSliderValueChanged(value: Float) {
            nestdropSendChannel.send(
                OSCMessage(
                    sliderAddress,
                    value
                )
            )
        }

        override suspend fun startFlows() {
            stateFlow
                .map {
                    it.coerceIn(range.start, range.endInclusive)
                }
                .onEach {
                    valueLabel.value = ((it * 100).roundToInt() / 100f).toString()
                    onSliderValueChanged(it)
                }
                .launchIn(flowScope)
        }
    }

    class ToggleButton(
        override val deck: Int,
        override val propertyName: String,
        private val initialValue: Boolean,
        private val stateFlow: MutableStateFlow<Boolean> = MutableStateFlow(initialValue),
    ) : MutableStateFlow<Boolean> by stateFlow, NestdropControl {
        private val buttonAddress: String get() = "/Controls/Deck$deck/bt$propertyName"

        private suspend fun doClick(value: Boolean) {
            nestdropSendChannel.send(
                OSCMessage(
                    buttonAddress,
                    if (value) 1 else 0
                )
            )
        }

        override suspend fun startFlows() {
            stateFlow
                .onEach { value ->
                    doClick(value)
                }
                .launchIn(flowScope)
        }
    }

//    class RangeSliderWithResetButton(
//        override val deck: Int,
//        override val propertyName: String,
//        val range: ClosedFloatingPointRange<Float>,
//        private val initialValue: Pair<Float, Float>,
//        private val defaultValue: Pair<Float, Float> = initialValue,
//        private val stateFlow: MutableStateFlow<Pair<Float, Float>> = MutableStateFlow(initialValue),
//    ) : MutableStateFlow<Pair<Float, Float>> by stateFlow, NestdropControl {
//        private val buttonAddress: String = "/Controls/Deck$deck/b$propertyName"
//        private val sliderAddressMin: String = "/Controls/Deck$deck/s$propertyName/Min"
//        private val sliderAddressMax: String = "/Controls/Deck$deck/s$propertyName/Max"
//
//        private val valueLabel = MutableStateFlow("")
//
//        suspend fun doReset() {
//            stateFlow.value = defaultValue
//            nestdropSendChannel.send(
//                OSCMessage(
//                    buttonAddress,
//                    1
//                )
//            )
//        }
//
//        private suspend fun onSliderValueChanged(min: Float, max: Float) {
//            nestdropSendChannel.send(
//                OSCMessage(
//                    sliderAddressMin,
//                    min
//                )
//            )
//            nestdropSendChannel.send(
//                OSCMessage(
//                    sliderAddressMax,
//                    max
//                )
//            )
//        }
//
//        override suspend fun startFlows() {
//
//            stateFlow
//                .map { (minValue, maxValue) ->
//                    minValue.coerceIn(range.start, range.endInclusive) to maxValue.coerceIn(
//                        range.start,
//                        range.endInclusive
//                    )
//                }
//                .onEach { (minValue, maxValue) ->
//                    valueLabel.value = "${round(minValue)} - ${round(maxValue)}"
//                    onSliderValueChanged(minValue, maxValue)
//                }
//                .launchIn(flowScope)
//        }
//    }

    class RangeSliderWithResetButton(
        override val deck: Int,
        override val propertyName: String,
        val range: ClosedFloatingPointRange<Float>,
        private val initialMin: Float,
        private val initialMax:Float,
        val minState: MutableStateFlow<Float> = MutableStateFlow(initialMin),
        val maxState: MutableStateFlow<Float> = MutableStateFlow(initialMin),
    ) : NestdropControl {
        private val buttonAddress: String = "/Controls/Deck$deck/b$propertyName"
        private val sliderAddressMin: String = "/Controls/Deck$deck/s$propertyName/Min"
        private val sliderAddressMax: String = "/Controls/Deck$deck/s$propertyName/Max"

        private val valueLabel = MutableStateFlow("")

        suspend fun doReset() {
            minState.value = initialMin
            maxState.value = initialMax
            nestdropSendChannel.send(
                OSCMessage(
                    buttonAddress,
                    1
                )
            )
        }

        private suspend fun onSliderValueChanged(min: Float, max: Float) {
            nestdropSendChannel.send(
                OSCMessage(
                    sliderAddressMin,
                    min
                )
            )
            nestdropSendChannel.send(
                OSCMessage(
                    sliderAddressMax,
                    max
                )
            )
        }

        override suspend fun startFlows() {
            combine(minState, maxState) { min, max ->
                min.coerceIn(range.start, range.endInclusive) to max.coerceIn(range.start, range.endInclusive)
            }
                .onEach { (minValue, maxValue) ->
                valueLabel.value = "${round(minValue)} - ${round(maxValue)}"
                onSliderValueChanged(minValue, maxValue)
            }
                .launchIn(flowScope)
        }
    }

    class Dropdown <T: Any>(
        override val deck: Int,
        override val propertyName: String,
        val options: List<T>,
        private val enumToValue: (T) -> Int,
        private val initialValue: T,
        private val stateFlow: MutableStateFlow<T> = MutableStateFlow(initialValue),
    ) : MutableStateFlow<T> by stateFlow, NestdropControl {
        private val dropdownAddress: String get() = "/Controls/Deck$deck/cb$propertyName"

        private val valueLabel = MutableStateFlow("")

        private suspend fun onValueChanged(value: Int) {
            nestdropSendChannel.send(
                OSCMessage(
                    dropdownAddress,
                    value
                )
            )
        }


        fun doReset() {
            stateFlow.value = initialValue
        }

        override suspend fun startFlows() {
            stateFlow
                .onEach {
                    valueLabel.value = it.toString()
                    onValueChanged(enumToValue(it))
//                    fader.value = valueToSlider(range, it)
                }
                .launchIn(flowScope)
        }
    }
}