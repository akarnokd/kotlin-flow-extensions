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

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals

@FlowPreview
@ExperimentalCoroutinesApi
class ReplaySubjectSizeBoundTest {

    @Test
    fun basicOnline() = runBlocking {
        withSingle { exec ->

            val replay = ReplaySubject<Int>(10)

            val result = ArrayList<Int>()

            val job = launch(exec.asCoroutineDispatcher()) {
                replay.collect {
                    delay(100)
                    result.add(it)
                }
            }

            while (!replay.hasCollectors()) {
                delay(1)
            }

            replay.emit(1)
            replay.emit(2)
            replay.emit(3)
            replay.emit(4)
            replay.emit(5)
            replay.complete()

            job.join()

            assertEquals(listOf(1, 2, 3, 4, 5), result)
        }
    }

    @Test
    fun basicOffline() = runBlocking {
        val replay = ReplaySubject<Int>(10)

        replay.emit(1)
        replay.emit(2)
        replay.emit(3)
        replay.emit(4)
        replay.emit(5)
        replay.complete()

        val result = ArrayList<Int>()
        replay.collect {
            delay(100)
            result.add(it)
        }

        assertEquals(listOf(1, 2, 3, 4, 5), result)
    }

    @Test
    fun errorOnline() = runBlocking {
        withSingle { exec ->

            val replay = ReplaySubject<Int>(10)

            val result = ArrayList<Int>()
            val exc = AtomicReference<Throwable>()

            val job = launch(exec.asCoroutineDispatcher()) {
                try {
                    replay.collect {
                        delay(100)
                        result.add(it)
                    }
                } catch (ex: Throwable) {
                    exc.set(ex)
                }
            }

            while (!replay.hasCollectors()) {
                delay(1)
            }

            replay.emit(1)
            replay.emit(2)
            replay.emit(3)
            replay.emit(4)
            replay.emit(5)
            replay.emitError(IOException())

            job.join()

            assertEquals(listOf(1, 2, 3, 4, 5), result)

            assertTrue("" + exc.get(), exc.get() is IOException)
        }
    }

    @Test
    fun errorOffline() = runBlocking {
        val replay = ReplaySubject<Int>(10)

        val result = ArrayList<Int>()
        val exc = AtomicReference<Throwable>()

        replay.emit(1)
        replay.emit(2)
        replay.emit(3)
        replay.emit(4)
        replay.emit(5)
        replay.emitError(IOException())

        try {
            replay.collect {
                result.add(it)
            }
        } catch (ex: Throwable) {
            exc.set(ex)
        }

        assertEquals(listOf(1, 2, 3, 4, 5), result)

        assertTrue("" + exc.get(), exc.get() is IOException)
    }


    @Test
    fun takeOnline() = runBlocking {
        withSingle { exec ->

            val replay = ReplaySubject<Int>(10)

            val result = ArrayList<Int>()

            val job = launch(exec.asCoroutineDispatcher()) {
                replay.take(3).collect {
                    delay(100)
                    result.add(it)
                }
            }

            while (!replay.hasCollectors()) {
                delay(1)
            }

            replay.emit(1)
            replay.emit(2)
            replay.emit(3)
            replay.emit(4)
            replay.emit(5)
            replay.complete()

            job.join()

            assertEquals(listOf(1, 2, 3), result)
        }
    }

