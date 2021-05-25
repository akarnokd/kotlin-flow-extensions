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
import kotlinx.coroutines.flow.collect
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@FlowPreview
internal class FlowOnBackpressureDrop<T>(private val source: Flow<T>) : AbstractFlow<T>() {
    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    override suspend fun collectSafely(collector: FlowCollector<T>) {
        coroutineScope {
            val consumerReady = AtomicBoolean()
            val producerReady = Resumable()
            val value = AtomicReference<T>()
            val done = AtomicBoolean()
            val error = AtomicReference<Throwable>();

            launch {
                try {
                    source.collect {
                        if (consumerReady.get()) {
                            value.set(it);
                            consumerReady.set(false);
                            producerReady.resume();
                        }
                    }
                    done.set(true)
                } catch (ex: Throwable) {
                    error.set(ex)
                }
                producerReady.resume()
            }

            while (true) {
                consumerReady.set(true)
                producerReady.await()

                val d = done.get()
                val ex = error.get()
                val v = value.getAndSet(null)

                if (ex != null) {
                    throw ex;
                }
                if (d) {
                    break;
                }

                collector.emit(v)
            }
        }
    }
}