package hu.akarnokd.kotlin.flow.impl

import hu.akarnokd.kotlin.flow.amb
import hu.akarnokd.kotlin.flow.assertResult
import hu.akarnokd.kotlin.flow.concatArrayEager
import hu.akarnokd.kotlin.flow.range
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

@FlowPreview
class FlowAmbIterableTest {
    @Test
    fun basic1() = runBlocking {
        amb(
                range(1, 5).onStart { delay(1000) },
                range(6, 5).onStart { delay(100) }
        )
        .assertResult(6, 7, 8, 9, 10)
    }

    @Test
    fun basic2() = runBlocking {
        amb(
                range(1, 5).onStart { delay(100) },
                range(6, 5).onStart { delay(1000) }
        )
                .assertResult(1, 2, 3, 4, 5)
    }

    @Test
    fun basic3() = runBlocking {
        val counter = AtomicInteger()
        amb(
                range(1, 5).onEach { delay(100) },
                range(6, 5)
                        .onEach {
                            delay(200)
                            counter.getAndIncrement()
                        }

        )
                .take(3)
                .assertResult(1, 2, 3)

        assertEquals(0, counter.get())
    }

}