    @Test
    fun takeOffline() = runBlocking {
        val replay = ReplaySubject<Int>(10)

        replay.emit(1)
        replay.emit(2)
        replay.emit(3)
        replay.emit(4)
        replay.emit(5)
        replay.complete()

        val result = ArrayList<Int>()
        replay.take(3).collect {
            result.add(it)
        }

        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun boundedOnline() = runBlocking {
        withSingle { exec ->

            val replay = ReplaySubject<Int>(2)

            val result = ArrayList<Int>()

            val job = launch(exec.asCoroutineDispatcher()) {
                replay.collect {
                    delay(100)
                    result.add(it)
                }
            }

            while (!replay.hasCollectors()) {
                delay(1)
            }

            replay.emit(1)
            replay.emit(2)
            replay.emit(3)
            replay.emit(4)
            replay.emit(5)
            replay.complete()

            job.join()

            assertEquals(listOf(1, 2, 3, 4, 5), result)

            result.clear()

            replay.collect {
                result.add(it)
            }

            assertEquals(listOf(4, 5), result)
        }
    }

    @Test
    fun boundedOffline() = runBlocking {
        val replay = ReplaySubject<Int>(2)

        replay.emit(1)
        replay.emit(2)
        replay.emit(3)
        replay.emit(4)
        replay.emit(5)
        replay.complete()

        val result = ArrayList<Int>()
        replay.collect {
            result.add(it)
        }

        assertEquals(listOf(4, 5), result)
    }


    @Test
    fun multipleOnline() = runBlocking {
        withSingle { exec ->

            val replay = ReplaySubject<Int>(10)

            val result1 = ArrayList<Int>()

            val job1 = launch(exec.asCoroutineDispatcher()) {
                replay.collect {
                    delay(50)
                    result1.add(it)
                }
            }

            val result2 = ArrayList<Int>()

            val job2 = launch(exec.asCoroutineDispatcher()) {
                replay.collect {
                    delay(100)
                    result2.add(it)
                }
            }

            while (replay.collectorCount() != 2) {
                delay(1)
            }

            replay.emit(1)
            replay.emit(2)
            replay.emit(3)
            replay.emit(4)
            replay.emit(5)
            replay.complete()

            job1.join()
            job2.join()

            assertEquals(listOf(1, 2, 3, 4, 5), result1)
            assertEquals(listOf(1, 2, 3, 4, 5), result2)
        }
    }

    @Test
    fun multipleWithTakeOnline() = runBlocking {
        withSingle { exec ->

            val replay = ReplaySubject<Int>(10)

            val result1 = ArrayList<Int>()

            val job1 = launch(exec.asCoroutineDispatcher()) {
                replay.collect {
                    delay(50)
                    result1.add(it)
                }
            }

            val result2 = ArrayList<Int>()

            val job2 = launch(exec.asCoroutineDispatcher()) {
                replay.take(3).collect {
                    delay(50)
                    result2.add(it)
                }
            }

            while (replay.collectorCount() != 2) {
                delay(1)
            }

            replay.emit(1)
            replay.emit(2)
            replay.emit(3)
            replay.emit(4)
            replay.emit(5)
            replay.complete()

            job1.join()
            job2.join()

            assertEquals(listOf(1, 2, 3, 4, 5), result1)
            assertEquals(listOf(1, 2, 3), result2)
        }
    }

    @Test
    fun cancelledConsumer() = runBlocking {
        withSingle {
            val subject = ReplaySubject<Int>(20)

            val expected = 3
            val n = 10

            val counter1 = AtomicInteger()

            val job1 = launch(it.asCoroutineDispatcher()) {
                subject.collect {
                    if (counter1.incrementAndGet() == expected) {
                        cancel()
                    }
                }
            }

            while (!subject.hasCollectors()) {
                delay(1)
            }

            for (i in 1..n) {
                subject.emit(i)
            }

            // wait for the subject to finish
            for (i in 1..1000) {
                if (job1.isCancelled && subject.collectorCount() == 0) {
                    break;
                }
                delay(10)
            }

            assertEquals(true, job1.isCancelled)
            assertEquals(expected, counter1.get())
            assertEquals(0, subject.collectorCount())
        }

    }

    @Test
    fun cancelledOneCollectorSecondCompletes() = runBlocking {
        withSingle {
            val subject = ReplaySubject<Int>(20)

            val expected = 3
            val n = 10

            val counter1 = AtomicInteger()
            val counter2 = AtomicInteger()

            val job1 = launch(it.asCoroutineDispatcher()) {
                subject.collect {
                    if (counter1.incrementAndGet() == expected) {
                        cancel()
                    }
                }
            }

            val job2 = launch(it.asCoroutineDispatcher()) {
                subject.collect { counter2.incrementAndGet() }
            }

            while (subject.collectorCount() != 2) {
                delay(1)
            }

            for (i in 1..n) {
                subject.emit(i)
            }

            subject.complete()
            job2.join()

            assertEquals(true, job1.isCancelled)
            assertEquals(true, job2.isCompleted)
            assertEquals(expected, counter1.get())
            assertEquals(n, counter2.get())
            assertEquals(0, subject.collectorCount())
        }

    }
}