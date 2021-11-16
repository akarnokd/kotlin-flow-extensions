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

import hu.akarnokd.kotlin.flow.ResumableCollector
import hu.akarnokd.kotlin.flow.SubjectAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Shares a single collector towards the upstream source and multicasts
 * values to any number of consumers which then can produce the output
 * flow of values.
 */
@FlowPreview
internal class FlowMulticastFunction<T, R>(
        private val source: Flow<T>,
        private val subject: () -> SubjectAPI<T>,
        private val transform: suspend (Flow<T>) -> Flow<R>
) : AbstractFlow<R>() {

    override suspend fun collectSafely(collector: FlowCollector<R>) {

        coroutineScope {
            val cancelled = AtomicBoolean()

            val subject = subject()

            val result = transform(subject)

            val inner = ResumableCollector<R>()

            launch {
                try {
                    result.onCompletion { cancelled.set(true) }
                            .collect {
                                inner.next(it)
                            }
                    inner.complete()
                } catch (ex: Throwable) {
                    inner.error(ex)
                }
            }

            launch {
                try {
                    source.collect {
                        if (cancelled.get()) {
                            throw CancellationException()
                        }
                        subject.emit(it)
                        if (cancelled.get()) {
                            throw CancellationException()
                        }
                    }
                    subject.complete()
                } catch (ex: Throwable) {
                    subject.emitError(ex)
                }
            }

            inner.drain(collector)
        }
    }
}