import io.github.cdimascio.dotenv.dotenv
import java.io.File

val dotenv = dotenv()

val userHome = File(System.getProperty("user.home"))

val nestdropFolder = dotenv["NESTDROP"]?.let {
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
        File(it)
    }
    ?: userHome.resolve("VJ").resolve("NestDropProV2") // File("C:\\Users\\nikky\\VJ\\NestDropProV2")

//val nestdropFolder = nestdropPath?.let {
//    File(it)
//} ?: userHome.resolve("VJ").resolve("NestDropProV2") // File("C:\\Users\\nikky\\VJ\\NestDropProV2")
val nestdropConfig = nestdropFolder.resolve("DefaultUserProfile.xml")

val presetsFolder = nestdropFolder.resolve("Plugins").resolve("Milkdrop2").resolve("Presets")

val nestdropPerformanceLog = nestdropFolder.resolve("PerformanceHistory").canonicalFile
val nestdropImgModes = nestdropFolder.resolve("Plugins\\milk2_img.ini").canonicalFile