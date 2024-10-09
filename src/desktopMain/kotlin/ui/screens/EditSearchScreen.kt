package ui.screens

import tags.Tag
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import tags.presetTagsMapping
import tags.TagMatcher
import tags.TagScoreEval
import tags.Term
import ui.components.DropDownPopupIconButton
import ui.components.verticalScroll

val customSearches = MutableStateFlow<List<TagScoreEval>>(emptyList())

@Composable
fun editSearchesScreen() {
    val searchesCollected by customSearches.collectAsState()
    val presetTags by presetTagsMapping.collectAsState()

    val contentState = rememberScrollState()

    var selectedSearch by remember { mutableStateOf<Pair<Int, TagScoreEval>?>(null) }
    val scope = rememberCoroutineScope()
    Row {
        verticalScroll {
            Column {
                Button(
                    onClick = {
                        selectedSearch = null
                        scope.launch {
                            contentState.scrollTo(0)
                        }
                    },
                    colors = if (selectedSearch == null) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("new")
                }
                searchesCollected.forEachIndexed() { i, clickedSearch ->
                    Button(
                        onClick = {
                            selectedSearch = i to clickedSearch
                            scope.launch {
                                contentState.scrollTo(0)
                            }
                        },
                        colors = if (selectedSearch?.first == i) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(clickedSearch.label)
                    }
                }
            }
        }
        verticalScroll(state = contentState) {
            Column() {
                if (selectedSearch == null) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var newSearchField by rememberSaveable(key = "new label") { mutableStateOf(TextFieldValue("")) }

                        Spacer(modifier = Modifier.weight(0.5f))
                        Text("Add new Entry", modifier = Modifier.padding(16.dp))

                        OutlinedTextField(
                            value = newSearchField,
                            onValueChange = { newText ->
                                newSearchField = newText
                            },
                            singleLine = true,
                            modifier = Modifier.onKeyEvent { event ->
                                if (event.key == Key.Enter) {
                                    if (newSearchField.text.isNotBlank()) {
                                        val newSearch = TagScoreEval(
                                            label = newSearchField.text, terms = emptyList()
                                        )
                                        selectedSearch = searchesCollected.size to newSearch
                                        customSearches.value = searchesCollected + newSearch
                                        newSearchField = TextFieldValue("")
                                    }

                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = {
                                val newSearch = TagScoreEval(
                                    label = newSearchField.text, terms = emptyList()
                                )
                                selectedSearch = searchesCollected.size to newSearch
                                customSearches.value = searchesCollected + newSearch
                                //TODO: autoselect new search ? calculate index
                                newSearchField = TextFieldValue("")
                            },
                            enabled = newSearchField.text.isNotBlank()
                        ) {
                            Icon(Icons.Filled.Add, "confirm")
                        }
                    }
                } else {
                    selectedSearch?.also { (searchIndex, search) ->
                        var newLabel by rememberSaveable(
                            key = "search.$searchIndex.editLabel"
                        ) { mutableStateOf(TextFieldValue(search.label)) }

                        Column {
                            // section header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Entry: ${search.label}")
                                Spacer(modifier = Modifier.weight(0.5f))
                                Text("Delete Entry", modifier = Modifier.padding(16.dp))
                                deleteButtonWithConfirmation() {
                                    val searchesMutable = searchesCollected.toMutableList()
                                    searchesMutable.removeAt(searchIndex)
                                    selectedSearch = null
                                    customSearches.value = searchesMutable.toList()
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.weight(0.5f))

                                Text("Edit Label", modifier = Modifier.padding(16.dp))
                                OutlinedTextField(
                                    value = newLabel,
                                    onValueChange = { newText ->
                                        newLabel = newText
                                    },
                                    modifier = Modifier.onKeyEvent { event ->
                                        if (event.key == Key.Enter) {
                                            if (newLabel.text != search.label) {
                                                val searchesMutable = searchesCollected.toMutableList()
                                                val newSearch = search.copy(label = newLabel.text)
                                                searchesMutable[searchIndex] = newSearch
                                                selectedSearch = searchIndex to newSearch
                                                customSearches.value = searchesMutable.toList()
                                            }

                                            true
                                        } else {
                                            false
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(16.dp))

                                IconButton(
                                    onClick = {
                                        val searchesMutable = searchesCollected.toMutableList()

                                        val newSearch = search.copy(label = newLabel.text)
                                        searchesMutable[searchIndex] = newSearch
                                        selectedSearch = searchIndex to newSearch
                                        customSearches.value = searchesMutable.toList()
                                    },
                                    enabled = newLabel.text != search.label
                                ) {
                                    Icon(Icons.Filled.Check, "confirm")
                                }
                            }


                            Column {
                                // section header
                                Row(verticalAlignment = Alignment.CenterVertically) {

                                    Text("Terms (${search.terms.size})")

                                    Spacer(modifier = Modifier.weight(0.5f))

                                    var newBoostValue by remember {
                                        mutableStateOf("10.0")
                                    }
                                    Text("Add weighted Term", modifier = Modifier.padding(16.dp))
                                    OutlinedTextField(
                                        value = newBoostValue,
                                        onValueChange = {
                                            newBoostValue = it
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal
                                        ),
                                        modifier = Modifier.onKeyEvent { event ->
                                            if (event.key == Key.Enter) {
                                                if (newBoostValue.toDoubleOrNull() != null) {

                                                    val searchesMutable = searchesCollected.toMutableList()
                                                    val mutableBoosts = search.terms.toMutableList()
                                                    mutableBoosts.add(
                                                        Term(
                                                            matcher = TagMatcher(
                                                                emptySet(), emptySet()
                                                            ),
                                                            boost = (newBoostValue.toDoubleOrNull() ?: 10.0)
                                                        )
                                                    )

                                                    val newSearch = search.copy(
                                                        terms = mutableBoosts.toList()
                                                    )
                                                    searchesMutable[searchIndex] = newSearch
                                                    selectedSearch = searchIndex to newSearch
                                                    customSearches.value = searchesMutable.toList()
                                                }

                                                true
                                            } else {
                                                false
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))

                                    IconButton(
                                        onClick = {
                                            val searchesMutable = searchesCollected.toMutableList()
                                            val mutableBoosts = search.terms.toMutableList()
                                            mutableBoosts.add(
                                                Term(
                                                    matcher = TagMatcher(
                                                        include = emptySet(), exclude = emptySet()
                                                    ),
                                                    boost = (newBoostValue.toDoubleOrNull() ?: 10.0)
                                                )
                                            )

                                            val newSearch = search.copy(
                                                terms = mutableBoosts.toList()
                                            )
                                            searchesMutable[searchIndex] = newSearch
                                            selectedSearch = searchIndex to newSearch
                                            customSearches.value = searchesMutable.toList()
                                        },
                                        enabled = newBoostValue.toDoubleOrNull() != null
                                    ) {
                                        Icon(Icons.Filled.Add, "add")
                                    }
                                }

                                search.terms.forEachIndexed() { termIndex, term ->
                                    val matcher = term.matcher
                                    val boost = term.boost
                                    ExpandableSection(
                                        key = "searches.$searchIndex.terms.$termIndex.expanded",
                                        default = true,
                                        headerContent = { expanded ->

                                            var boostField by remember {
                                                mutableStateOf("$boost")
                                            }
                                            if (expanded) {

                                                Text("Include: ${matcher.include.size}, Exclude: ${matcher.exclude.size} -> ${boost}")
                                            } else {
                                                Text("Include: ${matcher.include}, Exclude: ${matcher.exclude} -> ${boost}")
                                            }
                                            if (expanded) {

                                                Spacer(modifier = Modifier.weight(0.3f))
                                                Text("Boost", modifier = Modifier.padding(16.dp))
                                                OutlinedTextField(
                                                    value = boostField,
                                                    onValueChange = {
                                                        boostField = it
                                                    },
                                                    keyboardOptions = KeyboardOptions(
                                                        keyboardType = KeyboardType.Decimal
                                                    ),
                                                    modifier = Modifier.onKeyEvent { event ->
                                                        if (event.key == Key.Enter) {
                                                            if (boostField.toDoubleOrNull()?.let { it != boost } == true) {

                                                                val searchesMutable = searchesCollected.toMutableList()
                                                                val mutableBoosts = search.terms.toMutableList()
                                                                mutableBoosts[termIndex] = Term(
                                                                    matcher = matcher,
                                                                    boost = (boostField.toDoubleOrNull() ?: boost)
                                                                )
                                                                val newSearch = search.copy(
                                                                    terms = mutableBoosts.toList()
                                                                )
                                                                searchesMutable[searchIndex] = newSearch
                                                                selectedSearch = searchIndex to newSearch
                                                                customSearches.value = searchesMutable.toList()
                                                            }

                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    }
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val searchesMutable = searchesCollected.toMutableList()
                                                        val mutableBoosts = search.terms.toMutableList()
                                                        mutableBoosts[termIndex] = Term(
                                                            matcher = matcher,
                                                            boost = (boostField.toDoubleOrNull() ?: boost)
                                                        )
                                                        val newSearch = search.copy(
                                                            terms = mutableBoosts.toList()
                                                        )
                                                        searchesMutable[searchIndex] = newSearch
                                                        selectedSearch = searchIndex to newSearch
                                                        customSearches.value = searchesMutable.toList()
                                                    },
                                                    enabled = boostField.toDoubleOrNull()?.let { it != boost } == true
                                                ) {
                                                    Icon(Icons.Filled.Check, "confirm")
                                                }

                                                Spacer(modifier = Modifier.weight(0.1f))
                                                Text("Delete Term", modifier = Modifier.padding(16.dp))

                                                deleteButtonWithConfirmation() {
                                                    val searchesMutable = searchesCollected.toMutableList()

                                                    val mutableBoosts = search.terms.toMutableList()
                                                    mutableBoosts.removeAt(termIndex)

                                                    val newSearch = search.copy(
                                                        terms = mutableBoosts.toList()
                                                    )
                                                    searchesMutable[searchIndex] = newSearch
                                                    selectedSearch = searchIndex to newSearch

                                                    customSearches.value = searchesMutable.toList()
                                                }
                                            }
                                        },
                                        modifier = Modifier,
                                    ) {
                                        ExpandableSection(
                                            key = "searches.$searchIndex.terms.$termIndex.include.expanded",
                                            canExpand = matcher.include.isNotEmpty(),
                                            headerContent = { expand ->
                                                if (!expand) {
                                                    if (matcher.include.isNotEmpty()) {
                                                        Text("Include ${matcher.include}")
                                                    } else {
                                                        Text("Include")
                                                    }
                                                } else {
                                                    Text("Include ${matcher.include.size}")
                                                }
                                                Spacer(modifier = Modifier.weight(0.5f))

                                                val availableTags =
                                                    presetTags.values.flatten().toSet().sortedBy { it.toString() }

                                                DropDownPopupIconButton(
                                                    icon = { Icon(Icons.Filled.Add, "add") },
                                                    items = availableTags,
                                                    itemEnabled = { item -> (item !in matcher.include && item !in matcher.exclude) },
                                                    onItemClick = { item ->
                                                        val searchesMutable = searchesCollected.toMutableList()

                                                        val mutableBoosts = search.terms.toMutableList()
                                                        mutableBoosts[termIndex] = Term(
                                                            matcher = matcher.copy(
                                                                include = matcher.include + item
                                                            ),
                                                            boost = boost
                                                        )
                                                        val newSearch = search.copy(
                                                            terms = mutableBoosts.toList()
                                                        )
                                                        searchesMutable[searchIndex] = newSearch
                                                        selectedSearch = searchIndex to newSearch

                                                        customSearches.value = searchesMutable.toList()
                                                    },
                                                    renderItem = { item ->
                                                        Text(item.toString())
                                                    }
                                                )
                                            },
//                                    default = true,
                                            modifier = Modifier
                                        ) {
                                            matcher.include.forEachIndexed { tagIndex, tag ->
                                                Row {
                                                    Text(tag.toString())
                                                    Spacer(modifier = Modifier.weight(0.5f))
                                                    deleteButtonWithConfirmation() {
                                                        val searchesMutable = searchesCollected.toMutableList()

                                                        val mutableBoosts = search.terms.toMutableList()
                                                        mutableBoosts[termIndex] = Term(
                                                            matcher = matcher.copy(
                                                                include = matcher.include - tag
                                                            ),
                                                            boost = boost
                                                        )

                                                        val newSearch = search.copy(
                                                            terms = mutableBoosts.toList()
                                                        )
                                                        searchesMutable[searchIndex] = newSearch
                                                        selectedSearch = searchIndex to newSearch

                                                        customSearches.value = searchesMutable.toList()
                                                    }
                                                }
                                            }
                                        }
                                        ExpandableSection(
                                            key = "searches.$searchIndex.terms.$termIndex.exclude.expanded",
                                            canExpand = matcher.exclude.isNotEmpty(),
                                            headerContent = { expand ->
                                                if (!expand) {
                                                    if (matcher.exclude.isNotEmpty()) {
                                                        Text("Exclude ${matcher.exclude}")
                                                    } else {
                                                        Text("Exclude")
                                                    }
                                                } else {
                                                    Text("Exclude ${matcher.exclude.size}")
                                                }
                                                Spacer(modifier = Modifier.weight(0.5f))

                                                val availableTags =
                                                    presetTags.values.flatten().toSet().sortedBy { it.toString() }
                                                DropDownPopupIconButton(
                                                    icon = { Icon(Icons.Filled.Add, "add") },
                                                    items = availableTags,
                                                    itemEnabled = { item -> (item !in matcher.include && item !in matcher.exclude) },
                                                    onItemClick = { item ->
                                                        val searchesMutable = searchesCollected.toMutableList()

                                                        val mutableBoosts = search.terms.toMutableList()
                                                        mutableBoosts[termIndex] = Term(
                                                            matcher = matcher.copy(
                                                                exclude = matcher.exclude + item
                                                            ),
                                                            boost = boost
                                                        )

                                                        val newSearch = search.copy(
                                                            terms = mutableBoosts.toList()
                                                        )
                                                        searchesMutable[searchIndex] = newSearch
                                                        selectedSearch = searchIndex to newSearch

                                                        customSearches.value = searchesMutable.toList()
                                                    },
                                                    renderItem = { item ->
                                                        Text(item.toString())
                                                    }
                                                )
                                            },
//                                    default = true,
                                            modifier = Modifier
                                        ) {
                                            matcher.exclude.forEachIndexed { tagIndex, tag ->
                                                Row {
                                                    Text(tag.toString())
                                                    Spacer(modifier = Modifier.weight(0.5f))

                                                    deleteButtonWithConfirmation() {
                                                        val searchesMutable = searchesCollected.toMutableList()

                                                        val mutableBoosts = search.terms.toMutableList()
                                                        mutableBoosts[termIndex] = Term(
                                                            matcher = matcher.copy(
                                                                exclude = matcher.exclude - tag
                                                            ),
                                                            boost = boost
                                                        )

                                                        val newSearch = search.copy(
                                                            terms = mutableBoosts.toList()
                                                        )
                                                        searchesMutable[searchIndex] = newSearch
                                                        selectedSearch = searchIndex to newSearch

                                                        customSearches.value = searchesMutable.toList()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ExpandableSectionHeader(
    isExpanded: Boolean,
    canExpand: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(Boolean) -> Unit
) {

    val icon = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown

    Row(modifier = modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
//        if (canExpand){
        Image(
            modifier = Modifier.size(32.dp),
            imageVector = icon,
            colorFilter = if (canExpand) {
                ColorFilter.tint(color = MaterialTheme.colors.onSurface)
            } else {
                ColorFilter.tint(color = Color.DarkGray)
            },
            contentDescription = "expandable",
        )
//        }
        content(isExpanded)
//        Text(
//            text = title,
////            style = MaterialTheme.typography.headlineMedium
//        )
    }
}

private val expandedStates = mutableStateMapOf<String, Boolean>()

@Composable
fun ExpandableSection(
    key: String,
    modifier: Modifier = Modifier,
    default: Boolean = false,
    canExpand: Boolean = true,
    headerContent: @Composable RowScope.(Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isExpanded = expandedStates[key] ?: default
    val lineColor = MaterialTheme.colors.onSurface
    Column(
        modifier = modifier
            .background(color = MaterialTheme.colors.surface)
            .fillMaxWidth()
            .padding(4.dp)
            .drawBehind {
                if (canExpand && isExpanded) {
                    drawLine(
                        color = lineColor,
                        start = Offset(-1.0f, 0.0f),
                        end = Offset(-1.0f, size.height),
                        strokeWidth = Stroke.HairlineWidth
                    )
                }
            }
    ) {
        ExpandableSectionHeader(
            isExpanded = canExpand && isExpanded,
            canExpand = canExpand,
//            title = title,
            content = headerContent,
            modifier = Modifier
                .then(
                    if (canExpand) {
                        Modifier
                            .clickable {
                                expandedStates[key] = !isExpanded
                            }
                    } else {
                        Modifier
                    }
                )
//                .padding(4.dp)
        )


        AnimatedVisibility(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxWidth(),
            visible = canExpand && isExpanded
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun deleteButtonWithConfirmation(enabled: Boolean = true, onConfirm: () -> Unit) {
    var showConfirmation by mutableStateOf(false)
    IconButton(
        onClick = {
            if (!showConfirmation) {
                showConfirmation = true
            } else {
                showConfirmation = false
                onConfirm()
            }
        },
        enabled = enabled,
    ) {
        Icon(
            if (showConfirmation) {
                Icons.Filled.Delete
            } else {
                Icons.Outlined.Delete
            }, "delete",
            tint = Color.Red
        )
    }
}