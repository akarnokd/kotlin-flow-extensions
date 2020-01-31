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
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.isActive
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext

/**
 * Caches and replays some or all items to collectors.
 */
@FlowPreview
class ReplaySubject<T> : AbstractFlow<T>, SubjectAPI<T> {

    private val buffer: Buffer<T>

    private companion object {
        private val EMPTY = arrayOf<InnerCollector<Any>>()
        private val TERMINATED = arrayOf<InnerCollector<Any>>()
    }

    @Suppress("UNCHECKED_CAST")
    private val collectors = AtomicReference(EMPTY as Array<InnerCollector<T>>)

    private var done: Boolean = false

    /**
     * Creates a ReplaySubject with an unbounded internal buffer
     * caching all values received via [emit].
     */
    constructor() {
        buffer = UnboundedReplayBuffer()
    }

    /**
     * Creates a ReplaySubject that caches at most  [maxSize]
     * values to be replayed to late collectors.
     */
    constructor(maxSize: Int) {
        buffer = SizeBoundReplayBuffer(maxSize)
    }

    /**
     * Creates a ReplaySubject that caches values at most for
     * the given [maxTime] real-time duration and replays
     * those upfront to late collectors.
     */
    constructor(maxTime: Long, unit: TimeUnit) : this(Int.MAX_VALUE, maxTime, unit)

    /**
     * Creates a ReplaySubject that caches at most [maxSize] values
     * at most for the given [maxTime] real-time duration and
     * replays those upfront to late collectors.
     */
    constructor(maxSize: Int, maxTime: Long, unit: TimeUnit) : this(maxSize, maxTime, unit, {
        t -> t.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
    })

    /**
     * Creates a ReplaySubject that caches at most [maxSize] values
     * at most for the given [maxTime] duration (measured via the custom
     * [timeSource]) and replays those upfront to late collectors.
     */
    constructor(maxSize: Int, maxTime: Long, unit: TimeUnit, timeSource: (TimeUnit) -> Long) {
        buffer = TimeAndSizeBoundReplayBuffer(maxSize, maxTime, unit, timeSource)
    }

    /**
     * Accepts a [collector] and emits the cached values upfront
     * and any subsequent value received by this ReplaySubject until
     * the ReplaySubject gets terminated.
     */
    @FlowPreview
    override suspend fun collectSafely(collector: FlowCollector<T>) {
        val inner = InnerCollector(collector, this)
        add(inner)
        buffer.replay(inner)
    }

