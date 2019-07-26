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

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.FlowCollector
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

@FlowPreview
internal class FlowStartCollectOn<T>(
        val source: Flow<T>,
        val coroutineScope: CoroutineScope,
        val coroutineDispatcher: CoroutineDispatcher) : AbstractFlow<T>() {

    private companion object {
        val CANCELLED = Object()
    }

    @InternalCoroutinesApi
    override suspend fun collectSafely(collector: FlowCollector<T>) {
        val cancel = AtomicReference<Any>()

        val job = coroutineScope.launch(coroutineDispatcher) {
            try {
                source.collect {
                    collector.emit(it)
                }
            } finally {
                (cancel.getAndSet(CANCELLED) as? Job)?.cancel()
            }
        }
        if (!cancel.compareAndSet(null, job)) {
            job.cancelAndJoin()
        } else {
            job.join()
        }
    }
}