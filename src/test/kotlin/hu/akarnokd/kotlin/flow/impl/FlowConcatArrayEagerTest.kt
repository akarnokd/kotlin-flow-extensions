package hu.akarnokd.kotlin.flow.impl

import hu.akarnokd.kotlin.flow.assertResult
import hu.akarnokd.kotlin.flow.concatArrayEager
import hu.akarnokd.kotlin.flow.range
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

@FlowPreview
class FlowConcatArrayEagerTest {
    @Test
    fun basic() = runBlocking {
        val state1 = AtomicInteger()
        val state2 = AtomicInteger()
        concatArrayEager(
                range(1, 5).onStart {
                    delay(200)
                    state1.set(1)
                }.onEach { println(it) },
                range(6, 5).onStart {
                    state2.set(state1.get())
                }.onEach { println(it) },
        )
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

        assertEquals(0, state2.get())
    }

    @Test
    fun basic1() = runBlocking {
        concatArrayEager(
                range(1, 5)
        )
        .assertResult(1, 2, 3, 4, 5)

    }

    @Test
    fun take() = runBlocking {
        concatArrayEager(
                range(1, 5).onStart { delay(100) },
                range(6, 5)
        )
                .take(6)
                .assertResult(1, 2, 3, 4, 5, 6)
    }

    @Test
    fun cancel() = runBlocking() {
        var counter = AtomicInteger()
        concatArrayEager(
                range(1, 5).onEach {
                    delay(200)
                    counter.getAndIncrement()
                }
        )
        .take(3)
        .assertResult(1, 2, 3)

        delay(1200)
        assertEquals(3, counter.get())
    }
}