import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Execute a suspendable block with a single-threaded executor service.
 */
suspend fun withSingle(block: suspend (ExecutorService) -> Unit) {
    val exec = Executors.newSingleThreadExecutor()
    try {
        block(exec)
    } finally {
        exec.shutdownNow()
    }
}