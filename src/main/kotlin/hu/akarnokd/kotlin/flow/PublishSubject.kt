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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import java.util.concurrent.atomic.AtomicReference

/**
 * Multicasts items to any number of collectors when they are ready to receive.
 *
 * @param <T> the element type of the [Flow]
 */
@FlowPreview
class PublishSubject<T> : AbstractFlow<T>(), SubjectAPI<T>  {

    private companion object {
        private val EMPTY = arrayOf<ResumableCollector<Any>>()
        private val TERMINATED = arrayOf<ResumableCollector<Any>>()
    }

    @Suppress("UNCHECKED_CAST")
    private val collectors = AtomicReference(EMPTY as Array<ResumableCollector<T>>)

    private var error : Throwable? = null

    /**
     * Returns true if this PublishSubject has any collectors.
     */
    override fun hasCollectors() : Boolean = collectors.get().isNotEmpty()

    /**
     * Returns the current number of collectors.
     */
    override fun collectorCount() : Int = collectors.get().size

    /**
     * Emit the value to all current collectors, waiting for each of them
     * to be ready for consuming it.
     */
    override suspend fun emit(value: T) {
        for (collector in collectors.get()) {
            try {
                collector.next(value)
            } catch (ex: CancellationException) {
                remove(collector);
            }
        }
    }

    /**
     * Throw an error on the consumer side.
     */
    override suspend fun emitError(ex: Throwable) {
        if (this.error == null) {
            this.error = ex
            @Suppress("UNCHECKED_CAST")
            for (collector in collectors.getAndSet(TERMINATED as Array<ResumableCollector<T>>)) {
                try {
                    collector.error(ex)
                } catch (_: CancellationException) {
                    // ignored
                }
            }
        }
    }

    /**
     * Indicate no further items will be emitted
     */
    override suspend fun complete() {
        @Suppress("UNCHECKED_CAST")
        for (collector in collectors.getAndSet(TERMINATED as Array<ResumableCollector<T>>)) {
            try {
                collector.complete()
            } catch (_: CancellationException) {
                // ignored
            }
        }
    }

    @Suppress("UNCHECKED_CAST", "")
    private fun add(inner: ResumableCollector<T>) : Boolean {
        while (true) {

            val a = collectors.get()
            if (a as Any == TERMINATED as Any) {
                return false
            }
            val n = a.size
            val b = a.copyOf(n + 1)
            b[n] = inner
            if (collectors.compareAndSet(a, b as Array<ResumableCollector<T>>)) {
                return true
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun remove(inner: ResumableCollector<T>) {
        while (true) {
            val a = collectors.get()
            val n = a.size
            if (n == 0) {
                return
            }

            val j = a.indexOf(inner)
            if (j < 0) {
                return
            }

            var b = EMPTY as Array<ResumableCollector<T>?>
            if (n != 1) {
                b = Array(n - 1) { null }
                System.arraycopy(a, 0, b, 0, j)
                System.arraycopy(a, j + 1, b, j, n - j - 1)
            }
            if (collectors.compareAndSet(a, b as Array<ResumableCollector<T>>)) {
                return
            }
        }
    }

    /**
     * Start collecting signals from this PublishSubject.
     */
    @FlowPreview
    override suspend fun collectSafely(collector: FlowCollector<T>) {
        val inner = ResumableCollector<T>()
        if (add(inner)) {
            inner.drain(collector) { remove(it) }
            return
        }

        val ex = error
        if (ex != null) {
            throw ex
        }
    }

}