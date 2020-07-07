package hu.akarnokd.kotlin.flow.impl

import hu.akarnokd.kotlin.flow.amb
import hu.akarnokd.kotlin.flow.ambWith
import hu.akarnokd.kotlin.flow.assertError
import hu.akarnokd.kotlin.flow.assertResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

@ExperimentalCoroutinesApi
@FlowPreview
class FlowAmbTest {
    @Test
    fun ambPlainFlows() = runBlocking {
        listOf(
                flowOf(1)
                        .onEach { println("Emitting $it") },
                flowOf(2)
                        .onEach { println("Emitting $it") }
        ).amb()
                .onCompletion { println("Done") }
                .assertResult(1)
        listOf(
                flowOf(1, 2)
                        .onEach { println("Emitting $it") },
                flowOf(3, 4)
                        .onEach { println("Emitting $it") }
        ).amb()
                .onCompletion { println("Done") }
                .assertResult(1, 2)
    }

    @Test
    fun ambWithPlainFlows() = runBlocking {
        flowOf(1)
                .onEach { println("Emitting $it") }
                .ambWith(
                        flowOf(2)
                                .onEach { println("Emitting $it") })
                .onCompletion { println("Done") }
                .assertResult(1)
        flowOf(1, 2)
                .onEach { println("Emitting $it") }
                .ambWith(
                        flowOf(3, 4)
                                .onEach { println("Emitting $it") })
                .onCompletion { println("Done") }
                .assertResult(1, 2)
    }

    @Test
    fun ambEmptyFlow() = runBlocking {
        listOf(
                emptyFlow(),
                flowOf(1)
                        .onEach { println("Emitting $it") },
                flowOf(2)
                        .onEach { println("Emitting $it") }
        ).amb()
                .onCompletion { println("Done") }
                .assertResult()
    }

    @Test
    fun ambWithEmptyFlow() = runBlocking {
        emptyFlow<Int>()
                .ambWith(flowOf(1)
                        .onEach { println("Emitting $it") })
                .ambWith(flowOf(2)
                        .onEach { println("Emitting $it") })
                .onCompletion { println("Done") }
                // due to suspend nature of value emitting, last  flow wins
                .assertResult(2)
        flowOf(1)
                .onEach { println("Emitting $it") }
                .ambWith(flowOf(2)
                        .onEach { println("Emitting $it") })
                .ambWith(emptyFlow<Int>())
                .onCompletion { println("Done") }
                // due to suspend nature of value emitting, last  flow wins
                .assertResult()
    }

    @Test
    fun ambFlowWithDelay() = runBlocking {
        listOf(
                flowOf(1, 2)
                        .onEach { println("Emitting before delay $it") }
                        .onEach { delay(100) }
                        .onEach { println("Emitting $it") },
                flowOf(3, 4)
                        .onEach { println("Emitting before delay $it") }
                        .onEach { delay(50) }
                        .onEach { println("Emitting $it") },
                flowOf(5, 6)
                        .onEach { println("Emitting before delay $it") }
                        .onEach { delay(70) }
                        .onEach { println("Emitting $it") }
        ).amb()
                .onCompletion { println("Done") }
                .assertResult(3, 4)
        listOf(
                flowOf(1, 2)
                        .onEach { println("Emitting before delay $it") }
                        .onEach { delay(100) }
                        .onEach { println("Emitting $it") },
                flow {
                    delay(50)
                },
                flowOf(5, 6)
                        .onEach { println("Emitting before delay $it") }
                        .onEach { delay(70) }
                        .onEach { println("Emitting $it") }
        ).amb()
                .onCompletion { println("Done") }
                .assertResult()
    }

