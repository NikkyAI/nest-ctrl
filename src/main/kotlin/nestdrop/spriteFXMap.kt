package nestdrop

import flowScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import nestdropImgModes
import utils.parseINI
import utils.splitIntoLines

val imgFxMap = MutableStateFlow(
//    emptyMap<Int, String>()
    parseINI(nestdropImgModes).toMap()
)

val imgFxLabels = List(100) {
    MutableStateFlow("")
//    OscSynced.Value("/sprite_FX/label/${it}", "", target = OscSynced.Target.TouchOSC).apply {
//        logSending = false
//    }
}


suspend fun setupSpriteFX() {
    imgFxMap.value = parseINI(nestdropImgModes).toMap()


    imgFxMap.onEach { map ->
        fun hasBurnEffect(id: Int): Boolean {
            return map[id]?.contains(", include burn effect") ?: false
        }

        fun hasNoBurnEffect(id: Int): Boolean {
            return map[id]?.contains(", no burn effect") ?: false
        }

        fun getBurnStr(id: Int): String = when {
            hasBurnEffect(id) -> "🔥" //"\uD83D\uDD25"
            hasNoBurnEffect(id) -> "💧" //"\uD83D\uDCA7"
            else -> ""
        }
        map.forEach { (id, comment) ->
            val commentStripped = comment
                .replace(", include burn effect", "")
                .replace(", no burn effect", "")

            imgFxLabels.getOrNull(id)?.value = when (id) {
                in (0..33 step 2), in (50..50 + 33 step 2) -> {
                    val (line1, line2) = splitIntoLines(2, commentStripped, splitter = " ")
                        .map {
                            it.padStart(31, ' ')
                        }

                    listOf(
                        "[$id]".padEnd(4, ' ')
                                + getBurnStr(id)
                            .padStart(1, ' ')
                                + line1,
                        "[${id + 1}]".padEnd(4, ' ')
                                + getBurnStr(id + 1)
                            .padStart(1, ' ')
                                + line2,
                    ).joinToString("\n")

                    listOf(
                        getBurnStr(id).padEnd(1, ' ')
                                + "[$id]".padEnd(4, ' ')
                                + line1,
                        getBurnStr(id + 1).padEnd(1, ' ')
                                + "[${id + 1}]".padEnd(4, ' ')
                                + line2,
                    ).joinToString("\n")

                }

                in (34..37), in (50 + 34..50 + 37) -> {
                    val (line1, line2, line3) = splitIntoLines(3, commentStripped, splitter = " ")
                        .map { line -> line.padEnd(18, ' ') }
                    val burn = getBurnStr(id)
                        .padEnd(2, ' ')

                    listOf(
                        line1 + "|$burn|".padStart(5, ' '),
                        line2 + "[$id]".padStart(5, ' '),
                        line3 + "|$burn|".padStart(5, ' '),
                    ).joinToString("\n")
                }

                in (38..49 step 2), in (50 + 38..50 + 49 step 2) -> {
                    val (line1, line11, line2, line22) = splitIntoLines(4, commentStripped, splitter = " ")
                        .map { line -> line.padEnd(21, ' ') }

                    val burn1 = getBurnStr(id).padEnd(1, ' ')
                    val burn2 = getBurnStr(id + 1).padEnd(1, ' ')

                    listOf(
                        line1 + "$burn1 [$id]".padStart(8, ' '),
                        line11 + "|  |".padStart(8, ' '),
                        line2 + "|  |".padStart(8, ' '),
                        line22 + "$burn2 [${id + 1}]".padStart(8, ' '),
                    ).joinToString("\n")
                }

                else -> ""
            }
        }
    }.launchIn(flowScope)
}