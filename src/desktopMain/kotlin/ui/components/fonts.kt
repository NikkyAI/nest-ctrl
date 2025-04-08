package ui.components

import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.toFontFamily
import nestctrl.generated.resources.*
import nestctrl.generated.resources.Res
import org.jetbrains.compose.resources.Font

@Composable
fun Dseg14ClassicFontFamily() = FontFamily(
    Font(Res.font.DSEG14Classic_Light, weight = FontWeight.Light),
    Font(Res.font.DSEG14Classic_LightItalic, weight = FontWeight.Light, style = FontStyle.Italic),
    Font(Res.font.DSEG14Classic_Regular, weight = FontWeight.Normal),
    Font(Res.font.DSEG14Classic_Italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(Res.font.DSEG14Classic_Bold, weight = FontWeight.Bold),
    Font(Res.font.DSEG14Classic_BoldItalic, weight = FontWeight.Bold, style = FontStyle.Italic),
)