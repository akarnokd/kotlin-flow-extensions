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

import hu.akarnokd.kotlin.flow.ParallelFlow
import hu.akarnokd.kotlin.flow.Resumable
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Consumes the parallel flow and turns it into a sequential flow of values.
 */
@FlowPreview
internal class FlowSequential<T>(private val source: ParallelFlow<T>) : AbstractFlow<T>() {
    override suspend fun collectSafely(collector: FlowCollector<T>) {

        coroutineScope {
            val n = source.parallelism
            val resumeCollector = Resumable()
            val collectors = Array(n) { RailCollector<T>(resumeCollector) }

            val done = AtomicBoolean()
            val error = AtomicReference<Throwable>()

            launch {
                try {
                    source.collect(*collectors)

                    done.set(true)
                    resumeCollector.resume()
                } catch (ex: Throwable) {
                    error.set(ex)
                    done.set(true)
                    resumeCollector.resume()
                }
            }

            while (true) {

                val d = done.get()
                var empty = true

                for (rail in collectors) {
                    if (rail.hasValue) {
                        empty = false
                        val v = rail.value
                        @Suppress("UNCHECKED_CAST")
                        rail.value = null as T
                        rail.hasValue = false

                        try {
                            collector.emit(v)
                        } catch (ex: Throwable) {
                            for (r in collectors) {
                                r.error = ex
                                r.resume()
                            }
                            throw ex
                        }
                        rail.resume()

                        break
                    }
                }

                if (d && empty) {
                    val ex = error.get()
                    if (ex != null) {
                        throw ex
                    }
                    return@coroutineScope
                }
                if (empty) {
                    resumeCollector.await()
                }
            }
        }
    }

    class RailCollector<T>(private val resumeCollector: Resumable) : Resumable(), FlowCollector<T> {

        @Suppress("UNCHECKED_CAST")
        var value: T = null as T
        @Volatile
        var hasValue : Boolean = false

        @Volatile
        var error : Throwable? = null

        override suspend fun emit(value: T) {
            this.value = value
            hasValue = true
            resumeCollector.resume()

            await()

            val ex = error
            if (ex != null) {
                throw ex
            }
        }
    }
}