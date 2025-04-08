package ui.components

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.toFontFamily
import nestctrl.generated.resources.DSEG14_Classic
import nestctrl.generated.resources.Res
import org.jetbrains.compose.resources.Font

val fontDseg14 = Font(
    Res.font.DSEG14_Classic,
    weight = FontWeight.W400,
    style = FontStyle.Normal
).toFontFamily()