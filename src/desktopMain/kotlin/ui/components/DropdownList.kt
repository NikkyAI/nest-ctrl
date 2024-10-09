package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times

@Composable
fun <T : Any> Dropdown(
    color: Color = Color.DarkGray,
    activeColor: Color = Color.Gray,
    itemList: List<T>,
    selectedItem: T,
    renderItem: @Composable (item: T) -> Unit,
    onItemClick: (T) -> Unit
) {

//    val items = listOf("Item Number 1", "Item Number 2", "Item Number 3")
//    val selectedItem = itemList.getOrNull(selectedIndex)
    var expanded by remember { mutableStateOf(false) }

//    var showDropdown by remember { mutableStateOf(true) }
//    val scrollState = rememberScrollState()

//    Box(
//        modifier = Modifier
////            .fillMaxSize()
//            .background(Color.Black)
//            .padding(16.dp),
//        contentAlignment = Alignment.Center
//    ) {
    Column(
//        horizontalAlignment = Alignment.End,
        modifier = Modifier
            .width(250.dp)
            .height(50.dp)
            .background(color, RoundedCornerShape(4.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .clickable {
                    expanded = true
                }
        ) {
            renderItem(selectedItem)
//                Text(
//                    text = selectedItem ?: "error",
//                    color = Color.White,
//                    modifier = Modifier.weight(1f)
//                )
            Spacer(
                modifier = Modifier.weight(0.9f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.White,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .height(itemList.size * 36.dp + 16.dp)
                .align(Alignment.End)
                .background(color)
//                    .fillMaxWidth()
//                    .background(Color.Red)
        ) {
//            verticalScroll {
//                Column(
//                    modifier = Modifier
//                        .height(500.dp)
//                ) {
            itemList.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        onItemClick(item)
                        expanded = false
                    },
                    modifier = Modifier.background(
                        if (item == selectedItem) activeColor else color
                    ).height(36.dp).padding(2.dp)
                ) {
                    renderItem(item)
                }
            }
        }
    }

}

@Composable
fun <T> DropDownPopupIconButton(
    icon: @Composable () -> Unit,
    items: List<T>,
    onItemClick: (T) -> Unit,
    itemEnabled: (T) -> Boolean = { true },
    renderItem: @Composable (T) -> Unit
) {
//    Row {
        var opened by remember { mutableStateOf(false) }
        IconButton(
            onClick = {
                opened = true
            },
        ) {
            icon()
        }
        DropdownMenu(
            expanded = opened,
            onDismissRequest = {
                opened = false
            },
            modifier = Modifier
                .height(items.size * 36.dp + 16.dp)
//            .align(Alignment.CenterVertically)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        onItemClick(item)
                        opened = false
                    },
                    modifier = Modifier
                        .height(36.dp)
                        .padding(2.dp),
                    enabled = itemEnabled(item)
                ) {
                    renderItem(item)
                }
            }
        }
//    }
}