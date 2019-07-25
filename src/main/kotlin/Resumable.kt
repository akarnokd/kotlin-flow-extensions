package hu.akarnokd.kotlin.flow

import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.suspendCoroutine

/**
 * Primitive for suspending and resuming coroutines on demand
 */
open class Resumable : AtomicReference<Continuation<Any>>() {

    private companion object {
        val READY = ReadyContinuation();
        val VALUE = Object()
    }

    suspend fun await() {
        suspendCoroutine<Any> {
            while (true) {
                val current = get()
                if (current == READY) {
                    it.resumeWith(Result.success(VALUE))
                    break
                }
                if (current != null) {
                    throw IllegalStateException("Only one thread can await a Resumable")
                }
                if (compareAndSet(current, it)) {
                    break
                }
            }
        }
        getAndSet(null)
    }

    fun resume() {
        if (get() == READY) {
            return
        }
        getAndSet(READY)?.resumeWith(Result.success(VALUE))
    }

    private class ReadyContinuation : Continuation<Any> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<Any>) {
            // The existence already indicates resumption
        }
    }
}