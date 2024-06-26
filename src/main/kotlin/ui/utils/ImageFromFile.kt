package ui.screens

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.File

fun imageFromFile(file: File): ImageBitmap {
    return org.jetbrains.skia.Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
}