package hu.akarnokd.kotlin.flow

import hu.akarnokd.kotlin.flow.Resumable
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ResumableTest {

    @Test
    fun correctState() = runBlocking {
        val resumable = Resumable()

        resumable.resume()

        resumable.await()

        resumable.resume()

        resumable.await()

        resumable.resume()

        resumable.await()
    }
}