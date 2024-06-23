package ui.screens

import Tag
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import decks
import kotlinx.coroutines.launch
import presetTags
import presetsFolder
import tags.TagMatcher
import tags.TagScoreEval
import ui.components.lazyList
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun tagSearchScreen() {
    val tagScore = TagScoreEval(
        label = "Test",
        boosts = mapOf(
            TagMatcher(
                include = setOf(
                    Tag("wavy", namespace = listOf("nikky", "motion"))
                ),
                exclude = emptySet(),
            ) to 10.0,
            TagMatcher(
                include = setOf(
                    Tag("bright", namespace = listOf("nikky", "caution"))
                ),
                exclude = emptySet(),
            ) to -10.0,
            TagMatcher(
                include = setOf(
                    Tag("flashy", namespace = listOf("nikky", "caution"))
                ),
                exclude = emptySet(),
            ) to -10.0,
        )
    )

    val presets by presetsMap.collectAsState()
    val presetTags by presetTags.collectAsState()

    val sortedKeys = presets.keys.sortedByDescending { key ->
        val tags = presetTags[key].orEmpty()

        tagScore.score(tags)
    }

    val filtered = presets.keys.mapNotNull { key ->
        val tags = presetTags[key].orEmpty()

        val score = tagScore.score(tags)
        val preset = presets[key]
        if(preset != null && score >= 0.0) {
            preset to (score + 0.01)
        } else {
            null
        }
    }.toMap()

    lazyList {
        stickyHeader {
            Row {
                Row(
                    modifier = Modifier
                        .background(Color.Black)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    var presetEntry by remember { mutableStateOf(
                        pickItemToGenerate(filtered)
                    ) }
                    val tags = presetTags[presetEntry.name].orEmpty()
                    Button(
                        {
                            presetEntry = pickItemToGenerate(filtered)
                        }
                    ) {
                        Text("Next")
                    }


                    val image = imageFromFile(presetsFolder.resolve(presetEntry.previewPath))

                    Row {
                        Column {
                            Image(bitmap = image, contentDescription = presetEntry.previewPath)

                            Text("ID: ${presetEntry.id}")
                            val score = tagScore.score(tags)
                            Text("Score: $score")
                        }
//                Spacer(modifier = Modifier.width(10.dp))
//                Column {
//                }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier.width(300.dp)
                        ) {
                            tags.forEach {
                                Text(it.label)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(presetEntry.name)
                    }

                }
            }
        }


        items(
            sortedKeys
        ) { key ->
            val presetEntry = presets[key] ?: return@items
            val tags = presetTags[key].orEmpty()

            val image = remember { imageFromFile(presetsFolder.resolve(presetEntry.previewPath)) }
            Row {
                Column {
                    Image(bitmap = image, contentDescription = presetEntry.previewPath)

                    Text("ID: ${presetEntry.id}")
                    val score = tagScore.score(tags)
                    Text("Score: $score")
                }
//                Spacer(modifier = Modifier.width(10.dp))
//                Column {
//                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier.width(300.dp)
                ) {
                    tags.forEach {
                        Text(it.label)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(presetEntry.name)
            }

        }
    }
}

fun <T> pickItemToGenerate(options: Map<T, Double>): T {
    val randomNumber = Math.random() * options.values.sumOf { it }

    var probabilityIterator = 0.0
    options.forEach { (item, score) ->
        probabilityIterator += score
        if (probabilityIterator >= randomNumber) {
            return item
        }
    }
    error("no options")
}