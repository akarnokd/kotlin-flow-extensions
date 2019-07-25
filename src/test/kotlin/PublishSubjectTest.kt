import org.junit.Test
import kotlinx.coroutines.flow.*
import hu.akarnokd.kotlin.flow.*
import kotlinx.coroutines.*
import org.junit.Assert.assertTrue
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.ArrayList
import kotlin.test.assertEquals

@FlowPreview
class PublishSubjectTest {

    @Test
    fun basicCreate() = runBlocking {
        withSingle {
            val subject = PublishSubject<Int>();

            val result = ArrayList<Int>();

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
            val subject = PublishSubject<Int>();

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
            val subject = PublishSubject<Int>();

            val counter = AtomicInteger()
            val exc = AtomicReference<Throwable>()

            val job = launch(it.asCoroutineDispatcher()) {
                try {
                    subject.collect {
                        counter.lazySet(counter.get() + 1)
                    }
                } catch (ex: Throwable) {
                    exc.set(ex);
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
            val subject = PublishSubject<Int>();

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
            val subject = PublishSubject<Int>();

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
            val subject = PublishSubject<Int>();

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
        val subject = PublishSubject<Int>();
        subject.complete()

        val counter1 = AtomicInteger()

        subject.collect {
            counter1.lazySet(counter1.get() + 1)
        }

        assertEquals(0, counter1.get())
    }

    @Test
    fun alreadyErrored()  = runBlocking {
        val subject = PublishSubject<Int>();
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
}