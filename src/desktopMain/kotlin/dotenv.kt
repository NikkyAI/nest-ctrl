import io.github.cdimascio.dotenv.dotenv
import java.io.File

val dotenv = dotenv {
    if(File(".env").exists()) {
        println("loading ${File(".env").canonicalFile}")
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
