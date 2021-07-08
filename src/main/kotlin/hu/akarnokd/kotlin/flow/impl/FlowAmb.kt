/*
 * Copyright 2020 David Karnok
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

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select

@FlowPreview
internal class FlowAmb<T>(private val flows: Iterable<Flow<T>>) : AbstractFlow<T>() {
    @InternalCoroutinesApi
    override suspend fun collectSafely(collector: FlowCollector<T>) {
        coroutineScope {
            val channels = flows.map { it.produceIn(this@coroutineScope) }
            var selectedChannelIndex = 0
            select<Unit> {
                channels.forEachIndexed { index, channel ->
                    channel.onReceiveOrClosed { value ->
                        selectedChannelIndex = index
                        if (!value.isClosed) {
                            collector.emit(value.value)
                        }
                    }
                }
            }
            channels.forEachIndexed { index, channel ->
                if (index != selectedChannelIndex) channel.cancel()
            }
            collector.emitAll(channels[selectedChannelIndex])
        }
    }
}