    @Test
    fun ambWithFlowWithDelay() = runBlocking {
        flowOf(1, 2)
                .onEach { println("Emitting before delay $it") }
                .onEach { delay(100) }
                .onEach { println("Emitting $it") }
                .ambWith(flowOf(3, 4)
                        .onEach { println("Emitting before delay $it") }
                        .onEach { delay(50) }
                        .onEach { println("Emitting $it") })
                .ambWith(flowOf(5, 6)
                        .onEach { println("Emitting before delay $it") }
                        .onEach { delay(70) }
                        .onEach { println("Emitting $it") })
                .onCompletion { println("Done") }
                .assertResult(3, 4)
        flowOf(1, 2)
                .onEach { println("Emitting before delay $it") }
                .onEach { delay(100) }
                .onEach { println("Emitting $it") }
                .ambWith(flow {
                    delay(50)
                })
                .ambWith(flowOf(5, 6)
                        .onEach { println("Emitting before delay $it") }
                        .onEach { delay(70) }
                        .onEach { println("Emitting $it") })
                .onCompletion { println("Done") }
                .assertResult()
    }

    @Test
    fun ambError() = runBlocking {
        listOf(
                flowOf(1, 2)
                        .onEach { println("Emitting before delay $it") }
                        .onEach { delay(100) }
                        .onEach { println("Emitting $it") },
                flowOf(3, 4)
                        .onEach { println("Emitting before delay $it") }
                        .onEach { delay(50) }
                        .onEach { throw ArithmeticException() },
                flowOf(5, 6)
                        .onEach { println("Emitting before delay $it") }
                        .onEach { delay(70) }
                        .onEach { println("Emitting $it") }
        ).amb()
                .onCompletion { t -> println("Done: $t") }
                .assertError(ArithmeticException::class.java)
        listOf(
                flowOf(1, 2)
                        .onEach { println("Emitting before delay $it") }
                        .onEach {
                            println("Throwing exception")
                            throw ArithmeticException("first")
                        }
                        .onEach { delay(100) }
                        .onEach { println("Emitting $it") },
                flowOf(3, 4)
                        .onEach { println("Emitting before delay $it") }
                        .onEach { delay(50) }
                        .onEach { println("Emitting $it") },
                flowOf(5, 6)
                        .onEach { println("Emitting before delay $it") }
                        .onEach {
                            println("Throwing exception")
                            throw ArithmeticException("second")
                        }
                        .onEach { delay(70) }
                        .onEach { println("Emitting $it") }
        ).amb()
                .onCompletion { t -> println("Done: $t") }
                .assertError(ArithmeticException::class.java)
    }

    @Test
    fun ambWithError() = runBlocking {
        flowOf(1, 2)
                .onEach { println("Emitting before delay $it") }
                .onEach { delay(100) }
                .onEach { println("Emitting $it") }
                .ambWith(flowOf(3, 4)
                        .onEach { println("Emitting before delay $it") }
                        .onEach { delay(50) }
                        .onEach { throw ArithmeticException() })
                .ambWith(flowOf(5, 6)
                        .onEach { println("Emitting before delay $it") }
                        .onEach { delay(70) }
                        .onEach { println("Emitting $it") })
                .onCompletion { t -> println("Done: $t") }
                .assertError(ArithmeticException::class.java)
        flowOf(1, 2)
                .onEach { println("Emitting before delay $it") }
                .onEach {
                    println("Throwing exception")
                    throw ArithmeticException("first")
                }
                .onEach { delay(100) }
                .onEach { println("Emitting $it") }
                .ambWith(flowOf(3, 4)
                        .onEach { println("Emitting before delay $it") }
                        .onEach { delay(50) }
                        .onEach { println("Emitting $it") })
                .ambWith(flowOf(5, 6)
                        .onEach { println("Emitting before delay $it") }
                        .onEach {
                            println("Throwing exception")
                            throw ArithmeticException("second")
                        }
                        .onEach { delay(70) }
                        .onEach { println("Emitting $it") })
                .onCompletion { t -> println("Done: $t") }
                .assertError(ArithmeticException::class.java)
    }
}