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

import org.junit.Test
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import org.junit.Assert.assertTrue
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.ArrayList
import kotlin.test.assertEquals

@FlowPreview
class BehaviorSubjectTest {

    @Test
    fun basicCreate() = runBlocking {
        withSingle {
            val subject = BehaviorSubject<Int>()

            val result = ArrayList<Int>()

            val job = launch(it.asCoroutineDispatcher()) {
                subject.collect {
                    delay(100)
                    result.add(it)
                }
            }

            while (!subject.hasCollectors()) {
                delay(1)
            }

            subject.emit(1)
            subject.emit(2)
            subject.emit(3)
            subject.emit(4)
            subject.emit(5)
            subject.complete()

            job.join()

            assertEquals(listOf(1, 2, 3, 4, 5), result)
        }
    }

    @Test
    fun lotsOfItems() = runBlocking {

        withSingle {
            val subject = BehaviorSubject<Int>()

            val n = 100_000

            val counter = AtomicInteger()

            val job = launch(it.asCoroutineDispatcher()) {
                subject.collect {
                    counter.lazySet(counter.get() + 1)
                }
            }

            while (!subject.hasCollectors()) {
                delay(1)
            }

            for (i in 1..n) {
                subject.emit(i)
            }
            subject.complete()

            job.join()

            assertEquals(n, counter.get())
        }
    }

    @Test
    fun error()  = runBlocking {
        withSingle {
            val subject = BehaviorSubject<Int>()

            val counter = AtomicInteger()
            val exc = AtomicReference<Throwable>()

            val job = launch(it.asCoroutineDispatcher()) {
                try {
                    subject.collect {
                        counter.lazySet(counter.get() + 1)
                    }
                } catch (ex: Throwable) {
                    exc.set(ex)
                }
            }

            while (!subject.hasCollectors()) {
                delay(1)
            }

            subject.emitError(IOException())

            job.join()

            assertEquals(0, counter.get())
            assertTrue("" + exc.get(), exc.get() is IOException)
        }
    }

    @Test
    fun multiConsumer() = runBlocking {
        withSingle {
            val subject = BehaviorSubject<Int>()

            val n = 10_000

            val counter1 = AtomicInteger()
            val counter2 = AtomicInteger()

            val job1 = launch(it.asCoroutineDispatcher()) {
                subject.collect {
                    counter1.lazySet(counter1.get() + 1)
                }
            }

            val job2 = launch(it.asCoroutineDispatcher()) {
                subject.collect {
                    counter2.lazySet(counter2.get() + 1)
                }
            }

            while (subject.collectorCount() != 2) {
                delay(1)
            }

            for (i in 1..n) {
                subject.emit(i)
            }
            subject.complete()

            job1.join()
            job2.join()

            assertEquals(n, counter1.get())
            assertEquals(n, counter2.get())
        }
    }

    @Test
    fun multiConsumerWithDelay() = runBlocking {
        withSingle {
            val subject = BehaviorSubject<Int>()

            val n = 10

            val counter1 = AtomicInteger()
            val counter2 = AtomicInteger()

            val job1 = launch(it.asCoroutineDispatcher()) {
                subject.collect {
                    counter1.lazySet(counter1.get() + 1)
                }
            }

            val job2 = launch(it.asCoroutineDispatcher()) {
                subject.collect {
                    delay(10)
                    counter2.lazySet(counter2.get() + 1)
                }
            }

            while (subject.collectorCount() != 2) {
                delay(1)
            }

            for (i in 1..n) {
                subject.emit(i)
            }
            subject.complete()

            job1.join()
            job2.join()

            assertEquals(n, counter1.get())
            assertEquals(n, counter2.get())
        }
    }

