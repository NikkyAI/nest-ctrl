import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger { }
val userHome = File(System.getProperty("user.home")).also {
    logger.info { "user.home: $it" }
}
val configFolder = dotenv["NESTCTRL_CONFIG_FOLDER"]?.parsePath() ?: userHome.resolve(".nestctrl")
val tagsFolder = configFolder.resolve("tags").also {
    logger.info { "tags folder: $it" }
}
val configFile = configFolder.resolve("config.json5").also {
    logger.info { "config file: $it" }
}

private fun String.parsePath(): File {
    val rawPath = this
    val path = if (rawPath.startsWith("~/") || rawPath.startsWith("~\\")) {
        System.getProperty("user.home") + rawPath.substring(1)
    } else {
        rawPath
    }
        .replace("/", File.separator)
        .replace("\\", File.separator)
    return File(path).canonicalFile
}

val nestdropFolder
    get() = dotenv["NESTDROP_PATH"]
        ?.parsePath()
        ?: userHome.resolve("VJ").resolve("NestDropProV2")

val nestdropConfigFile: File
    get() {
        val filename = (dotenv["NESTDROP_PROFILE"] ?: System.getenv("NESTDROP_PROFILE")) ?: "DefaultUserProfile.xml"
        return nestdropFolder.resolve(filename).canonicalFile
    }

val presetsFolder: File = nestdropFolder.resolve("Plugins").resolve("Milkdrop2").resolve("Presets").canonicalFile
val spritesFolder: File = nestdropFolder.resolve("Plugins").resolve("Milkdrop2").resolve("Sprites").canonicalFile

val nestdropImgModes: File = nestdropFolder.resolve("Plugins\\milk2_img.ini").canonicalFile