    /**
     * Emit a value to all current collectors when they are ready.
     */
    override suspend fun emit(value: T) {
        if (!done) {
            buffer.emit(value)

            for (collector in collectors.get()) {
                collector.resume()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun emitError(ex: Throwable) {
        if (!done) {
            done = true
            buffer.error(ex)
            for (collector in collectors.getAndSet(TERMINATED as Array<InnerCollector<T>>)) {
                collector.resume()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun complete() {
        if (!done) {
            done = true
            buffer.complete()
            for (collector in collectors.getAndSet(TERMINATED as Array<InnerCollector<T>>)) {
                collector.resume()
            }
        }
    }

    /**
     * Returns true if this PublishSubject has any collectors.
     */
    override fun hasCollectors() : Boolean = collectors.get().isNotEmpty()

    /**
     * Returns the current number of collectors.
     */
    override fun collectorCount() : Int = collectors.get().size

    @Suppress("UNCHECKED_CAST", "")
    private fun add(inner: InnerCollector<T>) : Boolean {
        while (true) {

            val a = collectors.get()
            if (a as Any == TERMINATED as Any) {
                return false
            }
            val n = a.size
            val b = a.copyOf(n + 1)
            b[n] = inner
            if (collectors.compareAndSet(a, b as Array<InnerCollector<T>>)) {
                return true
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun remove(inner: InnerCollector<T>) {
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

            var b = EMPTY as Array<InnerCollector<T>?>
            if (n != 1) {
                b = Array(n - 1) { null }
                System.arraycopy(a, 0, b, 0, j)
                System.arraycopy(a, j + 1, b, j, n - j - 1)
            }
            if (collectors.compareAndSet(a, b as Array<InnerCollector<T>>)) {
                return
            }
        }
    }

    private interface Buffer<T> {

        fun emit(value: T)

        fun error(ex: Throwable)

        fun complete()

        suspend fun replay(consumer: InnerCollector<T>)
    }

    private class InnerCollector<T>(val consumer: FlowCollector<T>, val parent: ReplaySubject<T>) : Resumable() {

        var index: Long = 0L

        var node: Any? = null
    }

    private class UnboundedReplayBuffer<T> : Buffer<T> {

        @Volatile
        private var size: Long = 0

        private val list : ArrayList<T> = ArrayList()

        @Volatile
        private var done: Boolean = false
        private var error: Throwable? = null

        override fun emit(value: T) {
            list.add(value)
            size += 1
        }

        override fun error(ex: Throwable) {
            error = ex
            done = true
        }

        override fun complete() {
            done = true
        }

        override suspend fun replay(consumer: InnerCollector<T>) {
            while (true) {

                val d = done
                val empty = consumer.index == size

                if (d && empty) {
                    val ex = error
                    if (ex != null) {
                        throw ex
                    }
                    return
                }

                if (!empty) {
                    try {
                        if (coroutineContext.isActive) {
                            consumer.consumer.emit(list[consumer.index.toInt()])
                            consumer.index++
                        } else {
                            throw CancellationException()
                        }
                    } catch (ex: Throwable) {
                        consumer.parent.remove(consumer)

                        throw ex
                    }
                    continue
                }

                consumer.await()
            }
        }
    }

    private class SizeBoundReplayBuffer<T>(private val maxSize: Int) : Buffer<T> {

        private var size: Int = 0

        @Volatile
        private var done: Boolean = false
        private var error: Throwable? = null

        @Volatile
        private var head : Node<T>

        private var tail : Node<T>

        init {
            val h = Node<T>(null)
            tail = h
            head = h
        }

        override fun emit(value: T) {
            val next = Node<T>(value)
            tail.set(next)
            tail = next

            if (size == maxSize) {
                head = head.get()
            } else {
                size++
            }
        }

        override fun error(ex: Throwable) {
            error = ex
            done = true
        }

        override fun complete() {
            done = true
        }

        override suspend fun replay(consumer: InnerCollector<T>) {
            while (true) {
                val d = done
                @Suppress("UNCHECKED_CAST")
                var index = consumer.node as? Node<T>
                if (index == null) {
                    index = head
                    consumer.node = index
                }
                val next = index.get()
                val empty = next == null

                if (d && empty) {
                    val ex = error
                    if (ex != null) {
                        throw ex
                    }
                    return
                }

                if (!empty) {
                    try {
                        if (coroutineContext.isActive) {
                            consumer.consumer.emit(next.value!!)
                            consumer.node = next
                        } else {
                            throw CancellationException()
                        }
                    } catch (ex: Throwable) {
                        consumer.parent.remove(consumer)

                        throw ex
                    }
                    continue
                }

                consumer.await()
            }
        }

        private class Node<T>(val value: T?) : AtomicReference<Node<T>>()
    }

    private class TimeAndSizeBoundReplayBuffer<T>(
            private val maxSize: Int,
            private val maxTime: Long,
            private val unit: TimeUnit,
            private val timeSource: (TimeUnit) -> Long
    ) : Buffer<T> {

        private var size: Int = 0

        @Volatile
        private var done: Boolean = false
        private var error: Throwable? = null

        @Volatile
        private var head : Node<T>

        private var tail : Node<T>

        init {
            val h = Node<T>(null, 0L)
            tail = h
            head = h
        }

        override fun emit(value: T) {
            val now = timeSource(unit)
            val next = Node<T>(value, now)
            tail.set(next)
            tail = next

            if (size == maxSize) {
                head = head.get()
            } else {
                size++
            }

            trimTime(now)
        }

        fun trimTime(now: Long) {
            val limit = now - maxTime
            var h = head

            while (true) {
                val next = h.get()
                if (next != null && next.timestamp <= limit) {
                    h = next
                    size--
                } else {
                    break
                }
            }
            head = h
        }

        override fun error(ex: Throwable) {
            error = ex
            done = true
        }

        override fun complete() {
            done = true
        }

        fun findHead() : Node<T> {
            val limit = timeSource(unit) - maxTime
            var h = head

            while (true) {
                val next = h.get()
                if (next != null && next.timestamp <= limit) {
                    h = next
                } else {
                    break
                }
            }
            return h
        }

        override suspend fun replay(consumer: InnerCollector<T>) {
            while (true) {
                val d = done
                @Suppress("UNCHECKED_CAST")
                var index = consumer.node as? Node<T>
                if (index == null) {
                    index = findHead()
                    consumer.node = index
                }
                val next = index.get()
                val empty = next == null

                if (d && empty) {
                    val ex = error
                    if (ex != null) {
                        throw ex
                    }
                    return
                }

                if (!empty) {
                    try {
                        if (coroutineContext.isActive) {
                            consumer.consumer.emit(next.value!!)
                            consumer.node = next
                        } else {
                            throw CancellationException()
                        }
                    } catch (ex: Throwable) {
                        consumer.parent.remove(consumer)

                        throw ex
                    }
                    continue
                }

                consumer.await()
            }
        }

        private class Node<T>(val value: T?, val timestamp: Long) : AtomicReference<Node<T>>()
    }
}