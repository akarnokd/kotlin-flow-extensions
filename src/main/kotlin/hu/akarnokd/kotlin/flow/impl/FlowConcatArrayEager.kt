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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.collect
import java.util.concurrent.atomic.AtomicIntegerArray

@FlowPreview
class FlowConcatArrayEager<T>(private val sources: Array<out Flow<T>>) : AbstractFlow<T>() {

    @InternalCoroutinesApi
    override suspend fun collectSafely(collector: FlowCollector<T>) {
        val n = sources.size
        val queues = Array(n) { ConcurrentLinkedQueue<T>() }
        val done = AtomicIntegerArray(n)
        var index = 0;
        val reader = Resumable()

        coroutineScope {
            for (i in 0 until n) {
                val f = sources[i]
                val q = queues[i]
                val j = i
                launch {
                    try {
                        f.collect {
                            q.offer(it)
                            reader.resume()
                        }
                    } finally {
                        done.set(j, 1)
                        reader.resume()
                    }
                }
            }


            while (isActive && index < n) {
                val q = queues[index]
                val d = done.get(index) != 0

                if (d && q.isEmpty()) {
                    index++
                    continue
                }

                val v = q.poll()
                if (v != null) {
                    collector.emit(v)
                    continue;
                }

                reader.await()
            }
        }
    }
}