import java.io.File

val userHome = File(System.getProperty("user.home"))
val nestdropFolder = userHome.resolve("VJ").resolve("NestDropProV2") // File("C:\\Users\\nikky\\VJ\\NestDropProV2")
val nestdropConfig = nestdropFolder.resolve("DefaultUserProfile.xml")
val nestdropPerformanceLog = nestdropFolder.resolve("PerformanceHistory").canonicalFile
val nestdropImgModes = nestdropFolder.resolve("Plugins\\milk2_img.ini").canonicalFile