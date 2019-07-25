package hu.akarnokd.kotlin.flow

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals

@FlowPreview
@ExperimentalCoroutinesApi
class ReplaySubjectSizeAndTimeBoundTest {

    @Test
    fun basicOnline() = runBlocking {
        withSingle { exec ->

            val replay = ReplaySubject<Int>(10, 1, TimeUnit.MINUTES)

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
        val replay = ReplaySubject<Int>(10, 1, TimeUnit.MINUTES)

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
    fun timedOnline() = runBlocking {
        withSingle { exec ->

            val replay = ReplaySubject<Int>(1, TimeUnit.MINUTES)

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
    fun timedffline() = runBlocking {
        val replay = ReplaySubject<Int>(1, TimeUnit.MINUTES)

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

            val replay = ReplaySubject<Int>(10, 1, TimeUnit.MINUTES)

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
        val replay = ReplaySubject<Int>(10, 1, TimeUnit.MINUTES)

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

            val replay = ReplaySubject<Int>(10, 1, TimeUnit.MINUTES)

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
        val replay = ReplaySubject<Int>(10, 1, TimeUnit.MINUTES)

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

            val replay = ReplaySubject<Int>(2, 1, TimeUnit.MINUTES)

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
        val replay = ReplaySubject<Int>(2, 1, TimeUnit.MINUTES)

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
    fun timedOffline() = runBlocking {
        val replay = ReplaySubject<Int>(10, 100, TimeUnit.MILLISECONDS)

        replay.emit(1)
        replay.emit(2)
        replay.emit(3)
        replay.emit(4)
        replay.emit(5)
        replay.complete()

        delay(300)

        val result = ArrayList<Int>()
        replay.collect {
            result.add(it)
        }

        assertEquals(listOf<Int>(), result)
    }


    @Test
    fun timedOffline2() = runBlocking {
        val replay = ReplaySubject<Int>(10, 100, TimeUnit.MILLISECONDS)

        replay.emit(1)
        replay.emit(2)
        replay.emit(3)
        delay(300)

        replay.emit(4)
        replay.emit(5)
        replay.complete()

        val result = ArrayList<Int>()
        replay.collect {
            result.add(it)
        }

        assertEquals(listOf<Int>(4, 5), result)
    }

    @Test
    fun multipleOnline() = runBlocking {
        withSingle { exec ->

            val replay = ReplaySubject<Int>(10, 1, TimeUnit.MINUTES)

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

            val replay = ReplaySubject<Int>(10, 1, TimeUnit.MINUTES)

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
    }}