import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.awaitApplication
import androidx.compose.ui.window.rememberWindowState
import io.klogging.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import logging.debugF
import logging.errorF
import logging.fatalF
import logging.infoF
import logging.setupLogging
import logging.warnF
import nestdrop.PerformanceLogRow
import nestdrop.deck.Deck
import nestdrop.deck.PresetQueues
import nestdrop.deck.loadDeckSettings
import nestdrop.loadNestdropConfig
import nestdrop.parsePerformanceLog
import nestdrop.performanceLogsFlow
import nestdrop.setupSpriteFX
import osc.OscSynced
import osc.initializeSyncedValues
import osc.resolumeClipConnect
import osc.resolumeLayerClear
import osc.resolumeLayerStates
import osc.runNestDropSend
import osc.runResolumeSend
import osc.startResolumeListener
import tags.startTagsFileWatcher
import ui.App
import ui.splashScreen
import utils.KWatchChannel
import utils.KWatchEvent
import utils.asWatchChannel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


private val logger = logger(Main::class.qualifiedName!!)
//val decks = MutableStateFlow<List<Deck>>(emptyList())

val presetQueues = PresetQueues()
val decks = List(4) { index ->
    when(val n = index + 1) {
        1 -> Deck(n, first = true, last = false, 0xFFBB0000)
        2 -> Deck(n, first = false, last = false, 0xFF00BB00)
        3 -> Deck(n, first = false, last = false, 0xFF00A2FF)
        4 -> Deck(n, first = false, last = true, 0xFFBBBB00)
        else -> null
    }
}.filterNotNull()

object Main {

