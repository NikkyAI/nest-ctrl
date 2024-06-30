package nestdrop

import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import spritesFolder
import ui.screens.imageFromFile

sealed class PresetLocation {
    abstract val id: Int
    data class Milk(
        val name: String,
        override val id: Int,
        val path: String,
        val previewPath: String,
        val category: String,
        val subCategory: String? = null,
    ): PresetLocation()

    data class Img(
        val name: String,
        override val id: Int,
        val path: String,
        val category: String,
        val subCategory: String? = null,
    ): PresetLocation() {
        val image by lazy {
            val img = Image.makeFromEncoded(spritesFolder.resolve(path).readBytes())
            img.scaleToThumb(64).toComposeImageBitmap()
//            imageFromFile(spritesFolder.resolve(path))
        }
//        val previewImage by lazy {
//
//        }
    }
}

fun Image.scaleToThumb(newHeight: Int = 128): Image {
    val aspect = width.toFloat() / height.toFloat()

    val newWidth = (newHeight * aspect).toInt()

    val surface = Surface.makeRasterN32Premul(newWidth, newHeight)

    surface.canvas.drawImageRect(
        image = this,
        src = Rect.makeWH(width.toFloat(), height.toFloat()),
        dst = Rect.makeWH(newWidth.toFloat(), newHeight.toFloat()),
        samplingMode = SamplingMode.LINEAR,
        paint = null,
        strict = true
    )

    return surface.makeImageSnapshot()
}