package hu.akarnokd.kotlin.flow.impl

import hu.akarnokd.kotlin.flow.assertResult
import hu.akarnokd.kotlin.flow.publish
import hu.akarnokd.kotlin.flow.startCollectOn
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Ignore
import org.junit.Test

@FlowPreview
class FlowStartCollectOnTest {
    @Test
    fun basic() = runBlocking {

        arrayOf(1, 2, 3, 4, 5)
                .asFlow()
                .startCollectOn(Dispatchers.IO)
                .assertResult(1, 2, 3, 4, 5)
    }
}