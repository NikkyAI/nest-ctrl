package ui.components

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun verticalScroll(
    state: ScrollState = rememberScrollState(0),
    content: @Composable (BoxScope.() -> Unit)
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
//            .fillMaxSize()
//            .background(color = Color(180, 180, 180))
//            .padding(10.dp)
    ) {
//        val stateVertical = rememberScrollState(0)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(
                    end = 12.dp,
//                bottom = 12.dp,
                )
                .verticalScroll(state)
        ) {

            content()
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(state),
            style = LocalScrollbarStyle.current.copy(
                thickness = 16.dp,
                shape = RoundedCornerShape(8.dp)

            )
        )
    }
}
@Composable
fun verticalScrollStart(
    state: ScrollState = rememberScrollState(0),
    content: @Composable (BoxScope.() -> Unit)
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(
                    start = 12.dp,
//                bottom = 12.dp,
                )
                .verticalScroll(state)
        ) {

            content()
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterStart)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(state),
            style = LocalScrollbarStyle.current.copy(
                thickness = 16.dp,
                shape = RoundedCornerShape(8.dp)

            )
        )
    }
}


@Composable
fun lazyList(
    state: LazyListState = rememberLazyListState(),
    content: (LazyListScope.() -> Unit)
) {
    Box(
        modifier = Modifier.fillMaxSize()
            .padding(10.dp)
    ) {
//        val state = rememberLazyListState()
        LazyColumn(Modifier.fillMaxSize().padding(end = 12.dp), state) {
            content()
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(
                scrollState = state
            )
        )
    }
}