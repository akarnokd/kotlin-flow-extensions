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
 * Transform each upstream item into zero or more emits for the downstream
 * in parallel.
 */
internal class FlowParallelTransform<T, R>(
        private val source: ParallelFlow<T>,
        private val callback: suspend FlowCollector<R>.(T) -> Unit
) : ParallelFlow<R> {

    override val parallelism: Int
        get() = source.parallelism

    override suspend fun collect(vararg collectors: FlowCollector<R>) {
        val n = parallelism

        val rails = Array(n) { i -> OnEachCollector(collectors[i], callback) }

        source.collect(*rails)
    }

    class OnEachCollector<T, R>(val collector: FlowCollector<R>, val callback: suspend FlowCollector<R>.(T) -> Unit) : FlowCollector<T> {
        override suspend fun emit(value: T) {
            callback(collector, value)
        }
    }
}