package hu.akarnokd.kotlin.flow

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.IllegalStateException
import kotlin.test.assertNotNull

@FlowPreview
@ExperimentalCoroutinesApi
class FlowErrorExtensionTest {

    @Test
    fun flowShouldMapToNextCatchMultipleTimes() = runBlocking {

        var exception: Throwable? = null

        flow {
            repeat(3) {
                if (it == 2) {
                    throw IllegalArgumentException("error on emit")
                }
                emit(it)
            }
        }.mapToNextCatch {
            IllegalStateException("received ${it.message}")
        }.mapToNextCatch {
            MyFlowException("New throwable map ${it.message}")
        }.catch {
            exception = it
        }.collect {}

        assertNotNull(exception)
        assertTrue(exception is MyFlowException)
        assertEquals(exception?.message, "New throwable map received error on emit")
    }

    class MyFlowException(msg: String) : Throwable(msg)

}