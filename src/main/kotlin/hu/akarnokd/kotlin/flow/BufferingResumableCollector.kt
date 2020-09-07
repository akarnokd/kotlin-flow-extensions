/*
 * Copyright 2019-2020 David Karnok
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

import hu.akarnokd.kotlin.flow.impl.SpscArrayQueue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.FlowCollector
import java.util.concurrent.atomic.AtomicLong

/**
 * A collector that can buffer a fixed number of
 * items and drain it with much less need to
 * coordinate the resumption per items.
 */
open class BufferingResumableCollector<T> @Suppress("UNCHECKED_CAST") constructor(capacity: Int) : Resumable() {

    private val queue : SpscArrayQueue<T> = SpscArrayQueue(capacity)

    @Volatile
    private var done: Boolean = false
    private var error : Throwable? = null

    private val available = AtomicLong()

    private val valueReady = Resumable()

    private val output : Array<Any?> = Array(1) { null }

    private val limit : Int = capacity - (capacity shr 2)

    @Volatile
    private var cancelled = false

    suspend fun next(value: T) {
        while (!cancelled) {
            if (queue.offer(value)) {
                if (available.getAndIncrement() == 0L) {
                    valueReady.resume()
                }
                break
            }

            await()
        }
        if (cancelled) {
            throw CancellationException()
        }
    }

    fun error(ex: Throwable) {
        error = ex
        done = true
        valueReady.resume()
    }

    fun complete() {
        done = true
        valueReady.resume()
    }

    suspend fun drain(collector: FlowCollector<T>, onCrash: ((BufferingResumableCollector<T>) -> Unit)? = null) {
        val q = queue
        val avail = available
        val ready = valueReady
        var consumed = 0L
        val limit = this.limit.toLong()
        while (true) {
            val d = done

            val e = !q.poll(output)

            if (d && e) {
                val ex = error
                if (ex != null) {
                    throw ex
                }

                break
            }

            if (!e) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    collector.emit(output[0] as T)
                } catch (ex: Throwable) {
                    onCrash?.invoke(this)
                    cancelled = true
                    resume()

                    throw ex
                }

                if (consumed++ == limit) {
                    avail.addAndGet(-consumed)
                    consumed = 0
                    resume()
                }

                continue
            }

            if (avail.addAndGet(-consumed) == 0L) {
                resume()
                ready.await()
            }
            consumed = 0L
        }
    }
}