package osc

import kotlinx.coroutines.flow.MutableStateFlow

@Deprecated("stop using touch osc")
val resyncToTouchOSC = MutableStateFlow<Int>(0)