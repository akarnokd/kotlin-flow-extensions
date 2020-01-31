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

import hu.akarnokd.kotlin.flow.Resumable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@FlowPreview
internal class FlowFlatMapDrop<T, R>(private val source: Flow<T>, private val mapper: suspend (T) -> Flow<R>) : AbstractFlow<R>() {
    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    override suspend fun collectSafely(collector: FlowCollector<R>) {
        coroutineScope {

            val resume = Resumable();
            val consumerReady = AtomicBoolean(true)
            val value = AtomicReference<T>()
            val hasValue = AtomicBoolean()
            val done = AtomicBoolean()
            val error = AtomicReference<Throwable>()

            val job = launch {
                try {
                    source.collect {
                        if (consumerReady.get()) {
                            consumerReady.set(false)
                            value.lazySet(it)
                            hasValue.set(true);
                            resume.resume()
                        }
                    }
                    done.set(true)
                } catch (ex: Throwable) {
                    error.set(ex)
                }
                resume.resume()
            }

            while (coroutineContext.isActive) {
                resume.await()

                val d = done.get()
                val e = error.get()
                val h = hasValue.get()
                val v = value.get()

                if (e != null && !h) {
                    throw e;
                }

                if (d && !h) {
                    break;
                }

                if (h) {
                    value.lazySet(null)
                    hasValue.set(false)
                    try {
                        mapper(v).collect {
                            collector.emit(it)
                        }
                    } catch (ex: Throwable) {
                        job.cancel()
                        throw ex
                    }
                }

                consumerReady.set(true);
            }
        }
    }
}