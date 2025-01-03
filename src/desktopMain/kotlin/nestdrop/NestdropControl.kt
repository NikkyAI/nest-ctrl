package nestdrop

import flowScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import osc.OSCMessage
import osc.OscSynced
import osc.nestdropSendChannel
import kotlin.math.roundToInt

sealed interface NestdropControl {
    companion object {
        private val logger = KotlinLogging.logger { }

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

        private val syncedFlow = OscSynced.ValueSingle(
            address = sliderAddress,
            initialValue = initialValue,
            target = OscSynced.Target.Nestdrop,
        )

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
                    syncedFlow.setValue(it)
//                    onSliderValueChanged(it)
                }
                .launchIn(flowScope)


            syncedFlow
                .onEach { value ->
                    stateFlow.value = value
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

        private val syncedFlow = OscSynced.ValueSingle(
            address = sliderAddress,
            initialValue = initialValue,
            target = OscSynced.Target.Nestdrop,
        )

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

        override suspend fun startFlows() {
            stateFlow
                .map {
                    it.coerceIn(range.start, range.endInclusive)
                }
                .onEach {
                    valueLabel.value = ((it * 100).roundToInt() / 100f).toString()
                    syncedFlow.setValue(it)
                }
                .launchIn(flowScope)

            syncedFlow
                .onEach { value ->
                    stateFlow.value = value
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
        private val syncedFlow = OscSynced.ValueSingle<Int>(
            buttonAddress,
            if (initialValue) 0 else 1,
            target = OscSynced.Target.Nestdrop
        )

        override suspend fun startFlows() {
            stateFlow
                .onEach { value ->
                    syncedFlow.setValue(if (value) 1 else 0)
                }
                .launchIn(flowScope)

            syncedFlow
                .onEach { value ->
                    stateFlow.value = value != 0
                }
                .launchIn(flowScope)
        }
    }

    class RangeSliderWithResetButton(
        override val deck: Int,
        override val propertyName: String,
        val range: ClosedFloatingPointRange<Float>,
        private val initialMin: Float,
        private val initialMax: Float,
        val minState: MutableStateFlow<Float> = MutableStateFlow(initialMin),
        val maxState: MutableStateFlow<Float> = MutableStateFlow(initialMin),
    ) : NestdropControl {
        private val buttonAddress: String = "/Controls/Deck$deck/b$propertyName"
        private val sliderAddressMin: String = "/Controls/Deck$deck/s$propertyName/Min"
        private val sliderAddressMax: String = "/Controls/Deck$deck/s$propertyName/Max"
        private val syncedFlowMin = OscSynced.ValueSingle(
            address = sliderAddressMin,
            initialValue = initialMin,
            target = OscSynced.Target.Nestdrop
        )
        private val syncedFlowMax = OscSynced.ValueSingle(
            address = sliderAddressMax,
            initialValue = initialMax,
            target = OscSynced.Target.Nestdrop
        )

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
            syncedFlowMin.setValue(min)
            syncedFlowMax.setValue(max)
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

            syncedFlowMin
                .onEach { value ->
                    minState.value = value
                }
                .launchIn(flowScope)


            syncedFlowMax
                .onEach { value ->
                    maxState.value = value
                }
                .launchIn(flowScope)
        }
    }

    class Dropdown<T : Any>(
        override val deck: Int,
        override val propertyName: String,
        val options: List<T>,
        private val enumToValue: (T) -> Int,
        private val initialValue: T,
        private val stateFlow: MutableStateFlow<T> = MutableStateFlow(initialValue),
    ) : MutableStateFlow<T> by stateFlow, NestdropControl {
        private val dropdownAddress: String get() = "/Controls/Deck$deck/cb$propertyName"
        private val syncedFlow = OscSynced.ValueSingle<Int>(
            address = dropdownAddress,
            initialValue = enumToValue(initialValue),
            target = OscSynced.Target.Nestdrop
        )
        private val valueLabel = MutableStateFlow("")

        fun doReset() {
            stateFlow.value = initialValue
        }

        override suspend fun startFlows() {
            stateFlow
                .onEach {
                    valueLabel.value = it.toString()
                    syncedFlow.setValue(enumToValue(it))
//                    onValueChanged(enumToValue(it))
                }
                .launchIn(flowScope)

            syncedFlow
                .onEach { value ->
                    stateFlow.value = options[value]
                }
                .launchIn(flowScope)
        }
    }
}