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
 * A subject implementation that dispatches signals to multiple
 * consumers or buffers them until such consumers arrive.
 *
 * @param <T> the element type of the [Flow]
 * @param bufferSize the number of items to buffer until consumers arrive
 */
@FlowPreview
class MulticastSubject<T>(private val bufferSize: Int = 32) : AbstractFlow<T>(), SubjectAPI<T> {

    val queue = ConcurrentLinkedQueue<T>()

    val availableQueue = AtomicInteger(bufferSize)

    val consumers = AtomicReference(EMPTY as Array<ResumableCollector<T>>)

    val producerAwait = Resumable()

    val wip = AtomicInteger()

    @Volatile
    var error: Throwable? = null

    override suspend fun emit(value: T) {
        while (availableQueue.get() == 0) {
            producerAwait.await()
        }
        queue.offer(value)
        availableQueue.decrementAndGet();
        drain()
    }

    override suspend fun emitError(ex: Throwable) {
        error = ex
        drain()
    }

    override suspend fun complete() {
        error = DONE
        drain()
    }

    override fun hasCollectors(): Boolean {
        return consumers.get().isNotEmpty();
    }

    override fun collectorCount(): Int {
        return consumers.get().size;
    }

    override suspend fun collectSafely(collector: FlowCollector<T>) {
        val c = ResumableCollector<T>()
        if (add(c)) {
            c.readyConsumer()
            drain()
            c.drain(collector) { remove(it) }
        } else {
            val ex = error
            if (ex != null && ex != DONE) {
                throw ex
            }
        }
    }

    private fun add(collector: ResumableCollector<T>) : Boolean {
        while (true) {
            val a = consumers.get()
            if (a == TERMINATED) {
                return false
            }
            val b = Array<ResumableCollector<T>>(a.size + 1) { idx ->
                if (idx < a.size) a[idx] else collector
            }
            if (consumers.compareAndSet(a, b)) {
                return true
            }
        }
    }
    private fun remove(collector: ResumableCollector<T>) {
        while (true) {
            val a = consumers.get()
            val n = a.size
            if (n == 0) {
                return
            }
            var j = -1
            for (i in 0 until n) {
                if (a[i] == collector) {
                    j = i
                    break
                }
            }
            if (j < 0) {
                return;
            }
            var b = EMPTY as Array<ResumableCollector<T>?>
            if (n != 1) {
                b = Array<ResumableCollector<T>?>(n - 1) { null }
                System.arraycopy(a, 0, b, 0, j)
                System.arraycopy(a, j + 1, b, j, n - j - 1)
            }
            if (consumers.compareAndSet(a, b as Array<ResumableCollector<T>>)) {
                return;
            }
        }
    }

    private suspend fun drain() {
        if (wip.getAndIncrement() != 0) {
            return;
        }

        while (true) {
            val collectors = consumers.get()
            if (collectors.isNotEmpty()) {
                val ex = error;
                val v = queue.poll();

                if (v == null && ex != null) {
                    finish(ex)
                }
                else if (v != null) {
                    var k = 0;
                    for (collector in consumers.get()) {
                        try {
                            //println("MulticastSubject -> [$k]: $v")
                            collector.next(v)
                        } catch (ex: CancellationException) {
                            remove(collector);
                        }
                        k++
                    }
                    availableQueue.getAndIncrement()
                    producerAwait.resume()
                    continue
                }
            } else {
                val ex = error;
                if (ex != null && queue.isEmpty()) {
                    finish(ex)
                }
            }
            if (wip.decrementAndGet() == 0) {
                break
            }
        }
    }

    private suspend fun finish(ex: Throwable) {
        if (ex == DONE) {
            for (collector in consumers.getAndSet(TERMINATED as Array<ResumableCollector<T>>)) {
                try {
                    collector.complete()
                } catch (_: CancellationException) {
                    // ignored
                }
            }
        } else {
            for (collector in consumers.getAndSet(TERMINATED as Array<ResumableCollector<T>>)) {
                try {
                    collector.error(ex)
                } catch (_: CancellationException) {
                    // ignored
                }
            }
        }
    }

    companion object {
        val DONE: Throwable = Throwable("Subject Completed")

        val EMPTY = arrayOf<ResumableCollector<Any>>();

        val TERMINATED = arrayOf<ResumableCollector<Any>>();
    }
}