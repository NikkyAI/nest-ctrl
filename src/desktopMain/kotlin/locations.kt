import io.github.cdimascio.dotenv.dotenv
import java.io.File

val dotenv = dotenv {
    if(File(".env").exists()) {
        directory = "./"
        filename = ".env"
    } else {
        val dotenvFile = File(System.getProperty("user.home")).resolve(".nestctrl").resolve(".env")
        directory = dotenvFile.parentFile.path
        filename = dotenvFile.name
        dotenvFile.parentFile.mkdirs()
        dotenvFile.takeUnless { it.exists() }?.createNewFile()
    }
//    File(".env").takeIf { !it.exists() }?.createNewFile()
    ignoreIfMissing = true
}

val userHome = File(System.getProperty("user.home"))
val tagsFolder = userHome.resolve(".nestctrl").resolve("tags")

val nestdropFolder get() = (dotenv["NESTDROP"] ?: System.getenv("NESTDROP"))?.let {
    if (it.startsWith("~/") || it.startsWith("~\\")) {
        System.getProperty("user.home") + it.substring(1)
    } else {
        it
    }
}
    ?.replace("/", File.separator)
    ?.replace("\\", File.separator)
    ?.let {
//        System.err.println(it)
        File(it).canonicalFile
    }
    ?: userHome.resolve("VJ").resolve("NestDropProV2") // File("C:\\Users\\nikky\\VJ\\NestDropProV2")

val nestdropConfig: File
    get() {
        val filename = (dotenv["NESTDROP_PROFILE"] ?: System.getenv("NESTDROP_PROFILE")) ?: "DefaultUserProfile.xml"
        return nestdropFolder.resolve(filename).canonicalFile
    }

val presetsFolder: File = nestdropFolder.resolve("Plugins").resolve("Milkdrop2").resolve("Presets").canonicalFile
val spritesFolder: File = nestdropFolder.resolve("Plugins").resolve("Milkdrop2").resolve("Sprites").canonicalFile

val nestdropPerformanceLogFolder: File = nestdropFolder.resolve("PerformanceHistory").canonicalFile
val nestdropImgModes: File = nestdropFolder.resolve("Plugins\\milk2_img.ini").canonicalFile

val configFile = userHome.resolve(".nestctrl").resolve("config.json5")