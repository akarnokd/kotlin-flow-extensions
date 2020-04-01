/*
 * Copyright 2019 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        val READY = ReadyContinuation()
        val VALUE = Object()
    }

    /**
     * Await the resumption of this Resumable, suspending the
     * current coroutine if necessary.
     * Only one thread can call this method.
     */
    suspend fun await() {
        suspendCancellableCoroutine<Any> {
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

    /**
     * Resume this Resumable, resuming any currently suspended
     * [await] callers.
     * This method can be called by any number of threads.
     */
    fun resume() {
        if (get() == READY) {
            return
        }
        getAndSet(READY)?.resumeWith(Result.success(VALUE))
    }

    /**
     * Represents a stateless indicator if the continuation is already
     * ready for resumption, thus no need to get suspended.
     */
    private class ReadyContinuation : Continuation<Any> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<Any>) {
            // The existence already indicates resumption
        }
    }
}