    @OptIn(FlowPreview::class)
    suspend fun initApplication(
        presetQueues: PresetQueues,
//        deck1: Deck,
//        deck2: Deck,
    ) {
        setupLogging()
        println("testing logging")
        logger.warnF { "WARN" }
        logger.errorF { "ERROR" }
        logger.fatalF { "FATAL" }

        flowScope.launch {
            var delayCounter = 1L
            while (true) {
                val increaseDelay = Link.openConnection()
                if (increaseDelay) {
                    delayCounter++
                } else {
                    delayCounter = 1
                }
                logger.warnF { "reconnecting to carabiner $delayCounter" }
                delay(delayCounter * 250)
            }
        }
        withTimeoutOrNull(5000) {
            while (!Link.isConnected.value) {
                logger.warnF { "waiting for link to connect" }
                delay(500)
            }
        } ?: run {
            logger.error("failed to connect to link protocol")
            exitProcess(-1)
        }

        setupSpriteFX()

        flowScope.launch(Dispatchers.IO) {
            while (true) {
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
        flowScope.launch(Dispatchers.IO) {
            while (true) {
                runResolumeSend()
                delay(100)
            }
        }

        scanPresets()
        delay(100)
        startTagsFileWatcher(presetQueues)

        run {
            val json = Json {
                prettyPrint = false
            }

            val historyFolder = File("history")
            historyFolder.mkdirs()
            val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm").format(Date())
            val historyFile = historyFolder.resolve("$timestamp.ndjson")

            //TODO: write history to file again

//            decks.flatMapConcat { it ->
//                it.asFlow().map {
//                    it.currentState.debounce(1.seconds)
//                }
//            }.flattenConcat()
//                .onEach {
//                    historyFile.appendText(
//                        json.encodeToString(
//                            Deck.DeckState.serializer(), it
//                        ) + "\n"
//                    )
//                }

//            listOf(
//                deck1.currentState.debounce(3.seconds),
//                deck2.currentState.debounce(3.seconds),
//            )
//                .merge()
//                .onEach {
//                    historyFile.appendText(
//                        json.encodeToString(
//                            Deck.DeckState.serializer(), it
//                        ) + "\n"
//                    )
//                }
//                .flowOn(Dispatchers.IO)
//                .launchIn(flowScope)
        }

        // motion extraction toggles and sliders
        val groupSyncedValues = (1..4).associateWith { group ->
            val arenaMotionExtractionBypassed = OscSynced.Value<Int>(
                address = "/composition/groups/$group/video/effects/motionextraction/bypassed",
                initialValue = 1, target = OscSynced.Target.ResolumeArena
            ).also {
                it.logReceived = false
//                arenaSendChannel.send(OSCMessage(it.address, "?"))
                it.dropFirst = 1
            }
            val arenaMotionExtractionDelay = OscSynced.Value<Float>(
                address = "/composition/groups/$group/video/effects/motionextraction/effect/delay",
                initialValue = 0.25f, target = OscSynced.Target.ResolumeArena
            ).also {
                it.logReceived = false
//                arenaSendChannel.send(OSCMessage(it.address, "?"))
                it.dropFirst = 1
            }
            val arenaAutomaskBypassed = OscSynced.Value<Int>(
                "/composition/groups/$group/video/effects/automask/bypassed",
                initialValue = 1, target = OscSynced.Target.ResolumeArena
            ).also {
                it.logReceived = false
//                arenaSendChannel.send(OSCMessage(it.address, "?"))
                it.dropFirst = 1
            }
            val motionExtractionEnabled = MutableStateFlow(false) // OscSynced.Value("/resolume/$group/motion_extraction/enabled", false)
            motionExtractionEnabled
                .sample(100.milliseconds)
                .onEach {
                    arenaMotionExtractionBypassed.value = if (it) 0 else 1
                }
                .launchIn(flowScope)
            arenaMotionExtractionBypassed
                .sample(100.milliseconds)
                .onEach {
                    motionExtractionEnabled.value = it == 0
                }
                .launchIn(flowScope)
            val motionExtractionDelay = MutableStateFlow(0.5f) // OscSynced.Value("/resolume/$group/motion_extraction/delay", 0.5f)
            motionExtractionDelay
                .sample(100.milliseconds)
                .onEach {
                    arenaMotionExtractionDelay.value = it
                }
                .launchIn(flowScope)
            arenaMotionExtractionDelay
                .sample(100.milliseconds)
                .onEach {
                    motionExtractionDelay.value = it
                }
                .launchIn(flowScope)
            (motionExtractionEnabled to motionExtractionDelay)
            val automaskEnabled = MutableStateFlow(false) // OscSynced.Value("/resolume/$group/auto_mask", false)
            automaskEnabled
                .sample(100.milliseconds)
                .onEach {
                    arenaAutomaskBypassed.value = if (it) 0 else 1
                }
                .launchIn(flowScope)
            arenaAutomaskBypassed
                .sample(100.milliseconds)
                .onEach {
                    automaskEnabled.value = it == 0
                }
                .launchIn(flowScope)
            Triple(motionExtractionEnabled, motionExtractionDelay, automaskEnabled)
        }

        val resetResolumeTrigger = MutableStateFlow(0) // OscSynced.Trigger("/resolume/reset")
        resetResolumeTrigger
            .drop(1)
            .onEach {
                logger.errorF { "executing resolume reset now" }
                resolumeClipConnect(1, 1, 1)
                groupSyncedValues[1]?.let { (motionExtraction, delay, automask) ->
                    motionExtraction.value = false
                    delay.value = 0.25f
                    automask.value = false
                }
                resolumeLayerClear(1, 2)
                resolumeLayerClear(1, 3)

                resolumeClipConnect(2, 1, 2)
                groupSyncedValues[2]?.let { (motionExtraction, delay, automask) ->
                    motionExtraction.value = false
                    delay.value = 0.25f
                    automask.value = false
                }
                resolumeLayerClear(2, 2)
                resolumeLayerClear(2, 3)
//                resolumeClipConnect(2, 3, 1)

                resolumeClipConnect(3, 1, 1)
                groupSyncedValues[3]?.let { (motionExtraction, delay, automask) ->
                    motionExtraction.value = true
                    delay.value = 0.25f
                    automask.value = false
                }
                resolumeLayerClear(3, 2)
                resolumeLayerClear(3, 3)

                resolumeClipConnect(4, 1, 2)
                groupSyncedValues[4]?.let { (motionExtraction, delay, automask) ->
                    motionExtraction.value = true
                    delay.value = 0.25f
                    automask.value = false
                }
                resolumeLayerClear(4, 2)
                resolumeLayerClear(4, 3)
            }
            .launchIn(flowScope)

        startResolumeListener()

        run {
            val file = File("resolume_layers.json")
            val json = Json {
                prettyPrint = true
            }
            resolumeLayerStates
                .sample(1.seconds)
                .onEach {
                    file.writeText(
                        json.encodeToString(
                            MapSerializer(Int.serializer(), Int.serializer().nullable),
                            it
                        )
                    )
                }
                .launchIn(flowScope)
        }

        startBeatCounter()

        decks.forEach { deck ->
            performanceLogsFlow
                .sample(500.milliseconds)
                .filter {
                    it.deck == deck.N
                }
                .onEach {
                    deck.currentPreset.value = it
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

            nestdropPerformanceLog
                .listFiles()
                .orEmpty()
                .mapNotNull { file ->
                    parsePerformanceLog(file)
                }
                .flatten()
                .sortedBy { it.dateTime }
                .forEach {
                    performanceLogsFlow.emit(it)
                    performanceLogRows.add(it)
                }

            nestdropPerformanceLog
                .asWatchChannel(KWatchChannel.Mode.SingleDirectory)
                .consumeEach { event ->

//                logger.debugF { "watch-event: ${event.kind} ${event.file}" }
                    event.tag?.also {
                        logger.debugF { "watch-event.tag: $it" }
                    }
                    when (event.kind) {
                        KWatchEvent.Kind.Initialized -> {}
                        KWatchEvent.Kind.Created, KWatchEvent.Kind.Modified -> {
                            parsePerformanceLog(event.file)?.let { rows ->
//                                logger.infoF { "received ${rows.size} rows" }
                                val lastTimestamp = performanceLogRows.maxOfOrNull { it.dateTime }
                                    ?: Instant.DISTANT_PAST.toLocalDateTime(TimeZone.currentSystemDefault())
                                val newRows = rows.toSet() - performanceLogRows
//                                logger.infoF { "received ${newRows.size} new rows" }
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
            loadNestdropConfig(presetQueues, decks)
            nestdropConfig.asWatchChannel(KWatchChannel.Mode.SingleFile).consumeEach {
                if (it.kind == KWatchEvent.Kind.Modified) {
                    loadNestdropConfig(presetQueues, decks)
                }
            }
        }


        withTimeoutOrNull(10.seconds) {

            while (presetQueues.queues.value.isEmpty()) {
                delay(500)
            }
            true
        } ?: error ("failed to load queues")
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

        decks.forEach {
            it.imgSprite.index.value++
        }
        decks.forEach {
            it.imgSprite.index.value--
        }

        // TODO ensure that sprites are set AGAIN correctly
//        deck1.imgSprite.index.value++
//        deck1.imgSprite.index.value--
//        deck2.imgSprite.index.value++
//        deck2.imgSprite.index.value--

        logger.infoF { "initializing OSC" }
        initializeSyncedValues()
        delay(500)
        logger.infoF { "re-emitting all values" }
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
//        val presetQueues = PresetQueues()
        awaitApplication {
//            nestdropDeckCount.onEach { deckCount ->
//                val newDecks = List(deckCount) { index ->
//                    when(val n = index + 1) {
//                        1 -> Deck(n, first = true, last = n == deckCount, 0xFFBB0000, presetQueues)
//                        2 -> Deck(n, first = false, last = n == deckCount, 0xFF00BB00, presetQueues)
//                        3 -> Deck(n, first = false, last = n == deckCount, 0xFF0000BB, presetQueues)
//                        4 -> Deck(n, first = false, last = n == deckCount, 0xFFBBBB00, presetQueues)
//                        else -> null
//                    }
//                }.filterNotNull()
//                decks.value = newDecks
//            }.launchIn(flowScope)
//            val deck1 = Deck(1, first = true, last = false, 0xFFBB0000, presetQueues)
//            val deck2 = Deck(2, first = false, last = true, 0xFF00BB00, presetQueues)

            var isSplashScreenShowing by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                initApplication(presetQueues)
                logger.info { "await application" }
                isSplashScreenShowing = false
            }
            if (isSplashScreenShowing) {
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
                    icon = BitmapPainter(
                        useResource("drawable/blobhai_trans.png", ::loadImageBitmap)
                    )
                ) {
                    splashScreen()
                }
            } else {
                Window(
                    onCloseRequest = ::exitApplication,
                    title = "Nest Ctrl",
                    state = rememberWindowState(width = 1600.dp, height = 1200.dp),
                    icon = BitmapPainter(
                        useResource("drawable/blobhai_trans.png", ::loadImageBitmap)
                    )
                ) {
                    App()
                }


//        fader(
//            notches = 9,
//            color = Color.Red,
//        )
            }
        }
    }
}
