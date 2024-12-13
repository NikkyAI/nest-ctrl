import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.awaitApplication
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.rememberWindowState
import dev.reformator.stacktracedecoroutinator.runtime.DecoroutinatorRuntime
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import nestctrl.generated.resources.Res
import nestctrl.generated.resources.blobhai_trans
import nestdrop.PerformanceLogRow
import nestdrop.deck.Deck
import nestdrop.deck.PresetQueues
import nestdrop.deck.loadDeckSettings
import nestdrop.loadNestdropConfig
import nestdrop.parsePerformanceLog
import nestdrop.performanceLogsFlow
import nestdrop.setupSpriteFX
import org.jetbrains.compose.resources.painterResource
import osc.initializeSyncedValues
import osc.runNestDropSend
import osc.startNestdropListener
import tags.startTagsFileWatcher
import ui.App
import ui.components.verticalScroll
import ui.screens.imgSpritesMap
import ui.splashScreen
import utils.KWatchChannel
import utils.KWatchEvent
import utils.asWatchChannel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime


private val logger = KotlinLogging.logger { }
//val decks = MutableStateFlow<List<Deck>>(emptyList())

val presetQueues = PresetQueues()
val decks = List(4) { index ->
    when (val n = index + 1) {
        1 -> Deck(n, 0xFFBB0000)
        2 -> Deck(n, 0xFF00BB00)
        3 -> Deck(n, 0xFF00A2FF)
        4 -> Deck(n, 0xFFF9F900)
        else -> null
    }
}.filterNotNull()

object Main {

