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

package hu.akarnokd.kotlin.flow

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class BufferingResumableCollectorTest {

    @Test
    fun basicLong() = runBlocking {
        withSingle {
            val n = 10000

            val bc = BufferingResumableCollector<Int>(32)

            val job = launch(it.asCoroutineDispatcher()) {
                for (i in 0 until n) {
                    bc.next(i)
                }
                bc.complete()
            }

            val counter = AtomicInteger()

            bc.drain(object: FlowCollector<Int> {
                override suspend fun emit(value: Int) {
                    counter.getAndIncrement()
                }
            })

            job.join()

            assertEquals(n, counter.get())
        }
    }

    @Test
    fun basicLong1() = runBlocking {
        withSingle {
            val n = 10000

            val bc = BufferingResumableCollector<Int>(1)

            val job = launch(it.asCoroutineDispatcher()) {
                for (i in 0 until n) {
                    bc.next(i)
                }
                bc.complete()
            }

            val counter = AtomicInteger()

            bc.drain(object: FlowCollector<Int> {
                override suspend fun emit(value: Int) {
                    counter.getAndIncrement()
                }
            })

            job.join()

            assertEquals(n, counter.get())
        }
    }

    @Test
    fun basicLong2() = runBlocking {
        withSingle {
            val n = 100000

            val bc = BufferingResumableCollector<Int>(64)

            val job = launch(it.asCoroutineDispatcher()) {
                for (i in 0 until n) {
                    bc.next(i)
                }
                bc.complete()
            }

            val counter = AtomicInteger()

            bc.drain(object: FlowCollector<Int> {
                override suspend fun emit(value: Int) {
                    counter.getAndIncrement()
                }
            })

            job.join()

            assertEquals(n, counter.get())
        }
    }

    @Test
    fun basicLong3() = runBlocking {
        withSingle {
            val n = 1_000_000

            val bc = BufferingResumableCollector<Int>(256)

            val job = launch(it.asCoroutineDispatcher()) {
                for (i in 0 until n) {
                    bc.next(i)
                }
                bc.complete()
            }

            val counter = AtomicInteger()

            bc.drain(object: FlowCollector<Int> {
                override suspend fun emit(value: Int) {
                    counter.getAndIncrement()
                }
            })

            job.join()

            assertEquals(n, counter.get())
        }
    }
}