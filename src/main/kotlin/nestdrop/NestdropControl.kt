package nestdrop

import flowScope
import io.klogging.logger
import kotlinx.coroutines.flow.*
import logging.debugF
import osc.OSCMessage
import osc.OscSynced
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
        private val range: ClosedFloatingPointRange<Float>,
        private val initialValue: Float,
        private val stateFlow: MutableStateFlow<Float> = MutableStateFlow(initialValue),
    ) : MutableStateFlow<Float> by stateFlow, NestdropControl {
        private val sliderAddress: String get() = "/Controls/Deck$deck/s$propertyName"

        private val fader =
            OscSynced.Value("/nd/controls/Deck$deck/$propertyName", valueToSlider(range, initialValue))
        private val reset = OscSynced.Trigger("/nd/controls/reset/Deck$deck/$propertyName")
        private val propertyLabel =
            OscSynced.Value("/nd/controls/property/Deck$deck/$propertyName", propertyName, receive = false)
        private val valueLabel =
            OscSynced.Value("/nd/controls/value/Deck$deck/$propertyName", "", receive = false)

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
            propertyLabel.value = propertyName

            fader
                .onEach {
                    stateFlow.value = sliderToValue(range, it)
                }
                .launchIn(flowScope)

            reset
                .onEach {
                    doReset()
                }
                .launchIn(flowScope)

            stateFlow
                .map {
                    it.coerceIn(range.start, range.endInclusive)
                }
                .onEach {
                    valueLabel.value = ((it * 100).roundToInt() / 100f).toString()
                    onSliderValueChanged(it)
                    fader.value = valueToSlider(range, it)
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
        private val buttonAddress: String get() = "/Controls/Deck$deck/bt$propertyName"
        private val sliderAddress: String get() = "/Controls/Deck$deck/s$propertyName"

//        @Deprecated("stop using touch osc")
//        private val fader =
//            OscSynced.Value("/nd/controls/Deck$deck/$propertyName", valueToSlider(range, initialValue))
//
//        @Deprecated("stop using touch osc")
//        private val reset = OscSynced.Trigger("/nd/controls/reset/Deck$deck/$propertyName")
//        @Deprecated("stop using touch osc")
//        private val propertyLabel =
//            OscSynced.Value("/nd/controls/property/Deck$deck/$propertyName", propertyName, receive = false)
//        @Deprecated("stop using touch osc")
//        private val valueLabel =
//            OscSynced.Value("/nd/controls/value/Deck$deck/$propertyName", "", receive = false)

        suspend fun doReset() {
            stateFlow.value = defaultValue
            if (sendResetMessage) {
                nestdropSendChannel.send(
                    OSCMessage(
                        buttonAddress,
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
//            propertyLabel.value = propertyName

//            fader
//                .onEach {
//                    stateFlow.value = sliderToValue(range, it)
//                }
//                .launchIn(flowScope)
//
//            reset
//                .onEach {
//                    doReset()
//                }
//                .launchIn(flowScope)

            stateFlow
                .map {
                    it.coerceIn(range.start, range.endInclusive)
                }
                .onEach {
//                    valueLabel.value = ((it * 100).roundToInt() / 100f).toString()
                    onSliderValueChanged(it)
//                    fader.value = valueToSlider(range, it)
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

        private val button = OscSynced.Value("/nd/controls/Deck$deck/$propertyName", initialValue)

        private suspend fun doClick(value: Boolean) {
            nestdropSendChannel.send(
                OSCMessage(
                    buttonAddress,
                    if (value) 1 else 0
                )
            )
        }

        override suspend fun startFlows() {
            button
                .onEach {
                    logger.debugF { "received deck $deck $propertyName << $it" }
                    stateFlow.value = it
                }
                .launchIn(flowScope)

            stateFlow
                .onEach { value ->
                    doClick(value)
                    button.value = value
                }
                .launchIn(flowScope)
        }
    }

    class RangeSliderWithResetButton(
        override val deck: Int,
        override val propertyName: String,
        private val range: ClosedFloatingPointRange<Float>,
        private val initialValue: Pair<Float, Float>,
        private val defaultValue: Pair<Float, Float> = initialValue,
        private val stateFlow: MutableStateFlow<Pair<Float, Float>> = MutableStateFlow(initialValue),
    ) : MutableStateFlow<Pair<Float, Float>> by stateFlow, NestdropControl {
        private val buttonAddress: String = "/Controls/Deck$deck/b$propertyName"
        private val sliderAddressMin: String = "/Controls/Deck$deck/s$propertyName/Min"
        private val sliderAddressMax: String = "/Controls/Deck$deck/s$propertyName/Max"

        private val minFader =
            OscSynced.Value("/nd/controls/min/Deck$deck/$propertyName", valueToSlider(range, initialValue.first))
        private val maxFader = OscSynced.Value(
            "/nd/controls/max/Deck$deck/$propertyName",
            valueToSlider(range, initialValue.second)
        )
        private val reset = OscSynced.Trigger("/nd/controls/reset/Deck$deck/$propertyName")
        private val propertyLabel =
            OscSynced.Value("/nd/controls/property/Deck$deck/$propertyName", propertyName, receive = false)
        private val valueLabel =
            OscSynced.Value("/nd/controls/value/Deck$deck/$propertyName", "", receive = false)

        suspend fun doClick() {
            stateFlow.value = defaultValue
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
            propertyLabel.value = propertyName

            minFader.combine(maxFader) { min, max -> min to max }
                .onEach { (minValue, maxValue) ->
                    stateFlow.value = sliderToValue(range, minValue) to sliderToValue(range, maxValue)
                }
                .launchIn(flowScope)

            reset
                .onEach {
                    doClick()
                }
                .launchIn(flowScope)

            stateFlow
                .map { (minValue, maxValue) ->
                    minValue.coerceIn(range.start, range.endInclusive) to maxValue.coerceIn(
                        range.start,
                        range.endInclusive
                    )
                }
                .onEach { (minValue, maxValue) ->
                    valueLabel.value = "${round(minValue)} - ${round(maxValue)}"
                    onSliderValueChanged(minValue, maxValue)
                    minFader.value = valueToSlider(range, minValue)
                    maxFader.value = valueToSlider(range, maxValue)
                }
                .launchIn(flowScope)
        }
    }

    class Dropdown <T: Any>(
        override val deck: Int,
        override val propertyName: String,
//        private val range: ClosedFloatingPointRange<Float>,
        private val enumToValue: (T) -> Int,
        private val initialValue: T,
        private val stateFlow: MutableStateFlow<T> = MutableStateFlow(initialValue),
    ) : MutableStateFlow<T> by stateFlow, NestdropControl {
        private val dropdownAddress: String get() = "/Controls/Deck$deck/cb$propertyName"

        private suspend fun onValueChanged(value: Int) {
            nestdropSendChannel.send(
                OSCMessage(
                    dropdownAddress,
                    value
                )
            )
        }


        suspend fun doReset() {
            stateFlow.value = initialValue
        }

        override suspend fun startFlows() {
            stateFlow
                .map {
                    enumToValue(it)
                }
                .onEach {
//                    valueLabel.value = ((it * 100).roundToInt() / 100f).toString()
                    onValueChanged(it)
//                    fader.value = valueToSlider(range, it)
                }
                .launchIn(flowScope)
        }
    }
}