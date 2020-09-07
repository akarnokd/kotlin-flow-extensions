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

import hu.akarnokd.kotlin.flow.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit

@FlowPreview
class FlowMulticastFunctionTest {
    @Test
    fun publish() = runBlocking {
        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .publish {
                    shared ->
                        shared.filter { it % 2 == 0}
                }
                .assertResult(2, 4)
    }

    @Test(timeout = 5000)
    @Ignore("Doesn't work")
    fun publishMultipleConsumers() = runBlocking {
        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .publish {
                    shared ->
                    merge(shared.filter { it % 2 == 1 }, shared.filter { it % 2 == 0 })
                }
                .assertResult(1, 2, 3, 4, 5)
    }


    @Test(timeout = 5000)
    @Ignore("Doesn't work either")
    fun publishMultipleConsumersCustomMerge() = runBlocking {
        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .publish {
                    shared ->
                    mergeArray(shared.filter { it % 2 == 1 }, shared.filter { it % 2 == 0 })
                }
                .assertResult(1, 2, 3, 4, 5)
    }

    @Test(timeout = 5000)
    fun multicastMultipleConsumersCustomMerge() = runBlocking {
        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .publish(2) {
                    shared ->
                    mergeArray(shared.filter { it % 2 == 1 }, shared.filter { it % 2 == 0 })
                }
                .assertResult(1, 2, 3, 4, 5)
    }

    @Test(timeout = 5000)
    @Ignore("Doesn't work")
    fun replayMultipleConsumers() = runBlocking {
        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .replay {
                    shared ->
                    merge(shared.filter { it % 2 == 1 }, shared.filter { it % 2 == 0 })
                }
                .assertResult(1, 2, 3, 4, 5)
    }

    @Test
    fun replay() = runBlocking {
        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .replay {
                    shared ->
                    shared.filter { it % 2 == 0}
                }
                .assertResult(2, 4)
    }

    @Test
    fun replaySizeBound() = runBlocking {
        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .replay(2) {
                    shared ->
                        shared.filter { it % 2 == 0}
                                .concatWith(shared)

                }
                .assertResult(2, 4, 4, 5)
    }

    @Test
    fun replayTimeBound() = runBlocking {
        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .replay(1L, TimeUnit.MINUTES) {
                    shared ->
                    shared.filter { it % 2 == 0}
                            .concatWith(shared)

                }
                .assertResult(2, 4, 1, 2, 3, 4, 5)
    }

    @Test
    fun replaySizeAndTimeBound() = runBlocking {
        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .replay(2, 1L, TimeUnit.MINUTES) {
                    shared ->
                    shared.filter { it % 2 == 0}
                            .concatWith(shared)

                }
                .assertResult(2, 4, 4, 5)
    }

    @Test
    fun replaySizeAndTimeBoundCustomTime() = runBlocking {
        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .replay(2, 1L, TimeUnit.MINUTES, { 0L }) {
                    shared ->
                    shared.filter { it % 2 == 0}
                            .concatWith(shared)

                }
                .assertResult(2, 4, 4, 5)
    }
}