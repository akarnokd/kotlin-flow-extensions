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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

@FlowPreview
class FlowConcatMapEager<T, R>(private val source: Flow<T>, private val mapper : suspend (t: T) -> Flow<R>) : AbstractFlow<R>() {
    override suspend fun collectSafely(collector: FlowCollector<R>) {
        coroutineScope {
            val resumeOutput = Resumable()
            val innerQueue = ConcurrentLinkedQueue<InnerQueue<R>>()
            val innerDone = AtomicBoolean()

            launch {
                try {
                    source.collect {
                        val f = mapper(it);
                        val iq = InnerQueue<R>()
                        innerQueue.offer(iq);
                        resumeOutput.resume()
                        launch {
                            try {
                                f.collect {
                                    iq.queue.offer(it)
                                    resumeOutput.resume()
                                }
                            } finally {
                                iq.done.set(true)
                                resumeOutput.resume()
                            }
                        }
                    }
                } finally {
                    innerDone.set(true)
                    resumeOutput.resume()
                }
            }

            var iq : InnerQueue<R>? = null

            while (isActive) {

                if (iq == null) {
                    val id = innerDone.get()
                    iq = innerQueue.poll()

                    if (id && iq == null) {
                        break
                    }
                }

                if (iq != null) {
                    val d = iq.done.get()
                    val v = iq.queue.poll()

                    if (d && v == null) {
                        iq = null
                        continue
                    }

                    if (v != null) {
                        collector.emit(v)
                        continue
                    }
                }
                resumeOutput.await()
            }
        }
    }

    class InnerQueue<R> {
        val queue = ConcurrentLinkedQueue<R>()
        val done = AtomicBoolean()
    }
}