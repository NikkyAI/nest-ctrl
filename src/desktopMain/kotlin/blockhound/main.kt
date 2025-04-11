package blockhound

import reactor.blockhound.BlockHound
import kotlinx.coroutines.debug.CoroutinesBlockHoundIntegration
import kotlinx.coroutines.*

fun main() {
    BlockHound.install(CoroutinesBlockHoundIntegration())
    
    runBlocking { 
        launch(Dispatchers.Default) { 
            Thread.sleep(1000) // Exception
        }
    }
}