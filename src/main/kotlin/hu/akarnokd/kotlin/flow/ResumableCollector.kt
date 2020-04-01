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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * A collector that hosts a signal (value/error/completion)
 * and allows waiting for the signal and a consumer to be ready
 * to receive them.
 */
open class ResumableCollector<T> : Resumable() {
    @Suppress("UNCHECKED_CAST")
    var value: T = null as T
    var error: Throwable? = null
    private var done: Boolean = false
    private var hasValue: Boolean = false

    private val consumerReady = Resumable()

    suspend fun next(value : T) {
        consumerReady.await()

        this.value = value
        this.hasValue = true

        resume()
    }

    suspend fun error(error: Throwable) {
        consumerReady.await()

        this.error = error
        this.done = true

        resume()
    }

    suspend fun complete() {
        consumerReady.await()

        this.done = true

        resume()
    }

    private suspend fun awaitSignal() {
        await()
    }

    private fun readyConsumer() {
        consumerReady.resume()
    }

    suspend fun drain(collector: FlowCollector<T>, onComplete: ((ResumableCollector<T>) -> Unit)? = null) {
        while (coroutineContext.isActive) {

            readyConsumer()

            awaitSignal()

            if (hasValue) {
                val v = value
                @Suppress("UNCHECKED_CAST")
                value = null as T
                hasValue = false

                try {
                    if (coroutineContext.isActive) {
                        collector.emit(v)
                    } else {
                        throw CancellationException()
                    }
                } catch (exc: Throwable) {
                    onComplete?.invoke(this)

                    readyConsumer() // unblock waiters
                    throw exc
                }
            }

            if (done) {
                val ex = error
                if (ex != null) {
                    throw ex
                }
                break
            }
        }
    }
}