    @OptIn(FlowPreview::class)
    suspend fun initApplication(
        presetQueues: PresetQueues,
    ) {
//        setupLogging()
//        logger.info {"testing logging.."}
//        logger.warn { "WARN" }
//        logger.error { "ERROR" }

//        logger.info { "connecting to carabiner" }
//        flowScope.launch {
//            var delayCounter = 1L
//            while (true) {
//                val increaseDelay = Link.openConnection()
//                if (increaseDelay) {
//                    delayCounter++
//                } else {
//                    delayCounter = 1
//                }
//                logger.warn { "reconnecting to carabiner $delayCounter" }
//                delay(delayCounter * 250)
//            }
//        }
//        withTimeoutOrNull(5000) {
//            while (!Link.isConnected.value) {
//                logger.warn { "waiting for link to connect" }
//                delay(100)
//            }
//        } ?: run {
//            logger.error { "failed to connect to carabiner socket / (ableton link)" }
//            error("failed to connect to carabiner socket / (ableton link)")
//        }

        logger.info { "setup sprite FX" }
        setupSpriteFX()

        flowScope.launch(Dispatchers.IO) {
            while (true) {
                logger.info { "starting OSC sender" }
                runNestDropSend()
                delay(100)
            }
        }
//        flowScope.launch(Dispatchers.IO) {
//            while (true) {
//                runControlSend()
//                delay(100)
//            }
//        }
//        flowScope.launch(Dispatchers.IO) {
//            while (true) {
//                runResolumeSend()
//                delay(100)
//            }
//        }

        logger.info { "scanning presets" }
        measureTime {
            scanPresets()
        }.also {
            logger.info { "scan took $it" }
        }

//        delay(100)
        logger.info { "starting filewatcher ./tags" }
        startTagsFileWatcher(presetQueues)

        run {
            logger.info { "starting history writer" }
            val json = Json {
                prettyPrint = false
            }

            val historyFolder = configFolder.resolve("history")
            historyFolder.mkdirs()
            val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm").format(Date())
            val historyFile = historyFolder.resolve("$timestamp.ndjson")

            decks.map { it ->
                it.currentState.debounce(3.seconds)
            }
                .merge()
                .onEach {
                    historyFile.appendText(
                        json.encodeToString(
                            Deck.DeckState.serializer(), it
                        ) + "\n"
                    )
                }
                .flowOn(Dispatchers.IO)
                .launchIn(flowScope)
        }

        // motion extraction toggles and sliders
//        val groupSyncedValues = (1..4).associateWith { group ->
//            val arenaMotionExtractionBypassed = OscSynced.Value<Int>(
//                address = "/composition/groups/$group/video/effects/motionextraction/bypassed",
//                initialValue = 1, target = OscSynced.Target.ResolumeArena
//            ).also {
//                it.logReceived = false
////                arenaSendChannel.send(OSCMessage(it.address, "?"))
//                it.dropFirst = 1
//            }
//            val arenaMotionExtractionDelay = OscSynced.Value<Float>(
//                address = "/composition/groups/$group/video/effects/motionextraction/effect/delay",
//                initialValue = 0.25f, target = OscSynced.Target.ResolumeArena
//            ).also {
//                it.logReceived = false
////                arenaSendChannel.send(OSCMessage(it.address, "?"))
//                it.dropFirst = 1
//            }
//            val arenaAutomaskBypassed = OscSynced.Value<Int>(
//                "/composition/groups/$group/video/effects/automask/bypassed",
//                initialValue = 1, target = OscSynced.Target.ResolumeArena
//            ).also {
//                it.logReceived = false
////                arenaSendChannel.send(OSCMessage(it.address, "?"))
//                it.dropFirst = 1
//            }
//            val motionExtractionEnabled =
//                MutableStateFlow(false) // OscSynced.Value("/resolume/$group/motion_extraction/enabled", false)
//            motionExtractionEnabled
//                .sample(100.milliseconds)
//                .onEach {
//                    arenaMotionExtractionBypassed.value = if (it) 0 else 1
//                }
//                .launchIn(flowScope)
//            arenaMotionExtractionBypassed
//                .sample(100.milliseconds)
//                .onEach {
//                    motionExtractionEnabled.value = it == 0
//                }
//                .launchIn(flowScope)
//            val motionExtractionDelay =
//                MutableStateFlow(0.5f) // OscSynced.Value("/resolume/$group/motion_extraction/delay", 0.5f)
//            motionExtractionDelay
//                .sample(100.milliseconds)
//                .onEach {
//                    arenaMotionExtractionDelay.value = it
//                }
//                .launchIn(flowScope)
//            arenaMotionExtractionDelay
//                .sample(100.milliseconds)
//                .onEach {
//                    motionExtractionDelay.value = it
//                }
//                .launchIn(flowScope)
//            (motionExtractionEnabled to motionExtractionDelay)
//            val automaskEnabled = MutableStateFlow(false) // OscSynced.Value("/resolume/$group/auto_mask", false)
//            automaskEnabled
//                .sample(100.milliseconds)
//                .onEach {
//                    arenaAutomaskBypassed.value = if (it) 0 else 1
//                }
//                .launchIn(flowScope)
//            arenaAutomaskBypassed
//                .sample(100.milliseconds)
//                .onEach {
//                    automaskEnabled.value = it == 0
//                }
//                .launchIn(flowScope)
//            Triple(motionExtractionEnabled, motionExtractionDelay, automaskEnabled)
//        }
//
//        val resetResolumeTrigger = MutableStateFlow(0) // OscSynced.Trigger("/resolume/reset")
//        resetResolumeTrigger
//            .drop(1)
//            .onEach {
//                logger.error { "executing resolume reset now" }
//                resolumeClipConnect(1, 1, 1)
//                groupSyncedValues[1]?.let { (motionExtraction, delay, automask) ->
//                    motionExtraction.value = false
//                    delay.value = 0.25f
//                    automask.value = false
//                }
//                resolumeLayerClear(1, 2)
//                resolumeLayerClear(1, 3)
//
//                resolumeClipConnect(2, 1, 2)
//                groupSyncedValues[2]?.let { (motionExtraction, delay, automask) ->
//                    motionExtraction.value = false
//                    delay.value = 0.25f
//                    automask.value = false
//                }
//                resolumeLayerClear(2, 2)
//                resolumeLayerClear(2, 3)
////                resolumeClipConnect(2, 3, 1)
//
//                resolumeClipConnect(3, 1, 1)
//                groupSyncedValues[3]?.let { (motionExtraction, delay, automask) ->
//                    motionExtraction.value = true
//                    delay.value = 0.25f
//                    automask.value = false
//                }
//                resolumeLayerClear(3, 2)
//                resolumeLayerClear(3, 3)
//
//                resolumeClipConnect(4, 1, 2)
//                groupSyncedValues[4]?.let { (motionExtraction, delay, automask) ->
//                    motionExtraction.value = true
//                    delay.value = 0.25f
//                    automask.value = false
//                }
//                resolumeLayerClear(4, 2)
//                resolumeLayerClear(4, 3)
//
//            }
//            .launchIn(flowScope)

//        startResolumeListener()

//        run {
//            val file = File("resolume_layers.json")
//            val json = Json {
//                prettyPrint = true
//            }
//            resolumeLayerStates
//                .sample(1.seconds)
//                .onEach {
//                    file.writeText(
//                        json.encodeToString(
//                            MapSerializer(Int.serializer(), Int.serializer().nullable),
//                            it
//                        )
//                    )
//                }
//                .launchIn(flowScope)
//        }

        logger.info { "starting beat counter" }
        startBeatCounter()

        decks.forEach { deck ->
            performanceLogsFlow
                .filter {
                    it.deck == deck.id
                }
                .sample(500.milliseconds)
                .onEach {
                    deck.presetSwitching.currentPreset.value = it
                }
                .launchIn(flowScope)
        }

        flowScope.launch(Dispatchers.IO) {
            delay(100)

//            performanceLogsMap.value = nestdropPerformanceLog.listFiles().orEmpty().mapNotNull { file ->
//                parsePerformanceLog(file)?.let {
//                    file.nameWithoutExtension to it
//                }
//            }.toMap()

            val performanceLogRows = mutableSetOf<PerformanceLogRow>()

            nestdropPerformanceLogFolder
                .listFiles()
                .orEmpty()
                .mapNotNull { file ->
                    parsePerformanceLog(file)
                }
                .flatten()
                .sortedBy { it.dateTime }
                .takeLast(50)
                .forEach {
//                    delay(1)
                    performanceLogsFlow.emit(it)
                    performanceLogRows.add(it)
                }

            nestdropPerformanceLogFolder
                .asWatchChannel(KWatchChannel.Mode.SingleDirectory)
                .consumeEach { event ->

//                logger.debugF { "watch-event: ${event.kind} ${event.file}" }
                    event.tag?.also {
                        logger.debug { "watch-event.tag: $it" }
                    }
                    when (event.kind) {
                        KWatchEvent.Kind.Initialized -> {}
                        KWatchEvent.Kind.Created, KWatchEvent.Kind.Modified -> {
                            parsePerformanceLog(event.file)?.let { rows ->
//                                logger.info { "watch-event ${event.kind} ${event.file}" }
                                val lastTimestamp = performanceLogRows.maxOfOrNull { it.dateTime }
                                    ?: Instant.DISTANT_PAST.toLocalDateTime(TimeZone.currentSystemDefault())
                                val newRows = rows.toSet() - performanceLogRows
//                                logger.info { "received ${newRows.size} new rows" }
                                newRows
//                                    .filter { it.dateTime > lastTimestamp }
                                    .sortedBy { it.dateTime }
                                    .forEach {
//                                        logger.infoF { "emitting $it" }
                                        performanceLogsFlow.emit(it)
                                        performanceLogRows.add(it)
                                    }
//                                performanceLogsMap.value += event.file.nameWithoutExtension to rows
                            }
                        }

                        KWatchEvent.Kind.Deleted -> {
//                            performanceLogsMap.value -= event.file.nameWithoutExtension
                        }
                    }

                }
        }
//        OSCMessage("thiswillfail", "string", 'c', "" to "")

//        performanceLogsFlow
//            .sample(500.milliseconds)
//            .filter {
//                it.deck == deck1.N
//            }
//            .onEach {
//                deck1.currentPreset.value = it
//            }
//            .launchIn(flowScope)
//
//        performanceLogsFlow
//            .sample(500.milliseconds)
//            .filter {
//                it.deck == deck2.N
//            }
//            .onEach {
//                deck2.currentPreset.value = it
//            }
//            .launchIn(flowScope)

//        performanceLogsMap
//            .map {
//                it.values.flatten().sortedBy { it.dateTime }
//            }.onEach {
//                performanceLogs.value = it
//            }.onEach {
//                logger.traceF { "history rows: ${it.size}" }
//                val reversed = it.reversed()
//                deck1.currentPreset.value = reversed.firstOrNull { it.deck == 1 }
////                .also {
////                logger.debugF { "$it" }
////            }
//                deck2.currentPreset.value = reversed.firstOrNull { it.deck == 2 }
////                .also {
////                logger.debugF { "$it" }
////            }
//            }
//            .launchIn(flowScope)


        flowScope.launch(Dispatchers.IO) {
            logger.info { "loading nestdrop XML" }
            loadNestdropConfig(presetQueues, decks)
            nestdropConfig.asWatchChannel(KWatchChannel.Mode.SingleFile).consumeEach {
                if (it.kind == KWatchEvent.Kind.Modified) {
                    loadNestdropConfig(presetQueues, decks)
                }
            }
        }


        withTimeoutOrNull(45.seconds) {
            logger.info { "waiting for queues to be initialized" }
            while (!presetQueues.isInitialized.value) {
                delay(100)
            }
            true
        } ?: error("failed to load queues")
        logger.info { "queues are initialized" }

//        while(deck1.spriteQueues.value.isEmpty()) {
//            delay(500)
//        }
//        while(deck2.spriteQueues.value.isEmpty()) {
//            delay(500)
//        }

        loadConfig()
        delay(500)

        presetQueues.startFlows()
        flowScope.launch {
            decks.forEach { deck ->
                launch {
                    deck.startFlows()
                }
            }
        }
//        deck1.startFlows()
//        deck2.startFlows()

        loadDeckSettings(decks)

//        decks.forEach {
//            it.imgSprite.index.value++
//        }
//        decks.forEach {
//            it.imgSprite.index.value--
//        }

        // TODO ensure that sprites are set AGAIN correctly
//        decks.forEach {
//            val last = it.imgSprite.spriteImgLocation.value
//            if(last != null) {
//                imgSpritesMap.value.
//            }
//        }
//        deck1.imgSprite.index.value++
//        deck1.imgSprite.index.value--
//        deck2.imgSprite.index.value++
//        deck2.imgSprite.index.value--

        logger.info { "starting OSC listener" }
        startNestdropListener()

        logger.info { "initializing OSC synced values" }
        initializeSyncedValues()
        delay(200)
//        logger.info { "re-emitting all values" }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if (dotenv.get("DEBUG", "false").toBooleanStrictOrNull() == true) {
//        if(true) {
            val state = DecoroutinatorRuntime.load()
            logger.info { "enabling De-Corouti-nator: $state" }
        }

//        Activator.createParserTypes().forEach { (k,v) ->
//            logger.info { "parser $k: $v" }
//        }
//        Activator.createSerializerTypes().forEach { v ->
//            logger.info { "serializer: $v" }
//        }

