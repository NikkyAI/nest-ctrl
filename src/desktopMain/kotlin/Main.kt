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
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import nestctrl.generated.resources.Res
import nestctrl.generated.resources.blobhai_trans
import nestdrop.deck.Deck
import nestdrop.deck.PresetQueues
import nestdrop.deck.loadDeckSettings
import nestdrop.loadNestdropConfig
import nestdrop.parseNestdropXml
import nestdrop.setupSpriteFX
import org.jetbrains.compose.resources.painterResource
import osc.runNestDropSend
import osc.startNestdropOSC
import tags.startTagsFileWatcher
import ui.App
import ui.components.verticalScroll
import ui.splashScreen
import utils.KWatchChannel
import utils.KWatchEvent
import utils.asWatchChannel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime


private val logger = KotlinLogging.logger { }
//val decks = MutableStateFlow<List<Deck>>(emptyList())

val presetQueues = PresetQueues()
val decks = List(4) { index ->
    when (val n = index + 1) {
        1 -> Deck(n, Color.hsl(0f, 1f, 0.5f)) // 0xFFff0000
        2 -> Deck(n, Color.hsl(123f, 0.95f, 0.51f)) // 0xFF0df918)
        3 -> Deck(n, Color.hsl(202f, 1f, 0.5f)) //0xFF00A2FF)
        4 -> Deck(n, Color.hsl(60f, 1f, 0.5f)) // 0xFFffff00)
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
            scanMilkdrop()
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

        logger.info { "starting beat counter" }
        startBeatCounter()

//        OSCMessage("thiswillfail", "string", 'c', "" to "")

        run {
            logger.info { "loading nestdrop XML" }
            loadNestdropConfig(parseNestdropXml(), presetQueues, decks)

            nestdropConfig
                .asWatchChannel(KWatchChannel.Mode.SingleFile)
                .consumeAsFlow().mapNotNull {
                    if (it.kind == KWatchEvent.Kind.Modified) {
                        try {
                            parseNestdropXml()
                        } catch (e: Exception) {
                            logger.error(e) { "failed to load XML after file modification was detected" }
                            null
                        }
                    } else {
                        null
                    }
                }
                .distinctUntilChanged()
                .onEach {
                    logger.info { "parsed (changed) xml from $nestdropConfig" }
                    loadNestdropConfig(it, presetQueues, decks)
                }
                .launchIn(flowScope)
        }


        withTimeoutOrNull(45.seconds) {
            logger.info { "waiting for queues to be initialized" }
            while (!presetQueues.isInitialized.value) {
                delay(100)
            }
            true
        } ?: error("failed to load queues (timeout)")
        logger.info { "queues are initialized" }

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
        startNestdropOSC()

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
                        icon = painterResource(resource = Res.drawable.blobhai_trans)
                    ) {
                        splashScreen()
                    }
                } else {
                    Window(
                        onCloseRequest = ::exitApplication,
                        title = "Nest Ctrl",
                        state = rememberWindowState(width = 1600.dp, height = 1200.dp),
                        icon = painterResource(resource = Res.drawable.blobhai_trans)
                    ) {
                        App()
                    }
                }
            }
        }
    }
}
