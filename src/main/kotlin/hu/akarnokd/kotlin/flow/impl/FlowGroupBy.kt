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

package hu.akarnokd.kotlin.flow.impl

import hu.akarnokd.kotlin.flow.GroupedFlow
import hu.akarnokd.kotlin.flow.Resumable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.FlowCollector
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Groups transformed values of the source flow based on a key selector
 * function.
 */
@FlowPreview
internal class FlowGroupBy<T, K, V>(
        private val source: Flow<T>,
        private val keySelector: suspend (T) -> K,
        private val valueSelector: suspend (T) -> V
) : AbstractFlow<GroupedFlow<K, V>>() {

    override suspend fun collectSafely(collector: FlowCollector<GroupedFlow<K, V>>) {
        val map = ConcurrentHashMap<K, FlowGroup<K, V>>()

        val mainStopped = AtomicBoolean()

        try {
            source.collect {
                val k = keySelector(it)

                var group = map[k]

                if (group != null) {
                    group.next(valueSelector(it))
                } else {
                    if (!mainStopped.get()) {
                        group = FlowGroup(k, map)
                        map.put(k, group)

                        try {
                            collector.emit(group)
                        } catch (ex: CancellationException) {
                            mainStopped.set(true)
                            if (map.size == 0) {
                                throw CancellationException()
                            }
                        }

                        group.next(valueSelector(it))
                    } else {
                        if (map.size == 0) {
                            throw CancellationException()
                        }
                    }
                }
            }
            for (group in map.values) {
                group.complete()
            }
        } catch (ex: Throwable) {
            for (group in map.values) {
                group.error(ex)
            }
            throw ex
        }
    }

    class FlowGroup<K, V>(
            override val key: K,
            private val map : ConcurrentMap<K, FlowGroup<K, V>>
    ) : AbstractFlow<V>(), GroupedFlow<K, V> {

        @Suppress("UNCHECKED_CAST")
        private var value: V = null as V
        @Volatile
        private var hasValue: Boolean = false

        private var error: Throwable? = null
        @Volatile
        private var done: Boolean = false

        @Volatile
        private var cancelled: Boolean = false

        private val consumerReady = Resumable()

        private val valueReady = Resumable()

        private val once = AtomicBoolean()

        override suspend fun collectSafely(collector: FlowCollector<V>) {
            if (!once.compareAndSet(false, true)) {
                throw IllegalStateException("A GroupedFlow can only be collected at most once.")
            }

            consumerReady.resume()

            while (true) {
                val d = done
                val has = hasValue

                if (d && !has) {
                    val ex = error
                    if (ex != null) {
                        throw ex
                    }
                    break
                }

                if (has) {
                    val v = value
                    @Suppress("UNCHECKED_CAST")
                    value = null as V
                    hasValue = false

                    try {
                        collector.emit(v)
                    } catch (ex: Throwable) {
                        map.remove(this.key)
                        cancelled = true
                        consumerReady.resume()
                        throw ex
                    }

                    consumerReady.resume()
                    continue
                }

                valueReady.await()
            }
        }

        suspend fun next(value: V) {
            if (!cancelled) {
                consumerReady.await()
                this.value = value
                this.hasValue = true
                valueReady.resume()
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
    }
}