    @Test
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun multiConsumerTake() = runBlocking {
        withSingle {
            val subject = BehaviorSubject<Int>()

            val n = 10

            val counter1 = AtomicInteger()
            val counter2 = AtomicInteger()

            val job1 = launch(it.asCoroutineDispatcher()) {
                subject.collect {
                    counter1.lazySet(counter1.get() + 1)
                }
            }

            val job2 = launch(it.asCoroutineDispatcher()) {
                subject.take(n / 2)
                        .collect {
                            counter2.lazySet(counter2.get() + 1)
                        }
            }

            while (subject.collectorCount() != 2) {
                delay(1)
            }

            for (i in 1..n) {
                subject.emit(i)
            }
            subject.complete()

            job1.join()
            job2.join()

            assertEquals(n, counter1.get())
            assertEquals(n / 2, counter2.get())
        }
    }

    @Test
    fun alreadyCompleted()  = runBlocking {
        val subject = BehaviorSubject<Int>()
        subject.complete()

        val counter1 = AtomicInteger()

        subject.collect {
            counter1.lazySet(counter1.get() + 1)
        }

        assertEquals(0, counter1.get())
    }

    @Test
    fun alreadyErrored()  = runBlocking {
        val subject = BehaviorSubject<Int>()
        subject.emitError(IOException())

        val counter1 = AtomicInteger()

        try {
            subject.collect {
                counter1.lazySet(counter1.get() + 1)
            }
            counter1.lazySet(counter1.get() + 1)
        } catch (ex: IOException) {
            // expected
        }

        assertEquals(0, counter1.get())
    }

    @Test
    fun basicWithInitial() = runBlocking {
        withSingle {
            val subject = BehaviorSubject(0)

            val result = ArrayList<Int>()

            val job = launch(it.asCoroutineDispatcher()) {
                subject.collect {
                    delay(100)
                    result.add(it)
                }
            }

            while (!subject.hasCollectors()) {
                delay(1)
            }

            subject.emit(1)
            subject.emit(2)
            subject.emit(3)
            subject.emit(4)
            subject.emit(5)
            subject.complete()

            job.join()

            assertEquals(listOf(0, 1, 2, 3, 4, 5), result)
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun takeWithInitial() = runBlocking {
        withSingle {
            val subject = BehaviorSubject(0)

            val result = ArrayList<Int>()

            val job = launch(it.asCoroutineDispatcher()) {
                subject.take(1).collect {
                    delay(100)
                    result.add(it)
                }
            }

            while (!subject.hasCollectors()) {
                delay(1)
            }

            subject.emit(1)
            subject.emit(2)
            subject.emit(3)
            subject.emit(4)
            subject.emit(5)
            subject.complete()

            job.join()

            assertEquals(listOf(0), result)
        }
    }

    @Test
    fun nullInitial() = runBlocking {
        withSingle {
            val subject = BehaviorSubject<Any?>(null)

            val result = ArrayList<Any?>()

            val job = launch(it.asCoroutineDispatcher()) {
                subject.collect {
                    delay(100)
                    result.add(it)
                }
            }

            while (!subject.hasCollectors()) {
                delay(1)
            }

            subject.complete()

            job.join()

            assertEquals<Any?>(listOf(null), result)
        }
    }

    @Test(timeout = 1000)
    fun cancelledConsumer() = runBlocking {
        withSingle {
            val subject = BehaviorSubject<Int>()

            val expected = 3
            val n = 10

            val counter1 = AtomicInteger()

            val job1 = launch(it.asCoroutineDispatcher()) {
                subject.collect {
                    if (counter1.incrementAndGet() == expected) {
                        throw CancellationException();
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

    @Test(timeout = 1000)
    fun cancelledOneCollectorSecondCompletes() = runBlocking {
        withSingle {
            val subject = BehaviorSubject<Int>()

            val expected = 3
            val n = 10

            val counter1 = AtomicInteger()
            val counter2 = AtomicInteger()

            val job1 = launch(it.asCoroutineDispatcher()) {
                subject.collect {
                    if (counter1.incrementAndGet() == expected) {
                        throw CancellationException();
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