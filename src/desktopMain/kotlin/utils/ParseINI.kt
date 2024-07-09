package utils

import java.io.File


fun parseINI(imgModesFile: File): List<Pair<Int, String>> {
//    val imgModesFile = File("C:\\Users\\nikky\\VJ\\NestDropProV2\\Plugins\\milk2_img.ini")
    return imgModesFile.readText().split("\n[img")
        .mapNotNull { section ->
        val id = section.substringBefore(']').toIntOrNull() ?: return@mapNotNull null
        val comment = section
            .lines()
            .filter { it.trim().startsWith("//") }
            .joinToString("\n") {
                it.substringAfter("//")//.split(", ").joinToString("\n")
            }
        id to comment
    }
}