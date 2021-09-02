package hu.akarnokd.kotlin.flow.impl

import hu.akarnokd.kotlin.flow.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

@FlowPreview
class FlowConcatMapEagerTest {
    @Test
    fun basic() = runBlocking {
        range(1, 5)
        .concatMapEager {
            range(it * 10, 5).onEach { delay(100) }
        }
        .assertResult(
                10, 11, 12, 13, 14,
                20, 21, 22, 23, 24,
                30, 31, 32, 33, 34,
                40, 41, 42, 43, 44,
                50, 51, 52, 53, 54
        )
    }

    @Test
    fun take() = runBlocking {
        range(1, 5)
                .concatMapEager {
                    range(it * 10, 5).onEach { delay(100) }
                }
                .take(7)
                .assertResult(
                        10, 11, 12, 13, 14,
                        20, 21
                )
    }

    @Test
    fun crashMapper() = runBlocking {
        range(1, 5)
        .concatMapEager<Int, Int> {
            throw IllegalArgumentException()
        }
        .assertFailure<Int, IllegalArgumentException>(IllegalArgumentException::class.java)
    }

    @Test
    fun crashInner() = runBlocking {
        range(1, 1)
                .concatMapEager<Int, Int> {
                    flow {
                        throw IllegalArgumentException()
                    }
                }
                .assertFailure<Int, IllegalArgumentException>(IllegalArgumentException::class.java)
    }
}