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
import kotlinx.coroutines.flow.FlowCollector

/**
 * Maps values in parallel.
 */
internal class FlowParallelMap<T, R>(
        private val source: ParallelFlow<T>,
        private val mapper: suspend (T) -> R
) : ParallelFlow<R> {

    override val parallelism: Int
        get() = source.parallelism

    override suspend fun collect(vararg collectors: FlowCollector<R>) {
        val n = parallelism

        val rails = Array(n) { i -> MapperCollector(collectors[i], mapper) }

        source.collect(*rails)
    }

    private class MapperCollector<T, R>(val collector: FlowCollector<R>, val mapper: suspend (T) -> R) : FlowCollector<T> {
        override suspend fun emit(value: T) {
            collector.emit(mapper(value))
        }
    }
}