        runBlocking {
            //TODO: detect debug flags and such ?
            //        val presetQueues = PresetQueues()
            awaitApplication {

                var isSplashScreenShowing by remember { mutableStateOf(true) }
                var showException by remember { mutableStateOf<Throwable?>(null) }
                LaunchedEffect(Unit) {
                    try {
                        initApplication(presetQueues)
                        logger.info { "await application" }
                        isSplashScreenShowing = false
                    } catch (e: Exception) {
                        logger.error(e) { "unhandled error: TODO: show error" }
                        //                    error("unhandled exception ${e.message}")
                        showException = e
                    } catch (e: ExceptionInInitializerError) {
                        logger.error(e) { "unhandled error: TODO: show error" }
                        //                    error("unhandled exception ${e.exception.message}")
                        showException = e

                    }
                }
                if (showException != null) {
                    val e = showException!!
                    DialogWindow(
                        onCloseRequest = ::exitApplication,
                        //                    undecorated = true,
                        title = "Error",
                        state = rememberDialogState(
                            position = WindowPosition(Alignment.Center),
                            width = 1200.dp,
                            height = 800.dp
                        )
                    ) {
                        MaterialTheme(colors = darkColors()) {
                            Scaffold {
                                verticalScroll {
                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        val message = when (e) {
                                            is ExceptionInInitializerError -> {
                                                e.exception.message
                                            }

                                            is Exception -> e.message

                                            //                                    null -> "this should never happen"
                                            else -> "unhandled exception type: ${e::class.qualifiedName} ${e.message}"
                                        } ?: "no message provided, please check the logs"

                                        Text("NEST CTRL failed to initialize")
                                        //                                    Column {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "message",
                                                modifier = Modifier
                                                    .weight(0.2f)
                                            )

                                            Text(
                                                text = message,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier
                                                    .weight(0.8f)
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "stacktrace",
                                                modifier = Modifier
                                                    .weight(0.2f)
                                            )

                                            Text(
                                                text = e.stackTraceToString().replace("\t", "  "),
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier
                                                    .weight(0.8f)
                                            )
                                        }
                                        //                                    }
                                        Row {
                                            Button(
                                                onClick = ::exitApplication
                                            ) {
                                                Text("Close")
                                            }
                                        }
                                    }

                                    //                                Spacer(modifier = Modifier.weight(10.0f))
                                }
                            }
                        }
                    }
                } else if (isSplashScreenShowing) {
                    Window(
                        onCloseRequest = ::exitApplication,
                        title = "Splash",
                        state = rememberWindowState(
                            position = WindowPosition(BiasAlignment(0f, 0f)),
                            width = 300.dp, height = 200.dp
                        ),
                        undecorated = true,
                        //                    transparent = true,
                        focusable = false,
                        alwaysOnTop = true,
                        icon = painterResource (resource =  Res.drawable.blobhai_trans)
                    ) {
                        splashScreen()
                    }
                } else {
                    Window(
                        onCloseRequest = ::exitApplication,
                        title = "Nest Ctrl",
                        state = rememberWindowState(width = 1600.dp, height = 1200.dp),
                        icon = painterResource (resource =  Res.drawable.blobhai_trans)
                    ) {
                        App()
                    }
                }
            }
        }
    }
}
