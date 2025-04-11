import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.plus
import utils.LOOM

@OptIn(ExperimentalCoroutinesApi::class)
//val flowScope = CoroutineScope(
//    Dispatchers.Default
//        .limitedParallelism(32)
//) + CoroutineName("flows")
val flowScope = CoroutineScope(
    Dispatchers.LOOM
) + CoroutineName("flows-loom")