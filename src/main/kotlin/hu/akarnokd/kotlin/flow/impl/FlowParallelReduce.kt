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

package hu.akarnokd.kotlin.flow.impl

import hu.akarnokd.kotlin.flow.ParallelFlow
import kotlinx.coroutines.flow.FlowCollector

/**
 * Reduces the source items into a single value on each rail
 * and emits those.
 */
internal class FlowParallelReduce<T, R>(
        private val source: ParallelFlow<T>,
        private val seed: suspend () -> R,
        private val combine: suspend (R, T) -> R
) : ParallelFlow<R> {
    override val parallelism: Int
        get() = source.parallelism

    override suspend fun collect(vararg collectors: FlowCollector<R>) {
        val n = parallelism

        val rails = Array(n) { ReducerCollector(combine) }

        // Array constructor doesn't support suspendable initializer?
        for (i in 0 until n) {
            rails[i].accumulator = seed()
        }

        source.collect(*rails)

        for (i in 0 until n) {
            collectors[i].emit(rails[i].accumulator)
        }
    }

    class ReducerCollector<T, R>(private val combine: suspend (R, T) -> R) : FlowCollector<T> {

        @Suppress("UNCHECKED_CAST")
        var accumulator : R = null as R

        override suspend fun emit(value: T) {
            accumulator = combine(accumulator, value)
        }
    }

}