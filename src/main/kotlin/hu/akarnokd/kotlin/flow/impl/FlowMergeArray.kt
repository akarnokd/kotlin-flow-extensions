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

import hu.akarnokd.kotlin.flow.Resumable
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Merges an array of Flow instances in an unbounded manner.
 * @param <T> the shared element type
 * @param prefetch the number of items to allow through from each source before suspending
 */
@FlowPreview
class FlowMergeArray<T>(private val sources: Array<out Flow<T>>) : AbstractFlow<T>() {

    @InternalCoroutinesApi
    override suspend fun collectSafely(collector: FlowCollector<T>) {
        val queue = ConcurrentLinkedQueue<T>()
        val done = AtomicInteger(sources.size)
        val ready = Resumable()

        coroutineScope {
            for (source in sources) {
                launch {
                    try {
                        source.collect {
                            queue.offer(it)
                            ready.resume()
                        }
                    } finally {
                        done.decrementAndGet()
                        ready.resume()
                    }
                }
            }

            while (true) {
                val isDone = done.get() == 0
                val v = queue.poll()

                if (isDone && v == null) {
                    break;
                }
                else if (v != null) {
                    collector.emit(v)
                    continue;
                }
                ready.await()
            }
        }
    }
}