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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector

/**
 * Reduce the values within the parallel rails and
 * then reduce the rails to a single result value.
 */
@FlowPreview
internal class FlowParallelReduceSequential<T>(
        private val source: ParallelFlow<T>,
        private val combine: suspend (T, T) -> T
) : AbstractFlow<T>() {

    override suspend fun collectSafely(collector: FlowCollector<T>) {
        val n = source.parallelism

        val rails = Array(n) { ReducerCollector(combine) }

        source.collect(*rails)

        @Suppress("UNCHECKED_CAST")
        var accumulator : T = null as T
        var hasValue = false
        for (rail in rails) {
            if (!hasValue && rail.hasValue) {
                accumulator = rail.accumulator
                hasValue = true
            } else if (hasValue && rail.hasValue) {
                accumulator = combine(accumulator, rail.accumulator)
            }
        }

        if (hasValue) {
            collector.emit(accumulator)
        }
    }

    class ReducerCollector<T>(private val combine: suspend (T, T) -> T) : FlowCollector<T> {
        @Suppress("UNCHECKED_CAST")
        var accumulator : T = null as T
        var hasValue : Boolean = false

        override suspend fun emit(value: T) {
            if (hasValue) {
                accumulator = combine(accumulator, value)
            } else {
                hasValue = true
                accumulator = value
            }

        }

    }
}