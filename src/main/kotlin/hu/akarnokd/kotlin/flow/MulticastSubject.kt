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

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * A subject implementation that awaits a certain number of collectors
 * to start consuming, then allows the producer side to deliver items
 * to them.
 *
 * @param <T> the element type of the [Flow]
 * @param bufferSize the number of items to buffer until consumers arrive
 */
@FlowPreview
class MulticastSubject<T>(private val expectedCollectors: Int) : AbstractFlow<T>(), SubjectAPI<T> {

    val collectors = AtomicReference<Array<ResumableCollector<T>>>(EMPTY as Array<ResumableCollector<T>>)

    val producer = Resumable()

    val remainingCollectors = AtomicInteger(expectedCollectors)

    @Volatile
    var terminated : Throwable? = null

    override suspend fun emit(value: T) {
        awaitCollectors()
        for (collector in collectors.get()) {
            try {
                collector.next(value)
            } catch (ex: CancellationException) {
                remove(collector)
            }
        }
    }

    override suspend fun emitError(ex: Throwable) {
        //awaitCollectors()
        terminated = ex;
        for (collector in collectors.getAndSet(TERMINATED as Array<ResumableCollector<T>>)) {
            try {
                collector.error(ex)
            } catch (_: CancellationException) {
                // ignored at this point
            }
        }
    }

    override suspend fun complete() {
        //awaitCollectors()
        terminated = DONE
        for (collector in collectors.getAndSet(TERMINATED as Array<ResumableCollector<T>>)) {
            try {
                collector.complete()
            } catch (_: CancellationException) {
                // ignored at this point
            }
        }
    }

    override fun hasCollectors(): Boolean {
        return collectors.get().isNotEmpty()
    }

    override fun collectorCount(): Int {
        return collectors.get().size
    }

    private suspend fun awaitCollectors() {
        if (remainingCollectors.get() != 0) {
            producer.await()
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

    override suspend fun collectSafely(collector: FlowCollector<T>) {
        val rc = ResumableCollector<T>()
        if (add(rc)) {
            if (remainingCollectors.decrementAndGet() == 0) {
                producer.resume()
            }
            rc.drain(collector) { remove(it) }
        } else {
            val ex = terminated;
            if (ex != null && ex != DONE) {
                throw ex
            }
        }
    }

    companion object {
        val EMPTY = arrayOf<ResumableCollector<Any>>()

        val TERMINATED = arrayOf<ResumableCollector<Any>>()

        val DONE = Throwable("Subject completed")
    }
}