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

import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

/**
 * Buffers items until a single collector collects them.
 *
 * @param <T> the item type
 */
class UnicastWorkSubject<T> : AbstractFlow<T>(), SubjectAPI<T> {

    companion object {
        val terminated = Throwable("No more elements")
    }

    val queue = ConcurrentLinkedQueue<T>()

    @Volatile
    var terminal : Throwable? = null

    val resumable = Resumable()

    val current = AtomicReference<FlowCollector<T>>()

    override suspend fun collectSafely(collector: FlowCollector<T>) {
        while (true) {
            val curr = current.get()
            if (curr != null) {
                throw IllegalStateException("Only one collector allowed")
            }
            if (current.compareAndSet(curr, collector)) {
                break
            }
        }

        while (true) {
            val t = terminal
            val v = queue.poll()

            if (t != null && v == null) {
                current.getAndSet(null)
                if (t != terminated) {
                    throw t
                }
                return
            }
            if (v != null) {
                try {
                    collector.emit(v)
                } catch (e: Throwable) {
                    current.getAndSet(null)
                    throw e
                }
            } else {
                resumable.await()
            }
        }
    }

    override suspend fun emitError(ex: Throwable) {
        terminal = ex
        resumable.resume()
    }

    override suspend fun complete() {
        terminal = terminated
        resumable.resume()
    }

    override fun hasCollectors(): Boolean {
        val curr = current.get()
        return curr != null
    }

    override fun collectorCount(): Int {
        return if (hasCollectors()) 1 else 0
    }

    override suspend fun emit(value: T) {
        queue.offer(value)
        resumable.resume()
    }
}