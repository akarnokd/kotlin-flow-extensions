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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consumes the main source until the other source emits an item or completes.
 */
@FlowPreview
internal class FlowTakeUntil<T, U>(
        private val source: Flow<T>,
        private val other: Flow<U>
) : AbstractFlow<T>() {
    override suspend fun collectSafely(collector: FlowCollector<T>) {
        coroutineScope {
            val gate = AtomicBoolean()

            val job = launch {
                try {
                    other.collect {
                        throw STOP
                    }
                } catch (ex: StopException) {
                    // Okay
                } finally {
                    gate.set(true)
                }
            }

            try {
                source.collect {
                    if (gate.get()) {
                        throw STOP
                    }
                    collector.emit(it)
                }
            } catch (ex: StopException) {
                // Okay
            } finally {
                job.cancel(STOP)
            }
        }
    }

    class StopException : CancellationException()

    companion object {
        val STOP = StopException()
    }
}